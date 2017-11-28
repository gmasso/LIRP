package solverLIRP;
import java.io.PrintStream;

import instanceManager.Instance;


public class Solution {
	// The instance to which the solution relates
	private Instance instance;
	// The set of route that can be used
	private Route[] routes;

	// Variables related to depots
	private int[] openDepots;       		// Binary indicating whether depot j is opened or not
	private double[][][] deliveriesDepots;   // Quantity delivered to depot j in period t via route r
	private double[] initialStockDepots; 	// Initial inventory on hand at depot j at the beginning of the planning horizon
	private double[][] stockDepot;			// Stock at depot j in period t

	// Variables related to the clients
	private double[][][] deliveriesClients;	// Quantity delivered to client i in period t via route r
	private double[] initialStockClients;	// Initial inventory on hand at client i at the beginning of the planning horizon
	private double[][] stockClient;			// Stock at client i in period t

	// Variables related to routes
	private int[][] usedRoutes; 				// Binary indicating whether route r is used in period t


	//private double[][][] quantityDepotToClient; // quantity delivered from each depot to each client
	//private int[][] listOfRoutes;		// list of route performed in each period
	//private double[] coutRoute;  // cout de la route r le jour t. Vaut cout[r] si la route est faite, et 0 sinon.

	/*****************************************
	 *********** CONSTRUCTEUR ****************
	 *****************************************/
	public Solution(Instance instance, Route[] availableRoutes){
		this.instance = instance;
		this.routes = availableRoutes;

		// Initialization of all data
		this.openDepots = new int[instance.getNbDepots()];
		this.deliveriesDepots = new double[instance.getNbDepots()][instance.getNbPeriods()][availableRoutes.length];
		this.stockDepot = new double[instance.getNbDepots()][instance.getNbPeriods()];
		this.deliveriesClients = new double[instance.getNbClients()][instance.getNbPeriods()][availableRoutes.length];
		this.stockClient = new double[instance.getNbClients()][instance.getNbPeriods()];
		this.usedRoutes = new int[this.routes.length][instance.getNbPeriods()];

		// Initialization of variables for the depots 
		for (int j=0;j<instance.getNbDepots(); j++) {
			this.openDepots[j] = 0;
			this.initialStockDepots[j] = instance.getDepot(j).getInitialInventory();
			for (int t=0;t<instance.getNbPeriods();t++){
				this.stockDepot[j][t] = -1;
				for(int r = 0; r < availableRoutes.length; r++) {
					this.deliveriesDepots[j][t][r] = -1;
				}
			}
		}

		// Initialization of variables for the clients
		for (int i = 0; i < instance.getNbClients(); i++){
			this.initialStockClients[i] =instance.getClient(i).getInitialInventory();
			for (int t = 0; t < instance.getNbPeriods(); t++) {
				this.stockClient[i][t] = -1;
				for(int r = 0; r < availableRoutes.length; r++) {
					this.deliveriesClients[i][t][r] = -1;
				}
			}
		}

		// Initialization of the routes
		for (int r = 0; r < availableRoutes.length; r++){
			for (int t = 0;t < instance.getNbPeriods(); t++){
				usedRoutes[r][t] = 0;
			}
		}
	}

	/*****************************************
	 ************* ACCESSORS *****************
	 *****************************************/

	public int getOpenDepots(int j){
		return this.openDepots[j];	
	}

	public double  getDeliveryDepot(int j, int t, int r){
		return this.deliveriesDepots[j][t][r];
	}

	public double getDeliveryClient(int i, int t, int r){
		return this.deliveriesClients[i][t][r];
	}

	public double getStockDepot(int j, int t){
		return this.stockDepot[j][t];
	}

	public double getStockClient(int i, int t){
		return this.stockClient[i][t];
	}

	/*****************************************
	 ************** MUTATORS *****************
	 *****************************************/


	public void setOpenDepot(int j, int isOpened){
		this.openDepots[j] = isOpened;	
	}

	public void setStockDepot(int j, int t, double v){
		this.stockDepot[j][t] = v;
	}

	public void setDeliveryDepot(int j, int t, int r, double v){
		this.deliveriesDepots[j][t][r] = v;
	}

	public void setStockClient(int i, int t, double v){
		this.stockClient[i][t] = v;
	}

	public void setDeliveryClient(int i, int t, int r, double q){
		this.deliveriesClients[i][t][r] = q;
	}

	public void setusedRoutes(int r, int t, int value){
		this.usedRoutes[r][t] = value;	
	}

	/*************************************
	 *********** METHODES ****************
	 *************************************/

	// Print the open depots
	public void printOpenDepots(PrintStream printStreamSol){	

		int nbDepots = instance.getNbDepots();  // number of depots

		printStreamSol.println("----DEPOTS ----------------------  ");
		printStreamSol.print("Open depots: \t");
		for (int j = 0; j<nbDepots; j++){
			if (openDepots[j]==1) 
			{
				printStreamSol.print(j+" \t  ");
			}

		}
		printStreamSol.print("Depots not selected: \t");
		for (int j = 0; j<nbDepots; j++){
			if (openDepots[j]==0) 
			{
				printStreamSol.print(j+" \t  ");
			}

		}
		printStreamSol.println();
	}


	// Print the inventory at depot for every period
	public void printStockDepot(PrintStream printStreamSol){

		printStreamSol.println("------ INVENTORY AT DEPOTS ---------");	

		printStreamSol.print("Initial inventory :   ");
		for (int j = 0; j < this.instance.getNbDepots(); j++){
			if (openDepots[j]==0) {
				printStreamSol.print(" -- \t");
			}
			else
				printStreamSol.print(this.instance.getDepot(j).getInitialInventory()+"\t");	
		}
		printStreamSol.println();

		for (int t=0;t<instance.getNbPeriods();t++){
			printStreamSol.print("Period "+t+":   ");
			for (int j = 0; j < this.instance.getNbDepots(); j++){
				if (openDepots[j]==0) {
					printStreamSol.print(" -- \t");
				}
				else	printStreamSol.print(stockDepot[j][t]+"\t");	
			}
			printStreamSol.println();
		}
	}


	// Print the quantities delivered at depots at every period
	public void printDeliveryDepot(PrintStream printStreamSol){

		printStreamSol.println("------ QUANTITY DELIVERED TO DEPOTS ---------");	
		for (int t = 0; t < instance.getNbPeriods(); t++){
			printStreamSol.print("Period "+ t +":   ");
			for (int j = 0; j < this.instance.getNbDepots(); j++) {
				if (openDepots[j]==0) {
					printStreamSol.print(" -- \t");
				}
				else {
					double totalQuantity = 0;
					for(int r = 0; r < this.routes.length; r++) 
						totalQuantity += this.deliveriesDepots[j][t][r];
					printStreamSol.print(totalQuantity + "\t");	
				}
			}
			printStreamSol.println();
		}

		printStreamSol.println("------ QUANTITY DELIVERED FROM DEPOTS to CLIENTS ---------");	
		for (int t = 0; t < instance.getNbPeriods(); t++){
			printStreamSol.println("Period "+t+":   ");
			for (int j = 0; j < this.instance.getNbDepots(); j++){
				if (this.openDepots[j] == 1) {
					printStreamSol.print("Depot "+j+": \t  ");
					for (int i = 0; i < this.instance.getNbClients(); i++){
						double totalQuantity = 0;
						for(int r = 0; r < this.routes.length; r++) 
							totalQuantity += this.deliveriesClients[i][t][r];
						printStreamSol.print(totalQuantity + "\t");
					}
					printStreamSol.println();
				}
			}
			printStreamSol.println();
		}
	}


	// Print the inventory at client for every period
	public void printStockClient(PrintStream printStreamSol){

		printStreamSol.println("------ INVENTORY AT CLIENTS ---------");	
		printStreamSol.print("Initial inventory :");
		for (int i = 0; i < this.instance.getNbClients(); i++){
			printStreamSol.print(this.instance.getClient(i).getInitialInventory()+"\t");	
		}
		printStreamSol.println();


		for (int t=0;t<instance.getNbPeriods();t++){
			printStreamSol.print("Period "+t+":   ");
			for (int i = 0;i < this.instance.getNbClients(); i++){
				printStreamSol.print(stockClient[i][t]+"\t");	
			}
			printStreamSol.println();
		}
	}

	// Print LIST AND SET OF ROUTES
	public void printListOfRoutes(Instance instance, Route[] myRoutes, PrintStream printStreamSol){
		printStreamSol.println("------  LIST OF ROUTES ---------");	
		for (int t = 0; t < instance.getNbPeriods(); t++){
			printStreamSol.println("Period "+t+":   ");
			for (int r = 0; r < this.routes.length; r++){
				if (this.usedRoutes[r][t] == 1) 
				{
					printStreamSol.print(" route " + r + " visiting clients : \t");
					for (int i = 0; i < this.instance.getNbClients(); i++){
						double qu = this.deliveriesClients[i][t][r]; 
						if (qu > 0){
							printStreamSol.print(i+" \t ");
						}
					}
					printStreamSol.println(" (cost = " + this.routes[r].getCost() + ") \t");
				}
			}
		}

		printStreamSol.println("------  SET OF ROUTES ---------");
		printStreamSol.print("[");
		boolean firstPeriod = true;
		for(int t=0; t<instance.getNbPeriods();t++){
			if(!firstPeriod){
				printStreamSol.print(",");
			}
			printStreamSol.print("[");
			boolean firstCustomer = true;
			for(int  r= 0; r < this.routes.length; r++){
				if(this.usedRoutes[r][t]==1){
					if(!firstCustomer){
						printStreamSol.print(",");
					}
					printStreamSol.print(r);
					firstCustomer=false;
				}
			}
			printStreamSol.print("]");
			firstPeriod=false;
		}
		printStreamSol.println("]");
	}


	// Print the detail of each route 
	public void printDetailedRoutes(Route[] myRoutes, PrintStream printStreamSol){

		printStreamSol.println("----  DETAILED DELIVERIES AT CLIENTS -------------------------");
		for (int i = 0; i < this.instance.getNbClients(); i++){
			printStreamSol.println(" Client " + i + ": \t");
			for (int t = 0; t < this.instance.getNbPeriods(); t++){
				printStreamSol.print("Period "+t+": ");
				for (int r = 0; r < myRoutes.length; r++){
					double qu = this.deliveriesClients[i][t][r];
					if (qu>0){
						printStreamSol.print(" route " + r + "/ quantity = "+qu+ "\t");
					}
				}
				printStreamSol.println();
			}
			printStreamSol.println();
		}
	}	



	// Method to recompute the objective function
	public void evaluate(Route[] myRoutes, PrintStream printStreamSol){
		printStreamSol.println("----------- RECALCULATION OF THE OBJECTIVE FUNCTION  ----- ");
		double objective1 = 0;
		double objective2 = 0;
		double objective3 = 0;
		double objective4 = 0;
		double objective5 = 0;

		for (int j = 0; j < this.instance.getNbDepots(); j++) {
			objective1 = objective1 + instance.getDepot(j).getFixedCost() * openDepots[j]; 
		}

		for (int j = 0; j < this.instance.getNbDepots() ; j++) {
			if (openDepots[j]==1) {
				for (int t = 0; t < this.instance.getNbPeriods(); t++) {
					objective2 = objective2 + instance.getDepot(j).getOrderingCost() * this.deliveriesDepots[j][t][r] * this.openDepots[j];
				}
			}
		}

		for (int t = 0; t < this.instance.getNbPeriods(); t++) {
			for (int r = 0; r < this.routes.length; r++){
				objective3 = objective3 + this.routes[r].getCost() * this.usedRoutes[r][t];   
			}
		}

		for (int j = 0; j < this.instance.getNbDepots(); j++) {
			if (openDepots[j]==1) {
				for (int t = 0; t < this.instance.getNbPeriods(); t++) {
					objective4 = objective4 + this.instance.getDepot(j).getHoldingCost() * this.stockDepot[j][t];   
				}
			}
		}

		for (int i = 0; i < this.instance.getNbClients(); i++) {
			for (int t = 0; t < this.instance.getNbPeriods(); t++) {
				objective5 = objective5 + this.instance.getClient(i).getHoldingCost() * this.stockClient[i][t];   
			}
		}

		printStreamSol.println("Fixed cost of opening depots: "+ objective1);
		printStreamSol.println("Cost of delivering depots: "+ objective2);
		printStreamSol.println("Cost of routes: "+ objective3);
		printStreamSol.println("Inventory cost at depots: "+ objective4);
		printStreamSol.println("Inventory cost at clients: "+ objective5);

		double objective = objective1+objective2+objective3+objective4+objective5;
		printStreamSol.println("Objective function recalculated: "+ objective);
	}



	// PRINnbPeriods SOLUTION
	public void print(Route[] myRoutes, PrintStream printStreamSol) { 
		printOpenDepots(printStreamSol);
		printDeliveryDepot(printStreamSol);
		printStockDepot(printStreamSol);
		printStockClient(printStreamSol);
		printListOfRoutes(instance, myRoutes, printStreamSol);
		printDetailedRoutes(myRoutes,printStreamSol);
		evaluate(myRoutes, printStreamSol);

	}

}
