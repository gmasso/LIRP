package userInterfaceLIRP;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;

import ilog.concert.IloException;
import instanceManager.Instance;
import solverLIRP.LocManager;
import solverLIRP.Matheuristics;
import solverLIRP.RouteManager;
import solverLIRP.Solution;
import tools.JSONParser;

public class ResolutionMain {
	/**
	 * @param args
	 * @throws IOException
	 * @throws IloException
	 */

	public static void main(String[] args) throws IOException, IloException {	
		/*********************
		 *     PARAMETERS     
		 *********************/
		String fileName = "";
		String splitString = "";
		String loopString = "";

		boolean withLM = false;
		for(int argID = 0; argID < args.length; argID++) {
			if(args[argID].startsWith("-split=")) {
				splitString = args[argID].substring(args[argID].lastIndexOf("=") + 1);
			}
			else if(args[argID].startsWith("-lm")) {
				withLM = true;
			}
			else if(args[argID].startsWith("-loop_lvl=")) {
				loopString = args[argID].substring(args[argID].lastIndexOf("=") + 1);
			}

			else {
				fileName = args[argID];
			}
		}

		/* If no filename is given as an input, terminate without solving */
		if(fileName == "") {
			System.out.println("No instance file name given.");
			System.exit(1);
		}

		/* Extract the directory where the instance is located from the file name */
		String instDir = fileName.substring(0, fileName.lastIndexOf("/") + 1);
		String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
		if(extension!= null && extension.equals("json") ) {
			String instName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf(".json"));
			
			String logDir = instDir.substring(0, instDir.lastIndexOf("Instances/")) + "Log files/" + instName +"/";
			String solDir = instDir.substring(0, instDir.lastIndexOf("Instances/")) + "Solutions/" + instName +"/";

			/* Get all the instance files in the directory */
			File listInst = new File(instDir);
			File listSol = new File(solDir);
			HashSet<String> instSet = new HashSet<String>();
			if(listInst.exists())
				instSet = new HashSet<String>(Arrays.asList(listInst.list()));
			else {
				System.out.println("The directory where the instance should be located does not exists");
				System.exit(1);
			}		

			if(instSet.contains(instName + "." + extension)) {
				try {
					HashSet<String> solSet = new HashSet<String>(Arrays.asList(listSol.list()));

					/* Create the instance from the json file */
					Instance instLIRP = new Instance(instDir + instName + "." + extension);
					System.out.print("Solving instance " + instName + "...");

					String fileNameSol = "NoSplit_sol.json";

					boolean[] loopLvls = new boolean[instLIRP.getNbLevels()];
					int[] splitParam = new int[instLIRP.getNbLevels()];
					if(splitString.length() > 0) {
						splitParam = convertStrToIntArray(splitString);
						if(withLM)
							fileNameSol = "LM-Split" + splitString + "_sol.json";
						else
							fileNameSol = "Split" + splitString + "_sol.json";
					}
					if(loopString.length() > 0) {
						loopLvls = extractLoopLvls(instLIRP.getNbLevels(), loopString);
					}

					if(!solSet.contains(fileNameSol)) {
						/* Create the log file and solution file to store the results and the trace of the program */
						String fileNameLog = logDir + fileNameSol.replace(".json", ".log");
						System.out.println(fileNameLog);
						File fileLog = new File(fileNameLog);
						PrintStream printStreamLog = new PrintStream(fileLog);

						/* Outputs out and err are redirected to the log file */
						PrintStream original = System.out;

						System.out.print("Creating the RouteManager...");
						RouteManager rm = new RouteManager(instLIRP);
						rm.initialize(false);
						rm.writeToJSONFile(logDir + instName + "_rm.json");
						System.out.println("Done.");
						System.out.print("Creating the LocManager...");
						LocManager lm = withLM ? new LocManager(instLIRP) : null;
						if(lm != null) {
							lm.init();
						}
						System.out.println("Done.");
						System.out.print("Solving...");
						System.setOut(printStreamLog);
						System.setErr(printStreamLog);
						long startChrono = System.currentTimeMillis();
						Solution sol = Matheuristics.computeSolution(instLIRP, rm, loopLvls, splitParam, lm);

						long stopChrono = System.currentTimeMillis();
						long duration = (stopChrono - startChrono);
						System.out.println("==================================================");
						System.out.println("Time to solve the instance: " + duration + " milliseconds");
						System.out.println("=================================================");
						System.out.println();

						System.setOut(original);
						System.out.println("Done.");

						if (sol != null){
							/* Stream for the solution */
							sol.computeObjValue();
							sol.setSolvingTime(duration);
							System.out.println("Printing the solution in " + fileNameSol);
							JSONParser.writeJSONToFile(sol.getJSONSol(), solDir + fileNameSol);

						}
						else {
							System.out.println("Error on this instance");
						}

						rm = null;
						sol = null;
						instLIRP = null;

						System.out.println("Instance solved.");
					}
				}

				catch (IOException ioe) {
					System.out.println("Error: " + ioe.getMessage());
				}
			}
			else {
				System.out.println("Instance file " + instName + "." + extension + " not found in directory " + instDir);
			}
		}
	}

	private static int[] convertStrToIntArray(String strArray) {
		String[] items = strArray.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");

		int[] results = new int[items.length];

		for (int i = 0; i < items.length; i++) {
			try {
				results[i] = Integer.parseInt(items[i]);
			} catch (NumberFormatException nfe) {
				System.out.println("Error while trying to convert \"" + items[i] + "\" to an integer." );
				System.exit(1);
			};
		}

		return results;
	}

	private static boolean[] extractLoopLvls(int nbLvls, String strArray) {
		String[] items = strArray.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
		boolean[] results = new boolean[nbLvls];

		for (int i = 0; i < items.length; i++) {
			try {
				results[Integer.parseInt(items[i])] = true;
			} catch (NumberFormatException nfe) {
				System.out.println("Error while trying to convert \"" + items[i] + "\" to an integer." );
				System.exit(1);
			}
			catch (IndexOutOfBoundsException obe) {
				System.out.println("Error :" + Integer.parseInt(items[i]) + "is not an acceptable level index." );
				System.exit(1);
			}
		}
		return results;
	}
}
