package userInterfaceLIRP;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import ilog.concert.IloException;
import instanceManager.Instance;
import solverLIRP.Matheuristics;
import solverLIRP.RouteManager;
import solverLIRP.Solution;
import tools.Parameters;

public class ResolutionMain {
	/**
	 * @param args
	 * @throws IOException
	 * @throws IloException
	 */

	public static void main(String[] args) throws IOException, IloException {

		
		String instDir = "../Instances/Complete/";
		String solDir = "../Solutions/";
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
		File listInst = new File(instDir);
		File listSol = new File(solDir);


		//------------------------------------------------------------------------------------
		// PARAMETERS
		/* Number of routes in each subset when separating */

		int splitParam = 100; // Size of the subsets of routes
		//splitParam = 0; // Uncomment to solve the original instance without sampling the routes

		try {
			//IN THE DIRECTORY FOR EVERY FILE, YOU SOLVE AND SAVE
			for (String fileName : listInst.list() ) {
				int i = fileName.lastIndexOf('.');
				String fileNameSol = "../Solutions/" + fileName.replace(".json", "1.sol");
				if(splitParam > 0) {
					fileNameSol = "../Solutions/" + fileName.replace(".json", "_split"+ splitParam +"_improved.sol");
				}
				File fileSol = new File(fileNameSol);
				/* One instance is too big for the solver, we exclude it from the resolution */
				if (!fileName.startsWith("lirp20cl2ci_2") && fileName.startsWith("lirp10cl") && i > 0 &&  i < fileName.length() - 1 && !fileSol.exists()) {
					String extension = fileName.substring(i+1).toLowerCase();
					if(extension!= null && extension.equals("json")) {
						// Create the instance from the json file
						Instance instLIRP = new Instance(instDir + fileName);
						System.out.print("Solving instance " + fileName + "...");

						// Create the log file and solution file to store the results and the trace of the program
						String fichierLog = "../Log files/" + fileName.replace(".json", ".log");

						File fileLog = new File(fichierLog);
						PrintStream printStreamLog = new PrintStream(fileLog);
						// Outputs out and err are redirected to the log file
						PrintStream original = System.out;
						System.setOut(printStreamLog);
						System.setErr(printStreamLog);
						// Stream for the solution
						PrintStream printStreamSol = new PrintStream(fileSol);


						System.out.println("Creating the RouteManager...");
						RouteManager rm = new RouteManager(instLIRP);
						if(rm.allReachable()) {
							System.out.println("OK. Solving...");

							long startChrono = System.currentTimeMillis();
							Solution sol = Matheuristics.computeSolution(instLIRP, rm, splitParam, printStreamSol);
							
							long stopChrono = System.currentTimeMillis();
							long duration = (stopChrono-startChrono);
							System.out.println("Time to solve the instance: " + duration + " milliseconds");

							System.out.println("================================");
							System.out.println();

							if (sol != null){
								System.out.println("Printing the solution in " + fileNameSol);
								sol.print(printStreamSol);
								printStreamSol.println("Resolution time : " + duration + " milliseconds");
								printStreamSol.println();
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

}

