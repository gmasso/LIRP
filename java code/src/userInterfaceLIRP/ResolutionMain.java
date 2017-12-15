package userInterfaceLIRP;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

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

		try {
			//IN THE DIRECTORY FOR EVERY FILE, YOU SOLVE AND SAVE
			for (String fileName : listSol.list() ) { 
				// Create the instance from the json file
				Instance instLIRP = new Instance(instDir + fileName);
				System.out.print("Solving instance " + fileName + "...");

				// Create the log file and solution file to store the results and the trace of the program
				String fichierLog = "../Log files/" + fileName.replace(".json", ".log");
				String fichierSol = "../Solutions/" + fileName.replace(".json", ".sol");

				File fileLog = new File(fichierLog);
				PrintStream printStreamLog = new PrintStream(fileLog);
				// Outputs out and err are redirected to the log file
				PrintStream original = System.out;
				System.setOut(printStreamLog);
				System.setErr(printStreamLog);
				File fileSol = new File(fichierSol);
				// Stream for the solution
				PrintStream printStreamSol = new PrintStream(fileSol);

				System.out.println("Creating the RouteManager...");
				RouteManager rm = new RouteManager(instLIRP);
				System.out.println("OK. Solving...");
				Solver solverLIRP = new Solver(instLIRP, rm);
				System.out.println("done!");

				// Call the method from the solver
				long startChrono = System.currentTimeMillis();
				Solution sol = solverLIRP.getSolution(printStreamSol);
				long stopChrono = System.currentTimeMillis();
				long duration = (stopChrono-startChrono);
				System.out.println("Time to solve the instance: "+duration+" milliseconds");

				System.out.println("================================");
				System.out.println();

				if (sol != null){

					sol.print(printStreamSol);
				}
				else {
					System.out.println("Error on this instance");
				}

				System.setOut(original);
				System.out.println("Instance solved.");
			}

			System.out.println("All Instances solved. FINISHED :-)");
		}

		catch (IOException ioe) {
			System.out.println("Error: " + ioe.getMessage());
		}
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

