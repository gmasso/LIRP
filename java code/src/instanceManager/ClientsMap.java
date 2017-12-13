package instanceManager;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

public class ClientsMap extends Layer {

	private CitiesMap citiesMap;
	private double urbanRatio;
	private double holdingCost;

	/**
	 * Create a clientsMap object that defines the location and the attributes of the clients on the map
	 * @param gridSize		the size of the map	
	 * @param nbClients		the number of clients to locate on the map
	 * @param citiesSizes	the sizes of the cities on the map
	 * @param urbanRatio		the ratio of the area that belongs to cities
	 * @throws IOException
	 */
	public ClientsMap(double gridSize, int nbClients, double[] citiesSizes, double urbanRatio) throws IOException {
		// Draw the position of the clients on the grid. Their position may be
		// completely random or concentrated in several city areas on the grid.
		// The parameter urban_ratio determines the proportion of clients that belong
		// to an urban area
		super(gridSize, nbClients);
		this.generateClientsMap(citiesSizes, urbanRatio);
	}
	
	/**
	 * Create a clientsMap that defines the location and the attributes of the clients on the map, including their respective demand sequences
	 * @param gridSize			the size of the map	
	 * @param nbClients			the number of clients to locate on the map
	 * @param citiesSizes		the sizes of the cities on the map
	 * @param urbanRatio			the ratio of the area that belongs to cities
	 * @param holdingCost		the holding cost of the clients
	 * @param planningHorizon	the number of periods considered
	 * @param period				the cycle length of the demand pattern
	 * @param uniformDistrib		the type of distribution : uniform if true, normal if false
	 * @param minD				the lower bound on the demand values
	 * @param maxD				the upper bound on the demand values
	 * @throws IOException
	 */
	public ClientsMap(double gridSize, int nbClients, double[] citiesSizes, double urbanRatio, double holdingCost, int planningHorizon, int period, boolean uniformDistrib, double minD, double maxD) throws IOException {
		super(gridSize, nbClients);
		this.sites = new Client[this.nbSites];
		// Compute the location of each client on the map
		Point2D[] clientsCoords = this.generateClientsMap(citiesSizes, urbanRatio);
		
		// Fill the clients array with their respective coordinates
		for(int sIndex = 0; sIndex < this.nbSites; sIndex++) {
			this.sites[sIndex] = new Client(clientsCoords[sIndex], holdingCost, 0, -1);
		}
		// We set the average demand per box as the total demand (avgD/site * nbSites) divided among the number of demand boxes on the map
		double[][] clientsDemands = this.generateDemands(planningHorizon, period, uniformDistrib, ((maxD + minD) / 2) * nbSites / (Parameters.nbSteps * Parameters.nbSteps));

		// Set the clients demands
		for(int sIndex = 0; sIndex < this.nbSites; sIndex++) {
			((Client) this.sites[sIndex]).setDemands(clientsDemands[sIndex]);
		}
	}

	/**
	 * Creates a clientsMap object according to the data contained in a JSON array
	 * @param jsonClientsArray	the JSON array containing data about each client on the map
	 * @param planningHorizon	the number of periods that we consider (must be greater than the length of the demands sequences of the clients)
	 * @throws IOException
	 */
	public ClientsMap(JSONObject jsonClients, int planningHorizon) throws IOException {
		super(jsonClients.getDouble("map size"), jsonClients.getJSONArray("sites").length());
		
		JSONArray jsonClientsArray = jsonClients.getJSONArray("sites");
		// Loop through the depots and get the different parameters
		for(int clientIndex=0; clientIndex < jsonClientsArray.length(); clientIndex++) {
			this.sites[clientIndex] = new Client((JSONObject) jsonClientsArray.get(clientIndex), planningHorizon);
		}
	}

	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return	the cities map associated with this clients map
	 */
	public CitiesMap getCitiesMap() {
		return this.citiesMap;
	}

	/**
	 * 
	 * @return	the ratio of surface occupied by urban areas
	 */
	public double getUrbanRatio() {
		return this.urbanRatio;
	}
	
	/**
	 * 
	 * @return	the holding cost of the clients
	 */
	public double gethodingCosts() {
		return this.holdingCost;
	}

	/*
	 * PRIVATE METHODS
	 */
	/**
	 * Fill the clients map with the clients according to the positions of cities
	 * @param citiesSizes	the sizes of the cities that influence the density of the clients repartition
	 * @param urbanRatio		the proportion of clients that belong to an urban area
	 * @throws IOException
	 */
	private Point2D[] generateClientsMap(double[] citiesSizes, double urbanRatio) throws IOException {
		this.citiesMap = new CitiesMap(this.gridSize, citiesSizes);
		this.urbanRatio = urbanRatio;
		Point2D[] cCoords = new Point2D[this.nbSites];  

		// If there are no cities, draw the coordinates of the clients at random on the map
		if(citiesSizes.length == 0) {
			for(int sIndex = 0; sIndex < this.nbSites; sIndex++) {
				cCoords[sIndex] = drawLocation(0);
			}
			this.urbanRatio = 0;
		}

		// Otherwise (the map contain cities), draw the client coordinates according to the city sizes (std) and the urban ratio of the map 
		else {
			// Define the 'cdf' of city ratios
			double[] cityCumRatio = new double[citiesMap.getNbSites()]; 
			double cumSum = 0;
			for(int cityIter = 0; cityIter < citiesSizes.length; cityIter++) {
				cumSum += Math.PI * Math.pow(citiesSizes[cityIter], 2);
				cityCumRatio[cityIter] = cumSum;
			}

			// Normalize the cdf with respect to the total urban area of the grid
			for(int cityIter = 0; cityIter < citiesSizes.length; cityIter++) {
				cityCumRatio[cityIter] /= cumSum;
			}
			// Fill the coordinates of the clients at random according to the position of urban areas
			for(int clientIndex = 0; clientIndex < this.nbSites; clientIndex++) {
				double urbanProba = Parameters.rand.nextDouble();
				// If the client does not belong to an urban area, draw the client at random on the grid (whithout reject if it falls in a city)
				if(urbanProba > urbanRatio) {
					cCoords[clientIndex] = drawLocation(0);
				}
				// Otherwise, draw its coordinate according to the urban ratio and the sizes of the different cities
				else {
					// Select the index of the city to which the client belongs
					int cityIndex = 0;
					boolean cityFound = false;
					while(!cityFound) {
						if(urbanProba < cityCumRatio[cityIndex])
							cityFound = true;
						else
							cityIndex++;
					}
					// Compute the coordinates of the client in the selected city, using a normal distribution with mean the city coordinates and standard deviation the city size
					Point2D clientCoords = drawLocation(citiesMap.getSite(cityIndex).getCoordinates(), citiesSizes[cityIndex]);
					// If the client falls out of the grid, replace it on its border
					if(clientCoords.getX() < 0)
						clientCoords.setLocation(0, clientCoords.getY());
					else if(clientCoords.getX() > gridSize)
						clientCoords.setLocation(gridSize, clientCoords.getY());
					if(clientCoords.getY() < 0)
						clientCoords.setLocation(clientCoords.getX(), 0);
					else if(clientCoords.getY() > gridSize)
						clientCoords.setLocation(clientCoords.getX(), gridSize);

					cCoords[clientIndex] = clientCoords;
				}
			}
		}
		return cCoords;
	}
	
	/**
	 * Fill an array of demands for each customers
	 * @param nbPeriods		the length of the sequence of values generated for each client
	 * @param period	  		the length of a cycle (0 if the demand is aperiodic)
	 * @param uniformDistrib	a boolean indicator that states if the demand is uniformly distributed (true) or normally distributed (false)
	 * @param avgBoxD		the mean value of the demand in each box of the map
	 */
	private double[][] generateDemands(int nbPeriods, int period, boolean uniformDistrib, double avgBoxD) {
		// Store the demands for each client in an array
		double[][] clientsDemands = new double[this.nbSites][nbPeriods];

		// Divide the map into a grid and compute the size of each box
		double boxStep = this.gridSize/Parameters.nbSteps;

		// For each city of the map, create an array storing the probability to link to each square
		// A ratio of the demand corresponding to the urban ratio of the area is kept and 
		double baseIntensity = 1 -  this.urbanRatio;
		// Determine the intensity of each box of demand depending on how close they are to urban areas
		for(int x = 0; x < Parameters.nbSteps; x++) {
			for(int y = 0; y < Parameters.nbSteps; y++) {
				Point2D.Double demandLoc = new Point2D.Double((x + 0.5 )* boxStep, (y + 0.5 )* boxStep);
				double demandIntensity = avgBoxD * (baseIntensity + this.urbanRatio * getDemandIntensity(demandLoc));

				// Array to store the weight of each client for the allocation of the demand box (x, y)
				double[] clientsWeights = new double[this.nbSites];
				for(int clientsIndex = 0; clientsIndex < this.nbSites; clientsIndex++) {
					// The weight of each client to collect the demand at coords (x, y) is proportional to the inverse of the square of the distance 
					clientsWeights[clientsIndex] = 1 / Math.pow(demandLoc.distance(this.sites[clientsIndex].getCoordinates()), 2);
				}
				double[] dSeq = generateDemandSequence(nbPeriods, period, uniformDistrib);
				// Allocate the box demand to the clients randomly, period by period, according to their respective weights
				for(int periodIter = 0; periodIter < nbPeriods; periodIter++)
					clientsDemands[selectIndex(clientsWeights)][periodIter] += demandIntensity * dSeq[periodIter];
			}
		}
		
		/* Clean the demands sequences by replacing every negative demand by 0 */
		for (int cIter = 0; cIter < this.nbSites; cIter++) {
			for(int t = 0; t < nbPeriods; t++) {
				if(clientsDemands[cIter][t] < 0) {
					clientsDemands[cIter][t] = 0;
				}
			}
		}
			
		return clientsDemands;
	}
	
	/**
	 * Draw a set of coordinates on the map 
	 * @param coordCentre	the point of reference that is used 
	 * @param std			the standard deviation that determines the spread of the distribution around the center
	 * @return				a set of coordinates whose distance with the centre is normally distributed with a standard deviation std 
	 */
	private Point2D drawLocation(Point2D coordCentre, double std) {
		return new Point2D.Double(coordCentre.getX() + Parameters.rand.nextGaussian() * std, coordCentre.getY() + Parameters.rand.nextGaussian() * std);

	}

	/**
	 * Compute the intensity of the demand according to the distance of its location to cities
	 * @param loc	the coordinates at which we want to calculate the intensity
	 * @return		the intensity of the demand
	 */
	private double getDemandIntensity(Point2D.Double loc) {
		double intensity = 0;	
		// Get the cities map from the client map in order to determine the intensity of the demand on each box
		for(int cityIndex = 0; cityIndex < this.citiesMap.getNbSites(); cityIndex++) {
			double distWithCity = loc.distance(this.citiesMap.getSite(cityIndex).getCoordinates());
			intensity += 0.5 - cdfGaussian(1/Math.pow(distWithCity,2), 0, this.citiesMap.getCitySize(cityIndex));
		}
		return intensity;
	}

	// Select an index in the range of the array size with a probability proportional to the associated weight of each index
	/**
	 * Select randomly an index in an array according to the weights on each index
	 * @param weights	an array containing weights for each index, whose sum is equal to 1
	 * @return			the selected index
	 */
	private static int selectIndex(double[] weights) {
		double totalWeight = 0;
		for(int wIter = 0; wIter < weights.length; wIter++)
			totalWeight += weights[wIter];
		
		int index = -1;
		double randSelect = Parameters.rand.nextDouble();
		double cumProba = 0;
		while(randSelect > cumProba)
			cumProba += weights[++index] / totalWeight;
		
		return index;
	}

	// return pdf(x) = standard Gaussian pdf
	/**
	 * Compute the standard normal pdf
	 * @param x	the point at which to compute the pdf
	 * @return	the value of the standard normal pdf in x
	 */
	private static double pdfGaussian(double x) {
		return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
	}

	// return pdf(x, mu, signma) = Gaussian pdf with mean mu and stddev sigma
	/**
	 * Compute the normal pdf
	 * @param x		the point at which to compute the pdf
	 * @param mu		the mean of the gaussian pdf
	 * @param sigma	the std of the gaussian pdf
	 * @return		the value of the normal pdf in x
	 */
	public static double pdfGaussian(double x, double mu, double sigma) {
		return pdfGaussian((x - mu) / sigma) / sigma;
	}

	/**
	 * Approximate the standard normal distribution using Taylor approximation
	 * @param z	the point at which the cdf is calculated
	 * @return	the value of the cdf in z
	 */
	private static double cdfGaussian(double z) {
		if (z < -8.0) return 0.0;
		if (z >  8.0) return 1.0;
		double sum = 0.0, term = z;
		for (int i = 3; sum + term != sum; i += 2) {
			sum  = sum + term;
			term = term * z * z / i;
		}
		return 0.5 + sum * pdfGaussian(z);
	}

	/**
	 * Compute the cdf of a normal distribution 
	 * @param z		the point at which the cdf is calculated
	 * @param mu		the mean of the rv
	 * @param sigma	the standard deviation of the rv
	 * @return		the cdf(z, mu, sigma)
	 */
	private static double cdfGaussian(double z, double mu, double sigma) {
		return cdfGaussian((z - mu) / sigma);
	}

	/**
	 * Generate a random sequence of demand values
	 * @param planningHorizon	the length of the demand sequence
	 * @param period				the length of a cycle (0 if aperiodic)
	 * @param uniformDistrib		the type of distribution (uniform if true, normal if false)
	 * @return					the sequence of demand values generated
	 */
	private static double[] generateDemandSequence(int planningHorizon, int period, boolean uniformDistrib) {
		// Create a temporal sequence of individual demands with a period
		//'period_d' and an average value included in [min_d, 2 * avg_d].

		// Create an array of size the length of the planning horizon to store the demand in each period
		double[] demandSequence = new double[planningHorizon];
		
		// If the demand is uniform  the sequence of demands is filled with uniform r.v. in [min_d, max_d]
		if(uniformDistrib){
			for(int t = 0; t < planningHorizon; t++)
				for(int boxIter = 0; boxIter < Parameters.nbSteps * Parameters.nbSteps; boxIter++)
				demandSequence[t] = Parameters.rand.nextDouble();
		}
		// Otherwise the demand is drawn according to a truncated normal distribution, around its average value (sinus if periodic, constant otherwise)
		else {
			double avgDemand = 0.5;
			for(int t = 0; t < planningHorizon; t++) {
				double currentMean = avgDemand;
				double currentDemand = -1;
				// In the periodic case, the average demand follows sinusoidal
				if(period > 0) 
					currentMean += 0.5 * Math.sin(2 * Math.PI * t / period);
				while(currentDemand < 0 || currentDemand > currentMean + 0.5)
					currentDemand = currentMean + Parameters.rand.nextGaussian() / 6;
				demandSequence[t] = currentDemand;   
			}
		}
		return demandSequence;
	}
	
	/**
	 * Enrich the JSON object containing clients map data
	 */
	@Override
	protected JSONObject getJSONLayerSpec() throws IOException {
		// Create a JSON Object to describe the depots map
		JSONObject jsonSpec = new JSONObject();

		jsonSpec.put("cities", this.citiesMap.getJSONLayer());

		return jsonSpec;
	}
}
