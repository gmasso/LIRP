import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Instance {

	private int nbDepots;
	private int nbPeriods;
	private int nbClients; 
	private int nbSites; 
	 
	private double[][]Demand; //demand for each customer in each period 
	private int[]FixedCost; //cost of opening for candidate depot NAME
	private int[]OrderCost; //cost of ordering from plant to depot
	private int K; 			// max number of vehicle (constraint 7)
	private int vehicleCapacity; //capacity of each vehicle 
	public int []InventoryInitialDepot ;//initial inventory at depot j
	public int []InventoryInitialClient ;// initial inventory at customer i 
	private double [] holdingCost; //holding cost for depot i in period t   it should be in double
	private int[][]coord;
	private int capacityDepot;
	private int capacityClient;
	private double holdingCostClient;

	
	
	
	/*****************************************
	 *********** ACCESSEURS *****************
	 *****************************************/
	
	public int getNbDepots() { 
		return nbDepots;
	}
	
	public int getNbClients() { 
		return nbClients;
	}
	
	public int getNbPeriods() {
		return nbPeriods;
	}
	
	public int getNbSites() {
			return nbSites;
	}
	
	public int getCapacityVehicle() {
		return vehicleCapacity;
	}
	
	public int getCapacityDepot() {
		return capacityDepot;
	}
	
	public int getCapacityClient() {
		return capacityClient;
	}
	
	public void setNbDepots(int nbDepots) {     
		this.nbDepots = nbDepots;
	}
	

	public void setNbClients(int nbClients) {     
		this.nbClients = nbClients;
	}
	
	public void setNbPeriods (int nbPeriods) {
		this.nbPeriods = nbPeriods;
	}
	

	public double getDemand (int i, int t) {
		return Demand[i][t];
	}
	
	public double getFixedCost (int j) { 
		return FixedCost[j];
	}
	
	public double getOrderCost (int j) {
		return OrderCost[j];
	}
	
	public double getHoldingCost (int j) { 
		return holdingCost[j];
	}
	
	public double getHoldingCostClient() { 
		return holdingCostClient;
	}
	
	
	public double getInventoryInitialDepot (int j) { 
		return InventoryInitialDepot[j];
	}
	
	public double getInventoryInitialClient (int i) { 
		return InventoryInitialClient[i];
	}
	
		
	public int getNbVehicles() { //max number of vehicles
		return K;
	}
	
	public int getCoord(int i, int xy)  { // returns the coordinates
		return coord[i][xy];
	}
	
		
	/*****************************************
	 *********** CONSTRUCTEUR ****************
	 *****************************************/

	/**
	 * Constructeur d'une instance � partir d'un fichier de donn�es
	 * @param nomFichier
	 * @throws IOException
	 */
	public Instance(String nomFichier) throws IOException {

		File mfile = new File(nomFichier); //create a new file with the name 
		if (!mfile.exists()) {
			throw new IOException("Le fichier saisi : " + nomFichier + ", n'existe pas."); //translate the file 
		}
		Scanner sc = new Scanner(mfile); //reads the data file line by line ; sc name of the scanner

		nbDepots = sc.nextInt();	sc.nextLine();   //nb sites , next integer value it scans ! storing the values from the file
		nbClients = sc.nextInt(); sc.nextLine(); 
		nbPeriods = sc.nextInt();	sc.nextLine();
		K = sc.nextInt();	sc.nextLine();
		nbSites = nbClients + nbDepots; 
				
		vehicleCapacity = sc.nextInt();	sc.nextLine(); //vehicle capacity
		
		capacityDepot= sc.nextInt();	sc.nextLine(); 
		capacityClient= sc.nextInt();	sc.nextLine();  
		
		// fixed cost of opening each depots
		FixedCost = new int[nbDepots];
		for (int i=0;i<nbDepots;i++){
			FixedCost[i]= sc.nextInt();
			}
		sc.nextLine();
		
		// ordering cost for each depots
		OrderCost = new int[nbDepots];
		for (int i=0;i<nbDepots;i++){
			OrderCost[i]= sc.nextInt();
			}
		sc.nextLine();
		
		
		InventoryInitialDepot = new int[nbDepots];  //initial inventory at each depot j  
		for (int i=0;i<nbDepots;i++){
				InventoryInitialDepot[i]= sc.nextInt();
		}
		sc.nextLine();
		
		InventoryInitialClient = new int[nbClients];  //initial inventory at each depot j  
		for (int i=0;i<nbClients;i++){
				InventoryInitialClient[i]= sc.nextInt();
		}
		sc.nextLine();
		
		 
		holdingCost = new double[nbDepots];  //holding cost   convert to double
		for (int i=0;i<nbDepots;i++){
				holdingCost[i]= sc.nextInt();
				holdingCost[i] = holdingCost[i];
				sc.nextLine();
		}
		
		holdingCostClient =  sc.nextInt();	sc.nextLine(); 
		holdingCostClient = holdingCostClient/100;
	
		
		Demand = new double[nbClients][nbPeriods];  //for demand
		for (int i=0;i<nbClients;i++){
			for (int j=0;j<nbPeriods;j++){
				Demand[i][j]= sc.nextInt();
			}
			sc.nextLine();
		}

		// to take the coordinates [i][0], [i][1]] 
		coord = new int[nbClients + nbDepots+1][2];
		for (int i=0;i<nbClients + nbDepots+1;i++){
			for (int j=0;j<2;j++){
				coord[i][j]= sc.nextInt();
			}
			sc.nextLine();
		}
		
		int code = sc.nextInt(); 
	
		if (code==9999){
			System.out.println("Instance file correctly read: OK\t");	 //to see if we read the file 
		} 
		
		sc.close();
	}

	
	
	
	
	}
