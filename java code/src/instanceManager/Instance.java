package instanceManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.json.JSONArray;

import solverLIRP.Route;

import java.awt.geom.Point2D;

public class Instance {

	/*
	 * ATTRIBUTES
	 */
	private double gridSize;
	private int planningHorizon;
	private double[] vehiclesCapacity;
	private Location supplier;
	private DepotsMap depots;
	private ClientsMap clients;

	private Route[] routes; // Array of the possible routes for the instance

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
	public Instance(double gridSize, int planningHorizon, int nbDepots, double fc, double[] oc, int nbClients, double[] citiesSizes, double urbanRatio, double holdingRatio, int period, boolean uniformDistrib, double minD, double maxD, double[] vCapacities) throws IOException, NullPointerException {
		try {
			this.gridSize = gridSize;
			this.supplier = new Location(new Point2D.Double(gridSize/2, gridSize/2));
			this.depots = new DepotsMap(gridSize, fc, oc, 0, -1, this.supplier);
			this.clients = new ClientsMap(gridSize, nbClients, citiesSizes, urbanRatio, holdingRatio, planningHorizon, period, uniformDistrib, minD, maxD);
			// Set the planning horizon
			this.planningHorizon = planningHorizon;
			// Fill the corresponding attribute
			this.vehiclesCapacity = vCapacities; 
			
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
	public Instance(int planningHorizon, DepotsMap dMap, ClientsMap cMap, double[] vCapacities) throws IOException, NullPointerException {
		System.out.println("Creating instance...");
		try {
			this.gridSize = Math.max(dMap.getGridSize(), cMap.getGridSize());
			this.depots = dMap;
			this.clients = cMap;
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
			this.depots = new DepotsMap(jsonInstanceObject.getJSONObject("depots"));

			// Extract data from the JSONArrays containing data for the clients
			this.clients = new ClientsMap(jsonInstanceObject.getJSONObject("clients"), this.planningHorizon);

			this.gridSize = Math.max(this.depots.getGridSize(), this.clients.getGridSize());
			System.out.print("Instance created successfully.");
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
	public int getNbDepots() { 
		return this.depots.getNbSites();
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
		return this.getNbDepots() + this.getNbClients();
	}

	/**
	 * Get a specific depot
	 * @param d	the index of the depot of interest
	 * @return	the Depot object corresponding to the depot index
	 */
	public Depot getDepot(int d) {
		return (Depot) this.depots.getSite(d);
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
	 * @return	the array of Route objects containing all the available routes for this instance
	 */
	public Route[] getRoutes() {
		return this.routes;
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
	public int getDepotIndex(Location depot) {
		for(int dIndex = 0; dIndex < this.depots.getNbSites(); dIndex++) {
			if((Depot) this.depots.getSite(dIndex) == depot)
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
		jsonInstance.put("depots", this.depots.getJSONLayer());
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
