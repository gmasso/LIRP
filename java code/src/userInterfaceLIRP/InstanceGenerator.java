package userInterfaceLIRP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ilog.concert.IloException;
import instanceManager.ClientsMap;
import instanceManager.DemandsMap;
import instanceManager.DepotsMap;
import instanceManager.Instance;
import instanceManager.Mask;
import solverLIRP.RouteManager;
import tools.JSONParser;
import tools.Pair;
import tools.Parameters;

public class InstanceGenerator {

	private static ArrayList<Pair<Integer, Double>> vehicles = new ArrayList<Pair<Integer, Double>>(); 				// Number and capacity of vehicles for each level
	private static int[] nb_depots_inst = {/*3,6, 9, 12, 15,*/ 18};
	private static int[] nb_clients_inst = {/*10, 25, 50, 75, 100,*/ 150, 200};

	private static int planning_horizon = 30;
	private static double fc_factor = 1000;
	private static double oc_factor = 10;
	private static double holding_ratio = 1.5;

	public static void main(String[] args) throws IOException, IloException {
		/* Add the fleet specifications for each level */
		vehicles.add(new Pair<Integer, Double>(100, 100.0));
		vehicles.add(new Pair<Integer, Double>(200, 50.0));

		/* Define the directory name to store the complete instances */
		String instDir = "../Instances/Complete/";

		/* Directory names for the depots and clients layers */
		String dcDirName = "../Instances/Layers/Depots/";
		String clDirName = "../Instances/Layers/Clients/";

		/* Loop through the possible number of clients */
		for(int nbClients : nb_clients_inst) {
			/* Loop through the number of depots corresponding to this number of clients */
			for(int nbDC : nb_depots_inst) {
				ArrayList<String> dcFNames = selectLayersNames(nbDC + "s", dcDirName);
				String dcFName = dcFNames.get(Parameters.rand.nextInt(dcFNames.size()));

				for(int nbCities = 0; nbCities < 3; nbCities++) {
					/* Initialize the counter for the number of instances of this type */
					int count = 0;
					/* Generate 3 instances for each demand profile (light, heavy of mixture */
					int nbInstOfType = 3;
					while(count < nbInstOfType * Parameters.demand_profiles.length) {
						ArrayList<String> clFNames = selectLayersNames(nbCities + "c", clDirName);
						String clFName = clFNames.get(Parameters.rand.nextInt(clFNames.size()));		
						ClientsMap cMap = new ClientsMap(JSONParser.readJSONFromFile(clDirName + clFName + "/map.json"));

						DepotsMap[] dMaps = new DepotsMap[1];
						dMaps[0] = new DepotsMap(JSONParser.readJSONFromFile(dcDirName + dcFName));
						dcFName = dcFNames.get(Parameters.rand.nextInt(dcFNames.size()));

						Mask cMask = new Mask(cMap, nbClients);
						Mask dMask[] = new Mask[1];
						dMask[0] = new Mask(dMaps[0]);

						/* Select all the files associated with the clients map selected */
						ArrayList<String> demandsFNames = selectLayersNames("", clDirName + clFName + "/");
						String demandPattern = "";
						/* All files that do not describe the clients map are demands maps */
						for(String demandFName : demandsFNames) {
							if(!demandFName.startsWith("map")) {
								DemandsMap demandsMap = new DemandsMap(JSONParser.readJSONFromFile(clDirName + clFName + "/" + demandFName));
								demandPattern = demandFName.substring(0, demandFName.lastIndexOf("-"));
								for(int activeProfile = 0; activeProfile < Parameters.proba_actives.length; activeProfile++) {
									try {
										String instName = "lirp" + nbClients + "r-" + nbDC + "d-" + demandPattern + count;
										System.out.print("Creating instance " + instName + "...");

										Instance inst = new Instance(planning_horizon, dMask, cMask, demandsMap, vehicles, holding_ratio, fc_factor, oc_factor, count % 3, activeProfile);

										RouteManager rm = new RouteManager(inst, true);
										inst.assignDemands(planning_horizon);

										String instDirName = instDir + inst.getID();
										File instDirectory = new File(instDirName);
										/* if the directory does not exist, create it */
										if (!instDirectory.exists()) {
											boolean result = false;

											try{
												instDirectory.mkdir();
												result = true;
											} 
											catch(SecurityException se){
												/* handle it */
											}        
											if(result) {    
												System.out.println("DIR " + instDirName +" created");  
											}

											inst.writeToJSONFile(instDirName + "/instance.json");
											rm.writeToJSONFile(instDirName + "/rm.json");
										}
									}
									catch (IOException ioe) {
										System.out.println("Error: " + ioe.getMessage());
									}
									count++;
								}
							}
						}
					}
				}

			}
		}
	}


	private static ArrayList<String> selectLayersNames(String prefix, String dirPath) {
		ArrayList<String> layersList = new ArrayList<String>();
		File dir = new File(dirPath);
		for(String fName : dir.list()) {
			if(fName.startsWith(prefix)) {
				layersList.add(fName);
			}
		}

		return layersList;
	}
}
