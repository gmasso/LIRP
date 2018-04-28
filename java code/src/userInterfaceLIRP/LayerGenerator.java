package userInterfaceLIRP;

import java.io.File;
import java.io.IOException;

import instanceManager.ClientsMap;
import instanceManager.DemandsMap;
import instanceManager.DepotsMap;
import tools.Config;


public class LayerGenerator {

	private static double capa_loc = 100000;							// Capacity of any location

	private static int[] nb_depots = {3, 6, 9, 12, 15, 18};					// Possible number of depots on a layer
	private static double oc_depots = 5;								// Ordering costs for the depots (in addition to routing costs)

	private static int nb_clients = 300;
	private static double[] cities_sizes = {3, 6, 9, 12, 18};			// Possible city sizes
	private static double[] proba_sizes = {0.25, 0.3, 0.2, 0.18, 0.07};	// Probability of each size

	private static boolean gen_depots = false;
	private static boolean gen_clients = false;
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String layersDir = "../Instances/Layers/";

		if(gen_depots) {
			/* Create the layers for each level */
			/* DC levels */
			for(int lvl = 0; lvl < Config.nb_levels - 1; lvl++) {
				for(int nbDepots : nb_depots) {
					for(int i = 0; i < 30; i++) {
						DepotsMap dMap = new DepotsMap(Config.grid_size, nbDepots, Config.FIXED_COST_DC, oc_depots, 0, capa_loc);
						String layerName = layersDir + "Depots/" + dMap.getID() +".json";
						dMap.writeToJSONFile(layerName);
					}
				}
			}
		}
		if(gen_clients) {
			/* Create instances with 3 or 2 cities */
			for(int nbCities = 0; nbCities < 3; nbCities++) {
				/* Create 10 layers of each type */
				for(int i = 0; i < 10; i++) {
					ClientsMap cMap = new ClientsMap(Config.grid_size, nb_clients, selectCitiesSizes(nbCities), 0.75, capa_loc);
					DemandsMap dMapIID = new DemandsMap(cMap, 90, 0, true);
					DemandsMap dMapNormal = new DemandsMap(cMap, 90, 0, false);
					DemandsMap dMapPeriodic = new DemandsMap(cMap, 90, 7, false);

					String cMapDirName = layersDir + "Clients/" + cMap.getID() + "/";
					File cMapDir = new File(cMapDirName);
					/* if the directory does not exist, create it */
					if (!cMapDir.exists()) {
						boolean result = false;

						try{
							cMapDir.mkdir();
							result = true;
						} 
						catch(SecurityException se){
							/* handle it */
						}        
						if(result) {    
							System.out.println("DIR " + cMapDirName +" created");  
						}
					}

					cMap.writeToJSONFile(cMapDirName + "map.json");
					dMapIID.writeToJSONFile(cMapDirName + dMapIID.getID() + ".json");
					dMapNormal.writeToJSONFile(cMapDirName + dMapNormal.getID() + ".json");
					dMapPeriodic.writeToJSONFile(cMapDirName + dMapPeriodic.getID() + ".json");

				}
			}
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
			double proba = Config.RAND.nextDouble();
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
