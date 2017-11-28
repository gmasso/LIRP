package instanceManager;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.geom.Point2D;
		
public class DepotsMap extends Layer {

	/**
	 * Creates a DepotsMap object satisfying separation constraints on the distance between two sites
	 * @param gridSize	the size of the map
	 * @param fc			the cost for opening a depot at any location
	 * @param oc			the ordering cost for each depots (the length of the array gives the number of sites)
	 * @param initInv	the initial inventory at each depot
	 * @param capa		the upper bound on the inventory level for each depot
	 * @param supplier	the supplier of the distribution network
	 * @throws IOException
	 */
	public DepotsMap(double gridSize, double fc, double[] oc, double initInv, double capa, Location supplier) throws IOException{
		super(gridSize, oc.length);

		// Start by assigning virtual coordinates to all sites, out of the grid
        for(int sIndex = 0; sIndex < this.nbSites; sIndex++) {
        		this.sites[sIndex] = new Depot(new Point2D.Double(this.gridSize * 2, this.gridSize * 2), fc, 1, oc[sIndex], initInv, capa, supplier);
        }
        // The minimum distance between two sites must be at least half of the radius of a disk whose area is equal to the average area per site.
        double minDist = this.gridSize / Math.sqrt(this.nbSites * Math.PI * 2);
        // Compute the coordinates of all the sites
        for(int sIndex = 0; sIndex < this.nbSites; sIndex++) {
            // Create a new candidate site
            Point2D siteCandidate = drawLocation(0);
            // Draw new coordinates while the minimum distance with other sites is not respected
            while(getMinDist(siteCandidate) < minDist) 
                siteCandidate.setLocation(Parameters.rand.nextDouble() * this.gridSize, Parameters.rand.nextDouble() * this.gridSize);
            // Set the coordinates of the new depot to the first valid candidate
            this.setSiteCoords(sIndex, siteCandidate);
        }
	}

	/**
	 * Creates a DepotsMap object from the characteristics available in a JSON array
	 * @param jsonDepotsArray	the JSON array containing data about the location and attributes of the depots
	 * @throws IOException
	 */
	public DepotsMap(JSONArray jsonDepotsArray) throws IOException {
		super(jsonDepotsArray.length());
		
		// Loop through the depots and get the different parameters
		for(int depotIndex=0; depotIndex<jsonDepotsArray.length(); depotIndex++) {
			// Create a new depot with the corresponding features
			sites[depotIndex] = new Depot((JSONObject) jsonDepotsArray.get(depotIndex));
		}
	}
	
	/**
	 * Create a new JSON object to store the characteristics of the DepotsMap object  
	 */
	@Override
	protected JSONObject getJSONLayerSpec() throws IOException {	
		return new JSONObject();
	}
}
