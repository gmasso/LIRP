package instanceManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;

import org.json.JSONObject;

import tools.JSONParser;
import tools.Pair;
import tools.Config;

import org.json.JSONArray;

import java.awt.geom.Point2D;

public class Instance {

	/*
	 * ATTRIBUTES
	 */
	private double gridSize;
	private int planningHorizon;
	private ArrayList<Pair<Integer, Double>> fleetDesc;
	private Location supplier;
	private DepotsMap[] depots;
	private ClientsMap clients;
	private DemandsMap demands;
	private String instID;
	private int demandProfile;

	/*
	 * CONSTRUCTORS
	 */
	/**
	 * Create a new Instance object
	 * @param gridSize
	 * @param planningHorizon
	 * @param nbDepots
	 * @param fc
	 * @param oc
	 * @param nbClients
	 * @param citiesSizes
	 * @param urbanRatio
	 * @param holdingRatio
	 * @param period
	 * @param uniformDistrib
	 * @param minD
	 * @param maxD
	 * @param vCapacities
	 * @throws IOException
	 * @throws NullPointerException
	 */
	public Instance(double gridSize, int planningHorizon, int nbDepots, double fc, double[] oc, int nbClients, double[] citiesSizes, double urbanRatio, double holdingRatio, int period, boolean uniformDistrib, int demandProfile, ArrayList<Pair<Integer, Double>> vDesc) throws IOException, NullPointerException {
		try {
			this.gridSize = gridSize;
			this.supplier = new Location(new Point2D.Double(gridSize/2, gridSize/2));
			this.depots = new DepotsMap[Config.NB_LEVELS - 1];
			for(int lvl = 0; lvl < this.depots.length; lvl++) {
				this.depots[lvl] = new DepotsMap(gridSize, nbDepots, fc, oc[lvl], 0, -1);
			}
			this.clients = new ClientsMap(gridSize, nbClients, citiesSizes, urbanRatio, holdingRatio);
			this.demands = new DemandsMap(this.clients, planningHorizon, period, uniformDistrib);
			this.demandProfile = demandProfile; 
			this.fleetDesc = vDesc; 
			//this.clients.assignDemands(this.demands, planningHorizon, this.demandProfile, this.fleetDesc.get(Parameters.nb_levels - 1).getR());
			/* Set the planning horizon */
			this.planningHorizon = planningHorizon;
			/* Generate a unique ID */
			this.generateID();

			System.out.println("Instance created successfully.");
		}
		catch(IOException ioe) {
			System.out.println("Problem while generating the instance");
			System.out.println(ioe);
			System.exit(1);
		}
		catch(NullPointerException npe) {
			System.out.println("Problem while generating the instance");
			System.out.println(npe);
			System.exit(1);
		}
	}

	/**
	 * Create a new Instance object directly from its own attributes, the depots and clients maps and information on the fleet of vehicles
	 * @param planningHorizon	the number of periods
	 * @param dMap				the depots map
	 * @param cMap				the clients map
	 * @param vCapacities		the capacities of the vehicles in the fleet (also provide the fleet size)
	 * @throws IOException
	 * @throws NullPointerException
	 */
	public Instance(int planningHorizon, Mask[] dMasks, Mask cMask, DemandsMap dBoxMap, ArrayList<Pair<Integer, Double>> vDesc, double holdingRatio, double fcFactor, double ocFactor, int demandProfile, int activeProfile) throws IOException, NullPointerException {
		try {
			LinkedHashSet<Pair<Double, ArrayList<Boolean>>> activeDays = new LinkedHashSet<Pair<Double, ArrayList<Boolean>>>();
			for(int profile = 0; profile < Config.proba_actives.length; profile++) {
				activeDays.add(new Pair<Double, ArrayList<Boolean>>(Config.proba_actives[activeProfile][profile], new ArrayList<Boolean>(Arrays.asList(Config.active_profiles[profile]))));
			}
			this.gridSize = 0;
			for(Mask dMask: dMasks) {
				this.gridSize = Math.max(gridSize, dMask.getLayer().getGridSize());
			}
			this.gridSize = Math.max(this.gridSize, cMask.getLayer().getGridSize());

			this.supplier = new Location(new Point2D.Double(gridSize/2, gridSize/2));

			this.depots = new DepotsMap[dMasks.length];
			for(int lvl = 0; lvl < this.depots.length; lvl++) {
				this.depots[lvl] = new DepotsMap(dMasks[lvl]);
				for(int d = 0; d < this.depots[lvl].getNbSites(); d++) {
					((Depot) this.depots[lvl].getSite(d)).setFixedCost(fcFactor);
				}
			}

			this.clients = new ClientsMap(cMask);
			this.clients.setClientsActiveDays(activeDays);
			for(int c = 0; c < this.clients.getNbSites(); c++) {
				this.clients.getSite(c).setHC(holdingRatio * this.depots[this.depots.length - 1].getSite(0).getHoldingCost());
				this.clients.getSite(c).setInitInv(0);
			}
			this.fleetDesc = vDesc; 
			this.demands = dBoxMap;
			this.demandProfile = demandProfile;
			this.planningHorizon = planningHorizon;

			this.generateID();

			System.out.println("Instance created successfully.");
		}
		catch(IOException ioe) {
			System.out.println("Problem while generating the instance");
			System.out.println(ioe);
		}
		catch(NullPointerException npe) {
			System.out.println("Problem while generating the instance");
			System.out.println(npe);
		}
	}

	/**
	 * Create a new Instance object from data in JSON file
	 * @param jsonFile	the file containing the JSON string
	 * @throws IOException
	 * @throws NullPointerException
	 */
	public Instance(String fileName) throws IOException, NullPointerException {
		System.out.println("Getting data from the JSON file " + fileName + "...");
		// Get the JSON object contained in the file
		try {
			JSONObject jsonInstanceObject = JSONParser.readJSONFromFile(fileName);
			// Set the planning horizon
			this.planningHorizon = jsonInstanceObject.getInt("planning horizon");
			// Get the capacity of each vehicle
			JSONArray jsonFleetDesc = jsonInstanceObject.getJSONArray("fleet description");
			// Fill the corresponding attribute
			this.fleetDesc = new ArrayList<Pair<Integer,Double>>();
			for(int lvl = 0; lvl < jsonFleetDesc.length(); lvl++) {
				JSONArray jsonFleetLvl = jsonFleetDesc.getJSONArray(lvl);
				int nbVehicles = jsonFleetLvl.isNull(0) ? -1 : jsonFleetLvl.getInt(0);
				double capaVehicles = jsonFleetLvl.isNull(1) ? -1 : jsonFleetLvl.getDouble(1);

				this.fleetDesc.add(new Pair<Integer, Double>(nbVehicles, capaVehicles)); // Set the capacity of vehicle vIter if the field is not null, -1 otherwise (infinite capacity)
			}
			// Get the supplier coordinates
			JSONObject jsonSupplier = jsonInstanceObject.isNull("supplier") ? null : jsonInstanceObject.getJSONObject("supplier");

			this.supplier = new Location(jsonSupplier);

			// Create a map for the depots by extracting data from the corresponding JSONArray in the JSON file
			JSONArray jsonDCLayers = jsonInstanceObject.getJSONArray("depots layers");
			this.depots = new DepotsMap[jsonDCLayers.length()];
			for(int lvl = 0; lvl < this.depots.length; lvl++) {
				this.depots[lvl] = new DepotsMap(jsonDCLayers.getJSONObject(lvl));
			}

			// Extract data from the JSONArrays containing data for the clients
			this.clients = new ClientsMap(jsonInstanceObject.getJSONObject("clients"));

			this.gridSize = 0;
			for(DepotsMap dMap: this.depots) {
				this.gridSize = Math.max(gridSize, dMap.getGridSize());
			}
			this.gridSize = Math.max(this.gridSize, this.clients.getGridSize());

			this.instID = jsonInstanceObject.getString("id");
		}
		catch(IOException ioe) {
			System.out.println("Problem while reading the JSON file");
			System.out.println(ioe);
		}
		catch(NullPointerException npe) {
			System.out.println("Problem while reading the JSON file");
			System.out.println(npe);
		}

	}

	/*
	 * ACCESSORS 
	 */
	/**
	 * 
	 * @return	the number of depots
	 */
	public String getID() { 
		return this.instID;
	}

	public int getNbDepots(int lvl) {
		if(lvl == -1) {
			return 0;
		}
		if(lvl < this.depots.length) {
			return this.depots[lvl].getNbSites();
		}
		else 
			return 0;
	}

	public String getDemandProfile() {
		return this.demands.getPatternDesc() + Config.profile_names[this.demandProfile];
	}

	/**
	 * 
	 * @return	the number of clients
	 */
	public int getNbClients() { 
		return this.clients.getNbSites();
	}

	/**
	 * 
	 * @return	the length of the planning horizon
	 */
	public int getNbPeriods() {
		return this.planningHorizon;
	}

	/**
	 * 
	 * @return	the number of levels of the instance
	 */
	public int getNbLevels() {
		return this.depots.length + 1;
	}

	/**
	 * 
	 * @return	the total number of sites on the map (depots + clients)
	 */
	public int getNbSites() {
		int nbSites = 0;
		for(int lvl = 0; lvl < this.depots.length; lvl++) {
			nbSites += this.depots[lvl].getNbSites();
		}
		return nbSites + this.clients.getNbSites();
	}

	/**
	 * Get a specific depot
	 * @param d	the index of the depot of interest
	 * @return	the Depot object corresponding to the depot index
	 */
	public Depot getDepot(int lvl, int d) {
		if(lvl < 0)
			return (Depot) this.supplier;
		return (Depot) this.depots[lvl].getSite(d);
	}

	/**
	 * Get a specific client
	 * @param c	the index of the client of interest
	 * @return	the Client object corresponding to the client index
	 */
	public Client getClient(int c) {
		return (Client) this.clients.getSite(c);
	}

	/**
	 * Get the capacity of a given vehicle
	 * @param v	the index of the vehicle of interest
	 * @return	the capacity of the vehicles corresponding to the index
	 */
	public double getCapacityVehicle(int lvl) {
		if(lvl < this.depots.length + 1)
			return (this.fleetDesc.get(lvl).getR() > 0) ? this.fleetDesc.get(lvl).getR() : Config.BIGM;
			else
				throw new IndexOutOfBoundsException("Error: Level " + lvl + "does not exist");
	}

	/**
	 * 
	 * @return	the number of vehicles in the fleet
	 */
	public int getNbVehicles(int lvl) { 
		return this.fleetDesc.get(lvl).getL();
	}

	/**
	 * 
	 * @return the Location object corresponding to the supplier in this instance
	 */
	public Location getSupplier() {
		return this.supplier;
	}

	/**
	 * Return the index of a depot 
	 * @param depot	the Depot object of interest
	 * @return		the index of this depot in the DepotsMap associated with the instance
	 */
	public int getDepotIndex(int lvl, Location depot) {
		for(int dIndex = 0; dIndex < this.depots[lvl].getNbSites(); dIndex++) {
			if((Depot) this.depots[lvl].getSite(dIndex) == depot)
				return dIndex;
		}
		return -1;
	}

	/**
	 * Return the number of location at a given level of the network
	 * @param lvl	the level of interest in the network
	 * @return		the number of location at the level of interest (0 if the level corresponds to the external supplier at the root of the distribution network)
	 */
	public int getNbLocations(int lvl) {
		if(lvl < 0)
			return 0;
		if(lvl < this.depots.length) {
			return this.depots[lvl].getNbSites();
		}
		return this.clients.getNbSites();
	}

	/*
	 * MUTATORS
	 */
	/**
	 * Set the length of the planning horizon
	 * @param nbPeriods	the number of periods of the new planning horizon
	 */
	public void setNbPeriods (int nbPeriods) {
		if(this.planningHorizon < nbPeriods)
			throw new IndexOutOfBoundsException("Error: Not enough data to cover " + nbPeriods + "periods");
		else
			this.planningHorizon = nbPeriods;
	}

	/**
	 * Draw the position of depot d on the depots map
	 * @param d	the index of the depot to draw
	 */
	public void drawDepot(int lvl, int d) {
		this.depots[lvl].drawDepot(d);
	}

	/**
	 * Draw the position of client c on the clients map
	 * @param c	the index of the client to draw
	 */
	public void drawClient(int c) {
		this.clients.redrawClient(c);
	}

	public void assignDemands() {
		this.clients.assignDemands(this.demands, this.planningHorizon, demandProfile, this.fleetDesc.get(this.getNbLevels() - 1).getR());
	}

	/**
	 * Adjust the capacity of vehicles at the DC levels depending on the demands faced by the clients
	 */
	public void adjustCapaFleet() {
		double totalDemand = 0;
		for(int c = 0; c < this.clients.getNbSites(); c++) {
			totalDemand += this.getClient(c).getCumulDemands(0, this.planningHorizon);
		}
		
		for(int lvl = 0; lvl < this.depots.length; lvl++) {
			double scaleParam = Config.demand_profiles[0][0] + Config.RAND.nextDouble() * (Config.demand_profiles[0][1] - Config.demand_profiles[0][0]);
			this.fleetDesc.get(lvl).setR(Math.max(this.fleetDesc.get(lvl).getR(), totalDemand / (scaleParam *  this.planningHorizon)));
		}
	}
	
	/**
	 * Generate an ID for this instance
	 */
	private void generateID() {
		this.instID = this.getNbLevels() + "l";
		for(int lvl = 0; lvl < this.depots.length; lvl++) {
			this.instID += this.getNbDepots(lvl) + "dc" + lvl + "-";
		}
		int nbCities = (this.clients.getCitiesMap() == null) ? 0 : this.clients.getCitiesMap().getNbSites();
		this.instID += this.getNbClients() + "r-" + nbCities + "c-" + this.planningHorizon + "p-" + this.getDemandProfile() + "_" + UUID.randomUUID().toString();
	}

	/*
	 * METHODS
	 */
	/**
	 * Create a new JSON object to store the Instance object
	 * @return	a JSON object containing all attributes and components of the LIRP instance
	 * @throws IOException
	 */
	private JSONObject getJSONInstance() throws IOException{
		// Create a JSON Object to describe the instance
		JSONObject jsonInstance = new JSONObject();
		jsonInstance.put("id",  this.instID);
		jsonInstance.put("planning horizon", this.planningHorizon);
		jsonInstance.put("supplier", this.supplier.getJSONLoc());
		JSONArray jsonLayers = new JSONArray();
		for(int lvl = 0; lvl < this.depots.length; lvl++) {
			jsonLayers.put(this.depots[lvl].getJSONLayer());
		}
		jsonInstance.put("depots layers", jsonLayers);
		jsonInstance.put("clients", this.clients.getJSONLayer());
		JSONArray jsonFleet = new JSONArray();
		for(int lvl = 0; lvl < this.depots.length + 1; lvl++) {
			jsonFleet.put(this.fleetDesc.get(lvl).getJSON());
		}
		jsonInstance.put("fleet description", jsonFleet);

		return jsonInstance;
	}

	/**
	 * Write a JSON string with all characteristics of the instance into a file
	 * @param filename	the destination file
	 * @throws IOException
	 */
	public void writeToJSONFile(String filename) throws IOException {
		System.out.println("Writing instance characteristics to " + filename);
		JSONParser.writeJSONToFile(this.getJSONInstance(), filename);
	}
}
