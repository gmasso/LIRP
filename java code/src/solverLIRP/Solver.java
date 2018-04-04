package solverLIRP;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;

import instanceManager.Instance;
import tools.Parameters;

public class Solver{

	private Instance instLIRP;
	private Route[][] routes;

	private IloCplex LIRPSolver;
	/*===============
	 *   VARIABLES 
	 ================*/
	/* Boolean */
	private IloIntVar[][] y;  // facility location variables (depots)
	// Integer variables
	private IloIntVar[][] x;   // = 1 is route r is used on period t (supplier-depot)
	private IloIntVar[][][] z;  // = 1 if route r is used on period t (depot-clients)
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
	public Solver(Instance instLIRP, HashMap<Integer, ArrayList<Route>> availRoutes, Solution previousSol, boolean isFinal) throws IloException {

		/* Data */
		this.instLIRP = instLIRP;

		int nbClients = this.instLIRP.getNbClients();  // number of clients
		int nbDepots = this.instLIRP.getNbDepots(0);  // number of depots
		int nbPeriods = this.instLIRP.getNbPeriods(); // number of periods

		this.routes = new Route[Parameters.nb_levels][];
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			this.routes[lvl] = new Route[availRoutes.get(lvl).size()];
			this.routes[lvl] = availRoutes.get(lvl).toArray(this.routes[lvl]);
		}
		/* Definition of parameters Alpha and Beta (for the depots-clients routes)*/
		int[][][] Alpha = new int[Parameters.nb_levels][][]; 
		int[][][] Beta = new int[Parameters.nb_levels][][];
		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		/*Iterator rIter = availroutes[1].iterator();*/
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			Alpha[lvl] = new int[nbClients][this.routes[lvl].length];							// = 1 if location i is in route r
			if(lvl > 0)
				Beta[lvl - 1] = new int[this.instLIRP.getNbDepots(lvl)][this.routes[lvl].length];	// = 1 if depot j is in route r
			for(int rIndex = 0; rIndex < this.routes[1].length; rIndex++) {
				for(int cIter = 0; cIter < nbClients; cIter++)
					Alpha[lvl][cIter][rIndex] = this.routes[lvl][rIndex].containsStop(cIter) ? 1 : 0;
				//Alpha[cIter][rId] = rIter.containsLocation(this.instLIRP.getClient(cIter)) ? 1 : 0;
				for(int dIter = 0; dIter < nbDepots; dIter++)
					Beta[lvl - 1][dIter][rIndex] = this.routes[lvl-1][rIndex].hasStart(dIter) ? 1 : 0;
			}
		}
		//
		//		/* Definition of parameters Gamma (for the supplier-depots routes) */
		//		int[][] Gamma = new int[nbDepots][this.routes[0].length]; // = 1 if depot j is in route r
		//		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		//		for(int rIndex = 0; rIndex < this.routes[1].length; rIndex++) {
		//			for(int dIter = 0; dIter < nbClients; dIter++)
		//				Gamma[dIter][rIndex] = this.routes[0][rIndex].containsStop(dIter) ? 1 : 0;
		//		}

		this.isSolved = false;

		/* CPLEX solver */
		this.LIRPSolver = new IloCplex();

		// Set the time limit
		if(isFinal) {
			this.LIRPSolver.setParam(DoubleParam.TiLim, Parameters.mainTimeLimit);
		}
		else {
			this.LIRPSolver.setParam(DoubleParam.TiLim, Parameters.auxTimeLimit);
		}

		/* Initialization of the variables */
		this.initVariables(previousSol);


		/*=================
		 *   CONSTRAINTS 
		 ==================*/
		for (int t = 0; t < nbPeriods; t++) {
			/* Each client is served by at most one route in every period (3)*/
			for(int lvl = 1; lvl < Parameters.nb_levels - 1; lvl++) {
				int nbLoc = (lvl == Parameters.nb_levels - 1) ? this.instLIRP.getNbClients():this.instLIRP.getNbDepots(lvl);
				for(int locIter = 0; locIter < nbLoc; locIter++) {
					IloLinearIntExpr expr3 = this.LIRPSolver.linearIntExpr();
					for (int rIter = 0; rIter < this.routes[lvl].length; rIter++)
						expr3.addTerm(Alpha[lvl][locIter][rIter], this.z[lvl][rIter][t]);
					expr3.addTerm(-1, this.y[lvl][locIter]);
					this.LIRPSolver.addLe(expr3, 1);
				}


				/* Constraints on the route for depots */
				for(int dIter = 0; dIter < nbDepots; dIter++) {
					/* Each depot is served by at most one route in every period (2) */
					IloLinearIntExpr expr2 = this.LIRPSolver.linearIntExpr();
					for (int rIter = 0; rIter < availRoutes.get(0).size(); rIter++) 
						expr2.addTerm(Gamma[dIter][rIter], this.x[rIter][t]);
					this.LIRPSolver.addLe(expr2, 1);
				}

				/* Each client is served by at most one route in every period (3)*/
				for(int lvl = 1; lvl < Parameters.nb_levels; lvl++) {
					for(int locIter = 0; locIter < this.getNbLocations(lvl); locIter++) {
						IloLinearIntExpr expr3 = this.LIRPSolver.linearIntExpr();
						for (int rIter = 0; rIter < this.routes[lvl].length; rIter++)
							expr3.addTerm(Alpha[lvl][locIter][rIter], this.z[lvl][rIter][t]);
						expr3.addTerm(-1, this.y[lvl][locIter]);
						this.LIRPSolver.addLe(expr3, 1);
					}

					for(int dIter = 0; dIter < nbDepots; dIter++) {
						for (int rIter = 0; rIter < this.routes[0].length; rIter++) {
							/* A supplier-depot route is used only if all depots on this route are opened (4)*/
							IloLinearIntExpr expr4 = this.LIRPSolver.linearIntExpr();
							expr4.addTerm(Gamma[dIter][rIter], this.x[rIter][t]);
							this.LIRPSolver.addLe(expr4, 0);
						}
					}

					/* Each route delivering customers can only departs from an opened depot (5)*/
					for (int rIter = 0; rIter < this.routes[1].length; rIter++) {
						IloLinearIntExpr expr5 = this.LIRPSolver.linearIntExpr();
						expr5.addTerm(1, this.z[rIter][t]);
						for(int dIter = 0; dIter < nbDepots; dIter++) {
							expr5.addTerm(-Beta[dIter][rIter], this.y[dIter]);
						}
						this.LIRPSolver.addLe(expr5, 0);
					}

					/* Vehicle capacity on each delivery on each opened supplier-depot route (6)*/
					for (int rIter = 0; rIter < this.routes[0].length; rIter++) {
						IloLinearNumExpr expr6 = this.LIRPSolver.linearNumExpr();
						for(int dIter = 0; dIter < nbDepots; dIter++)
							expr6.addTerm(1, this.v[dIter][rIter][t]);
						// TO MODIFY IN THE FINAL VERSION
						expr6.addTerm(-this.instLIRP.getCapacityVehicle(0), this.x[rIter][t]);
						this.LIRPSolver.addLe(expr6, 0);
					}


					/* Vehicle capacity on each delivery on each opened depot-client route (7)*/
					for (int rIter = 0; rIter < this.routes[1].length; rIter++) {
						IloLinearNumExpr expr7 = this.LIRPSolver.linearNumExpr();
						for(int cIter = 0; cIter < nbClients; cIter++)
							expr7.addTerm(1, this.u[cIter][rIter][t]);
						// TO MODIFY IN THE FINAL VERSION
						expr7.addTerm(-this.instLIRP.getCapacityVehicle(0), this.z[rIter][t]);
						this.LIRPSolver.addLe(expr7, 0);
					}

					/* Flow conservation at the depots (8) */
					for(int dIter = 0; dIter < nbDepots; dIter++) {
						IloLinearNumExpr expr8 = this.LIRPSolver.linearNumExpr();
						expr8.addTerm(1, this.InvDepots[dIter][t]);
						double rhs8 = 0;
						if(t == 0)
							rhs8 = this.instLIRP.getDepot(0, dIter).getInitialInventory();
						else
							expr8.addTerm(-1, this.InvDepots[dIter][t-1]);
						for(int rSDIter = 0; rSDIter < this.routes[0].length; rSDIter++)
							expr8.addTerm(-1, this.v[dIter][rSDIter][t]);
						for (int rIter = 0; rIter < this.routes[1].length; rIter++) {
							for (int cIter = 0; cIter < nbClients; cIter++)
								expr8.addTerm(Beta[lvl][dIter][rIter], this.u[cIter][rIter][t]);
						}
						this.LIRPSolver.addEq(expr8, rhs8);
					}

					/* Flow conservation at the clients (9) */
					for (int cIter = 0; cIter < nbClients; cIter++){
						IloLinearNumExpr expr9 = this.LIRPSolver.linearNumExpr();
						expr9.addTerm(1, this.InvClients[cIter][t]);
						double rhs9 = -this.instLIRP.getClient(cIter).getDemand(t);
						if (t==0)
							rhs9 += this.instLIRP.getClient(cIter).getInitialInventory();
						else
							expr9.addTerm(-1, this.InvClients[cIter][t - 1]);
						for (int rIter = 0; rIter < this.routes[1].length; rIter++) {
							expr9.addTerm(-1, this.u[cIter][rIter][t]);
						}
						this.LIRPSolver.addEq(expr9, rhs9);
					}

					/* Stock capacity at the client or ensuring that the inventory is not greater than the sum of remaining demands (10) */
					// IS THIS LAST ASPECT OF THE CONSTRAINT REALLY USEFUL? 
					for (int cIter = 0; cIter < nbClients; cIter++) {
						double remainingDemand = this.instLIRP.getClient(cIter).getCumulDemands(t+1, nbPeriods);
						IloLinearNumExpr expr10 = this.LIRPSolver.linearNumExpr();
						expr10.addTerm(1, this.InvClients[cIter][t]);
						if(remainingDemand < this.instLIRP.getClient(cIter).getCapacity())
							this.LIRPSolver.addLe(expr10, remainingDemand);
						else
							this.LIRPSolver.addLe(expr10, this.instLIRP.getClient(cIter).getCapacity());
					}

					/* Capacity constraints at depots (11) */
					for (int dIter = 0; dIter < nbDepots; dIter++) {
						IloLinearNumExpr expr11 = this.LIRPSolver.linearNumExpr();
						expr11.addTerm(1, this.InvDepots[dIter][t]);
						this.LIRPSolver.addLe(expr11, this.instLIRP.getDepot(0, dIter).getCapacity());
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
				for (int dIter = 0; dIter < this.instLIRP.getNbDepots(0); dIter++) {
					objExpr.addTerm(this.instLIRP.getDepot(0, dIter).getFixedCost(), this.y[dIter]);
				}
				for (int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
					/* Delivering costs for the depots */
					for (int rSDIter = 0; rSDIter < this.routes[0].length; rSDIter++)
						objExpr.addTerm(this.routes[0][rSDIter].getCost(), this.x[rSDIter][t]);
					/* Delivering costs for the clients */
					for (int rDCIter = 0; rDCIter < this.routes[1].length; rDCIter++)
						objExpr.addTerm(this.routes[1][rDCIter].getCost(), this.z[rDCIter][t]);
					/* Holding cost incurred by the depots */
					for (int dIter = 0; dIter < this.instLIRP.getNbDepots(0); dIter++)
						objExpr.addTerm(this.instLIRP.getDepot(0, dIter).getHoldingCost(), this.InvDepots[dIter][t]);
					/* Holding cost incurred by the clients */
					for (int cIter = 0; cIter < this.instLIRP.getNbClients(); cIter++)
						objExpr.addTerm(this.instLIRP.getClient(cIter).getHoldingCost(), this.InvClients[cIter][t]);
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
				Solution sol  = new Solution(this.instLIRP, this.routes);

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
					for (int dIter = 0; dIter < this.instLIRP.getNbDepots(0); dIter++){
						if (this.LIRPSolver.getValue(this.y[dIter]) > Parameters.epsilon)
							sol.setOpenDepot(dIter, true);
						else
							sol.setOpenDepot(dIter, false);
					}

					/* Save the deliveries to depots */
					for (int t = 0; t < this.instLIRP.getNbPeriods(); t++){
						for (int dIter = 0; dIter < this.instLIRP.getNbDepots(0); dIter++){
							for(int rIter = 0; rIter < this.routes[0].length; rIter++)
								if (this.LIRPSolver.getValue(this.x[rIter][t]) > Parameters.epsilon) {
									sol.setUsedRoute(0, rIter, t, true);
									double vjrt = this.LIRPSolver.getValue(this.v[dIter][rIter][t]);
									sol.setDeliveryDepot(dIter, rIter, t, vjrt);
								}
								else {
									sol.setUsedRoute(0, rIter, t, false);
									sol.setDeliveryDepot(dIter, rIter, t, 0);
								}
						}
					}

					/* Save the inventory variables */
					for (int t = 0; t < this.instLIRP.getNbPeriods(); t++){
						for (int dIter = 0; dIter < this.instLIRP.getNbDepots(0); dIter++){
							sol.setStockDepot(dIter,t, this.LIRPSolver.getValue(this.InvDepots[dIter][t]));
						}
					}

					/* Save the quantity delivered to clients */
					for (int t = 0;t < this.instLIRP.getNbPeriods(); t++){
						for (int cIter = 0;cIter < this.instLIRP.getNbClients(); cIter++){
							for (int rIter = 0; rIter < this.routes[1].length; rIter++){
								if (this.LIRPSolver.getValue(this.z[rIter][t]) > Parameters.epsilon) {
									sol.setUsedRoute(1, rIter, t, true);
									double uirt = this.LIRPSolver.getValue(this.u[cIter][rIter][t]);
									sol.setDeliveryClient(cIter, rIter, t, uirt);			
								}
								else {
									sol.setUsedRoute(1, rIter, t, false);
									sol.setDeliveryClient(cIter, rIter, t, 0);
								}
							}
						}
					}

					/* Save inventory at clients for every period t */
					for (int t = 0; t < this.instLIRP.getNbPeriods(); t++){
						for (int cIter = 0; cIter < this.instLIRP.getNbClients(); cIter++){
							sol.setStockClient(cIter, t, this.LIRPSolver.getValue(this.InvClients[cIter][t]));
						}
					}

					Checker.check(sol, this.instLIRP, this.routes, printStreamSol);
				}
				return sol;

			}

			private int getNbLocation(int lvl) {
				if(lvl < Parameters.nb_levels - 1) {
					return this.instLIRP.getNbDepots(lvl);
				}
				return this.instLIRP.getNbClients();

			}

			/**
			 * Initialize the MIP variables and if a starting solution is provided, set the starting values of the variables to their values in the starting solution
			 * @param startSol	The solution from which the variables should be initialized
			 * @throws IloException
			 */
			private void initVariables(Solution startSol) throws IloException {
				int nbClients = this.instLIRP.getNbClients();  // number of clients
				int nbDepots = this.instLIRP.getNbDepots(0);  // number of depots
				int nbPeriods = this.instLIRP.getNbPeriods(); // number of periods

				this.y = this.LIRPSolver.boolVarArray(nbDepots);
				this.x = new IloIntVar[this.routes[0].length][nbPeriods];
				this.z = new IloIntVar[this.routes[1].length][nbPeriods];
				this.u = new IloNumVar[nbClients][this.routes[1].length][nbPeriods];
				this.v = new IloNumVar[nbDepots][this.routes[0].length][nbPeriods];
				this.InvClients = new IloNumVar[nbClients][nbPeriods];
				this.InvDepots = new IloNumVar[nbDepots][nbPeriods]; 

				for(int t = 0; t < nbPeriods; t++) {
					for(int rIter = 0; rIter < this.routes[0].length; rIter++){
						this.x[rIter][t] = this.LIRPSolver.boolVar();
						for(int dIter = 0; dIter < nbDepots; dIter++)
							this.v[dIter][rIter][t] = this.LIRPSolver.numVar(0, Double.MAX_VALUE);
					}
					for(int rIter = 0; rIter < this.routes[1].length; rIter++) {
						this.z[rIter][t] = this.LIRPSolver.boolVar();
						for(int cIter = 0; cIter < nbClients; cIter++) 
							this.u[cIter][rIter][t] = this.LIRPSolver.numVar(0, Double.MAX_VALUE);
					}
					for(int cIter = 0; cIter < nbClients; cIter++)
						this.InvClients[cIter][t] = this.LIRPSolver.numVar(0, Double.MAX_VALUE);
					for(int dIter = 0; dIter < nbDepots; dIter++)
						this.InvDepots[dIter][t] = this.LIRPSolver.numVar(0, Double.MAX_VALUE);
				}

				if(startSol != null) {
					this.LIRPSolver.addMIPStart(this.y, new double[] {});
					this.LIRPSolver.addMIPStart(this.u[0][0], new double[] {});
				}
			}

		}


