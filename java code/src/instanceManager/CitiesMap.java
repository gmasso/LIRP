package instanceManager;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import tools.Config;

public class CitiesMap extends Layer {
	private double[] sizes;
	private double[] cityCumRatio;
	private double urbanRatio;
	
	/**
	 * Create a CitiesMap object to locate the cities on the map and keep a record of their respective sizes
	 * @param gridSize		the size of the map
	 * @param citiesSizes	the sizes of the cities
	 * @throws IOException
	 */
	public CitiesMap(double gridSize, double[] citiesSizes, double urbanRatio) throws IOException {
		super(gridSize, citiesSizes.length);

		this.sites = new Location[this.nbSites];
		this.sizes = citiesSizes;
		this.urbanRatio = urbanRatio;
		
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
		this.computeCityCumRatio(urbanRatio);
	}
	
	/**
	 * Creates a citiesMap object according to the data contained in a JSON object 
	 * @param jsonCities
	 * @throws IOException
	 */
	public CitiesMap(JSONObject jsonCities) throws IOException {
		super(jsonCities);
		
		JSONArray jsonCitiesArray = jsonCities.getJSONArray("sites");
		JSONArray jsonSizesArray = jsonCities.getJSONArray("sizes");
		this.sizes = new double[jsonSizesArray.length()];
		/* Loop through the clients and get the different parameters */
		for (int cityIndex = 0; cityIndex < jsonCitiesArray.length(); cityIndex++) {
			this.sites[cityIndex] = new Location((JSONObject) jsonCitiesArray.get(cityIndex));
			this.sizes[cityIndex] = jsonSizesArray.getDouble(cityIndex);
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
	 * 
	 * @param cityIndex	the index of the city of interest
	 * @return
	 */
	public double getCumRatio(int cityIndex) {
		return this.cityCumRatio[cityIndex];
	}
	
	public double getUrbanRatio() {
		return this.urbanRatio;
	}
	
	/*
	 * METHODS
	 */
	/**
	 * Compute the cumulative ratio represented by each sity on the map
	 */
	private void computeCityCumRatio(double urbanRatio) {
		// Define the 'cdf' of city ratios
		this.cityCumRatio = new double[this.getNbSites()]; 
		double cumSum = 0;
		for(int cityIter = 0; cityIter < this.getNbSites(); cityIter++) {
			cumSum += Math.PI * Math.pow(sizes[cityIter], 2);
			this.cityCumRatio[cityIter] = cumSum;
		}

		// Normalize the cdf with respect to the total urban area of the grid
		for(int cityIter = 0; cityIter < this.getNbSites(); cityIter++) {
			this.cityCumRatio[cityIter] *= urbanRatio/cumSum;
		}
	}
	
	/**
	 * Return the coordinates of a client drawn at random with respect to the probabilities of it belonging to the cities
	 * @param urbanProba		the random number drawn for this client
	 * @return				the proposed coordinates of the client
	 */
	public Point2D positionClient(double urbanProba) {
		// Select the index of the city to which the client belongs
		int cityIndex = 0;
		while(urbanProba > this.cityCumRatio[cityIndex] && cityIndex < this.getNbSites()) {
			cityIndex++;
		}

		/* If no city has been selected, draw the position of the client at random on the map */
		if(cityIndex == this.getNbSites())
			return drawLocation(0);
		/* Otherwise, draw the position of the client according to the center and the size of the selected city */
		else
		{
			Point2D cityCoords = this.getSite(cityIndex).getCoordinates();
			double std = this.sizes[cityIndex];
			return new Point2D.Double(cityCoords.getX() + Config.RAND.nextGaussian() * std, cityCoords.getY() + Config.RAND.nextGaussian() * std);

		}
	}
	
	protected String getDescID() {
		return this.sites.length + "c-";
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
