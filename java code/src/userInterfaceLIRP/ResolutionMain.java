package userInterfaceLIRP;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import ilog.concert.IloException;
import instanceManager.Instance;
import instanceManager.Parameters;
import solverLIRP.RouteManager;
import solverLIRP.Solution;
import solverLIRP.Solver;

public class ResolutionMain {
	/**
	 * @param args
	 * @throws IOException
	 * @throws IloException
	 */

	public static int CMAX = 4;     // max number of clients on a route
	public static int LMAX = 50;   // max length of a route
	private static boolean create_inst = false;
	private static int[] nb_clients = {5, 10, 20};
	private static double[] capa_vehicles = {50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50};
	private static double[] oc_depots = {5, 5, 5};
	private static double[] cities_sizes = {3, 6, 9, 12, 18};
	private static double[] proba_sizes = {0.25, 0.3, 0.2, 0.18, 0.07};

	public static void main(String[] args) throws IOException, IloException {

		String instDir = "../Instances/Complete/";
		if (create_inst) {
			for(int nbClients : nb_clients) {
				/* Create instances with 0 or 2 cities */
				for(int nbCities = 0; nbCities < 3; nbCities += 2) {
					/* Create 5 instances of each type */
					for(int i = 0; i < 5; i++) {
						Instance inst = new Instance(Parameters.grid_size, 10, 3, 100, oc_depots, nbClients, selectCitiesSizes(nbCities), 0.75, 1.5, 0, true, 0, 10, capa_vehicles);
						inst.writeToJSONFile(instDir + "lirp" + nbClients + "cl" + nbCities + "ci_" + i + ".json");
					}
				}
			}
		}

		/* Solve all instances in the directory */
		File listSol = new File(instDir);

		//------------------------------------------------------------------------------------
		// PARAMETERS
		/* Number of routes in each subset when separating */

		int splitParam = 100; // Size of the subsets of routes
		// splitParam = 0; // Uncomment to solve the original instance without sampling the routes

		try {
			//IN THE DIRECTORY FOR EVERY FILE, YOU SOLVE AND SAVE
			for (String fileName : listSol.list() ) {
				int i = fileName.lastIndexOf('.');
				/* One instance is too big for the solver, we exclude it from the resolution */
				if (!fileName.startsWith("lirp20cl2ci_2") && i > 0 &&  i < fileName.length() - 1) {
					String extension = fileName.substring(i+1).toLowerCase();
					if(extension!= null && extension.equals("json")) {
						// Create the instance from the json file
						Instance instLIRP = new Instance(instDir + fileName);
						//						System.out.println("Capa clients: ");						
						//						for (int cIter = 0; cIter < instLIRP.getNbClients(); cIter++) {
						//							System.out.println(instLIRP.getClient(cIter).getCapacity());
						//						}
						//						System.out.println("Capa depots: ");						
						//						for (int dIter = 0; dIter < instLIRP.getNbDepots(); dIter++) {
						//							System.out.println(instLIRP.getDepot(dIter).getCapacity());
						//						}
						System.out.print("Solving instance " + fileName + "...");

						// Create the log file and solution file to store the results and the trace of the program
						String fichierLog = "../Log files/" + fileName.replace(".json", ".log");
						String fichierSol = "../Solutions/" + fileName.replace(".json", ".sol");
						String fileSplitSol = "../Solutions/" + fileName.replace(".json", "_split"+splitParam+".sol");

						File fileLog = new File(fichierLog);
						PrintStream printStreamLog = new PrintStream(fileLog);
						// Outputs out and err are redirected to the log file
						PrintStream original = System.out;
						System.setOut(printStreamLog);
						System.setErr(printStreamLog);
						File fileSol = new File(fichierSol);
						// Stream for the solution
						PrintStream printStreamSol = new PrintStream(fileSol);
						PrintStream printStreamSplitSol = new PrintStream(fileSplitSol);


						System.out.println("Creating the RouteManager...");
						RouteManager rm = new RouteManager(instLIRP);
						if(rm.allReachable()) {
							System.out.println("OK. Solving...");
							ArrayList<Integer> loopIndicesDC = new ArrayList<Integer>();
							for(int index = 0; index < rm.getNbLoopDC(); index++)
								loopIndicesDC.add(index);

							/* =======================================================
							 *  If we are solving the problem using cplex directly,
							 *  set the split parameter to the total number of routes 
							 * =======================================================*/
							if(splitParam < 1) {
								splitParam = loopIndicesDC.size();
							}

							long startChrono = System.currentTimeMillis();
							/* =======================================================
							 *  Apply the route sampling algorithm: If the split 
							 *  parameter is big enough, the set of routes will not be 
							 *  split
							 * =======================================================*/
							ArrayList<ArrayList<Integer>> rSample = sampleRoutes(loopIndicesDC, splitParam);
							System.out.println("========================================");
							System.out.println("== Solving with 	" + rSample.size() + " subsetsof routes ==" );
							System.out.println("========================================");

							while(rSample.size() > 1) {
								ArrayList<Integer> collectedRoutes = new ArrayList<Integer>();
								for(ArrayList<Integer> subsetRoutes : rSample) {
									Solver solverLIRP = new Solver(instLIRP, rm, new ArrayList<Integer> (), subsetRoutes);
									Solution partialSol = solverLIRP.getSolution(printStreamSol);
									if (partialSol != null){
										partialSol.print(printStreamSol);
										for(int collectedIndex : partialSol.collectRoutes())		
											collectedRoutes.add(subsetRoutes.get(collectedIndex));
									}
								}
								rSample = sampleRoutes(collectedRoutes, splitParam);
							}

							loopIndicesDC = rSample.get(0);
							Solver solverLIRP = new Solver(instLIRP, rm, new ArrayList<Integer> (), loopIndicesDC);
							System.out.println("done!");

							// Call the method from the solver
							Solution sol;
							if(splitParam < 1) {
								sol = solverLIRP.getSolution(printStreamSol);
							}
							else {
								sol = solverLIRP.getSolution(printStreamSplitSol);
							}
							
							long stopChrono = System.currentTimeMillis();
							long duration = (stopChrono-startChrono);
							System.out.println("Time to solve the instance: " + duration + " milliseconds");

							System.out.println("================================");
							System.out.println();

							if (sol != null){
								if(splitParam < 1) {
									sol.print(printStreamSol);
									printStreamSol.println("Resolution time : " + duration + " milliseconds");
								}
								else {
									sol.print(printStreamSplitSol);
									printStreamSplitSol.println("Resolution time : " + duration + " milliseconds");
								}
							}
							else {
								System.out.println("Error on this instance");
							}

							System.setOut(original);
							System.out.println("Instance solved.");
						}
						else {
							System.out.println("At least one client is not reachable. Moving on.");
							printStreamSol.println("At least one client is not reachable. Moving on.");
							printStreamSplitSol.println("At least one client is not reachable. Moving on.");
							System.setOut(original);
							System.out.println("At least one client is not reachable. Moving on.");
						}
					}
				}
			}
			System.out.println("All Instances solved. FINISHED :-)");
		}

		catch (IOException ioe) {
			System.out.println("Error: " + ioe.getMessage());
		}
	}

	/**
	 * 
	 * @param loopIndices	the indices of routes from which we form the subsets
	 * @param subsetSizes	the desired size for the subsets produced
	 * @return				a double-entry containing the indices of the routes contained in each subset
	 */
	private static ArrayList<ArrayList<Integer>> sampleRoutes(ArrayList<Integer> loopIndices, int subsetSizes){	
		int nbSubsets = (int) Math.ceil(((double) loopIndices.size())/subsetSizes);
		ArrayList<ArrayList<Integer>> rSubsets = new ArrayList<ArrayList<Integer>>();
		
		/* If there is only one subset, return directly the set of indices taken as input */
		if(nbSubsets < 2) {
			rSubsets.add(loopIndices);
			return rSubsets;
		}

		/* Shuffle code */
		int[] permutDC = new int[nbSubsets * subsetSizes];
		/* Shuffle the array at random */
		Random rnd = ThreadLocalRandom.current();
		/* Fill an array with the indices of possible routes */
		for(int loopIndex = 0; loopIndex < loopIndices.size(); loopIndex++)
			permutDC[loopIndex] = loopIndex;
		for (int i = loopIndices.size(); i > 0; i--) {
			int index = rnd.nextInt(i);
			int permutVal = permutDC[index];
			permutDC[index] = permutDC[i - 1];
			permutDC[i - 1] = permutVal;
		}

		/* Complete the remaining indices at the end */
		/* Define a range from which we draw the additional values */
		int rangeSelect = subsetSizes;
		/* Define the first index for which we do not have a route index */
		int missingIndex = nbSubsets * subsetSizes;
		while(missingIndex > loopIndices.size()) {
			/* Select a complete subset at random */
			int subsetRand = rnd.nextInt(nbSubsets - 1);
			/* Select the index of the element to copy from this subset at random */
			int swapIndex = rnd.nextInt(rangeSelect);
			
			/* Select an element to copy in the subset subsetRand at position swapIndex */
			int copyID = permutDC[subsetRand * subsetSizes + swapIndex];
			
			/* Swap the element to copy with the last one available in the selected subset 
			 * (this ensures it will not be selected in a subsequent iteration) */
			permutDC[subsetRand * subsetSizes + swapIndex] = permutDC[subsetRand * subsetSizes + rangeSelect - 1];
			permutDC[subsetRand * subsetSizes + rangeSelect - 1] = copyID;
			
			/* Add the item to copy t of the missing indices */
			permutDC[missingIndex - 1] = copyID;
			
			/* Increment the iterators */
			rangeSelect--;
			missingIndex--;
		}

		/* Copy the permutation to the result array */
		for(int subsetIndex = 0; subsetIndex < nbSubsets; subsetIndex++) {
			ArrayList<Integer> currentSubset = new ArrayList<Integer>();
			for(int idIter = 0; idIter < subsetSizes; idIter++) {
				currentSubset.add(permutDC[subsetIndex * subsetSizes + idIter]);
			}
			rSubsets.add(currentSubset);
		}

		return rSubsets;
	}

	private static double[] selectCitiesSizes(int nbCities) {
		// Array to store the different cities sizes
		double[] citiesSizes = new double[nbCities];

		// Loop through the different cities to select their sizes
		for(int cIndex = 0; cIndex < citiesSizes.length; cIndex++) {
			int sizeIndex = 0;
			double cdf = proba_sizes[0];
			// Draw a random number
			double proba = Parameters.rand.nextDouble();
			// Determine to which size it corresponds
			while(proba > cdf) {
				sizeIndex++;
				cdf += proba_sizes[sizeIndex];
			}
			// Set its size accordingly
			citiesSizes[cIndex] = cities_sizes[sizeIndex];
		}
		return citiesSizes;
	}
}

