import java.io.PrintStream;


public class Solution {
	private Instance instance;
	//private Routes myRoutes;
	
	private int [] openDepots;
	private double[][] deliveryDepot;   // quantity delivered au site j le jour t
	private double[][] deliveryClient;   // quantity delivered to client i at period t
	private double[] stockInitialDepot;		// initial inventory at depots 
	private double[] stockInitialClient;		// initial inventory at clients
	private double[][] stockDepot;		// stock au site j le jour t
	private double[][] stockClient;		// stock au site j le jour t
	private double[][][] quantityDeliveredToClient;  // quantity delivered to client u by route r on period t
	private double[][] quantityDeliveredToDepot;  // quantity delivered to client u by route r on period t
	private double[][][] quantityDepotToClient; // quantity delivered from each depot to each client
	private int[][] listOfRoutes;		// list of route performed in each period
	private double[] coutRoute;  // cout de la route r le jour t. Vaut cout[r] si la route est faite, et 0 sinon.
	/*****************************************
	 *********** ACCESSEURS *****************
	 *****************************************/

	public double  getDeliveryDepot(int j, int t){
		return deliveryDepot[j][t];
	}
	
	public void setDeliveryDepot(int j, int t, int v){
		deliveryDepot[j][t]=v;
	}
	
	public void setStockDepot(int j, int t, double v){
		stockDepot[j][t]=v;
	}
	
	public void setStockClient(int j, int t, double v){
		stockClient[j][t]=v;
	}
	
	
	public void setCoutRoute(int r, double co){
		coutRoute[r] = co;
	}
	
	public int setOpenDepots(int j, int value){
	 return openDepots[j]=value ;	
	}
	
	public void setQuantityDeliveredToClient(int i, int r, int t, double q){
		quantityDeliveredToClient[i][r][t]=q;
	}
	
	
	public void setQuantityDeliveredToDepot(int j, int t, double q){
			quantityDeliveredToDepot[j][t]=q;
	}
	
	public void setQuantityDepotToClient(int i, int j, int t, double q){
		quantityDepotToClient[i][j][t]=q;
	}
		
	public void setListOfRoutes(int r, int t, int value){
			listOfRoutes[r][t]=value;	
	}
		
	public int getListOfRoutes(int r, int t){
			return listOfRoutes[r][t];	
	}
	
	public int getOpenDepots(int j){
		 return openDepots[j];	
	}
	
	
	public double getQuantityDeliveredToClient(int i, int r, int t){
			return quantityDeliveredToClient[i][r][t];
	}	
	
	public double getQuantityDeliveredToDepot(int j, int t){
			return quantityDeliveredToDepot[j][t];
	}
	
	public double getQuantityDepotToClient(int i, int j, int t){
			return quantityDepotToClient[i][j][t];
	}
	
	public double getStockDepot(int j, int t){
			return stockDepot[j][t];
	}
	
	public double getStockClient(int i, int t){
			return stockClient[i][t];
	}
	/*****************************************
	 *********** CONSTRUCTEUR ****************
	 *****************************************/
	public Solution(Instance instance, Routes myRoutes, int nbRoutes){
		this.instance = instance;
		
		 int J = instance.getNbDepots();  // number of depots
		 int T = instance.getNbPeriods(); // number of periods
		 int I = instance.getNbClients();  // number of clients
		 int R = myRoutes.getNbRoutes();  // number of routes
		
		// initialisation of all data
		openDepots = new int[J];
		deliveryDepot = new double[J][T];
		deliveryClient = new double[I][T];
		stockInitialDepot = new double[J];
		stockInitialClient = new double[I];
		stockDepot = new double[J][T];
		stockClient = new double[I][T];
		quantityDeliveredToClient = new double[I][R][T];
		quantityDeliveredToDepot = new double[J][T];
		quantityDepotToClient = new double[I][J][T];
		listOfRoutes = new int[R][T];
		coutRoute = new double[R];
		
		for (int j=0;j<J;j++){
			{
			  	openDepots[j] =-1;
			}
		}

		// initialisation of inventory at depots (-1)
		for (int j=0;j<J; j++){
			stockInitialDepot[j] =instance.getInventoryInitialDepot(j);
		}
		
		
		// intialisation of inventory at clients (-1)
		for (int i=0;i<I; i++){
			stockInitialClient[i] =instance.getInventoryInitialClient(i);
		}
				
		// initialisation of inventory at depots (-1)
		for (int t=0;t<T;t++){
			for (int j=0;j<J; j++){
				deliveryDepot[j][t] =-1;
				stockDepot[j][t] =-1;
			}
		}
		
		// intialisation of inventory at clients (-1)
		for (int t=0;t<T;t++){
			for (int i=0;i<I; i++){
				deliveryClient[i][t] =-1;
				stockClient[i][t] =-1;
			}
		}
		
		// initialisation  of routes
		for (int t=0;t<T;t++){
			for (int r=0;r<R; r++){
				listOfRoutes[r][t] =-1;	
				coutRoute[r] =-1;
			}
		}
					
		// initialisation of the quantities delivered to depots
		for (int j=0;j<J;j++){
			for (int t=0;t<T;t++){
				quantityDeliveredToDepot[j][t] =-1;
			}
		}
				
		// initialisation of the quantities delivered to clients
		for (int i=0;i<I;i++){
			for (int t=0;t<T;t++){
				for (int r=0;r<R; r++){
					quantityDeliveredToClient[i][r][t] =-1;
				}
			}
		}
		
		// initialisation of the quantities delivered from depots to clients
		for (int i=0;i<I;i++){
			for (int j=0;j<J;j++){
				for (int t=0;t<T; t++){
					quantityDepotToClient[i][j][t] =-1;
				}
			}
		}
	}
	
	/*************************************
	 *********** METHODES ****************
	 *************************************/
	
	// Print the open depots
	public void printOpenDepots(PrintStream printStreamSol){	
		
		 int J = instance.getNbDepots();  // number of depots
	
		printStreamSol.println("----DEPOTS ----------------------  ");
		printStreamSol.print("Open depots: \t");
		for (int j = 0; j<J; j++){
			if (openDepots[j]==1) 
			{
				printStreamSol.print(j+" \t  ");
			}
			
		}
		printStreamSol.print("Depots not selected: \t");
		for (int j = 0; j<J; j++){
			if (openDepots[j]==0) 
			{
				printStreamSol.print(j+" \t  ");
			}
			
		}
		printStreamSol.println();
		}
	
	
	// Print the inventory at depot for every period
	public void afficheStockDepot(PrintStream printStreamSol){
		
		 int J = instance.getNbDepots();  // number of depots
		 int T = instance.getNbPeriods(); // number of periods
	
	
		 printStreamSol.println("------ INVENTORY AT DEPOTS ---------");	
		
		 printStreamSol.print("Initial inventory :   ");
		for (int j=0;j<J; j++){
			if (openDepots[j]==0) {
				printStreamSol.print(" -- \t");
			}
			else
				printStreamSol.print(stockInitialDepot[j]+"\t");	
		}
		printStreamSol.println();
	
		for (int t=0;t<T;t++){
			printStreamSol.print("Period "+t+":   ");
			for (int j=0;j<J; j++){
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
		
		 int J = instance.getNbDepots();  // number of depots
		 int T = instance.getNbPeriods(); // number of periods
		 int I = instance.getNbClients();  // number of clients

		 printStreamSol.println("------ QUANTITY DELIVERED TO DEPOTS ---------");	
		for (int t=0;t<T;t++){
			printStreamSol.print("Period "+t+":   ");
			for (int j=0;j<J; j++){
						if (openDepots[j]==0) {
							printStreamSol.print(" -- \t");
						}
						else printStreamSol.print(quantityDeliveredToDepot[j][t]+"\t");	
			}
			printStreamSol.println();
		}
		
		printStreamSol.println("------ QUANTITY DELIVERED FROM DEPOTS to CLIENTS ---------");	
		for (int t=0;t<T;t++){
			printStreamSol.println("Period "+t+":   ");
			for (int j=0;j<J; j++){
				if (openDepots[j]==1) {
					printStreamSol.print("Depot "+j+": \t  ");
					for (int i=0;i<I; i++){
						printStreamSol.print(quantityDepotToClient[i][j][t]+"\t");
					}
				printStreamSol.println();
				}
			}
			printStreamSol.println();
		}
	}
	
		
	// Print the inventory at client for every period
	public void afficheStockClient(PrintStream printStreamSol){
		
		 int T = instance.getNbPeriods(); // number of periods
		 int I = instance.getNbClients();  // number of clients

			printStreamSol.println("------ INVENTORY AT CLIENTS ---------");	
			printStreamSol.print("Initial inventory :");
			for (int i=0;i<I; i++){
						printStreamSol.print(stockInitialClient[i]+"\t");	
			}
			printStreamSol.println();
			
			
			for (int t=0;t<T;t++){
				printStreamSol.print("Period "+t+":   ");
				for (int i=0;i<I; i++){
							printStreamSol.print(stockClient[i][t]+"\t");	
				}
				printStreamSol.println();
				}
		}
	
	// Print LIST AND SET OF ROUTES
	public void printListOfRoutes(Instance instance, Routes myRoutes, PrintStream printStreamSol){
					
		int T = instance.getNbPeriods(); // number of periods
		int R = myRoutes.getNbRoutes();  // number of routes
		int I = instance.getNbClients();  // number of clients

					 
		printStreamSol.println("------  LIST OF ROUTES ---------");	
			for (int t=0;t<T;t++){
				printStreamSol.println("Period "+t+":   ");
					for (int r=0;r<R; r++){
						if (listOfRoutes[r][t]==1) 
						{
							printStreamSol.print(" route " + r + " visiting clients : \t");
							for (int i=0;i<I;i++){
							double qu = quantityDeliveredToClient[i][r][t]; 
							if (qu>0){
							printStreamSol.print(i+" \t ");
							}
							}
							printStreamSol.println(" (cost = " + coutRoute[r] + ") \t");
						}
					}
				}
			
		printStreamSol.println("------  SET OF ROUTES ---------");
					printStreamSol.print("[");
					boolean firstPeriod = true;
					for(int t=0; t<T;t++){
						if(!firstPeriod){
							printStreamSol.print(",");
						}
						printStreamSol.print("[");
						boolean firstCustomer = true;
						for(int r=0;r<R;r++){
							if(listOfRoutes[r][t]==1){
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
		public void printDetailedRoutes(Routes myRoutes, PrintStream printStreamSol){
			 int T = instance.getNbPeriods(); // number of periods
			 int R = myRoutes.getNbRoutes();  // number of routes
			 int I = instance.getNbClients();  // number of clients

			printStreamSol.println("----  DETAILED DELIVERIES AT CLIENTS -------------------------");
			for (int i=0;i<I;i++){
				printStreamSol.println(" Client " + i + ": \t");
				for (int t=0;t<T;t++){
					printStreamSol.print("Period "+t+": ");
						for (int r=0;r<R; r++){
						double qu = quantityDeliveredToClient[i][r][t]; 
						//printStreamSol.println(qu);
						if (qu>0){
							printStreamSol.print(" route " + r + "/ quantity = "+qu+ "\t");
						}
					}
					printStreamSol.println();
				}
				printStreamSol.println();
			}
		}	
		
		
		
		// Methode permettant de recalculer la fonction objectif
		 
		public void evaluate(Routes myRoutes, PrintStream printStreamSol){
			
			 int J = instance.getNbDepots();  // number of depots
			 int T = instance.getNbPeriods(); // number of periods
			 int R = myRoutes.getNbRoutes();  // number of routes
			 int I = instance.getNbClients();  // number of clients

			printStreamSol.println("----------- RECALCULATION OF THE OBJECTIVE FUNCTION  ----- ");
			double objective1 = 0;
			double objective2 = 0;
			double objective3 = 0;
			double objective4 = 0;
			double objective5 = 0;

			for (int j = 0; j < J; j++) {
				objective1 = objective1 + instance.getFixedCost(j)*openDepots[j]; 
			}
			
			for (int j = 0; j <J ; j++) {
				if (openDepots[j]==1) {
					for (int t = 0; t < T; t++) {
						objective2 = objective2 + instance.getOrderCost(j) * deliveryDepot[j][t]*openDepots[j];
					}
				}
			}
				
			for (int t = 0; t < T; t++) {
				for (int r=0;r<R; r++){
					objective3 = objective3 + coutRoute[r]*listOfRoutes[r][t];   
				}
			}
			
			for (int j = 0; j < J; j++) {
				if (openDepots[j]==1) {
					for (int t = 0; t < T; t++) {
						objective4 = objective4 + instance.getHoldingCost(j)*stockDepot[j][t];   
					}
				}
			}
				
			for (int i = 0; i < I; i++) {
				for (int t = 0; t < T; t++) {
					objective5 = objective5 + instance.getHoldingCostClient()*stockClient[i][t];   
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

		
		
	// PRINT SOLUTION
	public void print(Routes myRoutes, PrintStream printStreamSol) { 
		printOpenDepots(printStreamSol);
		printDeliveryDepot(printStreamSol);
		afficheStockDepot(printStreamSol);
		afficheStockClient(printStreamSol);
		printListOfRoutes(instance, myRoutes, printStreamSol);
		printDetailedRoutes(myRoutes,printStreamSol);
		evaluate(myRoutes, printStreamSol);
		
	}
	
}
