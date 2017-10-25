import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import ilog.concert.IloException;

public class ResolutionMain {
	/**
	 * @param args
	 * @throws IOException
	 * @throws IloException
	 */
	
	public static int RMAX = 1000;  // max number of routes in the mathematical model
	public static int CMAX = 4;     // max number of clients on a route
	public static int LMAX = 12;   // max length of a route
	public static int verbose = 0;
	public static double epsilon = 0.000001;
	public static final double TimeLimit = 300;  // time limit for the solver in seconds
	
	//GENERAL PARAMETER
	public static int max_time_route = 1000;
	public static int stopping_time = 1000;
	public static int avg_speed = 60;
 
	public static void main(String[] args) throws IOException, IloException {
		 
		// Resolution de toutes les instances presentes dans le repertoire instance
		String instanceDirectory = "instance/"; //ALL MY DATA IS HERE
		//String instanceDirectory = "debug/";
		File listSol = new File(instanceDirectory);
   
		//------------------------------------------------------------------------------------
		// PARAMETERS
 

		//IN THE DIRECTORY FOR EVERY FILE, YOU SOLVE AND SAVE
		for (String fileName : listSol.list() ) 
			{ 
				// Lecture de l'instance
				Instance instance = new Instance(instanceDirectory + fileName);//CREATE AN OBJECT
				System.out.print("Solving instance " + fileName + "...");
      
				String fichierLog = "./log/" + fileName.replace(".txt", ".log"); // CREATE A LOG FILE
				String fichierSol = "./sol/" + fileName.replace(".txt", ".sol");
				File fileLog = new File(fichierLog);
				PrintStream printStreamLog = new PrintStream(fileLog);
				// Les sorties out et err sont redirigees dans le fichier de log
				PrintStream original = System.out;
				System.setOut(printStreamLog);
				System.setErr(printStreamLog);
				File fileSol = new File(fichierSol);
				// Stream for the solution
				PrintStream printStreamSol = new PrintStream(fileSol);

				Routes myRoutes = new Routes(instance,RMAX,CMAX,LMAX); 
				myRoutes.generateAllRoutes();
				
				// Call the method of resolution
				long startChrono = System.currentTimeMillis();
				Solution sol = Solver.solve(instance, myRoutes, printStreamSol);
//				Heuristic heuristic = new Heuristic(instance, myRoutes, printStreamSol);
//				heuristic.getClass();
				
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
}



