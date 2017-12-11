package solverLIRP;
import java.io.PrintStream;

import instanceManager.Instance;


public class Solution {

	/* ============================
	 *         ATTRIBUTES
	 ==============================*/
	private Instance instanceLIRP;
	private Route[] routesSD;
	private Route[] routesDC;
	
	/* Depots */
	private boolean[] openDepots;       			// Binary indicating whether a depot is opened or not
	private double[][][] deliveriesDepots;   // Quantity delivered to the depots in period t via route r
	private double[][] stockDepot;			// Stock at depot j in period t

	/* Clients */
	private double[][][] deliveriesClients;	// Quantity delivered to the clients i in period t via route r
	private double[][] stockClient;			// Stock at client i in period t

	/* Routes */
	private boolean[][] usedSDRoutes; 				// Binary indicating whether route r between the supplier and depots is used in period t
	private boolean[][] usedDCRoutes;				// Binary indicating whether route r between the depots and clients is used in period t

	/*==============================
	 *        CONSTRUCTEUR  
	 ===============================*/
	public Solution(Instance instance, Route[] routesSD, Route[] routesDC){

		/* Initialization of the attributes */
		this.instanceLIRP = instance;
		this.routesSD = routesSD;
		this.routesDC = routesDC;
		
		/* Variables of the model */
		this.openDepots = new boolean[instance.getNbDepots()];
		this.deliveriesDepots = new double[instance.getNbDepots()][routesSD.length][instance.getNbPeriods()];
		this.stockDepot = new double[instance.getNbDepots()][instance.getNbPeriods()];
		this.deliveriesClients = new double[instance.getNbClients()][routesDC.length][instance.getNbPeriods()];
		this.stockClient = new double[instance.getNbClients()][instance.getNbPeriods()];
		this.usedSDRoutes = new boolean[routesSD.length][instance.getNbPeriods()];
		this.usedDCRoutes = new boolean[routesDC.length][instance.getNbPeriods()];

		/* Initialization of the values of the variables for the depots */
		for (int dIter = 0; dIter < instance.getNbDepots(); dIter++) {
			this.openDepots[dIter] = false;
			for (int t = 0; t < instance.getNbPeriods(); t++){
				this.stockDepot[dIter][t] = 0;
				for(int rIter = 0; rIter < routesSD.length; rIter++) {
					this.deliveriesDepots[dIter][rIter][t] = 0;
				}
			}
		}

		/* Initialization of the values of the variables for the clients */
		for (int cIter = 0; cIter < instance.getNbClients(); cIter++){
			for (int t = 0; t < instance.getNbPeriods(); t++) {
				this.stockClient[cIter][t] = 0;
				for(int rIter = 0; rIter < routesDC.length; rIter++) {
					this.deliveriesClients[cIter][rIter][t] = 0;
				}
			}
		}

		/* Initialization of the boolean values for the depots-clients routes binary variables */
		for (int rIter = 0; rIter < usedSDRoutes.length; rIter++){
			for (int t = 0;t < instance.getNbPeriods(); t++){
				usedSDRoutes[rIter][t] = false;
			}
		}
		
		/* Initialization of the boolean values for the supplier-depots routes binary variables */
		for (int rIter = 0; rIter < usedDCRoutes.length; rIter++){
			for (int t = 0;t < instance.getNbPeriods(); t++){
				usedDCRoutes[rIter][t] = false;
			}
		}
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
	 * @param rIndex	the route index in the routesSD array
	 * @return		the quantity delivered to depot dIndex by route rIndex in period t
	 */
	public double getDeliveryDepot(int dIndex, int rIndex, int t){
		return this.deliveriesDepots[dIndex][rIndex][t];
	}

	/**
	 * 
	 * @param cIndex	the index of the client of interest
	 * @param t		the period index
	 * @param rIndex	the route index in the routesSD array
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
	 * @param rIndex
	 * @param t
	 * @param isUsed
	 */
	public void setusedSDRoutes(int rIndex, int t, boolean isUsed){
		this.usedSDRoutes[rIndex][t] = isUsed;	
	}

	/**
	 * 
	 * @param rIndex
	 * @param t
	 * @param isUsed
	 */
	public void setusedDCRoutes(int rIndex, int t, boolean isUsed){
		this.usedDCRoutes[rIndex][t] = isUsed;	
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
				printStreamSol.print(this.instanceLIRP.getDepot(dIter).getInitialInventory()+"\t");	
		}
		printStreamSol.println();

		for (int t = 0;t < this.instanceLIRP.getNbPeriods(); t++){
			printStreamSol.print("Period "+ t +":   ");
			for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(); dIter++){
				if (!openDepots[dIter]) {
					printStreamSol.print(" -- \t");
				}
				else	printStreamSol.print(stockDepot[dIter][t]+"\t");	
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
			for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(); dIter++) {
				if (!openDepots[dIter])
					printStreamSol.print(" -- \t");
				else {
					double totalQuantity = 0;
					for(int rIter = 0; rIter < this.usedSDRoutes.length; rIter++) 
						totalQuantity += this.deliveriesDepots[dIter][rIter][t];
					printStreamSol.print(totalQuantity + "\t");	
				}
			}
			printStreamSol.println();
		}

		printStreamSol.println("------  QUANTITY DELIVERED FROM DEPOTS to CLIENTS  -----");	
		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++){
			printStreamSol.println("Period "+t+":   ");
			for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(); dIter++){
				if (this.openDepots[dIter]) {
					printStreamSol.print("Depot "+ dIter +": \t  ");
					for (int cIter = 0; cIter < this.instanceLIRP.getNbClients(); cIter++){
						double totalQuantity = 0;
						for(int rIter = 0; rIter < this.usedDCRoutes.length; rIter++) 
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

	/**
	 * 
	 * @param printStreamSol
	 */
	public void printListOfRoutes(PrintStream printStreamSol){
		printStreamSol.println("------  LIST OF ROUTES  -----");	
		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++){
			printStreamSol.println("Period "+ t +":   ");
			for (int rIter = 0; rIter < this.usedDCRoutes.length; rIter++){
				if (this.usedDCRoutes[rIter][t]) {
					printStreamSol.print(" route " + rIter + " starting from depot " + this.instanceLIRP.getDepotIndex(this.routesDC[rIter].getStart()) + " and visiting clients : \t");
					for (int cIter = 0; cIter < this.instanceLIRP.getNbClients(); cIter++){ 
						if (this.deliveriesClients[cIter][rIter][t] > 0)
							printStreamSol.print(cIter + " \t ");
					}
					printStreamSol.println(" (cost = " + this.routesDC[rIter].getCost() + ") \t");
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
			for(int rIter = 0; rIter < this.routesSD.length; rIter++){
				if(this.usedSDRoutes[rIter][t]){
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
				for (int r = 0; r < this.routesDC.length; r++){
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
		for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(); dIter++) {
			if(this.openDepots[dIter])
				objective1 += this.instanceLIRP.getDepot(dIter).getFixedCost(); 
		}

		/* Transportation cost */
		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++) {
			for (int rIter = 0; rIter < this.routesSD.length; rIter++) {
				if(this.usedSDRoutes[rIter][t])
					objective2 += this.routesSD[rIter].getCost();
			}
			for (int rIter = 0; rIter < this.routesDC.length; rIter++) {
				if(this.usedDCRoutes[rIter][t])
					objective2 += this.routesDC[rIter].getCost();
			}
		}

		for (int t = 0; t < this.instanceLIRP.getNbPeriods(); t++) {
			for (int dIter = 0; dIter < this.instanceLIRP.getNbDepots(); dIter++) {
				objective3 += this.instanceLIRP.getDepot(dIter).getHoldingCost() * this.stockDepot[dIter][t];   
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
