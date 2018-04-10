package solverLIRP;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashSet;

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
	private IloIntVar[][] y;  			// facility location variables (depots) for each level
	/* Integer variables */
	private IloIntVar[][][] z;  		// = 1 if route r is used on period t (for each level)
	/* Continuous */
	private IloNumVar[][][][] q; 			// quantity delivered by route r to location i of level l in period t
	private IloNumVar[][][] invLoc; 	// inventory at depots

	private boolean isSolved; 			// States if the MIP has been solved or not

	/**
	 * Creates a Solver object for the LIRP instance, setting the variables, available routes and the CPLEX model
	 * @param LIRPInstance		the instance upon which is built the model	
	 * @param availableRoutes	the direct and multi-stops routes that are available in this model
	 * @throws IloException
	 */
	public Solver(Instance instLIRP, HashMap<Integer, LinkedHashSet<Route>> availRoutes, Solution previousSol, boolean isFinal) throws IloException {

		/* Data */
		this.instLIRP = instLIRP;

		this.routes = new Route[Parameters.nb_levels][];
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			this.routes[lvl] = new Route[availRoutes.get(lvl).size()];
			this.routes[lvl] = availRoutes.get(lvl).toArray(this.routes[lvl]);
		}

		/* Definition of parameters Alpha and Beta*/
		/* Alpha_lir = 1 if location i of level l is visited by route r */
		int[][][] Alpha = new int[Parameters.nb_levels][][]; 
		/* Beta_lir = 1 if route r with stops at level l starts from location j of level l - 1 */
		int[][][] Beta = new int[Parameters.nb_levels][][];
		/* Fill the two arrays by checking for each route which locations it visits and from which depots it starts */
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			Alpha[lvl] = new int[nbLocLvl][this.routes[lvl].length];							// = 1 if route r stops in location i
			/* Beta[lvl] is allocated only if we consider routes stopping at a level >= 1 */
			if(lvl > 0)
				Beta[lvl] = new int[this.instLIRP.getNbDepots(lvl - 1)][this.routes[lvl].length];	// = 1 if route r starts from depot j
			for(int rIndex = 0; rIndex < this.routes[lvl].length; rIndex++) {
				for(int stop = 0; stop < nbLocLvl; stop++)
					Alpha[lvl][stop][rIndex] = this.routes[lvl][rIndex].containsStop(stop) ? 1 : 0;
				if(lvl > 0) {
					for(int start = 0; start < this.instLIRP.getNbDepots(lvl - 1); start++)
						Beta[lvl][start][rIndex] = this.routes[lvl-1][rIndex].hasStart(start) ? 1 : 0;
				}
			}
		}

		this.isSolved = false;

		/* CPLEX solver */
		this.LIRPSolver = new IloCplex();

		/* Set the time limit */
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
		for (int t = 0; t < this.instLIRP.getNbPeriods(); t++) {

			/*         =========
			 * Constraints on routes usage
			           =========          */
			for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
				/* Get the number of locations at this level and at the upper level */
				int nbLocLvl = this.instLIRP.getNbLocations(lvl);
				int nbLocUp = this.instLIRP.getNbLocations(lvl - 1); 

				/* Each location is served by at most one route in every period (2-3) */
				for(int loc = 0; loc < nbLocLvl; loc++) {
					IloLinearIntExpr expr2 = this.LIRPSolver.linearIntExpr();
					for (int r = 0; r < this.routes[lvl].length; r++)
						expr2.addTerm(Alpha[lvl][loc][r], this.z[lvl][r][t]);
					/* If the location is a dc (constraint (3)), the rhs uses the boolean variable y_{j} to check that the location is open */
					if(lvl < Parameters.nb_levels - 1) {
						expr2.addTerm(-1, this.y[lvl][loc]);
						this.LIRPSolver.addLe(expr2, 0);
					}
					else {
						this.LIRPSolver.addLe(expr2, 1);
					}
				}

				/* Constraints on resources available in period t */

				/* The total number of routes used on a given level must be lower than the number of vehicles available for this level (5) */
				IloLinearIntExpr expr5 = this.LIRPSolver.linearIntExpr();

				for (int r = 0; r < this.routes[lvl].length; r++) {
					if(lvl > 0) {
						/* Each active route must start from an open depot (4) */
						IloLinearIntExpr expr4 = this.LIRPSolver.linearIntExpr();
						expr4.addTerm(1, this.z[lvl][r][t]);
						/* NB : if lvl == 0 locUP = 0 and we do not explore the following loop */
						for(int locUp = 0; locUp < nbLocUp; locUp++) {
							expr4.addTerm(- Beta[lvl][locUp][r], this.y[lvl - 1][locUp]);
						}
						this.LIRPSolver.addLe(expr4, 0);
					}

					/* The quantity delivered to the locations of the level through a given route cannot exceed the capacity of a vehicle (6) */
					IloLinearNumExpr expr6 = this.LIRPSolver.linearNumExpr();
					expr6.addTerm(-this.instLIRP.getCapacityVehicle(lvl), this.z[lvl][r][t]);
					for(int loc = 0; loc < nbLocLvl; loc++) {
						expr6.addTerm(1, this.q[lvl][loc][r][t]);
					}
					this.LIRPSolver.addLe(expr6, 0);

					expr5.addTerm(1, this.z[lvl][r][t]);
				}
				this.LIRPSolver.addLe(expr5, this.instLIRP.getNbVehicles(lvl));

				/*         ======
				 * Constraints on inventory
				           ======          */
				/* Flow conservation (7-8) */
				for(int loc = 0; loc < nbLocLvl; loc++) {
					/* If we are at a dc level, take into account the incoming and outgoing quantities through routes (7) */
					if(lvl < Parameters.nb_levels - 1) {
						int nbLocDown = this.instLIRP.getNbLocations(lvl + 1);
						IloLinearNumExpr expr7 = this.LIRPSolver.linearNumExpr();
						expr7.addTerm(1, this.invLoc[lvl][loc][t]);
						double rhs7 = 0;
						if(t == 0)
							rhs7 = this.instLIRP.getDepot(lvl, loc).getInitialInventory();
						else
							expr7.addTerm(-1, this.invLoc[lvl][loc][t - 1]);
						for (int r = 0; r < this.routes[lvl].length; r++) {
							expr7.addTerm(-1, q[lvl][loc][r][t]);
						}
						for (int rDown = 0; rDown < this.routes[lvl + 1].length; rDown++) {
							for(int locDown = 0; locDown < nbLocDown; locDown++) {
								expr7.addTerm(Beta[lvl + 1][loc][rDown], q[lvl][locDown][rDown][t]);
							}
						}
						LIRPSolver.addEq(expr7, rhs7);

						/* Capacity constraints at depots (9) */
						IloLinearNumExpr expr9 = this.LIRPSolver.linearNumExpr();
						expr9.addTerm(1, this.invLoc[lvl][loc][t]);
						expr9.addTerm(- this.instLIRP.getDepot(lvl, loc).getCapacity(), y[lvl][loc]);
						this.LIRPSolver.addLe(expr9, 0);
					}
					/* If we are at a clients level, take into account the incoming  quantities through routes and the final customers demands (8) */
					else {
						IloLinearNumExpr expr8 = this.LIRPSolver.linearNumExpr();
						expr8.addTerm(1, this.invLoc[lvl][loc][t]);
						double rhs8 = - this.instLIRP.getClient(loc).getDemand(t);
						if(t == 0)
							rhs8 += this.instLIRP.getClient(loc).getInitialInventory();
						else
							expr8.addTerm(-1, this.invLoc[lvl][loc][t - 1]);

						for (int r = 0; r < this.routes[lvl].length; r++) {
							expr8.addTerm(-1, q[lvl][loc][r][t]);
						}
						LIRPSolver.addEq(expr8, rhs8);

						/* Stock capacity at the client or ensuring that the inventory is not greater than the sum of remaining demands (10) */
						double remainingDemand = this.instLIRP.getClient(loc).getCumulDemands(t + 1, this.instLIRP.getNbPeriods());
						IloLinearNumExpr expr10 = this.LIRPSolver.linearNumExpr();
						expr10.addTerm(1, this.invLoc[lvl][loc][t]);
						this.LIRPSolver.addLe(expr10, Math.min(remainingDemand, this.instLIRP.getClient(loc).getCapacity()));
					}
				}
			}
		}
	}

	/*============================
	 *  VARIABLES INITIALISATION 
	 =============================*/
	/**
	 * Initialize the MIP variables and if a starting solution is provided, set the starting values of the variables to their values in the starting solution
	 * @param startSol	The solution from which the variables should be initialized
	 * @throws IloException
	 */
	private void initVariables(Solution startSol) throws IloException {

		this.y = new IloIntVar[Parameters.nb_levels - 1][];
		this.q = new IloIntVar[Parameters.nb_levels][][][];
		this.z = new IloIntVar[Parameters.nb_levels][][];
		this.invLoc = new IloNumVar[Parameters.nb_levels][][];

		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			this.z[lvl] = new IloIntVar[this.routes[lvl].length][];
			this.y[lvl] = this.LIRPSolver.boolVarArray(nbLocLvl);
			this.invLoc[lvl] = new IloNumVar[nbLocLvl][]; 
			this.q[lvl] = new IloIntVar[nbLocLvl][this.routes[lvl].length][];
			for(int loc = 0; loc < nbLocLvl; loc++) {
				invLoc[lvl][loc] =  this.LIRPSolver.numVarArray(this.instLIRP.getNbPeriods(), 0, Double.MAX_VALUE);
			}
			for(int r = 0; r < this.routes[lvl].length; r++) {
				this.z[lvl][r] = this.LIRPSolver.boolVarArray(this.instLIRP.getNbDepots(lvl));
				for(int loc = 0; loc < nbLocLvl; loc++) {
					this.q[lvl][loc][r] = this.LIRPSolver.numVarArray(this.instLIRP.getNbPeriods(), 0, Double.MAX_VALUE);
				}
			}

			/* If a starting solution is provided, set the different variables accordingly */
			if(startSol != null) {
				/* Set the depots opened in the starting solution */
				double[] oDC = new double[this.instLIRP.getNbLocations(lvl - 1)];
				for(int dc = 0; dc < this.instLIRP.getNbLocations(lvl - 1); dc++) {
					oDC[dc] = (startSol.isOpenDepot(lvl, dc)) ? 1 : 0;
				}
				this.LIRPSolver.addMIPStart(this.y[lvl], oDC);

				/* Set up the route usage in the starting solution */
				for(int r = 0; r < this.routes[lvl].length; r++) {
					double[] rUsed = new double[this.instLIRP.getNbPeriods()];
					for(int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
						rUsed[t] = startSol.isUsedRoute(lvl, r, t) ? 1 : 0;
					}
					this.LIRPSolver.addMIPStart(this.z[lvl][r], rUsed);
				}
				
				/* Set up the quantities delivered and the inventory levels in the starting solution */
				for(int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
					double[] inv = new double[this.instLIRP.getNbPeriods()];
					for(int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
						inv[t] = startSol.getInvLoc(lvl, loc, t);
					}
					this.LIRPSolver.addMIPStart(this.invLoc[lvl][loc], inv);

					for(int r = 0; r < this.routes[lvl].length; r++) {
						double[] del = new double[this.instLIRP.getNbPeriods()];
						for(int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
							del[t] = startSol.getQuantityDelivered(lvl, loc, r, t);
						}
						this.LIRPSolver.addMIPStart(this.q[lvl][loc][r], del);
					}
				}
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

		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			/* Fixed opening costs */
			if(lvl < Parameters.nb_levels - 1) {
				for (int loc = 0; loc < this.instLIRP.getNbDepots(lvl); loc++) {
					objExpr.addTerm(this.instLIRP.getDepot(lvl, loc).getFixedCost(), this.y[lvl][loc]);
				}
			}

			for (int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
				/* Delivering costs */
				for (int r = 0; r < this.routes[lvl].length; r++)
					objExpr.addTerm(this.routes[lvl][r].getCost(), this.z[lvl][r][t]);
				if(lvl < Parameters.nb_levels - 1) {
					/* Holding cost incurred by the depots */
					for (int dc = 0; dc < this.instLIRP.getNbDepots(lvl); dc++)
						objExpr.addTerm(this.instLIRP.getDepot(lvl, dc).getHoldingCost(), this.invLoc[lvl][dc][t]);
				}
				else {
					/* Holding cost incurred by the clients */
					for (int c = 0; c < this.instLIRP.getNbClients(); c++)
						objExpr.addTerm(this.instLIRP.getClient(c).getHoldingCost(), this.invLoc[lvl][c][t]);
				}
			}
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
	public Solution getSolution() throws IloException {

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
			System.out.print("Status of the solver :   ");
			System.out.println(this.LIRPSolver.getStatus());
			System.out.print(" Best solution found :   ");
			/* Get the current integral solution */
			double bestFeasible = this.LIRPSolver.getObjValue();
			System.out.println(" Best LB : ");
			/* Get the best LB on the problem */
			double bestLB = this.LIRPSolver.getBestObjValue();

			/*=======================
			 *    SOLUTION VALUES
			 ========================*/
			/* Save the status of depots (open/closed) */
			for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
				for(int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
					if (this.LIRPSolver.getValue(this.y[lvl][loc]) > Parameters.epsilon)
						sol.setOpenDepot(lvl, loc, true);
					else
						sol.setOpenDepot(lvl, loc, false);
				}

				/* Save the quantities delivered to each location in each period */
				for (int t = 0; t < this.instLIRP.getNbPeriods(); t++){
					for(int r = 0; r < this.routes[lvl].length; r++) {
						if (this.LIRPSolver.getValue(this.z[lvl][r][t]) > Parameters.epsilon) {
							sol.setUsedRoute(lvl, r, t, true);
							for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++){
								double q = this.LIRPSolver.getValue(this.q[lvl][loc][r][t]);
								sol.setDeliveryLocation(lvl, loc, r, t, q);
							}
						}
						else {
							sol.setUsedRoute(lvl, r, t, false);
							for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++){
								sol.setDeliveryLocation(lvl, loc, r, t, 0);
							}
						}
					}
					/* Save the inventory in each location in each period */
					for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++){
						if(this.LIRPSolver.getValue(this.invLoc[lvl][loc][t]) > Parameters.epsilon)
							sol.setInvLoc(lvl, loc, t, this.LIRPSolver.getValue(this.invLoc[lvl][loc][t]));
						else
							sol.setInvLoc(lvl, loc, t, 0);
					}

				}
			}

			Checker.check(sol, this.instLIRP, this.routes);
		}
		return sol;
	}
}