package instanceManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.stream.Stream;

import org.json.JSONObject;

import tools.JSONParser;
import tools.Pair;
import tools.Parameters;

import org.json.JSONArray;

import java.awt.geom.Point2D;

public class Instance {

	/*
	 * ATTRIBUTES
	 */
	private double gridSize;
	private int planningHorizon;
	private double[] vehiclesCapacity;
	private Location supplier;
	private DepotsMap[] depots;
	private ClientsMap clients;
	private DemandsMap demands;
	private String instID;
	
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
	public Instance(double gridSize, int planningHorizon, int nbDepots, double fc, double[] oc, int nbClients, double[] citiesSizes, double urbanRatio, double holdingRatio, int period, boolean uniformDistrib, int demandProfile, double[] vCapacities) throws IOException, NullPointerException {
		try {
			this.gridSize = gridSize;
			this.supplier = new Location(new Point2D.Double(gridSize/2, gridSize/2));
			this.depots = new DepotsMap[Parameters.nb_levels - 1];
			for(int lvl = 0; lvl < Parameters.nb_levels - 1; lvl++) {
				this.depots[lvl] = new DepotsMap(gridSize, nbDepots, fc, oc[lvl], 0, -1, this.supplier);
			}
			this.clients = new ClientsMap(gridSize, nbClients, citiesSizes, urbanRatio, holdingRatio);
			this.demands = new DemandsMap(this.clients, planningHorizon, period, uniformDistrib);
			this.clients.assignDemands(this.demands, demandProfile, vCapacities[Parameters.nb_levels-1]);
			// Set the planning horizon
			this.planningHorizon = planningHorizon;
			// Fill the corresponding attribute
			this.vehiclesCapacity = vCapacities; 

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
	public Instance(int planningHorizon, DepotsMap[] dMaps, ClientsMap cMap, double[] vCapacities, DemandsMap dBoxMap, int demandProfile) throws IOException, NullPointerException {
		System.out.println("Creating instance...");
		try {
			LinkedHashSet<Pair<Double, ArrayList<Boolean>>> activeDays = new LinkedHashSet<Pair<Double, ArrayList<Boolean>>>();
			for(int profile = 0; profile < Parameters.proba_profiles.length; profile++) {
				activeDays.add(new Pair<Double, ArrayList<Boolean>>(Parameters.proba_profiles[profile], new ArrayList<Boolean>(Arrays.asList(Parameters.active_profiles[profile]))));
			}
			this.gridSize = 0;
			for(DepotsMap dMap: dMaps) {
				this.gridSize = Math.max(gridSize, dMap.getGridSize());
			}
			this.gridSize = Math.max(this.gridSize, cMap.getGridSize());
			this.depots = dMaps;
			this.clients = cMap;
			this.clients.setClientsActiveDays(activeDays);
			this.clients.assignDemands(dBoxMap, demandProfile, vCapacities[Parameters.nb_levels - 1]);
			// Set the planning horizon
			this.planningHorizon = planningHorizon;
			// Fill the corresponding attribute
			this.vehiclesCapacity = vCapacities; 
			this.supplier = new Location(new Point2D.Double(gridSize/2, gridSize/2));

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
		try (Stream<String> jsonStream = Files.lines(Paths.get(fileName))){
			StringBuilder jsonSB = new StringBuilder();
			jsonStream.forEach(l -> jsonSB.append(l));
			JSONObject jsonInstanceObject = new JSONObject(jsonSB.toString());
			// Set the planning horizon
			this.planningHorizon = jsonInstanceObject.getInt("planning horizon");
			// Get the capacity of each vehicle
			JSONArray jsonCapaVehicles = jsonInstanceObject.getJSONArray("vehicles capacities");
			// Fill the corresponding attribute
			this.vehiclesCapacity = new double[jsonCapaVehicles.length()]; 
			for(int vIter=0; vIter<jsonCapaVehicles.length(); vIter++) {
				this.vehiclesCapacity[vIter] = jsonCapaVehicles.isNull(vIter) ? -1 : jsonCapaVehicles.getDouble(vIter); // Set the capacity of vehicle vIter if the field is not null, -1 otherwise (infinite capacity)
			}
			// Get the supplier coordinates
			JSONObject jsonSupplier = jsonInstanceObject.isNull("supplier") ? null : jsonInstanceObject.getJSONObject("supplier");

			// If no coordinates are given for the supplier, set its coordinates to the center of the grid
			double supplier_x = (jsonSupplier==null || jsonSupplier.isNull("x")) ? Math.floor(this.gridSize/2) : jsonSupplier.getDouble("x"); // Coordinate of the depot on the x-axis
			double supplier_y = (jsonSupplier==null || jsonSupplier.isNull("y")) ? Math.floor(this.gridSize/2) : jsonSupplier.getDouble("y"); // Coordinate of the depot on the y-axis

			this.supplier = new Location(new Point2D.Double(supplier_x, supplier_y));

			// Create a map for the depots by extracting data from the corresponding JSONArray in the JSON file
			this.depots = new DepotsMap[Parameters.nb_levels-1];
			for(int lvl = 0; lvl < Parameters.nb_levels - 1; lvl++) {
				this.depots[lvl] = new DepotsMap(jsonInstanceObject.getJSONObject("depots"));
			}

			// Extract data from the JSONArrays containing data for the clients
			this.clients = new ClientsMap(jsonInstanceObject.getJSONObject("clients"));

			this.gridSize = 0;
			for(DepotsMap dMap: this.depots) {
				this.gridSize = Math.max(gridSize, dMap.getGridSize());
			}
			this.gridSize = Math.max(this.gridSize, this.clients.getGridSize());
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
	/**
	 * 
	 * @return	the number of depots
	 */
	public int getNbDepots(int lvl) { 
		return this.depots[lvl].getNbSites();
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
	 * @return	the total number of sites on the map (depots + clients)
	 */
	public int getNbSites() {
		int nbSites = 0;
		for(int lvl = 0; lvl < Parameters.nb_levels - 1; lvl++) {
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
	public double getCapacityVehicle(int v) {
		if(v<this.vehiclesCapacity.length)
			if(this.vehiclesCapacity[v]>0)
				return this.vehiclesCapacity[v];
			else
				return Parameters.bigM;
		else
			throw new IndexOutOfBoundsException("Error: Vehicle " + v + "does not exist");
	}

	/**
	 * 
	 * @return	the number of vehicles in the fleet
	 */
	public int getNbVehicles() { 
		return this.vehiclesCapacity.length;
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

	/**
	 * Generate an ID for this instance
	 */
	private void generateID() {
		this.instID = Parameters.nb_levels + "l";
		for(int lvl = 0; lvl < Parameters.nb_levels - 1; lvl++) {
			this.instID += this.getNbDepots(lvl) + "dc" + lvl;
		}
		this.instID += this.getNbClients() + "cl" + this.clients.getCitiesMap().getNbSites() + "ci_" + UUID.randomUUID().toString();
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
		jsonInstance.put("planning horizon", this.planningHorizon);
		jsonInstance.put("supplier", this.supplier.getJSONLoc());
		JSONArray jsonLayers = new JSONArray();
		for(int lvl = 0; lvl < Parameters.nb_levels - 1; lvl++) {
			jsonLayers.put(this.depots[lvl].getJSONLayer());
		}
		jsonInstance.put("depots layers", jsonLayers);
		jsonInstance.put("clients", this.clients.getJSONLayer());
		jsonInstance.put("vehicles capacities", new JSONArray(this.vehiclesCapacity));

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
