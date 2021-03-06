package instanceManager;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.json.JSONArray;
import org.json.JSONObject;

import tools.Pair;
import tools.Config;


public class ClientsMap extends Layer {

	private CitiesMap cities;


	/**
	 * Create a clientsMap object that defines the location and the attributes of
	 * the clients on the map
	 * 
	 * @param gridSize
	 *            the size of the map
	 * @param nbClients
	 *            the number of clients to locate on the map
	 * @param citiesSizes
	 *            the sizes of the cities on the map
	 * @param urbanRatio
	 *            the ratio of the area that belongs to cities
	 * @throws IOException
	 */
	public ClientsMap(double gridSize, int nbClients, double[] citiesSizes, double urbanRatio) throws IOException {
		// Draw the position of the clients on the grid. Their position may be
		// completely random or concentrated in several city areas on the grid.
		// The parameter urbanRatio determines the proportion of clients that belong
		// to an urban area
		super(gridSize, nbClients);
		this.sites = new Client[this.nbSites];
		this.cities = new CitiesMap(this.gridSize, citiesSizes, urbanRatio);

		// Fill the clients array with their respective coordinates
		for (int cIndex = 0; cIndex < this.nbSites; cIndex++) {
			this.sites[cIndex] = new Client(drawClient());
		}
		this.generateID();
	}
	/**
	 * Create a clientsMap that defines the location and the attributes of the
	 * clients on the map, including their respective demand sequences
	 * 
	 * @param gridSize
	 *            the size of the map
	 * @param nbClients
	 *            the number of clients to locate on the map
	 * @param citiesSizes
	 *            the sizes of the cities on the map
	 * @param urbanRatio
	 *            the ratio of the area that belongs to cities
	 * @param holdingCost
	 *            the holding cost of the clients
	 * @throws IOException
	 */
	public ClientsMap(double gridSize, int nbClients, double[] citiesSizes, double urbanRatio, double capacity) throws IOException {
		super(gridSize, nbClients);
		this.sites = new Client[this.nbSites];
		this.cities = new CitiesMap(this.gridSize, citiesSizes, urbanRatio);

		// Fill the clients array with their respective coordinates
		for (int cIndex = 0; cIndex < this.nbSites; cIndex++) {
			this.sites[cIndex] = new Client(drawClient(), 0, 0, capacity);
		}
		this.generateID();
	}

	/**
	 * Create a clientsMap that defines the location and the attributes of the
	 * clients on the map, including their respective demand sequences
	 * @param gridSize		The size of the map
	 * @param nbClients		The number of clients to locate on the map
	 * @param citiesSizes	The sizes of the cities on the map
	 * @param urbanRatio		The ratio of urban areas on the map
	 * @param holdingCost	The holding cost of the clients compared to the one of the depots on the upper level
	 * @param demandsMap		The DemandsMap object containing the location of the demands on the grid
	 * @param demandProfile	The type of demand profile (weak, heavy of mixture of both)
	 * @param vCapacity		The vehicles capacity at the clients level
	 * @throws IOException
	 */
	public ClientsMap(double gridSize, int nbClients, double[] citiesSizes, double urbanRatio, double holdingCost,
			DemandsMap demandsMap, int demandProfile, double vCapacity) throws IOException {
		super(gridSize, nbClients);
		this.sites = new Client[this.nbSites];
		this.cities = new CitiesMap(this.gridSize, citiesSizes, urbanRatio);

		// Fill the clients array with their respective coordinates
		for (int cIndex = 0; cIndex < this.nbSites; cIndex++) {
			this.sites[cIndex] = new Client(drawClient(), holdingCost, 0, -1);
		}
		// We set the average demand per box as the total demand (avgD/site * nbSites)
		// divided among the number of demand boxes on the map
		this.assignDemands(demandsMap, demandsMap.getPlanningHorizon(), demandProfile, vCapacity);
		this.generateID();

	}

	/**
	 * Creates a clientsMap object according to the data contained in a JSON array
	 * 
	 * @param jsonClientsArray
	 *            the JSON array containing data about each client on the map
	 * @param planningHorizon
	 *            the number of periods that we consider (must be greater than the
	 *            length of the demands sequences of the clients)
	 * @throws IOException
	 */
	public ClientsMap(JSONObject jsonClients) throws IOException {
		super(jsonClients);

		JSONArray jsonClientsArray = jsonClients.getJSONArray("sites");
		/* Loop through the clients and get the different parameters */
		for (int clientIndex = 0; clientIndex < jsonClientsArray.length(); clientIndex++) {
			this.sites[clientIndex] = new Client((JSONObject) jsonClientsArray.get(clientIndex));
		}
		if(jsonClients.has("cities")) {
			this.cities = new CitiesMap(jsonClients.getJSONObject("cities"));
		}
	}


	public ClientsMap(Mask cMask) throws IOException {
		super(cMask);

		if(cMask.getLayer().getClass() == ClientsMap.class) {
			this.cities = ((ClientsMap) cMask.getLayer()).getCitiesMap();
			this.mapID = cMask.getLayer().getID().replaceFirst(cMask.getLayer().getDescID(), this.getDescID());
		}
		else {
			System.out.println("Trying to build a clients map from another type of layer. Stopping.");
			System.exit(1);
		}
	}

	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return the cities map associated with this clients map
	 */
	public CitiesMap getCitiesMap() {
		return this.cities;
	}

	/*
	 * PUBLIC METHODS
	 */
	/**
	 * 
	 * @param c	the client we want to re-compute the coordinates of
	 */
	public void redrawClient(int c) {
		this.sites[c].setCoordinates(drawClient());
	}

	protected String getDescID() {
		if(this.cities != null) {
			return this.cities.getDescID();
		}
		return "0c-";
	}

	public void assignDemands(DemandsMap demandsMap, int horizon, int demandProfile, double vCapacity) {
		int planningHorizon = Math.min(horizon, demandsMap.getPlanningHorizon());
		int startPlanning = Config.RAND.nextInt(demandsMap.getPlanningHorizon() - planningHorizon);
		if(this.isCompatible(demandsMap)) {
			double[][] clientsDemands = new double[this.sites.length][planningHorizon];

			/* Assign a weight to each client for the allocation of each demand box */
			double[][] clientsWeights = new double[demandsMap.getNbSites()][this.sites.length];
			double totalWeight = 0;

			for(int dBoxIndex = 0; dBoxIndex < clientsWeights.length; dBoxIndex++) {
				Location dLoc = demandsMap.getSite(dBoxIndex);
				totalWeight = 0;
				for (int cIndex = 0; cIndex < this.sites.length; cIndex++) {
					Location cLoc = this.sites[cIndex];
					((Client) cLoc).initDemandSeq(demandsMap.getPlanningHorizon());

					/* The weight of each client to collect each demand of the DemandsMap object is
					 * proportional to the inverse of the square of the distance
					 */
					clientsWeights[dBoxIndex][cIndex] = 1/Math.pow(dLoc.getDistance(cLoc), 2);
					totalWeight += clientsWeights[dBoxIndex][cIndex];
				}

				for (int cwIter = 0; cwIter < clientsWeights[dBoxIndex].length; cwIter++)
					clientsWeights[dBoxIndex][cwIter] /= totalWeight;
			}

			double normFactor = vCapacity * this.nbSites / demandsMap.getNbSites();

			for(int t = 0; t < planningHorizon; t++) {
				// Allocate each box demand to the clients randomly, according
				// to their respective weights
				for(int dBoxIndex = 0; dBoxIndex < clientsWeights.length; dBoxIndex++) {
					int c = selectIndex(clientsWeights[dBoxIndex]); 
					double rnd = Config.RAND.nextDouble();
					while(!((Client) this.sites[c]).isActiveDay(t) && rnd < Config.spatial_threshold){
						c = selectIndex(clientsWeights[dBoxIndex]); 
						rnd = Config.RAND.nextDouble();
					}
					clientsDemands[c][t] +=  this.scaleDemand(demandProfile) * ((DemandSequence) demandsMap.getSite(dBoxIndex)).getValue(startPlanning + t);
				}
			}
			/* Set the clients demands in every period */
			for (int cIndex = 0; cIndex < clientsDemands.length; cIndex++) {
				((Client) this.sites[cIndex]).setDemands(normFactor, clientsDemands[cIndex]);
			}
		}
		else {
			System.out.println("Trying to assign demands for clients map " + demandsMap.getClients().getID() + " to the clients map " + this.mapID);
		}
	}	

	/*
	 * PRIVATE METHODS
	 */
	/**
	 * 
	 * @param demandProfile
	 * @return
	 */
	private double scaleDemand(int demandProfile) {
		return (Config.demand_profiles[demandProfile][0] + (Config.demand_profiles[demandProfile][1] - Config.demand_profiles[demandProfile][0]) * Config.RAND.nextDouble());
	}
	//Select an index in the range of the array size with a probability
	//proportional to the associated weight of each index
	/**
	 * Select randomly an index in an array according to the weights on each index
	 * 
	 * @param weights
	 *            an array containing weights for each index, whose sum is equal to
	 *            1
	 * @return the selected index
	 */
	private static int selectIndex(double[] weights) {

		int index = -1;
		double randSelect = Config.RAND.nextDouble();
		double cumProba = 0;
		while (randSelect > cumProba)
			cumProba += weights[++index];

		return index;
	}

	/**
	 * 
	 * @return	the coordinates of a client, drawn at random according to the urban ratio and the sizes of the cities on the map
	 */
	private Point2D drawClient() {
		// Fill the coordinates of the clients at random according to the position of urban areas
		double urbanProba = Config.RAND.nextDouble();
		Point2D clientCoords = this.cities.positionClient(urbanProba);

		return clientCoords;
	}

	private boolean isCompatible(DemandsMap dMap) {
		String dMapID = dMap.getID();
		int prefix = dMapID.indexOf("s-") + 2;
		int postfix = dMapID.indexOf("*-");
		String subMap = dMapID.substring(prefix, postfix);
		return (this.mapID.contains(subMap));
	}

	/**
	 * 
	 * @param activeDays
	 */
	public void setClientsActiveDays(LinkedHashSet<Pair<Double, ArrayList<Boolean>>> activeDays) {
		double totalWeight = 0;
		for(Pair<Double, ArrayList<Boolean>> pairIter : activeDays) {
			totalWeight += pairIter.getL();
		}

		double cumSum = 0;
		double rnd = 0;
		Pair<Double, ArrayList<Boolean>> activePair = new Pair<Double, ArrayList<Boolean>> (0.0, new ArrayList<Boolean>());
		for(Location client : this.sites) {
			cumSum = 0;
			rnd = Config.RAND.nextDouble();
			Iterator<Pair<Double, ArrayList<Boolean>>> pairIter = activeDays.iterator();
			while(cumSum < rnd && pairIter.hasNext()) {
				activePair = pairIter.next();
				cumSum += activePair.getL() / totalWeight;
			}
			((Client) client).setActiveDays(activePair.getR());
		}
	}

	/**
	 * Enrich the JSON object containing clients map data
	 */
	@Override
	protected JSONObject getJSONLayerSpec() throws IOException {
		// Create a JSON Object to describe the depots map
		JSONObject jsonSpec = new JSONObject();
		if(this.cities != null) {
			jsonSpec.put("cities", this.cities.getJSONLayer());
		}

		return jsonSpec;
	}
}
