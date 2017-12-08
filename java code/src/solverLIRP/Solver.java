package solverLIRP;
import java.io.PrintStream;

import javax.swing.JFileChooser;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;

import instanceManager.Instance;
import instanceManager.Parameters;

public class Solver{

	public static Solution solve(Instance LIRPInstance, RouteManager availableRoutes, PrintStream printStreamSol) throws IloException {

		/* Data */
		int nbClients = LIRPInstance.getNbClients();  // number of clients
		int nbDepots = LIRPInstance.getNbDepots();  // number of depots
		int nbPeriods = LIRPInstance.getNbPeriods(); // number of periods
		Route[] routesSD = availableRoutes.getSDRoutes(); // array of routes from the supplier to the depot
		Route[] routesDC = availableRoutes.getDCRoutes(); // array of available routes from the depots to the clients

		/* Definition of parameters Alpha and Beta (for the depots-clients routes)*/
		int[][] Alpha = new int[nbClients][routesDC.length]; // = 1 if client i is in route r
		int[][] Beta = new int[nbDepots][routesDC.length]; // = 1 if depot j is in route r
		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		for(int rIter = 0; rIter < routesDC.length; rIter++) {
			for(int cIter = 0; cIter < nbClients; cIter++)
				Alpha[cIter][rIter] = routesDC[rIter].containsLocation(LIRPInstance.getClient(cIter)) ? 1 : 0;
			for(int dIter = 0; dIter < nbDepots; dIter++)
				Beta[dIter][rIter] = routesDC[rIter].containsLocation(LIRPInstance.getDepot(dIter)) ? 1 : 0;
		}

		/* Definition of parameters Gamma (for the supplier-depots routes) */
		int[][] Gamma = new int[nbDepots][routesSD.length]; // = 1 if depot j is in route r
		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		for(int rIter = 0; rIter < routesSD.length; rIter++) {
			for(int dIter = 0; dIter < nbDepots; dIter++)
				Gamma[dIter][rIter] = routesSD[rIter].containsLocation(LIRPInstance.getDepot(dIter)) ? 1 : 0;
		}
		
		/* CPLEX solver */
		IloCplex LIRPSolver = new IloCplex();
		// Set the time limit
		LIRPSolver.setParam(DoubleParam.TiLim, Parameters.TimeLimit);

		/*===============
		 *   VARIABLES 
		 ================*/
		/* Boolean */
		IloIntVar[] y = LIRPSolver.boolVarArray(nbDepots);  // facility location variables (depots)
		// Integer variables
		IloIntVar[][] x = new IloIntVar[routesSD.length][nbPeriods];   // = 1 is route r is delivered on period t (supplier-depot)
		IloIntVar[][] z = new IloIntVar[routesDC.length][nbPeriods];  // = 1 if route r is used on period t (depot-clients)
		/* Continuous */
		IloNumVar[][][] u = new IloNumVar[nbClients][routesDC.length][nbPeriods]; // quantity delivered by route r to client i on period t
		IloNumVar[][][] v = new IloNumVar[nbDepots][routesSD.length][nbPeriods]; // quantity delivered by route r to depot j on period t
		IloNumVar[][] InvClients = new IloNumVar[nbClients][nbPeriods]; // inventory  at clients
		IloNumVar[][] InvDepots = new IloNumVar[nbDepots][nbPeriods]; // inventory at depots

		/* Initialization */
		for(int t = 0; t < nbPeriods; t++) {
			for(int rIter = 0; rIter < routesSD.length; rIter++){
				x[rIter][t] = LIRPSolver.boolVar();
				for(int dIter = 0; dIter < nbDepots; dIter++)
					v[dIter][rIter][t] = LIRPSolver.numVar(0, Double.MAX_VALUE);
			}
			for(int rIter = 0; rIter < routesDC.length; rIter++) {
				z[rIter][t] = LIRPSolver.boolVar();
				for(int cIter = 0; cIter < nbClients; cIter++) 
					u[cIter][rIter][t] = LIRPSolver.numVar(0, Double.MAX_VALUE);
			}
			for(int cIter = 0; cIter < nbClients; cIter++)
				InvClients[cIter][t] = LIRPSolver.numVar(0, Double.MAX_VALUE);
			for(int dIter = 0; dIter < nbDepots; dIter++)
				InvDepots[dIter][t] = LIRPSolver.numVar(0, Double.MAX_VALUE);
		}

		/*=================
		 *   CONSTRAINTS 
		 ==================*/
		for (int t = 0; t < nbPeriods; t++) {
			/* Constraints on the route for depots */
			for(int dIter = 0; dIter < nbDepots; dIter++) {
				/* Each depot is served by at most one route in every period (2) */
				IloLinearNumExpr expr2 = LIRPSolver.linearNumExpr();
				for (int rIter = 0; rIter < routesSD.length; rIter++) 
					expr2.addTerm(Beta[dIter][rIter], x[rIter][t]);
				LIRPSolver.addLe(expr2, 1);
			}

			/* Each client is served by at most one route in every period (3)*/
			for(int cIter = 0; cIter < nbClients; cIter++) {
				IloLinearNumExpr expr3 = LIRPSolver.linearNumExpr();
				for (int rIter = 0; rIter < routesDC.length; rIter++)
					expr3.addTerm(Alpha[cIter][rIter], z[rIter][t]);
				LIRPSolver.addLe(expr3, 1);
			}

			for(int dIter = 0; dIter < nbDepots; dIter++) {
				for (int rIter = 0; rIter < routesSD.length; rIter++) {
					/* A supplier-depot route is used only if all depots on this route are opened (4)*/
					IloLinearNumExpr expr4 = LIRPSolver.linearNumExpr();
					expr4.addTerm(Gamma[dIter][rIter], x[rIter][t]);
					expr4.addTerm(-1, y[dIter]);
					LIRPSolver.addLe(expr4, 0);
				}
			}

			/* Each route delivering customers can only departs from an opened depot (5)*/
			for (int rIter = 0; rIter < routesDC.length; rIter++) {
				IloLinearNumExpr expr5 = LIRPSolver.linearNumExpr();
				expr5.addTerm(1, z[rIter][t]);
				for(int dIter = 0; dIter < nbDepots; dIter++) {
					expr5.addTerm(-Beta[dIter][rIter], y[dIter]);
				}
				LIRPSolver.addLe(expr5, 0);
			}

			/* Vehicle capacity on each delivery on each opened supplier-depot route (6)*/
			for (int rIter = 0; rIter < routesSD.length; rIter++) {
				IloLinearNumExpr expr6 = LIRPSolver.linearNumExpr();
				for(int dIter = 0; dIter < nbDepots; dIter++)
					expr6.addTerm(1, v[dIter][rIter][t]);
				// TO MODIFY IN THE FINAL VERSION
				expr6.addTerm(-LIRPInstance.getCapacityVehicle(0), x[rIter][t]);
				LIRPSolver.addLe(expr6, 0);
			}


			/* Vehicle capacity on each delivery on each opened depot-client route (7)*/
			for (int rIter = 0; rIter < routesDC.length; rIter++) {
				IloLinearNumExpr expr7 = LIRPSolver.linearNumExpr();
				for(int cIter = 0; cIter < nbClients; cIter++)
					expr7.addTerm(1, u[cIter][rIter][t]);
				// TO MODIFY IN THE FINAL VERSION
				expr7.addTerm(-LIRPInstance.getCapacityVehicle(0), x[rIter][t]);
				LIRPSolver.addLe(expr7, 0);
			}

			/* Flow conservation at the depots (8) */
			for(int dIter = 0; dIter < nbDepots; dIter++) {
				IloLinearNumExpr expr8 = LIRPSolver.linearNumExpr();
				expr8.addTerm(1, InvDepots[dIter][t]);
				double rhs8 = 0;
				if(t == 0)
					rhs8 = LIRPInstance.getDepot(dIter).getInitialInventory();
				else
					expr8.addTerm(-1, InvDepots[dIter][t-1]);
				for(int rSDIter = 0; rSDIter < routesSD.length; rSDIter++)
					expr8.addTerm(-1, v[dIter][rSDIter][t]);
				for (int rIter = 0; rIter < routesDC.length; rIter++) {
					for (int cIter = 0; cIter < nbClients; cIter++)
						expr8.addTerm(Beta[dIter][rIter], u[cIter][rIter][t]);
				}
				LIRPSolver.addEq(expr8, rhs8);
			}
			
			/* Flow conservation at the clients (9) */
			for (int cIter = 0; cIter < nbClients; cIter++){
				IloLinearNumExpr expr9 = LIRPSolver.linearNumExpr();
				expr9.addTerm(1, InvClients[cIter][t]);
				double rhs9 = -LIRPInstance.getClient(cIter).getDemand(t);
				if (t==0)
					rhs9 += LIRPInstance.getClient(cIter).getInitialInventory();
				else
					expr9.addTerm(-1, InvClients[cIter][t - 1]);
				for (int rIter = 0; rIter < routesDC.length; rIter++) {
					expr9.addTerm(-1, u[cIter][rIter][t]);
				}
				LIRPSolver.addEq(expr9, rhs9);
			}

			/* Stock capacity at the client or ensuring that the inventory is not greater than the sum of remaining demands (10) */
			// IS THIS LAST ASPECT OF THE CONSTRAINT REALLY USEFUL? 
			for (int cIter = 0; cIter < nbClients; cIter++) {
				double remainingDemand = LIRPInstance.getClient(cIter).getCumulDemands(t+1, nbPeriods);
				IloLinearNumExpr expr10 = LIRPSolver.linearNumExpr();
				expr10.addTerm(1, InvClients[cIter][t]);
				if(remainingDemand < LIRPInstance.getClient(cIter).getCapacity())
					LIRPSolver.addLe(expr10, remainingDemand);
				else
					LIRPSolver.addLe(expr10, LIRPInstance.getClient(cIter).getCapacity());
			}

			/* Capacity constraints at depots (11) */
			for (int dIter = 0; dIter < nbDepots; dIter++) {
				IloLinearNumExpr expr11 = LIRPSolver.linearNumExpr();
				expr11.addTerm(1, InvDepots[dIter][t]);
				LIRPSolver.addLe(expr11, LIRPInstance.getDepot(dIter).getCapacity());
			}
		}

		/*=======================
		 *  OBJECTIVE FUNCTION 
		 ========================*/
		IloNumVar obj = LIRPSolver.numVar(0, Double.MAX_VALUE, "obj"); // objective function

		/* Definition of the objective function */
		IloLinearNumExpr objExpr = LIRPSolver.linearNumExpr();

		/* Fixed opening costs */
		for (int dIter = 0; dIter < nbDepots; dIter++) {
			objExpr.addTerm(LIRPInstance.getDepot(dIter).getFixedCost(), y[dIter]);
		}
		for (int t = 0; t < nbPeriods; t++) {
			/* Delivering costs for the depots */
			for (int rSDIter = 0; rSDIter < routesSD.length; rSDIter++)
				objExpr.addTerm(routesSD[rSDIter].getCost(), x[rSDIter][t]);
			/* Delivering costs for the clients */
			for (int rDCIter = 0; rDCIter < routesDC.length; rDCIter++)
				objExpr.addTerm(routesDC[rDCIter].getCost(), z[rDCIter][t]);
			/* Holding cost incurred by the depots */
			for (int dIter = 0; dIter < nbDepots; dIter++)
				objExpr.addTerm(LIRPInstance.getDepot(dIter).getHoldingCost(), InvDepots[dIter][t]);
			/* Holding cost incurred by the clients */
			for (int cIter = 0; cIter < nbClients; cIter++)
				objExpr.addTerm(LIRPInstance.getClient(cIter).getHoldingCost(), InvClients[cIter][t]);
		}
		
		/*================
		 *   RESOLUTION 
		 =================*/
		LIRPSolver.addLe(objExpr, obj);
		LIRPSolver.addObjective(IloObjectiveSense.Minimize, obj);
		LIRPSolver.solve();
		
		
		/*===============================
		*     SAVE THE SOLVER OUTPUT
		=================================*/
		Solution sol  = new Solution(LIRPInstance, availableRoutes);
		if (LIRPSolver.getStatus().equals(IloCplex.Status.Infeasible))
			System.out.println("There is no solution");
		else {

			System.out.println();
			System.out.println("===========  RESULTS  ===========");
			System.out.println();
			System.out.print("Status of LIRPSolver :   ");
			System.out.println(LIRPSolver.getStatus());
			System.out.print("Objective function :   ");
			System.out.println(LIRPSolver.getObjValue());

			/*=======================
			 *    SOLUTION VALUES
			 ========================*/
			//-----------------------------------------------------	
			/* Save the status of depots (open/closed) */
			for (int dIter = 0; dIter < nbDepots; dIter++){
				if (LIRPSolver.getValue(y[dIter])>0)
					sol.setOpenDepot(dIter, 1);
				else
					sol.setOpenDepot(dIter,0);
			}

			// Save the deliveries to depots (1 if the depot is delivered, 0 otherwise)
			for (int t = 0; t < nbPeriods; t++){
				for (int dIter = 0; dIter < nbDepots; dIter++){
					if (LIRPSolver.getValue(x[dIter][t]) >0) {
						sol.setDeliveryDepot(dIter,t,1);
					}
					else {sol.setDeliveryDepot(dIter,t, 0);	}
				}
			}

			// Save quantities delivered to depot
			for (int t = 0; t < nbPeriods; t++){
				for (int dIter = 0; dIter < nbDepots; dIter++){
					double qjt = Math.round(LIRPSolver.getValue(q[dIter][t]));
					sol.setDeliveryDepot(dIter,t,qjt);
				}
			}

			// Save the inventory variables
			for (int t = 0; t < nbPeriods; t++){
				for (int dIter = 0; dIter < nbDepots; dIter++){
					sol.setStockDepot(dIter,t, LIRPSolver.getValue(InvDepots[dIter][t]));
				}
			}

			// Save the quantity delivered to clients
			for (int t = 0;t < nbPeriods; t++){
				for (int cIter = 0;cIter <nbClients; cIter++){
					for (int rIter = 0; rIter < nbRoutes; rIter++){
						double uirt = Math.round(LIRPSolver.getValue(u[cIter][rIter][t]));
						sol.setQuantityDeliveredToClient(cIter, rIter, t, uirt);			
					}
				}
			}

			// Save the quantity delivered from each depot to each client
			for (int t=0; t < nbPeriods; t++){
				for (int dIter = 0; dIter < nbDepots; dIter++){
					for (int cIter = 0; cIter < nbClients; cIter++){
						double flow = 0;
						for (int rIter = 0; rIter < nbRoutes; rIter++){
							//System.out.println("Route " +r+ " Brj = "+B[r][j]+" Ari= "+A[r][i]);
							flow = flow + Beta[dIter][rIter] * Alpha[cIter][rIter] * LIRPSolver.getValue(u[cIter][rIter][t]); // See the sum in constraints (8)  
						}
						flow =  Math.round(flow);
						sol.setQuantityDepotToClient(cIter, dIter, t, flow);
					}
				}
			}

			//-------------------------------------------------
			// Save inventory at clients for every period t>=1
			for (int t = 0; t < nbPeriods; t++){
				for (int cIter = 0; cIter < nbClients; cIter++){
					double stcli =  LIRPSolver.getValue(InvClients[cIter][t]);
					stcli = Math.round(stcli);
					sol.setStockClient(cIter, t, stcli);
				}
			}

			// Save the routes performed in each period
			for (int t = 0; t < nbPeriods; t++){
				for (int rIter = 0; rIter < nbRoutes; rIter++){
					if (LIRPSolver.getValue(z[rIter][t]) >0)
						sol.setListOfRoutes(rIter, t, 1);
					else 
						sol.setListOfRoutes(rIter, t, 0);
				}
			}

			// Save the route costs
			for (int rIter = 0; rIter < nbRoutes; rIter++){
				sol.setRouteCost(rIter, availableRoutes[rIter].getCost());
			}
			Checker checker = new Checker();
			checker.check(sol, LIRPInstance, availableRoutes, printStreamSol);
			//		MathHeuristic mathHeuristic = new MathHeuristic();
			//		mathHeuristic.ArrayListofRoutes(myRoutes, printStreamSol);
			printStreamSol.println("--------------------------------------------");
		}
		return sol;

	}
}


