package instanceManager;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import tools.Config;

import java.awt.geom.Point2D;

public class DepotsMap extends Layer {

	private double minDist; 

	/**
	 * Creates a DepotsMap object satisfying separation constraints on the distance between two sites
	 * @param gridSize	the size of the map
	 * @param fc			the cost for opening a depot at any location
	 * @param oc			the ordering cost for each depots (the length of the array gives the number of sites)
	 * @param initInv	the initial inventory at each depot
	 * @param capa		the upper bound on the inventory level for each depot
	 * @throws IOException
	 */
	public DepotsMap(double gridSize, int nbDepots, double fc, double oc, double initInv, double capa) throws IOException{
		super(gridSize, nbDepots);
		// Start by assigning virtual coordinates to all sites, out of the grid
		for(int sIndex = 0; sIndex < this.nbSites; sIndex++) {
			this.sites[sIndex] = new Depot(new Point2D.Double(this.gridSize * 2, this.gridSize * 2), fc, 1, oc, initInv, capa);
		}
		// The minimum distance between two sites must be at least half of the radius of a disk whose area is equal to the average area per site.
		this.minDist = this.gridSize / Math.sqrt(this.nbSites * Math.PI * 2);
		// Compute the coordinates of all the sites
		for(int sIndex = 0; sIndex < this.nbSites; sIndex++) {
			this.drawDepot(sIndex);
		}
		this.generateID();
	}

	/**
	 * Creates a DepotsMap object from the characteristics available in a JSON array
	 * @param jsonDepotsArray	the JSON array containing data about the location and attributes of the depots
	 * @throws IOException
	 */
	public DepotsMap(JSONObject jsonDepots) throws IOException {
		super(jsonDepots);

		JSONArray jsonDepotsArray = jsonDepots.getJSONArray("sites");
		/* Loop through the depots and get the different parameters */
		for(int depotIndex = 0; depotIndex < jsonDepotsArray.length(); depotIndex++) {
			/* Create a new depot with the corresponding features */
			sites[depotIndex] = new Depot((JSONObject) jsonDepotsArray.get(depotIndex));
		}
	}

	public DepotsMap(Mask dMask) throws IOException {
		super(dMask);

		if(dMask.getLayer().getClass() == DepotsMap.class) {
			this.mapID = dMask.getLayer().getID().replaceFirst(dMask.getLayer().getDescID(), this.getDescID());
		}
		else {
			System.out.println("Trying to build a depots map from another type of layer. Stopping.");
			System.exit(1);
		}
	}

	/**
	 * 
	 * @param d	the index of the depot that we want to position on the map
	 */
	public void drawDepot(int d) {
		// Create a new candidate site
		Point2D siteCandidate = drawLocation(0);
		// Draw new coordinates while the minimum distance with other sites is not respected
		while(getMinDist(siteCandidate) < this.minDist) 
			siteCandidate.setLocation(Config.RAND.nextDouble() * this.gridSize, Config.RAND.nextDouble() * this.gridSize);
		// Set the coordinates of the new depot to the first valid candidate
		this.setSiteCoords(d, siteCandidate);
	}


	protected String getDescID() {
		return "";
	}

	/**
	 * Create a new JSON object to store the characteristics of the DepotsMap object  
	 */
	@Override
	protected JSONObject getJSONLayerSpec() throws IOException {	
		return new JSONObject();
	}
}
