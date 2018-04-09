package solverLIRP;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import instanceManager.Instance;
import tools.Parameters;


public class Solution {

	/* ============================
	 *         ATTRIBUTES
	 ==============================*/
	private Instance instLIRP;
	private Route[][] routes;

	/* Depots */
	private boolean[][] openDepots;       			// Binary indicating whether a depot is opened or not
	private double[][][][] q;   // Quantity delivered to the depots in period t via route r
	private double[][][] invLoc;			// Stock at depot j in period t


	/* Routes */
	private boolean[][][] usedRoutes; 				// Binary indicating whether route r at level l is used in period t

	private double openingCosts;
	private double transportationCosts;
	private double inventoryCosts;

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
		this.openDepots = new boolean[Parameters.nb_levels - 1][];
		this.q = new double[Parameters.nb_levels][][][];
		this.invLoc = new double[Parameters.nb_levels][][];
		this.usedRoutes = new boolean[Parameters.nb_levels][][];

		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			if(lvl < Parameters.nb_levels - 1) {
				this.openDepots[lvl] = new boolean[this.instLIRP.getNbDepots(lvl)];
			}
			this.q[lvl] = new double[this.instLIRP.getNbLocations(lvl)][this.routes[lvl].length][this.instLIRP.getNbPeriods()];
			this.invLoc[lvl] = new double[this.instLIRP.getNbLocations(lvl)][this.instLIRP.getNbPeriods()];
			this.usedRoutes[lvl] = new boolean[this.routes[lvl].length][this.instLIRP.getNbPeriods()];
		}

		this.openingCosts = -1;
		this.transportationCosts = -1;
		this.inventoryCosts = -1;
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
		if(lvl < Parameters.nb_levels - 1 && d < this.openDepots[lvl].length)
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
			return this.q[lvl][loc][r][t];
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
		if(lvl > -1 && lvl < this.invLoc.length && loc < this.invLoc[lvl].length && t < this.invLoc[lvl][loc].length)
			return this.invLoc[lvl][loc][t];
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
	public ArrayList<Route> getUsedRoutes(int lvl) {
		ArrayList<Route> loopUsed = new ArrayList<Route>();
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

		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			/* Fixed cost for opening the depots */
			for (int d = 0; d < this.openDepots[lvl].length; d++) {
				if(this.openDepots[lvl][d])
					this.openingCosts += this.instLIRP.getDepot(lvl, d).getFixedCost(); 
			}

			for (int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
				/* Transportation costs */
				for (int r = 0; r < this.usedRoutes[lvl].length; r++) {
					if(this.usedRoutes[lvl][r][t])
						this.transportationCosts += this.routes[lvl][r].getCost();
				}

				/* Inventory costs */
				for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
					double hc = (lvl < Parameters.nb_levels - 1) ? this.instLIRP.getDepot(lvl, loc).getHoldingCost() : this.instLIRP.getClient(loc).getHoldingCost();
					this.inventoryCosts += hc * this.invLoc[lvl][loc][t]; 
				}
			}
		}
	}

	/*==========================
	 *         METHODS
	 ===========================*/
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
				else
					jsonOpenDCLvl.put(0);
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
			JSONArray jsonInvLvl = new JSONArray();
			for (int d = 0; d < this.openDepots[lvl].length; d++) {
				jsonInvLvl.put(this.instLIRP.getDepot(lvl, d).getInitialInventory());	
				jsonInvLvl.put(new JSONArray(invLoc[lvl][d]));
			}
			jsonInv.put(jsonInvLvl);
		}
		return jsonInv;
	}

	/**
	 * Print the quantities delivered at the depots in every period
	 * @param printStreamSol
	 */
	private JSONArray storeDeliveries(){
		JSONArray jsonDeliveries = new JSONArray();
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			JSONArray jsonDeliveriesLvl = new JSONArray();
			for(int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
				JSONArray jsonDeliveriesLoc = new JSONArray();
				for(int r = 0; r < this.routes[lvl].length; r++) {
					jsonDeliveriesLoc.put(new JSONArray(this.q[lvl][loc][r]));
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
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			JSONArray jsonRoutesLvl = new JSONArray();
			for(int r = 0; r < this.routes[lvl].length; r++) {
				JSONObject jsonRoute =new JSONObject();
				if(lvl > 0)
					jsonRoute.put("start", this.instLIRP.getDepotIndex(lvl - 1, this.routes[lvl][r].getStart()));
				else
					jsonRoute.put("start", -1);
				jsonRoute.put("stops", new JSONArray(this.routes[lvl][r].getStops()));
				JSONArray jsonPeriods = new JSONArray();
				for(int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
					if(this.usedRoutes[lvl][r][t])
						jsonPeriods.put(t);
				}
				jsonRoute.put("usage periods", jsonPeriods);
				jsonRoutesLvl.put(jsonRoute);
			}
			jsonRoutes.put("lvl"+lvl, jsonRoutesLvl);
		}
		return jsonRoutes;
	}	

	/**
	 * 
	 * @return	a HashMap containing the route used in the solution
	 */
	public HashMap<Integer, ArrayList<Route>> collectUsedRoutes(){
		HashMap<Integer, ArrayList<Route>> collectedRoutes = new HashMap<Integer, ArrayList<Route>>();
		for(int lvl=0; lvl < Parameters.nb_levels; lvl++) {
			ArrayList<Route> routesLvl = new ArrayList<Route>();
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
		jsonSol.put("open depots", this.storeOpenDepots());
		jsonSol.put("deliveries", this.storeDeliveries());
		jsonSol.put("inventories", this.storeInvLoc());
		jsonSol.put("routes", this.storeRoutes());
		jsonSol.put("objective value", this.storeObj());
		return jsonSol;
	}

}
