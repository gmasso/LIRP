package instanceManager;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

public class CitiesMap extends Layer {
	private double[] sizes;
	
	/**
	 * Create a CitiesMap object to locate the cities on the map and keep a record of their respective sizes
	 * @param gridSize		the size of the map
	 * @param citiesSizes	the sizes of the cities
	 * @throws IOException
	 */
	public CitiesMap(double gridSize, double[] citiesSizes) throws IOException {
		super(gridSize, citiesSizes.length);

		this.sites = new Location[this.nbSites];
		this.sizes = citiesSizes;
		boolean cityOK = true;
		// Draw their location at random, such that their coordinates are at least 2 * city sizes away from the limits of the grid 
		int cityIndex = 0;
		while(cityOK && cityIndex < this.nbSites) {		
			Point2D cityCandidate = drawLocation(this.sizes[cityIndex]);
			// Check that two urban areas are at least separated by the sum of their city sizes
			int cityIter = 0;
			while(cityOK && cityIter < cityIndex) {
				// Compute the distance between the current city and the city at cityIter  
				double distWithSite = cityCandidate.distance(this.getSite(cityIter).getCoordinates());
				cityOK = distWithSite > this.sizes[cityIndex] + this.sizes[cityIter];
				cityIter++;
			}
			// If the distances are respected, add the candidate to the map and move to the next city
			if(cityOK) {
				this.sites[cityIndex] = new Location(cityCandidate);
				cityIndex++;
			}
			// Otherwise, re-draw this city and re-check the distances
			else {
				cityCandidate = drawLocation(this.sizes[cityIndex]);
				cityOK = true;
			}
		}
	}
	
	/*
	 * ACCESSORS
	 */
	/**
	 * Get the size of a given city
	 * @param cityIndex	the city we are interested in
	 * @return			the size of the city
	 */
	public double getCitySize(int cityIndex) {
		if(cityIndex < this.nbSites)
			return this.sizes[cityIndex];
		else
			return 0;
	}

	/**
	 * Enrich a JSON object related to a ClientsMap with information on the cities
	 */
	@Override
	protected JSONObject getJSONLayerSpec() throws IOException {
		// Create a JSON Object to describe the depots map
		JSONObject jsonSpec = new JSONObject();
		JSONArray jsonSizes = new JSONArray();
		
		// Loop through the sites and add their sizes to the JSON Object
		for(int sIter = 0; sIter < this.nbSites; sIter++) 
			jsonSizes.put(this.sizes[sIter]);

		jsonSpec.put("sizes", jsonSizes);
		
		return jsonSpec;
	}
}
