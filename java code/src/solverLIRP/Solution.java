package solverLIRP;
import java.util.LinkedHashSet;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import instanceManager.Instance;
import tools.Config;


public class Solution {

	/* ============================
	 *         ATTRIBUTES
	 ==============================*/
	private Instance instLIRP;
	private Route[][] routes;

	/* Depots */
	private boolean[][] openDepots;	// Binary indicating whether a depot is opened or not
	private double[][][][] q;   	// Quantity delivered to the depots in period t via route r
	private double[][][] invLoc;	// Stock at depot j in period t


	/* Routes */
	private boolean[][][] usedRoutes; 				// Binary indicating whether route r at level l is used in period t

	private double openingCosts;
	private double transportationCosts;
	private double inventoryCosts;

	private String status;
	private double bestLB;
	private double solTime;
	private JSONArray interSol = new JSONArray();

	/*==============================
	 *        CONSTRUCTEUR  
	 ===============================*/
	/**
	 * Return a dull solution with a impossible objective value
	 */
	public Solution() {
		this.openingCosts = -1;
		this.transportationCosts = -1;
		this.inventoryCosts = -1;
	}

	/**
	 * Create an Solution object that contains the solution produced by CPLEX
	 * @param instance	The LIRP instance
	 * @param routes		The array of available routes for each level
	 */
	public Solution(Instance instance, Route[][] routes){

		/* Initialization of the attributes */
		this.instLIRP = instance;
		this.routes = routes;

		/* Variables of the model */
		this.openDepots = new boolean[this.instLIRP.getNbLevels() - 1][];
		this.q = new double[this.instLIRP.getNbLevels()][][][];
		this.invLoc = new double[this.instLIRP.getNbLevels()][][];
		this.usedRoutes = new boolean[this.instLIRP.getNbLevels()][][];

		for(int lvl = 0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			if(lvl < this.instLIRP.getNbLevels() - 1) {
				this.openDepots[lvl] = new boolean[this.instLIRP.getNbDepots(lvl)];
			}
			this.q[lvl] = new double[this.instLIRP.getNbLocations(lvl)][this.routes[lvl].length][this.instLIRP.getNbPeriods()];
			this.invLoc[lvl] = new double[this.instLIRP.getNbLocations(lvl)][this.instLIRP.getNbPeriods()];
			this.usedRoutes[lvl] = new boolean[this.routes[lvl].length][this.instLIRP.getNbPeriods()];
		}

		this.openingCosts = -1;
		this.transportationCosts = -1;
		this.inventoryCosts = -1;

		this.status = "Feasible";
	}

	/*==============================
	 *          ACCESSORS  
	 ===============================*/
	/**
	 * Get the value of the binary variable stating if the depot is opened or not
	 * @param lvl		the level on which the depot is located
	 * @param dIndex	the index of the depot of interest
	 * @return			the value of the binary variable stating if the depot is opened
	 */
	public boolean isOpenDepot(int lvl, int d){
		if(lvl < this.instLIRP.getNbLevels() - 1 && d < this.openDepots[lvl].length)
			return this.openDepots[lvl][d];
		else {
			System.out.println("Impossible to determine if depot " + d + " on level " + lvl + " is opened.");
			System.exit(1);
			return false;
		}
	}

	/**
	 * 
	 * @param lvl	the level at which the location is positioned
	 * @param loc	the index of the location of interest
	 * @param r		the route index in the corresponding level
	 * @param t		the period index
	 * @return		the quantity delivered to location loc by route r in period t
	 */
	public double getQuantityDelivered(int lvl, int loc, int r, int t){
		if(lvl > -1 && lvl < this.q.length && loc < this.q[lvl].length && r < this.q[lvl][loc].length && t < this.q[lvl][loc][r].length)
			if(this.q[lvl][loc][r][t] < this.instLIRP.getCapacityVehicle(lvl) + Config.EPSILON)
				return Math.min(this.q[lvl][loc][r][t], this.instLIRP.getCapacityVehicle(lvl));
			else {
				System.out.println("Quantity impossible to deliver in period " + t + " (capacity vehicles " + this.instLIRP.getCapacityVehicle(lvl) + " at level" + lvl + ")");
				System.exit(1);
				return -1;
			}
		else {
			System.out.println("Impossible to access a quantity delivered to location " + loc + " on level " + lvl + " through route " + r + " in period " + t);
			System.exit(1);
			return -1;
		}	
	}

	/**
	 * 
	 * @param lvl	the level at which the location is positioned
	 * @param loc	the index of the location of interest
	 * @param t		the period index
	 * @return		the inventory level at location loc in period t
	 */
	public double getInvLoc(int lvl, int loc, int t){
		if(lvl > -1 && lvl < this.invLoc.length && loc < this.invLoc[lvl].length && t < this.invLoc[lvl][loc].length) {
			double capaLoc = (lvl < this.instLIRP.getNbLevels() - 1) ? this.instLIRP.getDepot(lvl, loc).getCapacity() : this.instLIRP.getClient(loc).getCapacity();
			if(this.invLoc[lvl][loc][t] < capaLoc + Config.EPSILON)
				return Math.min(this.invLoc[lvl][loc][t], capaLoc);
			else {
				System.out.println("Not enough capacity at location " + loc + " of level " + lvl + "to store " + this.invLoc[lvl][loc][t] + "units in period " + t);
				System.exit(1);
				return -1;
			}
		}
		else {
			System.out.println("Impossible to access the inventory of location " + loc + " on level " + lvl + " in period " + t);
			System.exit(1);
			return -1;
		}
	}

	/**
	 * 
	 * @param lvl	the level for which routes are considered
	 * @param r		the route index in level lvl
	 * @param t		the period of interest
	 * @return		true if the route r stopping at level lvl is used in period t
	 */
	public boolean isUsedRoute(int lvl, int r, int t) {
		if(lvl > -1 && lvl < this.usedRoutes.length && r < this.usedRoutes[lvl].length && t < this.usedRoutes[lvl][r].length)
			return this.usedRoutes[lvl][r][t];
		else {
			System.out.println("Impossible to access the usage of route " + r + " on level " + lvl + " in period " + t);
			System.exit(1);
			return false;
		}
	}

	/**
	 * 
	 * @param lvl	the level for which routes are studied
	 * @return		the set of routes used in the solution between the location at level lvl and its upper layer
	 */
	public LinkedHashSet<Route> getUsedLoops(int lvl) {
		LinkedHashSet<Route> loopUsed = new LinkedHashSet<Route>();
		for(int r = 0; r < this.routes[lvl].length; r++) {
			/* Restrict the search only to multi-stops routes */
			if(this.routes[lvl][r].getNbStops() > 1) {
				boolean notUsed = true;
				int t = 0;
				/* Check if the route is used in the solution in at least one period */
				while(notUsed && t < this.instLIRP.getNbPeriods()) {
					/* If the route is used in period t, add its index to the looopUsed list */
					if(this.usedRoutes[lvl][r][t]) {
						notUsed = false;
						loopUsed.add(routes[lvl][r]);
					}
					t++;
				}
			}
		}
		return loopUsed;
	}

	/**
	 * 
	 * @return	the value of the objective function for this solution
	 */
	public double getObjVal() {
		return 	this.openingCosts + this.transportationCosts + this.inventoryCosts;
	}

	/**
	 * 
	 * @return	The solving time necessary to obtain this solution
	 */
	public double getSolvingTime() {
		return this.solTime;
	}

	/*==========================
	 *         MUTATORS 
	 ===========================*/
	/**
	 * Sets the value of the binary variable stating if a depot is opened or not
	 * @param dIndex		the index of the depot to open or close
	 * @param isOpened	the binary corresponding to the depot state
	 */
	public void setOpenDepot(int lvl, int d, boolean isOpened){
		if(lvl > -1 && lvl < this.openDepots.length && d < this.openDepots[lvl].length)
			this.openDepots[lvl][d] = isOpened;	
		else {
			System.out.println("Impossible to set the boolean variable y for depot " + d + " on level " + lvl);
			System.exit(1);
		}
	}

	/**
	 * Set the stock level of a depot
	 * @param dIndex	the index of the depot
	 * @param t		the period index
	 * @param inv	the inventory value for depot dIndex in period t
	 */
	public void setInvLoc(int lvl, int loc, int t, double inv){
		if(lvl > -1 && lvl < this.invLoc.length && loc < this.invLoc[lvl].length && t < this.invLoc[lvl][loc].length)
			this.invLoc[lvl][loc][t] = inv;
		else {
			System.out.println("Impossible to set the inventory of location " + loc + " on level " + lvl + " in period " + t);
			System.exit(1);
		}
	}

	/**
	 * 
	 * @param dIndex
	 * @param t
	 * @param rIndex
	 * @param q
	 */
	public void setDeliveryLocation(int lvl, int loc, int r, int t, double quantity){
		if(lvl > -1 && lvl < this.q.length && loc < this.q[lvl].length && r < this.q[lvl][loc].length && t < this.q[lvl][loc][r].length)
			this.q[lvl][loc][r][t] = quantity;
		else {
			System.out.println("Impossible to set the quantity delivered to location " + loc + " on level " + lvl + " through route " + r + " in period " + t);
			System.exit(1);
		}
	}

	/**
	 * 
	 * @param lvl
	 * @param rIndex
	 * @param t
	 * @param isUsed
	 */
	public void setUsedRoute(int lvl, int r, int t, boolean isUsed){
		if(lvl > -1 && lvl < this.usedRoutes.length && r < this.usedRoutes[lvl].length && t < this.usedRoutes[lvl][r].length)
			this.usedRoutes[lvl][r][t] = isUsed;		
		else {
			System.out.println("Impossible to set the usage of route " + r + " on level " + lvl + " in period " + t);
			System.exit(1);
		}
	}

	/**
	 * Set the objective value associated with this Solution object
	 */
	public void computeObjValue(){
		this.openingCosts = 0;
		this.transportationCosts = 0;
		this.inventoryCosts = 0;

		for(int lvl = 0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			if(lvl < this.instLIRP.getNbLevels() - 1) {
				/* Fixed cost for opening the depots */
				for (int d = 0; d < this.openDepots[lvl].length; d++) {
					if(this.openDepots[lvl][d])
						this.openingCosts += this.instLIRP.getDepot(lvl, d).getFixedCost(); 
				}
			}

			for (int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
				/* Transportation costs */
				for (int r = 0; r < this.usedRoutes[lvl].length; r++) {
					if(this.usedRoutes[lvl][r][t])
						this.transportationCosts += this.routes[lvl][r].getCost();
				}

				/* Inventory costs */
				for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
					double hc = (lvl < this.instLIRP.getNbLevels() - 1) ? this.instLIRP.getDepot(lvl, loc).getHoldingCost() : this.instLIRP.getClient(loc).getHoldingCost();
					this.inventoryCosts += hc * this.invLoc[lvl][loc][t]; 
				}
			}
		}

		this.openingCosts = ((double) Math.floor(this.openingCosts * 1000)) / 1000.0;
		this.transportationCosts = ((double) Math.floor(this.transportationCosts * 1000)) / 1000.0;
		this.inventoryCosts = ((double) Math.floor(this.inventoryCosts * 1000)) / 1000.0;
	}

	/**
	 * 
	 * @param status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * 
	 * @param status
	 */
	public void setLB(double lb) {
		this.bestLB = ((double) Math.floor(lb * 1000)) / 1000.0;
	}

	/**
	 * Set the time to get the solution (in seconds)
	 * @param timeMillis	the solving time in milliseconds
	 */
	public void setSolvingTime(long timeMillis) {
		this.solTime = ((double) timeMillis) / 1000.0;
	}

	/**
	 * 
	 * @param interResults
	 */
	public void setInterResults(JSONObject interResults) {
		this.interSol.put(interResults);
	}

	/*==========================
	 *         METHODS
	 ===========================*/
	/**
	 * 
	 * @return	a HashMap containing the route used in the solution
	 */
	public HashMap<Integer, LinkedHashSet<Route>> collectUsedRoutes(){
		HashMap<Integer, LinkedHashSet<Route>> collectedRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
		for(int lvl=0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			LinkedHashSet<Route> routesLvl = new LinkedHashSet<Route>();
			for (int rIter = 0; rIter < this.usedRoutes[lvl].length; rIter++){
				boolean used = false;
				int t = 0;
				while (!used && t < this.instLIRP.getNbPeriods()){
					used = this.usedRoutes[lvl][rIter][t];
					t++;
				}
				if(used)
					routesLvl.add(this.routes[lvl][rIter]);
			}
			collectedRoutes.put(lvl, routesLvl);
		}
		return collectedRoutes;
	}

	/**
	 * 
	 * @return	a HashMap containing the multi-stops route used in the solution
	 */
	public HashMap<Integer, LinkedHashSet<Route>> collectUsedLoops(){
		HashMap<Integer, LinkedHashSet<Route>> usedLoops = new HashMap<Integer, LinkedHashSet<Route>>();
		for(int lvl=0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			LinkedHashSet<Route> routesLvl = new LinkedHashSet<Route>();
			for (int r = 0; r < this.usedRoutes[lvl].length; r++){
				boolean used = false;
				if(this.routes[lvl][r].getNbStops() > 1) {
					int t = 0;
					while (!used && t < this.instLIRP.getNbPeriods()){
						used = this.usedRoutes[lvl][r][t];
						t++;
					}
				}
				if(used)
					routesLvl.add(this.routes[lvl][r]);
			}
			usedLoops.put(lvl, routesLvl);
		}
		return usedLoops;
	}

	/**
	 * Print the open depots
	 * @param printStreamSol
	 */
	private JSONArray storeOpenDepots() {
		JSONArray jsonOpenDC = new JSONArray();
		for(int lvl = 0; lvl < this.openDepots.length; lvl++) {
			JSONArray jsonOpenDCLvl = new JSONArray();
			for (int d = 0; d < this.openDepots[lvl].length; d++){
				if (this.openDepots[lvl][d]) 
					jsonOpenDCLvl.put(1);
			}
			jsonOpenDC.put(jsonOpenDCLvl);
		}
		return jsonOpenDC;
	}

	/**
	 * Print the inventory at every depot for every period
	 * @param printStreamSol
	 */
	private JSONArray storeInvLoc(){
		JSONArray jsonInv = new JSONArray();
		for(int lvl = 0; lvl < this.openDepots.length; lvl++) {
			/* For each opened dc on the current level, store the evolution of the inventory level */
			JSONArray jsonInvLvl = new JSONArray();
			for (int d = 0; d < this.openDepots[lvl].length; d++) {
				if(this.openDepots[lvl][d]) {
					JSONObject jsonInvLoc = new JSONObject();
					jsonInvLoc.put("dc", d);
					jsonInvLoc.put("init", this.instLIRP.getDepot(lvl, d).getInitialInventory());	
					jsonInvLoc.put("seq", new JSONArray(invLoc[lvl][d]));

					jsonInvLvl.put(jsonInvLoc);
				}
			}
			jsonInv.put(jsonInvLvl);
		}
		/* Same process with the clients */
		JSONArray jsonInvClients = new JSONArray();
		for (int c = 0; c < this.instLIRP.getNbClients(); c++) {
			JSONObject jsonInvCl = new JSONObject();
			jsonInvCl.put("client", c);
			jsonInvCl.put("init", this.instLIRP.getClient(c).getInitialInventory());	
			jsonInvCl.put("seq", new JSONArray(invLoc[this.openDepots.length][c]));

			jsonInvClients.put(jsonInvCl);
		}
		jsonInv.put(jsonInvClients);

		return jsonInv;
	}

	/**
	 * Print the quantities delivered at the depots in every period
	 * @param printStreamSol
	 */
	private JSONArray storeDeliveries(){
		JSONArray jsonDeliveries = new JSONArray();
		for(int lvl = 0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			JSONArray jsonDeliveriesLvl = new JSONArray();
			for(int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
				JSONArray jsonDeliveriesLoc = new JSONArray();
				for(int r = 0; r < this.routes[lvl].length; r++) {
					for(int t = 0; t < this.q[lvl][loc][r].length; t++) {
						if(this.q[lvl][loc][r][t] > Config.EPSILON) {
							double quantity = ((double) Math.floor(this.q[lvl][loc][r][t] * 100)) / 100.0;
							jsonDeliveriesLoc.put("{(" + (t + 1) + ", " + r + "): " +  quantity + "}");
						}
					}
				}
				jsonDeliveriesLvl.put(jsonDeliveriesLoc);
			}
			jsonDeliveries.put(jsonDeliveriesLvl);
		}
		return jsonDeliveries;
	}

	/**
	 * 
	 * @param printStreamSol
	 */
	private JSONObject storeRoutes() {
		JSONObject jsonRoutes = new JSONObject();
		for(int lvl = 0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			JSONArray jsonRoutesLvl = new JSONArray();
			for(int r = 0; r < this.routes[lvl].length; r++) {
				JSONArray jsonUsagePeriods = new JSONArray();
				for(int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
					if(this.usedRoutes[lvl][r][t])
						jsonUsagePeriods.put(t);
				}
				if(jsonUsagePeriods.length() > 0) {
					JSONObject jsonRoute = new JSONObject();
					if(lvl > 0)
						jsonRoute.put("start", this.instLIRP.getDepotIndex(lvl - 1, this.routes[lvl][r].getStart()));
					else
						jsonRoute.put("start", -1);
					jsonRoute.put("stops", new JSONArray(this.routes[lvl][r].getStops()));
					jsonRoute.put("usage periods", jsonUsagePeriods);
					jsonRoutesLvl.put(jsonRoute);
				}
			}
			jsonRoutes.put("lvl"+lvl, jsonRoutesLvl);
		}
		return jsonRoutes;
	}	

	/**
	 * Recomputes the objective function from the Solution object attributes		
	 * @param printStreamSol
	 */
	private JSONObject storeObj(){
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("dc opening", this.openingCosts);
		jsonObj.put("transportation",  this.transportationCosts);
		jsonObj.put("inventory",  this.inventoryCosts);
		jsonObj.put("total", this.openingCosts + this.transportationCosts + this.inventoryCosts);
		return jsonObj;
	}

	/**
	 * 
	 * @param printStreamSol
	 */
	public JSONObject getJSONSol() { 
		JSONObject jsonSol = new JSONObject();

		jsonSol.put("status", this.status);
		jsonSol.put("objective value", this.storeObj());
		jsonSol.put("LB", this.bestLB);
		jsonSol.put("resolution time", this.solTime);
		jsonSol.put("intermediate stages", this.interSol);

		jsonSol.put("open depots", this.storeOpenDepots());

		jsonSol.put("routes", this.storeRoutes());
		jsonSol.put("deliveries", this.storeDeliveries());
		jsonSol.put("inventories", this.storeInvLoc());

		return jsonSol;
	}

	public void getUsedRoutesDesc() {
		System.out.println(this.storeRoutes());
	}
}
