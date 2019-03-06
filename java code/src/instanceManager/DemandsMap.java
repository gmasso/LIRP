package instanceManager;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import tools.Config;

public class DemandsMap extends Layer {

	private int planningHorizon;
	private int period;
	private boolean isUniform;
	private ClientsMap clients;

	/**
	 * 
	 * @param clientsMap		The client map from which the instance is generated
	 * @param planningHorizon	The length of the planning horizon
	 * @param period			The length of a cycle in the demand sequences (0 if the demand sequences are aperiodic)
	 * @param isUniform			True if the demand distribution is uniform
	 * @throws IOException
	 */
	public DemandsMap(ClientsMap clientsMap, int planningHorizon, int period, boolean isUniform) throws IOException {
		super(clientsMap.getGridSize(), Config.nb_steps * Config.nb_steps);

		/* Set up the shared characteristics of the demands sequences */
		this.planningHorizon = planningHorizon;
		this.isUniform = isUniform;
		this.clients = clientsMap;
		this.period = period;

		double boxStep = gridSize / Config.nb_steps;
		for (int x = 0; x < Config.nb_steps; x++) {
			for (int y = 0; y < Config.nb_steps; y++) {
				Point2D boxLoc = new Point2D.Double((x + 0.5) * boxStep, (y + 0.5) * boxStep);
				this.sites[x * Config.nb_steps + y] = new DemandSequence(boxLoc);
			}
		}
		this.fillDemands();
		this.generateID();
	}

	public DemandsMap(JSONObject jsonDMap) throws IOException {
		super(jsonDMap);

		/* Set up the shared characteristics of the demands sequences */
		this.planningHorizon = jsonDMap.getInt("planning horizon");
		this.isUniform = jsonDMap.getBoolean("uniform");
		this.period = jsonDMap.getInt("period");

		JSONArray jsonDBoxes = jsonDMap.getJSONArray("sites");
		for (int sIndex = 0; sIndex < this.nbSites; sIndex++) {
			this.sites[sIndex] = new DemandSequence(jsonDBoxes.getJSONObject(sIndex));
		}
	}
	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return the length of the planning horizon
	 */
	public int getPlanningHorizon() {
		return this.planningHorizon;
	}

	/**
	 * 
	 * @return	The ClientsMap object linked to this DemandsMap object
	 */
	public ClientsMap getClients() {
		return this.clients;
	}

	/**
	 * Return the demand of a specific demand box in a given period
	 * @param dBoxIndex	The index of the demand box of interest
	 * @param period		The period of interest
	 * @return			The value of the corresponding demand box in the period
	 */
	public double mandBoxInPeriod(int dBoxIndex, int period) {
		return ((DemandSequence) this.sites[dBoxIndex]).getValue(period);
	}

	/**
	 * 
	 * @return The description of the demand pattern
	 */
	public String getPatternDesc() {
		if(this.isUniform) {
			return "Uni";
		}
		else if(this.period > 0) {
			return "P-" + this.period;
		}
		else {
			return "Gau";
		}
	}
	/*
	 * PRIVATE METHODS
	 */
	private void fillDemands() {
		for(int dBoxIndex = 0; dBoxIndex < this.sites.length; dBoxIndex++) {
			this.generateDemandSeq(dBoxIndex);
		}
	}
	/**
	 * Generate a random sequence of demand values
	 * 
	 * @param planningHorizon
	 *            the length of the demand sequence
	 * @param period
	 *            the length of a cycle (0 if aperiodic)
	 * @param uniformDistrib
	 *            the type of distribution (uniform if true, normal if false)
	 * @return the sequence of demand values generated
	 */
	private void generateDemandSeq(int dBoxIndex) {
		// A ratio of the demand corresponding to the urban ratio of the area is kept
		// Determine the intensity of the demand box based on its distance with urban areas
		((DemandSequence) this.sites[dBoxIndex]).setIntensity((1 - this.clients.getCitiesMap().getUrbanRatio()) * truncInvExp(0.5, 1) + this.clients.getCitiesMap().getUrbanRatio() * getDemandIntensity(this.sites[dBoxIndex]));
		
		((DemandSequence) this.sites[dBoxIndex]).fillValues(this.planningHorizon, this.isUniform, this.period);
	}

	/**
	 * Compute the intensity of the demand according to the distance of its location
	 * to cities
	 * @param loc	The coordinates at which we want to calculate the intensity
	 * @return the intensity of the demand
	 */
	private double getDemandIntensity(Location loc) {
		double intensity = 0;
		double citiesInfluence = 0;
		CitiesMap cities = this.clients.getCitiesMap();
		double distWithCity, citySize = 0;
		/* Get the cities map from the client map in order to determine the intensity of
		 * the demand on each box */
		for (int cityIndex = 0; cityIndex < cities.getNbSites(); cityIndex++) {
			distWithCity = loc.getDistance(this.clients.getCitiesMap().getSite(cityIndex));
			citySize = cities.getCitySize(cityIndex);
			citiesInfluence += citySize;
			intensity += citySize * cdfGaussian(1/(Math.pow(distWithCity, 2)), 0, 1/Math.pow(citySize,2));
		}

		if(citiesInfluence > 0) {
			return 2.0 * (intensity / citiesInfluence);
		}
		return 1.0;
	}


	/**
	 * Compute the standard normal pdf
	 * 
	 * @param x
	 *            the point at which to compute the pdf
	 * @return the value of the standard normal pdf in x
	 */
	private static double pdfGaussian(double x) {
		return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
	}

	/**
	 * Compute the normal pdf
	 * 
	 * @param x
	 *            the point at which to compute the pdf
	 * @param mu
	 *            the mean of the gaussian pdf
	 * @param sigma
	 *            the std of the gaussian pdf
	 * @return the value of the normal pdf in x
	 */
	public static double pdfGaussian(double x, double mu, double sigma) {
		return pdfGaussian((x - mu) / sigma) / sigma;
	}

	/**
	 * Approximate the standard normal distribution using Taylor approximation
	 * 
	 * @param z
	 *            the point at which the cdf is calculated
	 * @return the value of the cdf in z
	 */
	private static double cdfGaussian(double z) {
		if (z < -8.0)
			return 0.0;
		if (z > 8.0)
			return 1.0;
		double sum = 0.0, term = z;
		for (int i = 3; sum + term != sum; i += 2) {
			sum = sum + term;
			term = term * z * z / i;
		}
		return 0.5 + sum * pdfGaussian(z);
	}

	/**
	 * Compute the cdf of a normal distribution
	 * 
	 * @param z
	 *            the point at which the cdf is calculated
	 * @param mu
	 *            the mean of the rv
	 * @param sigma
	 *            the standard deviation of the rv
	 * @return the cdf(z, mu, sigma)
	 */
	private static double cdfGaussian(double z, double mu, double sigma) {
		return cdfGaussian((z - mu) / sigma);
	}
	
	
	private static double truncInvExp(double _lambda, double ub) {
		double val = ub + 1;
		while(val > ub)
			val = - _lambda * Math.log(1 - Config.RAND.nextDouble());

		return 1.0 - val;
	}
	
	@Override
	protected String getDescID() {
		if(this.period > 0) {
			/* Change the ID depending on if the demand pattern differentiates week days from week end (WD)
			 * or if it does not exclude any day of the week (AW)
			 */
			return "periodic-" + this.clients.getID() + "*-";
		}
		else
			return "iid-" + this.clients.getID() + "*-";
	}

	@Override
	/**
	 * Return a JSON Object containing the specificities of the demand map
	 */
	protected JSONObject getJSONLayerSpec() throws IOException {
		JSONObject jsonSpec = new JSONObject();
		jsonSpec.put("planning horizon", this.planningHorizon);
		jsonSpec.put("uniform", this.isUniform);
		jsonSpec.put("period", this.period);

		return jsonSpec;
	}

}
