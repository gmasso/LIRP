package solverLIRP;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import instanceManager.Instance;
import tools.Parameters;


public class Solution {

	/* ============================
	 *         ATTRIBUTES
	 ==============================*/
	private Instance instanceLIRP;
	private Route[][] routes;

	/* Depots */
	private boolean[] openDepots;       			// Binary indicating whether a depot is opened or not
	private double[][][] deliveriesDepots;   // Quantity delivered to the depots in period t via route r
	private double[][] stockDepot;			// Stock at depot j in period t

	/* Clients */
	private double[][][] deliveriesClients;	// Quantity delivered to the clients i in period t via route r
	private double[][] stockClient;			// Stock at client i in period t

	/* Routes */
	private boolean[][][] usedRoutes; 				// Binary indicating whether route r at level l is used in period t

	private double objValue;
	/*==============================
	 *        CONSTRUCTEUR  
	 ===============================*/
	/**
	 * Return a dull solution with a impossible objective value
	 */
	public Solution() {
		this.objValue = -1;
	}

	/**
	 * Create an Solution object that contains the solution produced by CPLEX
	 * @param instance	The LIRP instance
	 * @param routes		The array of available routes for each level
	 */
	public Solution(Instance instance, Route[][] routes){

		/* Initialization of the attributes */
		this.instanceLIRP = instance;
		this.routes = routes;

		/* Variables of the model */
		this.openDepots = new boolean[instance.getNbDepots(0)];
		this.deliveriesDepots = new double[instance.getNbDepots(0)][this.routes[0].length][instance.getNbPeriods()];
		this.stockDepot = new double[instance.getNbDepots(0)][instance.getNbPeriods()];
		this.deliveriesClients = new double[instance.getNbClients()][this.routes[1].length][instance.getNbPeriods()];
		this.stockClient = new double[instance.getNbClients()][instance.getNbPeriods()];
		this.usedRoutes = new boolean[Parameters.nb_levels][][];
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++)
			this.usedRoutes[lvl] = new boolean[this.routes[lvl].length][instance.getNbPeriods()];

		/* Initialization of the values of the variables for the depots */
		for (int dIter = 0; dIter < instance.getNbDepots(0); dIter++) {
			this.openDepots[dIter] = false;
			for (int t = 0; t < instance.getNbPeriods(); t++){
				this.stockDepot[dIter][t] = 0;
				for(int rIter = 0; rIter < this.routes[0].length; rIter++) {
					this.deliveriesDepots[dIter][rIter][t] = 0;
				}
			}
		}

		/* Initialization of the values of the variables for the clients */
		for (int cIter = 0; cIter < instance.getNbClients(); cIter++){
			for (int t = 0; t < instance.getNbPeriods(); t++) {
				this.stockClient[cIter][t] = 0;
				for(int rIter = 0; rIter < this.routes[1].length; rIter++) {
					this.deliveriesClients[cIter][rIter][t] = 0;
				}
			}
		}

		//		/* Initialization of the boolean values for the depots-clients routes binary variables */
		//		for (int rIter = 0; rIter < this.routes.length; rIter++){
		//			for (int t = 0;t < instance.getNbPeriods(); t++){
		//				usedSDRoutes[rIter][t] = false;
		//			}
		//		}
		//		
		//		/* Initialization of the boolean values for the supplier-depots routes binary variables */
		//		for (int rIter = 0; rIter < usedDCRoutes.length; rIter++){
		//			for (int t = 0;t < instance.getNbPeriods(); t++){
		//				usedDCRoutes[rIter][t] = false;
		//			}
		//		}

		this.objValue = -1;
	}

	/*==============================
	 *          ACCESSORS  
	 ===============================*/
	/**
	 * Get the value of the binary variable stating if the depot is opened or not
	 * @param dIndex	the index of the depot of interest
	 * @return		the value of the binary variable for the opening of the depot
	 */
	public boolean isOpenDepot(int dIndex){
		return this.openDepots[dIndex];	
	}

	/**
	 * 
	 * @param dIndex	the index of the depot of interest
	 * @param t		the period index
	 * @param rIndex	the route index in the this.routes[0] array
	 * @return		the quantity delivered to depot dIndex by route rIndex in period t
	 */
	public double getDeliveryDepot(int dIndex, int rIndex, int t){
		return this.deliveriesDepots[dIndex][rIndex][t];
	}

	/**
	 * 
	 * @param cIndex	the index of the client of interest
	 * @param t		the period index
	 * @param rIndex	the route index in the this.routes[0] array
	 * @return		the quantity delivered to client cIndex by route rIndex in period t
	 */
	public double getDeliveryClient(int cIndex, int rIndex, int t){
		return this.deliveriesClients[cIndex][rIndex][t];
	}

	/**
	 * 
	 * @param dIndex	the index of the depot of interest
	 * @param t		the index of the period
	 * @return		the inventory level at depot dIndex in period t
	 */
	public double getStockDepot(int dIndex, int t){
		return this.stockDepot[dIndex][t];
	}

	/**
	 * 
	 * @param cIndex	the index of the client of interest
	 * @param t		the index of the period
	 * @return		the inventory level at depot cIndex in period t
	 */
	public double getStockClient(int cIndex, int t){
		return this.stockClient[cIndex][t];
	}

	/**
	 * 
	 * @param rIndex
	 * @param period
	 * @return
	 */
	public boolean isUsedRoute(int lvl, int rIndex, int period) {
		return this.usedRoutes[lvl][rIndex][period];
	}

	/**
	 * 
	 * @return	the set of Routes used in the solution between the supplier and the depots that are used in the solution
	 */
	public ArrayList<Route> getUsedRoutes(int lvl) {
		ArrayList<Route> loopUsed = new ArrayList<Route>();
		for(int rIter = 0; rIter < this.routes[lvl].length; rIter++) {
			/* Restrict the search only to multi-stops routes */
			if(this.routes[lvl][rIter].getNbStops() > 1) {
				boolean notUsed = true;
				int t = 0;
				/* Check if the route is used in the solution in at least one period */
				while(notUsed && t < this.instanceLIRP.getNbPeriods()) {
					/* If the route is used in period t, add its index to the looopUsed list */
					if(this.usedRoutes[lvl][rIter][t]) {
						notUsed = false;
						loopUsed.add(routes[lvl][rIter]);
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
		return this.objValue;
	}

	/*==========================
	 *         MUTATORS 
	 ===========================*/
	/**
	 * Sets the value of the binary variable stating if a depot is opened or not
	 * @param dIndex		the index of the depot to open or close
	 * @param isOpened	the binary corresponding to the depot state
	 */
	public void setOpenDepot(int dIndex, boolean isOpened){
		this.openDepots[dIndex] = isOpened;	
	}

	/**
	 * Set the stock level of a depot
	 * @param dIndex	the index of the depot
	 * @param t		the period index
	 * @param inv	the inventory value for depot dIndex in period t
	 */
	public void setStockDepot(int dIndex, int t, double inv){
		this.stockDepot[dIndex][t] = inv;
	}

	/**
	 * 
	 * @param dIndex
	 * @param t
	 * @param rIndex
	 * @param q
	 */
	public void setDeliveryDepot(int dIndex, int rIndex, int t, double q){
		this.deliveriesDepots[dIndex][rIndex][t] = q;
	}

	/**
	 * 
	 * @param cIndex
	 * @param t
	 * @param inv
	 */
	public void setStockClient(int cIndex, int t, double inv){
		this.stockClient[cIndex][t] = inv;
	}

	/**
	 * 
	 * @param cIndex
	 * @param t
	 * @param rIndex
	 * @param q
	 */
	public void setDeliveryClient(int cIndex, int rIndex, int t, double q){
		this.deliveriesClients[cIndex][rIndex][t] = q;
	}

	/**
	 * 
	 * @param lvl
	 * @param rIndex
	 * @param t
	 * @param isUsed
	 */
	public void setUsedRoute(int lvl, int rIndex, int t, boolean isUsed){
		this.usedRoutes[lvl][rIndex][t] = isUsed;	
	}

	public void setObjValue(){
		double objective1 = 0;
		double objective2 = 0;
		double objective3 = 0;

		/* Fixed cost for opening the depots */
		for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(0); dIter++) {
			if(this.openDepots[dIter])
				objective1 += this.instanceLIRP.getDepot(0, dIter).getFixedCost(); 
		}

		/* Transportation cost */
		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++) {
			for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
				for (int rIter = 0; rIter < this.routes[lvl].length; rIter++) {
					if(this.usedRoutes[lvl][rIter][t])
						objective2 += this.routes[lvl][rIter].getCost();
				}
			}
		}

		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++) {
			for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(0); dIter++) {
				objective3 += this.instanceLIRP.getDepot(0, dIter).getHoldingCost() * this.stockDepot[dIter][t];   
			}
			for (int cIter = 0; cIter < this.instanceLIRP.getNbClients(); cIter++) {
				objective3 += this.instanceLIRP.getClient(cIter).getHoldingCost() * this.stockClient[cIter][t];   
			}
		}

		this.objValue = objective1 + objective2 + objective3;
	}

	/*==========================
	 *         METHODS
	 ===========================*/
	/**
	 * Print the open depots
	 * @param printStreamSol
	 */
	public void printOpenDepots(PrintStream printStreamSol){	

		printStreamSol.println("----- DEPOTS -----");
		printStreamSol.print("Open depots: \t");
		for (int dIndex = 0; dIndex < this.openDepots.length; dIndex++){
			if (openDepots[dIndex]) 
				printStreamSol.print(dIndex+" \t  ");
		}
		printStreamSol.print("Depots not selected: \t");
		for (int dIndex = 0; dIndex < this.openDepots.length; dIndex++){
			if (!openDepots[dIndex]) 
				printStreamSol.print(dIndex+" \t  ");
		}
		printStreamSol.println();
	}

	/**
	 * Print the inventory at every depot for every period
	 * @param printStreamSol
	 */
	public void printStockDepot(PrintStream printStreamSol){
		printStreamSol.println("------  INVENTORY AT DEPOTS  -----");
		printStreamSol.print("Initial inventory:   ");
		for (int dIter = 0; dIter < this.openDepots.length; dIter++){
			if (!openDepots[dIter]) {
				printStreamSol.print(" -- \t");
			}
			else
				printStreamSol.print(this.instanceLIRP.getDepot(0, dIter).getInitialInventory()+"\t");	
		}
		printStreamSol.println();

		for (int t = 0;t < this.instanceLIRP.getNbPeriods(); t++){
			printStreamSol.print("Period "+ t +":   ");
			for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(0); dIter++){
				if (!openDepots[dIter]) {
					printStreamSol.print(" -- \t");
				}
				else	
					printStreamSol.print(stockDepot[dIter][t]+"\t");	
			}
			printStreamSol.println();
		}
	}

	/**
	 * Print the quantities delivered at the depots in every period
	 * @param printStreamSol
	 */
	public void printDeliveryDepot(PrintStream printStreamSol){

		printStreamSol.println("------  QUANTITY DELIVERED TO DEPOTS  -----");	
		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++){
			printStreamSol.print("Period "+ t +":   ");
			for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(0); dIter++) {
				if (!openDepots[dIter])
					printStreamSol.print(" -- \t");
				else {
					double totalQuantity = 0;
					for(int rIter = 0; rIter < this.usedRoutes[0].length; rIter++) 
						totalQuantity += this.deliveriesDepots[dIter][rIter][t];
					printStreamSol.print(totalQuantity + "\t");	
				}
			}
			printStreamSol.println();
		}

		printStreamSol.println("------  QUANTITY DELIVERED FROM DEPOTS to CLIENTS  -----");	
		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++){
			printStreamSol.println("Period "+t+":   ");
			for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(0); dIter++){
				if (this.openDepots[dIter]) {
					printStreamSol.print("Depot "+ dIter +": \t  ");
					for (int cIter = 0; cIter < this.instanceLIRP.getNbClients(); cIter++){
						double totalQuantity = 0;
						for(int rIter = 0; rIter < this.usedRoutes[1].length; rIter++) 
							totalQuantity += this.deliveriesClients[cIter][rIter][t];
						printStreamSol.print(totalQuantity + "\t");
					}
					printStreamSol.println();
				}
			}
			printStreamSol.println();
		}
	}

	/**
	 * Print the inventory at every client in every period
	 * @param printStreamSol
	 */
	public void printStockClient(PrintStream printStreamSol){
		printStreamSol.println("------ INVENTORY AT CLIENTS  -----");	
		printStreamSol.print("Initial inventory :");
		for (int cIter = 0; cIter < this.instanceLIRP.getNbClients(); cIter++){
			printStreamSol.print(this.instanceLIRP.getClient(cIter).getInitialInventory()+"\t");	
		}
		printStreamSol.println();

		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++){
			printStreamSol.print("Period "+t+":   ");
			for (int i = 0;i < this.instanceLIRP.getNbClients(); i++){
				printStreamSol.print(stockClient[i][t]+"\t");	
			}
			printStreamSol.println();
		}
	}

	public HashMap<Integer, ArrayList<Route>> collectUsedRoutes(){
		HashMap<Integer, ArrayList<Route>> collectedRoutes = new HashMap<Integer, ArrayList<Route>>();
		for(int lvl=0; lvl < Parameters.nb_levels; lvl++) {
			ArrayList<Route> routesLvl = new ArrayList<Route>();
			for (int rIter = 0; rIter < this.usedRoutes[lvl].length; rIter++){
				boolean used = false;
				int t = 0;
				while (!used && t < this.instanceLIRP.getNbPeriods()){
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
	 * @param printStreamSol
	 */
	public void printListOfRoutes(PrintStream printStreamSol){
		printStreamSol.println("------  LIST OF ROUTES  -----");	
		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++){
			printStreamSol.println("Period "+ t +":   ");
			for (int rIter = 0; rIter < this.usedRoutes[1].length; rIter++){
				if (this.usedRoutes[1][rIter][t]) {
					printStreamSol.print(" route " + rIter + " starting from depot " + this.instanceLIRP.getDepotIndex(0, this.routes[1][rIter].getStart()) + " and visiting clients : \t");
					for (int cIter = 0; cIter < this.instanceLIRP.getNbClients(); cIter++){ 
						if (this.deliveriesClients[cIter][rIter][t] > 0)
							printStreamSol.print(cIter + " \t ");
					}
					printStreamSol.println(" (cost = " + this.routes[1][rIter].getCost() + ") \t");
				}
			}
		}

		printStreamSol.println("------  SET OF ROUTES  -----");
		printStreamSol.print("[");
		boolean firstPeriod = true;
		for(int t = 0; t < this.instanceLIRP.getNbPeriods(); t++){
			if(!firstPeriod)
				printStreamSol.print(",");
			printStreamSol.print("[");
			boolean firstCustomer = true;
			for(int rIter = 0; rIter < this.routes[0].length; rIter++){
				if(this.usedRoutes[0][rIter][t]){
					if(!firstCustomer){
						printStreamSol.print(",");
					}
					printStreamSol.print(rIter);
					firstCustomer=false;
				}
			}
			printStreamSol.print("]");
			firstPeriod=false;
		}
		printStreamSol.println("]");
	}


	// Print the detail of each route 
	// TO BE MODIFIED
	public void printDetailedRoutes(PrintStream printStreamSol){

		printStreamSol.println("----  DETAILED DELIVERIES AT CLIENTS -------------------------");
		for (int i = 0; i < this.instanceLIRP.getNbClients(); i++){
			printStreamSol.println(" Client " + i + ": \t");
			for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++){
				printStreamSol.print("Period "+t+": ");
				for (int r = 0; r < this.routes[1].length; r++){
					double qu = this.deliveriesClients[i][r][t];
					if (qu>0){
						printStreamSol.print(" route " + r + "/ quantity = "+qu+ "\t");
					}
				}
				printStreamSol.println();
			}
			printStreamSol.println();
		}
	}	

	/**
	 * Recomputes the objective function from the Solution object attributes		
	 * @param printStreamSol
	 */
	public void evaluate(PrintStream printStreamSol){
		printStreamSol.println("-----  RECALCULATION OF THE OBJECTIVE FUNCTION  -----");
		double objective1 = 0;
		double objective2 = 0;
		double objective3 = 0;

		/* Fixed cost for opening the depots */
		for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(0); dIter++) {
			if(this.openDepots[dIter])
				objective1 += this.instanceLIRP.getDepot(0, dIter).getFixedCost(); 
		}

		/* Transportation cost */
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++) {
				for (int rIter = 0; rIter < this.routes[lvl].length; rIter++) {
					if(this.usedRoutes[lvl][rIter][t])
						objective2 += this.routes[0][rIter].getCost();
				}
			}
		}

		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++) {
			for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(0); dIter++) {
				objective3 += this.instanceLIRP.getDepot(0, dIter).getHoldingCost() * this.stockDepot[dIter][t];   
			}
			for (int cIter = 0; cIter < this.instanceLIRP.getNbClients(); cIter++) {
				objective3 += this.instanceLIRP.getClient(cIter).getHoldingCost() * this.stockClient[cIter][t];   
			}
		}

		printStreamSol.println("Fixed cost of opening depots: "+ objective1);
		printStreamSol.println("Cost of routes: "+ objective2);
		printStreamSol.println("Inventory costs: "+ objective3);

		double objective = objective1 + objective2 + objective3;
		printStreamSol.println("Objective function recalculated: "+ objective);
		this.objValue = objective;
	}

	/**
	 * 
	 * @param printStreamSol
	 */
	public void print(PrintStream printStreamSol) { 
		printOpenDepots(printStreamSol);
		printDeliveryDepot(printStreamSol);
		printStockDepot(printStreamSol);
		printStockClient(printStreamSol);
		printListOfRoutes(printStreamSol);
		// printDetailedRoutes(printStreamSol);
		evaluate(printStreamSol);
	}

}
