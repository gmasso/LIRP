package solverLIRP;
import java.io.PrintStream;

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

	private IloCplex LIRPSolver;
	private Instance LIRPInstance;
	private Route[] routesSD;
	private Route[] routesDC;

	/*===============
	 *   VARIABLES 
	 ================*/
	/* Boolean */
	private IloIntVar[] y;  // facility location variables (depots)
	// Integer variables
	private IloIntVar[][] x;   // = 1 is route r is delivered on period t (supplier-depot)
	private IloIntVar[][] z;  // = 1 if route r is used on period t (depot-clients)
	/* Continuous */
	private IloNumVar[][][] u; // quantity delivered by route r to client i on period t
	private IloNumVar[][][] v; // quantity delivered by route r to depot j on period t
	private IloNumVar[][] InvClients; // inventory  at clients
	private IloNumVar[][] InvDepots; // inventory at depots

	private boolean isSolved; // States if the MIP has been solved or not

	/**
	 * Creates a Solver object for the LIRP instance, setting the variables, available routes and the CPLEX model
	 * @param LIRPInstance		the instance upon which is built the model	
	 * @param availableRoutes	the direct and multi-stops routes that are available in this model
	 * @throws IloException
	 */
	public Solver(Instance LIRPInstance, RouteManager availableRoutes) throws IloException {

		/* Data */
		this.LIRPInstance = LIRPInstance;
		this.routesSD = availableRoutes.getSDRoutes(); // array of routes from the supplier to the depot
		this.routesDC = availableRoutes.getDCRoutes(); // array of available routes from the depots to the clients

		int nbClients = this.LIRPInstance.getNbClients();  // number of clients
		int nbDepots = this.LIRPInstance.getNbDepots();  // number of depots
		int nbPeriods = this.LIRPInstance.getNbPeriods(); // number of periods

		/* Definition of parameters Alpha and Beta (for the depots-clients routes)*/
		int[][] Alpha = new int[nbClients][this.routesDC.length]; // = 1 if client i is in route r
		int[][] Beta = new int[nbDepots][this.routesDC.length]; // = 1 if depot j is in route r
		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		for(int rIter = 0; rIter < this.routesDC.length; rIter++) {
			for(int cIter = 0; cIter < nbClients; cIter++)
				Alpha[cIter][rIter] = this.routesDC[rIter].containsLocation(this.LIRPInstance.getClient(cIter)) ? 1 : 0;
			for(int dIter = 0; dIter < nbDepots; dIter++)
				Beta[dIter][rIter] = this.routesDC[rIter].containsLocation(this.LIRPInstance.getDepot(dIter)) ? 1 : 0;
		}

		/* Definition of parameters Gamma (for the supplier-depots routes) */
		int[][] Gamma = new int[nbDepots][this.routesSD.length]; // = 1 if depot j is in route r
		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		for(int rIter = 0; rIter < this.routesSD.length; rIter++) {
			for(int dIter = 0; dIter < nbDepots; dIter++)
				Gamma[dIter][rIter] = this.routesSD[rIter].containsLocation(this.LIRPInstance.getDepot(dIter)) ? 1 : 0;
		}

		this.isSolved = false;

		/* CPLEX solver */
		this.LIRPSolver = new IloCplex();
		// Set the time limit
		this.LIRPSolver.setParam(DoubleParam.TiLim, Parameters.TimeLimit);

		/* Initialization of the variables */
		this.y = this.LIRPSolver.boolVarArray(nbDepots);
		this.x = new IloIntVar[this.routesSD.length][nbPeriods];
		this.z = new IloIntVar[this.routesDC.length][nbPeriods];
		this.u = new IloNumVar[nbClients][this.routesDC.length][nbPeriods];
		this.v = new IloNumVar[nbDepots][this.routesSD.length][nbPeriods];
		this.InvClients = new IloNumVar[nbClients][nbPeriods];
		this.InvDepots = new IloNumVar[nbDepots][nbPeriods]; 

		for(int t = 0; t < nbPeriods; t++) {
			for(int rIter = 0; rIter < this.routesSD.length; rIter++){
				this.x[rIter][t] = this.LIRPSolver.boolVar();
				for(int dIter = 0; dIter < nbDepots; dIter++)
					this.v[dIter][rIter][t] = this.LIRPSolver.numVar(0, Double.MAX_VALUE);
			}
			for(int rIter = 0; rIter < this.routesDC.length; rIter++) {
				this.z[rIter][t] = this.LIRPSolver.boolVar();
				for(int cIter = 0; cIter < nbClients; cIter++) 
					this.u[cIter][rIter][t] = this.LIRPSolver.numVar(0, Double.MAX_VALUE);
			}
			for(int cIter = 0; cIter < nbClients; cIter++)
				this.InvClients[cIter][t] = this.LIRPSolver.numVar(0, Double.MAX_VALUE);
			for(int dIter = 0; dIter < nbDepots; dIter++)
				this.InvDepots[dIter][t] = this.LIRPSolver.numVar(0, Double.MAX_VALUE);
		}

		/*=================
		 *   CONSTRAINTS 
		 ==================*/
		for (int t = 0; t < nbPeriods; t++) {
			/* Constraints on the route for depots */
			for(int dIter = 0; dIter < nbDepots; dIter++) {
				/* Each depot is served by at most one route in every period (2) */
				IloLinearIntExpr expr2 = this.LIRPSolver.linearIntExpr();
				for (int rIter = 0; rIter < this.routesSD.length; rIter++) 
					expr2.addTerm(Gamma[dIter][rIter], this.x[rIter][t]);
				this.LIRPSolver.addLe(expr2, 1);
			}

			/* Each client is served by at most one route in every period (3)*/
			for(int cIter = 0; cIter < nbClients; cIter++) {
				IloLinearIntExpr expr3 = this.LIRPSolver.linearIntExpr();
				for (int rIter = 0; rIter < this.routesDC.length; rIter++)
					expr3.addTerm(Alpha[cIter][rIter], this.z[rIter][t]);
				this.LIRPSolver.addLe(expr3, 1);
			}

			for(int dIter = 0; dIter < nbDepots; dIter++) {
				for (int rIter = 0; rIter < this.routesSD.length; rIter++) {
					/* A supplier-depot route is used only if all depots on this route are opened (4)*/
					IloLinearIntExpr expr4 = this.LIRPSolver.linearIntExpr();
					expr4.addTerm(Gamma[dIter][rIter], this.x[rIter][t]);
					expr4.addTerm(-1, this.y[dIter]);
					this.LIRPSolver.addLe(expr4, 0);
				}
			}

			/* Each route delivering customers can only departs from an opened depot (5)*/
			for (int rIter = 0; rIter < this.routesDC.length; rIter++) {
				IloLinearIntExpr expr5 = this.LIRPSolver.linearIntExpr();
				expr5.addTerm(1, this.z[rIter][t]);
				for(int dIter = 0; dIter < nbDepots; dIter++) {
					expr5.addTerm(-Beta[dIter][rIter], this.y[dIter]);
				}
				this.LIRPSolver.addLe(expr5, 0);
			}

			/* Vehicle capacity on each delivery on each opened supplier-depot route (6)*/
			for (int rIter = 0; rIter < this.routesSD.length; rIter++) {
				IloLinearNumExpr expr6 = this.LIRPSolver.linearNumExpr();
				for(int dIter = 0; dIter < nbDepots; dIter++)
					expr6.addTerm(1, this.v[dIter][rIter][t]);
				// TO MODIFY IN THE FINAL VERSION
				expr6.addTerm(-this.LIRPInstance.getCapacityVehicle(0), this.x[rIter][t]);
				this.LIRPSolver.addLe(expr6, 0);
			}


			/* Vehicle capacity on each delivery on each opened depot-client route (7)*/
			for (int rIter = 0; rIter < this.routesDC.length; rIter++) {
				IloLinearNumExpr expr7 = this.LIRPSolver.linearNumExpr();
				for(int cIter = 0; cIter < nbClients; cIter++)
					expr7.addTerm(1, this.u[cIter][rIter][t]);
				// TO MODIFY IN THE FINAL VERSION
				expr7.addTerm(-this.LIRPInstance.getCapacityVehicle(0), this.z[rIter][t]);
				this.LIRPSolver.addLe(expr7, 0);
			}

			/* Flow conservation at the depots (8) */
			for(int dIter = 0; dIter < nbDepots; dIter++) {
				IloLinearNumExpr expr8 = this.LIRPSolver.linearNumExpr();
				expr8.addTerm(1, this.InvDepots[dIter][t]);
				double rhs8 = 0;
				if(t == 0)
					rhs8 = this.LIRPInstance.getDepot(dIter).getInitialInventory();
				else
					expr8.addTerm(-1, this.InvDepots[dIter][t-1]);
				for(int rSDIter = 0; rSDIter < this.routesSD.length; rSDIter++)
					expr8.addTerm(-1, this.v[dIter][rSDIter][t]);
				for (int rIter = 0; rIter < this.routesDC.length; rIter++) {
					for (int cIter = 0; cIter < nbClients; cIter++)
						expr8.addTerm(Beta[dIter][rIter], this.u[cIter][rIter][t]);
				}
				this.LIRPSolver.addEq(expr8, rhs8);
			}

			/* Flow conservation at the clients (9) */
			for (int cIter = 0; cIter < nbClients; cIter++){
				IloLinearNumExpr expr9 = this.LIRPSolver.linearNumExpr();
				expr9.addTerm(1, this.InvClients[cIter][t]);
				double rhs9 = -this.LIRPInstance.getClient(cIter).getDemand(t);
				if (t==0)
					rhs9 += this.LIRPInstance.getClient(cIter).getInitialInventory();
				else
					expr9.addTerm(-1, this.InvClients[cIter][t - 1]);
				for (int rIter = 0; rIter < this.routesDC.length; rIter++) {
					expr9.addTerm(-1, this.u[cIter][rIter][t]);
				}
				this.LIRPSolver.addEq(expr9, rhs9);
			}

			/* Stock capacity at the client or ensuring that the inventory is not greater than the sum of remaining demands (10) */
			// IS THIS LAST ASPECT OF THE CONSTRAINT REALLY USEFUL? 
			for (int cIter = 0; cIter < nbClients; cIter++) {
				double remainingDemand = this.LIRPInstance.getClient(cIter).getCumulDemands(t+1, nbPeriods);
				IloLinearNumExpr expr10 = this.LIRPSolver.linearNumExpr();
				expr10.addTerm(1, this.InvClients[cIter][t]);
				if(remainingDemand < this.LIRPInstance.getClient(cIter).getCapacity())
					this.LIRPSolver.addLe(expr10, remainingDemand);
				else
					this.LIRPSolver.addLe(expr10, this.LIRPInstance.getClient(cIter).getCapacity());
			}

			/* Capacity constraints at depots (11) */
			for (int dIter = 0; dIter < nbDepots; dIter++) {
				IloLinearNumExpr expr11 = this.LIRPSolver.linearNumExpr();
				expr11.addTerm(1, this.InvDepots[dIter][t]);
				this.LIRPSolver.addLe(expr11, this.LIRPInstance.getDepot(dIter).getCapacity());
			}
		}
	}

	/*=======================
	 *  OBJECTIVE FUNCTION 
		 ========================*/
	/**
	 * Solves the MIP related to the LIRP, setting the value of all the variables
	 * @throws IloException
	 */
	private void solveMIP() throws IloException {

		IloNumVar obj = this.LIRPSolver.numVar(0, Double.MAX_VALUE, "obj"); // objective function

		/* Definition of the objective function */
		IloLinearNumExpr objExpr = this.LIRPSolver.linearNumExpr();

		/* Fixed opening costs */
		for (int dIter = 0; dIter < this.LIRPInstance.getNbDepots(); dIter++) {
			objExpr.addTerm(this.LIRPInstance.getDepot(dIter).getFixedCost(), this.y[dIter]);
		}
		for (int t = 0; t < this.LIRPInstance.getNbPeriods(); t++) {
			/* Delivering costs for the depots */
			for (int rSDIter = 0; rSDIter < this.routesSD.length; rSDIter++)
				objExpr.addTerm(this.routesSD[rSDIter].getCost(), this.x[rSDIter][t]);
			/* Delivering costs for the clients */
			for (int rDCIter = 0; rDCIter < this.routesDC.length; rDCIter++)
				objExpr.addTerm(this.routesDC[rDCIter].getCost(), this.z[rDCIter][t]);
			/* Holding cost incurred by the depots */
			for (int dIter = 0; dIter < this.LIRPInstance.getNbDepots(); dIter++)
				objExpr.addTerm(this.LIRPInstance.getDepot(dIter).getHoldingCost(), this.InvDepots[dIter][t]);
			/* Holding cost incurred by the clients */
			for (int cIter = 0; cIter < this.LIRPInstance.getNbClients(); cIter++)
				objExpr.addTerm(this.LIRPInstance.getClient(cIter).getHoldingCost(), this.InvClients[cIter][t]);
		}

		/*================
		 *   RESOLUTION 
		 =================*/
		this.LIRPSolver.addLe(objExpr, obj);
		this.LIRPSolver.addObjective(IloObjectiveSense.Minimize, obj);
		this.LIRPSolver.solve();

		this.isSolved = true;
	}

	/**
	 * Creates a Solution object from the results of the solver on the LIRP problem considered
	 * @param printStreamSol	the stream on which to print the solution
	 * @return				the solution obtained from
	 */
	public Solution getSolution(PrintStream printStreamSol) throws IloException {

		/*===============================
		 *     SAVE THE SOLVER OUTPUT
		=================================*/
		Solution sol  = new Solution(this.LIRPInstance, this.routesSD, this.routesDC);

		if(!this.isSolved)
			this.solveMIP();

		if (this.LIRPSolver.getStatus().equals(IloCplex.Status.Infeasible))
			System.out.println("There is no solution");
		else {
			System.out.println();
			System.out.println("===========  RESULTS  ===========");
			System.out.println();
			System.out.print("Status of this.LIRPSolver :   ");
			System.out.println(this.LIRPSolver.getStatus());
			System.out.print("Objective function :   ");
			System.out.println(this.LIRPSolver.getObjValue());

			/*=======================
			 *    SOLUTION VALUES
			 ========================*/
			//-----------------------------------------------------	
			/* Save the status of depots (open/closed) */
			for (int dIter = 0; dIter < this.LIRPInstance.getNbDepots(); dIter++){
				if (this.LIRPSolver.getValue(this.y[dIter])>0)
					sol.setOpenDepot(dIter, true);
				else
					sol.setOpenDepot(dIter, false);
			}

			/* Save the deliveries to depots */
			for (int t = 0; t < this.LIRPInstance.getNbPeriods(); t++){
				for (int dIter = 0; dIter < this.LIRPInstance.getNbDepots(); dIter++){
					for(int rIter = 0; rIter < this.routesSD.length; rIter++)
						if (this.LIRPSolver.getValue(this.x[rIter][t]) > 0) {
							sol.setusedSDRoutes(rIter, t, true);
							double vjrt = this.LIRPSolver.getValue(this.v[dIter][rIter][t]);
							sol.setDeliveryDepot(dIter, rIter, t, vjrt);
						}
						else {
							sol.setusedSDRoutes(rIter, t, false);
							sol.setDeliveryDepot(dIter, rIter, t, 0);
						}
				}
			}

			/* Save the inventory variables */
			for (int t = 0; t < this.LIRPInstance.getNbPeriods(); t++){
				for (int dIter = 0; dIter < this.LIRPInstance.getNbDepots(); dIter++){
					sol.setStockDepot(dIter,t, Math.round(this.LIRPSolver.getValue(this.InvDepots[dIter][t])));
				}
			}

			/* Save the quantity delivered to clients */
			for (int t = 0;t < this.LIRPInstance.getNbPeriods(); t++){
				for (int cIter = 0;cIter < this.LIRPInstance.getNbClients(); cIter++){
					for (int rIter = 0; rIter < this.routesDC.length; rIter++){
						if (this.LIRPSolver.getValue(this.x[rIter][t]) > 0) {
							sol.setusedDCRoutes(rIter, t, true);
							double uirt = Math.round(this.LIRPSolver.getValue(this.u[cIter][rIter][t]));
							sol.setDeliveryClient(cIter, rIter, t, uirt);			
						}
						else {
							sol.setusedDCRoutes(rIter, t, false);
							sol.setDeliveryClient(cIter, rIter, t, 0);
						}
					}
				}
			}

			/* Save inventory at clients for every period t */
			for (int t = 0; t < this.LIRPInstance.getNbPeriods(); t++){
				for (int cIter = 0; cIter < this.LIRPInstance.getNbClients(); cIter++){
					sol.setStockClient(cIter, t, Math.round(this.LIRPSolver.getValue(this.InvClients[cIter][t])));
				}
			}

			Checker.check(sol, this.LIRPInstance, this.routesSD, this.routesDC, printStreamSol);
			printStreamSol.println("--------------------------------------------");
		}
		return sol;

	}
}


