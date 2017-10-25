import java.io.IOException;
import java.io.PrintStream;


import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.CpxModel.ModelIterator;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;


public class Solver{
	
	// TIME LIMIT FOR THE SOLVER
	private static final double TimeLimit = 300;  // in seconds
	//---------------------------------------------------

	public static Solution solve(Instance instance, Routes myRoutes, PrintStream printStreamSol) throws IloException, IOException {
	//==============================================================================
	// DATA
		
		int bigM = 1000;
		int I = instance.getNbClients();  // number of clients
		int J = instance.getNbDepots();  // number of depots
		int T = instance.getNbPeriods(); // number of periods
		int R = myRoutes.getNbRoutes();  // number of routes
		int CMAX = 4 ;
		
		// Definition of parameters Alpha and Beta

		int[][] Alpha = new int[I][R]; // =1 if client i is in route r
		int[][] Beta = new int[J][R]; // = 1 if depot j is in route r

		
		for (int r = 0; r < R; r++) {
			for (int i = 0; i < I; i++)				
				Alpha[i][r] = 0;
			for (int j = 0; j < J; j++)				
				Beta[j][r] = 0;
		}
		
		// cost of routes
		double[] costRoutes = new double[R]; 
		for (int r = 0; r < R; r++) {
			costRoutes[r] = myRoutes.getListRoutes(r,0);
		}
		
		
		// loading the starting point of each route
		int[] startRoute = new int[R];
		for (int r = 0; r < R; r++) {
			startRoute[r] = (int) myRoutes.getListRoutes(r, 1)-I;
		}
		
		// loading the clients of each route
		int[][] routeClients = new int[R][CMAX];
		for (int r = 0; r < R; r++) {
			for (int c = 0; c < CMAX; c++) {
				routeClients[r][c] = (int) myRoutes.getListRoutes(r, c+2);
			}
		}
		

		
		// Loop through the routes to initialize Alpha and Beta
		for (int r = 0; r < R; r++) {
			// For each client c, set Alpha[client][r]
			for (int c = 0; c < CMAX ; c++) {
				int cli = (int) myRoutes.getListRoutes(r, c+2);
					if (cli == -1)
					{
						break;
					}
				Alpha[cli][r]= 1;
			}
			
		// Check which depot is in the route and set Beta[depot][r]
			int depot = (int) myRoutes.getListRoutes(r, 1)-I;
			if (depot == -1) {
				break;
			}
			Beta[depot][r] = 1;
		}
		
		IloCplex solver = new IloCplex();
		solver.setParam(DoubleParam.TiLim, TimeLimit);

		//===================================================================
		// Definition of Boolean VARIABLES
		IloIntVar[] y = solver.boolVarArray(J);  // facility location variables (depots)
		
		IloIntVar[][] z = new IloIntVar[R][T];  // =1 if route r is performed on period t
		for (int r = 0; r < R; r++) {
			for (int t = 0; t < T; t++) {
				z[r][t] = solver.boolVar(); 
			}
		}
		
		IloIntVar[][] x = new IloIntVar[J][T];   // =1 is depot j is delivered on period t
		for (int j = 0; j < J; j++) {
			for (int t = 0; t < T; t++) {
				x[j][t] = solver.boolVar(); 
			}
		}

		
		// Definition of Continuous Variables
		
		IloNumVar[][] q = new IloNumVar[J][T]; 	// quantity delivered to j in t
		for (int j = 0; j < J; j++) {
			for (int t = 0; t < T; t++) {
				q[j][t] = solver.numVar(0, Double.MAX_VALUE);
			}
		}

		
		IloNumVar[][][] u = new IloNumVar[I][R][T]; // quantity delivered by route r to client i on period t
		for (int i = 0; i < I; i++) {
			for (int r = 0; r < R; r++) {
				for (int t = 0; t < T; t++) {
					u[i][r][t] = solver.numVar(0, Double.MAX_VALUE);
				}
			}
		}

		IloNumVar[][] InvClients = new IloNumVar[I][T]; // inventory  at clients
		for (int i = 0; i < I; i++) {
			for (int t = 0; t < T; t++) {
				InvClients[i][t] = solver.numVar(0, Double.MAX_VALUE);
			}
		}

		IloNumVar[][] InvDepots = new IloNumVar[J][T]; // inventory at depots
		for (int j = 0; j < J; j++) {
			for (int t = 0; t < T; t++) {
				InvDepots[j][t] = solver.numVar(0, Double.MAX_VALUE);
			}
		}

		IloNumVar obj = solver.numVar(0, Double.MAX_VALUE, "obj"); // objective function

		
		

		// constraint 2 // client is served by at most one route everyday // VALIDATED OP
		for (int i = 0; i < I; i++)
			for (int t = 0; t < T; t++) {
				IloLinearNumExpr expr = solver.linearNumExpr();
				for (int r = 0; r < R; r++) {
					expr.addTerm(Alpha[i][r], z[r][t]);
				}
				solver.addLe(expr, 1);
			}

		// Constraint 3 // Capacity constraint between the plant and DCs // VALIDATED OP
		for (int j = 0; j < J; j++) {
			for (int t = 0; t < T; t++) {
				IloLinearNumExpr expr = solver.linearNumExpr();
				expr.addTerm(1, q[j][t]);
				expr.addTerm(-instance.getCapacityVehicle(), x[j][t]);
				
				solver.addLe(expr, 0);
			}
		}

		// constraint 4 // VALIDATED OP
		for (int j = 0; j < J; j++) {
			for (int t = 0; t < T; t++) {
				IloLinearIntExpr expr = solver.linearIntExpr();
				expr.addTerm(1, x[j][t]);
				expr.addTerm(-1, y[j]);
				
				solver.addLe(expr, 0);
			}
		}

		// constraint 5 // VALIDATED OP
		for (int r = 0; r < R; r++) {
			for (int t = 0; t < T; t++) {
				IloLinearNumExpr expr = solver.linearNumExpr();
				for (int i = 0; i < I; i++) {
					expr.addTerm(1, u[i][r][t]);
				}
				
				expr.addTerm(-instance.getCapacityVehicle(), z[r][t]);
				solver.addLe(expr, 0);
			}
		}	

		//constraint 6 // Max number of vehicles between DCs and clients // VALIDATED OP		
		for (int t = 0; t < T; t++) {
			IloLinearNumExpr expr = solver.linearNumExpr();
			for (int r = 0; r < R; r++) { 
				expr.addTerm(1, z[r][t]);
			}
			solver.addLe(expr, instance.getNbVehicles());
		}
				
				
		// constraint 7  // Route r exist only if its starting point is not selected. // VALIDATED OP   
		for (int r = 0; r < R; r++) {
			for (int t = 0; t < T; t++) {
				IloLinearNumExpr expr = solver.linearNumExpr();
				expr.addTerm(1, z[r][t]);
				for (int j = 0; j < J; j++) {
					expr.addTerm(-Beta[j][r], y[j]);
				}
				solver.addLe(expr, 0);
			}
		}
		
		
		// constraint 8bis --------> between initial inventory and the first period (t=0) // VALIDATED OP
		for (int j = 0; j < J; j++)// for all j
		{
			IloLinearNumExpr expr = solver.linearNumExpr();
			expr.addTerm(1, InvDepots[j][0]);
			expr.addTerm(-1, q[j][0]);
			for (int r = 0; r < R; r++) {
				for (int i = 0; i < I; i++) // is it good ?
				{
					expr.addTerm(Beta[j][r], u[i][r][0]);
				}
			}
			solver.addEq(expr, instance.getInventoryInitialDepot(j));
		}
	
		
		// constraint 8 --------> between any period t>=1 and t-1 // VALIDATED OP
		for (int t = 1; t < T; t++) { 		// for all t
			for (int j = 0; j < J; j++)// for all j
			{
				IloLinearNumExpr expr = solver.linearNumExpr();
				expr.addTerm(1, InvDepots[j][t]);
				expr.addTerm(-1, InvDepots[j][t - 1]);
				expr.addTerm(-1, q[j][t]);

				for (int r = 0; r < R; r++) {
					for (int i = 0; i < I; i++) // is it good ?
					{
						expr.addTerm(Beta[j][r], u[i][r][t]);
					}
				}
				solver.addEq(expr, 0);
			}
		}

			
		// constraint 9bis inventory at clients, period 0 ---> 1 // VALIDATED OP
		for (int i = 0; i < I; i++)
		{
			IloLinearNumExpr expr = solver.linearNumExpr();
			expr.addTerm(1, InvClients[i][0]);
			for (int r = 0; r < R; r++) {
				expr.addTerm(-1, u[i][r][0]);
			}
			solver.addEq(expr, instance.getInventoryInitialClient(i)-instance.getDemand(i,0));
		}

			
				
		// constraint 9 inventory at clients, period t-1 ---> t  // VALIDATED OP
		for (int i = 0; i < I; i++)
		{
			for (int t = 1; t < T; t++) {
				IloLinearNumExpr expr = solver.linearNumExpr();
				expr.addTerm(1, InvClients[i][t]);
				expr.addTerm(-1, InvClients[i][t - 1]);
				for (int r = 0; r < R; r++) {
					expr.addTerm(-1, u[i][r][t]);
				}
				solver.addEq(expr, -instance.getDemand(i, t));
			}
		}
	
		
	
		// constraint 10 // Inventory at client is less than the sum of future demands
		// I am not sure this constraint is really useful, since optimal solutions won't contain over-stock. 
		for (int i = 0; i < I; i++) {
			//Set a variable for the remaining demand till the end of the horizon
			double remainingDemand = 0;
			for (int t2 = 0; t2 < T; t2++) {
				remainingDemand = remainingDemand + instance.getDemand(i, t2);
			}
			
			// Loop through the periods
			for (int t = 0; t < T; t++) {
				// remove the next demand to the remaining demand
				remainingDemand -= instance.getDemand(i,t);
				
				//Implement constraint 10 with the new value and add it to the model
				IloLinearNumExpr expr = solver.linearNumExpr();
				expr.addTerm(1, InvClients[i][t]);
				
				solver.addLe(expr, remainingDemand);
				solver.addLe(expr, instance.getCapacityClient());
				}
		}
		
		// constraint 10 : capacity constraints at client = less than their capacity // VALIDATED OP
				for (int t = 0; t < T; t++) {
					for (int i = 0; i < I; i++) {
						IloLinearNumExpr expr = solver.linearNumExpr();
						expr.addTerm(1, InvClients[i][t]);
						int capC = instance.getCapacityClient();
						solver.addLe(expr, capC);
						}
				}
		
		
		// constraint 11: capacity constraints at depots // VALIDATED OP
		for (int t = 0; t < T; t++) {
			for (int j = 0; j < J; j++) {
				IloLinearNumExpr expr = solver.linearNumExpr();
				expr.addTerm(1, InvDepots[j][t]);
				expr.addTerm(-instance.getCapacityDepot(), y[j]);
				solver.addLe(expr, 0);
			}
		}
				
		// constraint 12: if one customer does not belong to a route, then this route does not deliver it
		for (int r = 0; r < R; r++) {
			for (int t = 0; t < T; t++) {
				for (int i = 0; i < I; i++) {
					IloLinearNumExpr expr = solver.linearNumExpr();
					expr.addTerm(1, u[i][r][t]);
					solver.addLe(expr, bigM*Alpha[i][r]);
				}
			}
		}
	
	//---------------------------------------------------------------------------------------------------
		
	//VALID INEQUALITY: if a route is not performed, then it cannot deliver any customer 
		
		for (int r = 0; r < R; r++) {
			for (int t = 0; t < T; t++) {
				for (int i = 0; i < I; i++) {
					IloLinearNumExpr expr = solver.linearNumExpr();
					expr.addTerm(1, u[i][r][t]);
					expr.addTerm(-bigM , z[r][t]);
					solver.addLe(expr, 0);
				}
			}
		}
		
			
		
		//VALID INEQUALITY: Depot cannot send more than inventory at preceding period + flow at current period
		for (int t = 1; t < T; t++) { 		// for all t
			for (int j = 0; j < J; j++)// for all j
			{
				IloLinearNumExpr expr = solver.linearNumExpr();
				for (int r = 0; r < R; r++) {
		 			for (int i = 0; i < I; i++) // is it good ?
					{
					expr.addTerm(Beta[j][r], u[i][r][t]);
		 			}
				}
		 		expr.addTerm(-1, InvDepots[j][t - 1]);
		 		expr.addTerm(-1, q[j][t]);
		 		
					solver.addLe(expr, 0);
			}
		 
		 }
	//VALID INEQUALITY : Depot cannot send more than initial inventory + flow at period 0
		 	for (int j = 0; j < J; j++)// for all j
	 		{
		 			IloLinearNumExpr expr = solver.linearNumExpr();
		 			for (int r = 0; r < R; r++) {
		 				for (int i = 0; i < I; i++) // is it good ?
						{
		 					expr.addTerm(Beta[j][r], u[i][r][0]);
		 				}
		 				expr.addTerm(-1, q[j][0]);
		 				
		 			}
		 			solver.addLe(expr, instance.getInventoryInitialDepot(j));	
	 		}		
		
		
		
	// VALID INEQUALITY : customer satisfaction at period 0
        for (int i = 0; i < I; i++)
        {
         IloLinearNumExpr expr = solver.linearNumExpr();
                for (int r = 0; r < R; r++) {
                    for (int j = 0; j < J; j++) // is it good ?
                    {
                    expr.addTerm(Alpha[i][r] * Beta[j][r], u[i][r][0]);
                    }
                }

            solver.addGe(expr, instance.getDemand(i,0)-instance.getInventoryInitialClient(i));
         }

    // VALID INEQUALITY :customer satisfaction constraint
         for (int i = 0; i < I; i++)
         {
             for (int t = 1; t < T; t++)
             {
                 IloLinearNumExpr expr = solver.linearNumExpr();
                 expr.addTerm(1, InvClients[i][t - 1]);
                 for (int r = 0; r < R; r++) {
                    for (int j = 0; j < J; j++) // is it good ?
                    {
                    	expr.addTerm(Alpha[i][r] * Beta[j][r], u[i][r][t]);
                  }
                }
                solver.addGe(expr, instance.getDemand(i, t));
             }
         }

		
		
		
		// ===================
		// OBJECTIVE FUNCTION

		IloLinearNumExpr expr = solver.linearNumExpr();

		// term 1: fixed location costs
		for (int j = 0; j < J; j++) {
			expr.addTerm(instance.getFixedCost(j), y[j]); 
		}

		// 2nd term: cost of delivering depots
		for (int t = 0; t < T; t++) {
			for (int j = 0; j < J; j++) {
				expr.addTerm(instance.getOrderCost(j), x[j][t]);
			}
		}

		// 3rd term : routing costs from depots to clients
		for (int t = 0; t < T; t++) {
			for (int r = 0; r < R; r++) {
				expr.addTerm(costRoutes[r], z[r][t]);
			}
		}

		// 4th term inventory at depots
		for (int t = 0; t < T; t++) {
			for (int j = 0; j < J; j++) {
				expr.addTerm(instance.getHoldingCost(j), InvDepots[j][t]);
			}
		}
		
		// 5th term inventory at clients
				for (int t = 0; t < T; t++) {
					for (int i = 0; i < I; i++) {
						expr.addTerm(instance.getHoldingCostClient(), InvClients[i][t]);
					}
				}
		
		solver.addLe(expr, obj);
		solver.addObjective(IloObjectiveSense.Minimize, obj);
		solver.solve();
		solver.exportModel("Model.lp");
//		solver.getMIPRelativeGap();
		Solution sol  = new Solution(instance,myRoutes,R );
		if (solver.getStatus().equals("Infeasible"))
			System.out.println("There is no solution");
		else {
		
		
		
		//===============================================
		// SAVE THE OUTPUT OF THE SOLVER
		
		System.out.println();
		System.out.println("======== RESULTS  =================");
		System.out.println();
		System.out.print("Status of solver :   ");
		System.out.println(solver.getStatus());
		System.out.print("Objective function :   ");
		System.out.println(solver.getObjValue());		
		
	//-----------------------------------------------------	
		

		//save the status of depots (open/closed)
			for (int j = 0; j<J; j++){
				if (solver.getValue(y[j])>0) 
				{
					sol.setOpenDepots(j,1);
					//System.out.println("Depot open"+j);
				}
				else
				{
					sol.setOpenDepots(j,0);
					//System.out.println("Depot closed"+j);
				}
			}
		
		// save the deliveries to depots (1 if the depot is delivered, 0 otherwise)
		for (int t = 0; t<T; t++){
				for (int j = 0; j<J; j++){
				if (solver.getValue(x[j][t]) >0) {
					sol.setDeliveryDepot(j,t,1);
					}
				else {sol.setDeliveryDepot(j,t, 0);	}
			}
		}
		
		// save quantities delivered to depot
		for (int t = 0; t<T; t++){
			for (int j = 0; j<J; j++){
				double qjt = Math.round(solver.getValue(q[j][t]));
				sol.setQuantityDeliveredToDepot(j,t,qjt);
			}
		}
		
		// save the inventory variables
		for (int t = 0; t<T; t++){
			for (int j = 0; j<J; j++){
				sol.setStockDepot(j,t, solver.getValue(InvDepots[j][t]));
			}
		}
		
		// save the quantity delivered to clients
			for (int t=0;t<instance.getNbPeriods();t++){
				for (int i=0;i<instance.getNbClients();i++){
					for (int r=0;r<R; r++){
						double uirt = Math.round(solver.getValue(u[i][r][t]));
						sol.setQuantityDeliveredToClient(i,r,t, uirt);			
				}
			}
		}
		
		// save the quantity delivered from each depot to each client
		for (int t=0;t<instance.getNbPeriods();t++){
			for (int j=0;j<instance.getNbDepots();j++){
				for (int i=0;i<instance.getNbClients(); i++){
					double flow = 0;
					for (int r=0;r<R; r++){
						//System.out.println("Route " +r+ " Brj = "+B[r][j]+" Ari= "+A[r][i]);
						flow = flow + Beta[j][r]*Alpha[i][r]*solver.getValue(u[i][r][t]); // see the sum in constraints (8)  
					}
					flow =  Math.round(flow);
					sol.setQuantityDepotToClient(i,j,t,flow);
				}
			}
		}
		
		//-------------------------------------------------
		// save inventory at clients for every period t>=1
		for (int t = 0; t<T; t++){
			for (int i = 0; i<I; i++){
				double stcli =  solver.getValue(InvClients[i][t]);
				stcli = Math.round(stcli);
				sol.setStockClient(i, t, stcli);
			}
		}
			
		//save the routes performed in each period
		for (int t = 0; t<T; t++){
			for (int r = 0; r<R; r++){
				if (solver.getValue(z[r][t]) >0) {
					sol.setListOfRoutes(r, t, 1);
				}
				else sol.setListOfRoutes(r, t, 0);
			}
		}
		
		// save the route costs
		for (int r = 0; r<R; r++){
				sol.setCoutRoute(r, costRoutes[r]);
			}
		
//		Checker checker = new Checker();
//		checker.check(sol, instance, myRoutes, printStreamSol);
		printStreamSol.println("--------------------------------------------");
		}
return sol;
	
}


	
}
		
	
		
	
	
