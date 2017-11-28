package instanceManager;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.geom.Point2D;


public abstract class Layer {
	protected double gridSize; // Map is a square of size gridSize * gridSize
	protected int nbSites; // Number of sites on the map
	protected Location[] sites; // Coordinates of the different sites

	/*
	 * CONSTRUCTORS
	 */
	/**
	 * Create a new Layer object
	 * @param gridSize	the size of the map
	 * @param nbSites	the number of location to position on the map
	 * @throws IOException
	 */
	public Layer(double gridSize, int nbSites) throws IOException {
		this.gridSize = gridSize;
		this.nbSites = nbSites;
	}

	/**
	 * Create a new Layer object
	 * @param nbSites	the number of location to position on the map
	 * @throws IOException
	 */
	public Layer(int nbSites) throws IOException {
		this.nbSites = nbSites;
	}

	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return	the size of the grid
	 */
	protected double getGridSize() {
		return this.gridSize;
	}

	/**
	 * 
	 * @return the number of sites on the map
	 */
	protected int getNbSites() {
		return this.nbSites;
	}
	
	/**
	 * 
	 * @param siteIndex	the site of interest
	 * @return			the Location object corresponding to the site of interest
	 */
	public Location getSite(int siteIndex) {
		return sites[siteIndex];
	}

	/*
	 * MUTATORS
	 */
	/**
	 * Change a site coordinates
	 * @param siteIndex	the location of interest
	 * @param loc		the new coordinates for the location of interest
	 */
	protected void setSiteCoords(int siteIndex, Point2D loc) {
		this.sites[siteIndex].setCoordinates(loc);
	}

	/**
	 * Get the minimum distance between a point and any site on the map
	 * @param loc	the point of interest
	 * @return		the distance between loc and the closest point on the map
	 */
	protected double getMinDist(Point2D loc) {
		double minDist = this.gridSize;
		for(int sIter = 0; sIter < nbSites; sIter++) {
			double distWithSite = loc.distance(this.sites[sIter].getCoordinates());
			if(distWithSite < minDist)
				minDist = distWithSite;
		}
		return minDist;
	}

	/*
	 * PROTECTED METHODS
	 */
	/**
	 * Draw a location at random, separated from the borders of the map
	 * @param siteSize	the diameter of the site
	 * @return			a Point2D object that is separated from the borders by at least twice the size of the site
	 */
	protected Point2D drawLocation(double siteSize) {
		double distWithBoundaries = 2 * siteSize;
		double xCoord = distWithBoundaries + Parameters.rand.nextDouble() * (this.gridSize - distWithBoundaries);
		double yCoord = distWithBoundaries + Parameters.rand.nextDouble() * (this.gridSize - distWithBoundaries);
		return new Point2D.Double(xCoord, yCoord);
	}

	/**
	 * 
	 * @return	a JSON object containing the attributes of each site on the map
	 * @throws IOException
	 */
	protected JSONObject getJSONLayer() throws IOException {
		// Create a JSON Object to describe the depots map
		JSONObject jsonMap = this.getJSONLayerSpec();
		JSONArray jsonSites = new JSONArray();

		// Loop through the sites and add their coordinates to the JSON Object
		for(int locIter = 0; locIter < this.nbSites; locIter++) {
			JSONObject jsonLoc = this.sites[locIter].getJSONLoc();
			jsonSites.put(locIter, jsonLoc);
		}

		jsonMap.put("nb sites", this.nbSites);
		jsonMap.put("sites", jsonSites);

		return jsonMap;
	}
	
	/**
	 * 
	 * @return	a JSON object containing specific attributes depending on the type of Layer
	 * @throws IOException
	 */
	protected abstract JSONObject getJSONLayerSpec() throws IOException;

	/**
	 * Write the JSON object of this map to a file
	 * @param filename	the destination file
	 * @throws IOException
	 */
	protected void writeToJSONFile(String filename) throws IOException {
		System.out.println(filename);
		JSONParser.writeJSONToFile(this.getJSONLayer(), filename);
	}
}
