package userInterfaceLIRP;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import ilog.concert.IloException;
import instanceManager.Instance;
import solverLIRP.Route;
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

	public static void main(String[] args) throws IOException, IloException {

		// Solve all instances in the directory
		String instanceDirectory = "Instance/Complete/"; 
		File listSol = new File(instanceDirectory);

		//------------------------------------------------------------------------------------
		// PARAMETERS


		//IN THE DIRECTORY FOR EVERY FILE, YOU SOLVE AND SAVE
		for (String fileName : listSol.list() ) 
		{ 
			// Create the instance from the json file
			Instance instLIRP = new Instance(new File(instanceDirectory + fileName));
			System.out.print("Solving instance " + fileName + "...");

			// Create the log file and solution file to store the results and the trace of the program
			String fichierLog = "./log/" + fileName.replace(".json", ".log");
			String fichierSol = "./Solutions/" + fileName.replace(".json", ".sol");
			
			
			File fileLog = new File(fichierLog);
			PrintStream printStreamLog = new PrintStream(fileLog);
			// Outputs out and err are redirected to the log file
			PrintStream original = System.out;
			System.setOut(printStreamLog);
			System.setErr(printStreamLog);
			File fileSol = new File(fichierSol);
			// Stream pour la solution
			PrintStream printStreamSol = new PrintStream(fileSol);

			Route myRoutes = new Routes(instance,max_nb_routes,CMAX,LMAX);
			Route[] availableRoutes = instance.getRoutes();
			private Route[][] routesSelection(Route[] availableRoutes){
				int nb_subsets = (int)Math.ceil(max_nb_routes/availableRoutes.length);
				Route[][] subsetOfRoutes = new Route[nb_subsets][max_nb_routes];
				for( int i= 1 ; i<nb_subsets ; i++ ){
					for ( int r= 0 ; r<max_nb_routes ; r++ ){
						do {
							if (r >= (i-1)* availableRoutes.length + 1 && r <= i*availableRoutes.length) {  
								System.out.println(subsetOfRoutes);;
							}
						}		
						while (availableRoutes.length <= max_nb_routes );
					}
				}
				// Route[][] subsetOfRoutes = routesSelection(availableRoutes)
				// for each subset (subsetOfRoutes[i]) :
				// 1/ solve the problem using CPLEX
				// 2/ Get the routes used in the solution
				// 3/ redefine available routes
				// 4/ Resolve, etc.


				// Call the method from the solver
				long startChrono = System.currentTimeMillis();
				Solution sol = Solver.solve(instance, subsetOfRoutes, printStreamSol);
				long stopChrono = System.currentTimeMillis();
				long duration = (stopChrono-startChrono);
				System.out.println("Time to solve the instance: "+duration+" milliseconds");

				System.out.println("================================");
				System.out.println();



				if (sol != null){

					sol.print(myRoutes, printStreamSol);
				}
				else
				{
					System.out.println("Error on this instance");
				}

				System.setOut(original);
				System.out.println("Instance solved.");

			}

			System.out.println("All Instances solved. FINISHED :-)");


		}


		//select at random the avilable routes to put in each subset
		// Make sure the routes are sufficient to produce a feasible solution

		return subsetOfRoutes;
	}

	catch (IOException ioe) {

	}
}

