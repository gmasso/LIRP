package solverLIRP;

import ilog.concert.IloConversion;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;

import instanceManager.Instance;
import tools.Config;

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
	public Solver(Instance instLIRP, RouteMap availRoutes, Solution previousSol, double timeLimit) throws IloException {

		/* Data */
		this.instLIRP = instLIRP;
		//		System.out.print("ROUTES AVAILABLE : ");
		//		for(Route r : availRoutes.get(1)) {
		//				System.out.print("{start: " + r.getStartIndex() + ", stops: " + r.getStops().toString() + "}, ");
		//		};

		this.routes = new Route[this.instLIRP.getNbLevels()][];
		for(int lvl = 0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			this.routes[lvl] = new Route[availRoutes.get(lvl).size()];
			this.routes[lvl] = availRoutes.get(lvl).toArray(this.routes[lvl]);
		}

		this.isSolved = false;

		/* CPLEX solver */
		this.LIRPSolver = new IloCplex();

		/* Set the time limit */
		this.LIRPSolver.setParam(DoubleParam.TiLim, timeLimit);
		//this.LIRPSolver.setParam(DoubleParam.TiLim, 20);
		this.LIRPSolver.setParam(IloCplex.IntParam.Threads, Config.MAX_THREADS);

		/* Initialization of the variables */
		this.initVariables(previousSol);

		/*=================
		 *   CONSTRAINTS 
		 ==================*/
		for (int t = 0; t < this.instLIRP.getNbPeriods(); t++) {

			/*         =========
			 * Constraints on routes usage
			           =========          */
			for(int lvl = 0; lvl < this.instLIRP.getNbLevels(); lvl++) {
				/* Get the number of locations at this level and at the upper level */
				int nbLocLvl = this.instLIRP.getNbLocations(lvl);

				/* Each location is served by at most one route in every period (2-3) */
				for(int loc = 0; loc < nbLocLvl; loc++) {
					IloLinearIntExpr lhs2 = this.LIRPSolver.linearIntExpr();
					for (int r = 0; r < this.routes[lvl].length; r++)
						if(this.routes[lvl][r].containsStop(loc))
							lhs2.addTerm(1,  this.z[lvl][r][t]);
					/* If the location is a dc (constraint (3)), the rhs uses the boolean variable y_{j} to check that the location is open */
					if(lvl < this.instLIRP.getNbLevels() - 1) {
						lhs2.addTerm(-1, this.y[lvl][loc]);
						this.LIRPSolver.addLe(lhs2, 0);
					}
					else {
						this.LIRPSolver.addLe(lhs2, 1);
					}
				}

				/* Constraints on resources available in period t */

				/* The total number of routes used on a given level must be lower than the number of vehicles available for this level (5) */
				IloLinearIntExpr lhs5 = this.LIRPSolver.linearIntExpr();

				for (int r = 0; r < this.routes[lvl].length; r++) {
					if(lvl > 0) {
						/* Each active route must start from an open depot (4) */
						IloLinearIntExpr lhs4 = this.LIRPSolver.linearIntExpr();
						lhs4.addTerm(1, this.z[lvl][r][t]);
						/* NB : if lvl == 0 locUP = 0 and we do not explore the following loop */
						lhs4.addTerm(-1, this.y[lvl - 1][this.routes[lvl][r].getStartIndex()]);

						this.LIRPSolver.addLe(lhs4, 0);
					}

					/* The quantity delivered to the locations of the level through a given route cannot exceed the capacity of a vehicle (6) */
					IloLinearNumExpr lhs6 = this.LIRPSolver.linearNumExpr();
					lhs6.addTerm(-this.instLIRP.getCapacityVehicle(lvl), this.z[lvl][r][t]);
					for(int loc = 0; loc < nbLocLvl; loc++) {
						lhs6.addTerm(1, this.q[lvl][loc][r][t]);
						/* A positive quantity can be delivered to location loc using route r only if loc belongs to route r (7) */
						IloLinearNumExpr lhs7 = this.LIRPSolver.linearNumExpr();
						lhs7.addTerm(1, this.q[lvl][loc][r][t]);
						double rhs7 = (this.routes[lvl][r].containsStop(loc)) ? this.instLIRP.getCapacityVehicle(lvl) : 0;
						this.LIRPSolver.addLe(lhs7, rhs7);
					}
					this.LIRPSolver.addLe(lhs6, 0);

					lhs5.addTerm(1, this.z[lvl][r][t]);
				}
				/* The sum of all the routes used at this level in period t must be lower than the fleet size at this level */
				this.LIRPSolver.addLe(lhs5, this.instLIRP.getNbVehicles(lvl));

				/*         ======
				 * Constraints on inventory
				           ======          */
				/* Flow conservation (8-11) */
				for(int loc = 0; loc < nbLocLvl; loc++) {
					/* If we are at a dc level, take into account the incoming and outgoing quantities through routes (8) */
					if(lvl < this.instLIRP.getNbLevels() - 1) {
						int nbLocDown = this.instLIRP.getNbLocations(lvl + 1);
						IloLinearNumExpr lhs8 = this.LIRPSolver.linearNumExpr();
						lhs8.addTerm(-1, this.invLoc[lvl][loc][t]);
						double rhs8 = 0;
						if(t == 0)
							rhs8 -= this.instLIRP.getDepot(lvl, loc).getInitialInventory();
						else
							lhs8.addTerm(1, this.invLoc[lvl][loc][t - 1]);
						for (int r = 0; r < this.routes[lvl].length; r++) {
							if(this.routes[lvl][r].containsStop(loc)) {
								lhs8.addTerm(1, q[lvl][loc][r][t]);
							}

						}
						for (int rDown = 0; rDown < this.routes[lvl + 1].length; rDown++) {
							for(int locDown = 0; locDown < nbLocDown; locDown++) {
								if(this.routes[lvl + 1][rDown].hasStart(loc))
									lhs8.addTerm(-1, q[lvl + 1][locDown][rDown][t]);
							}
						}
						LIRPSolver.addEq(lhs8, rhs8);

						/* Capacity constraints at depots (10) */
						IloLinearNumExpr lhs10 = this.LIRPSolver.linearNumExpr();
						lhs10.addTerm(1, this.invLoc[lvl][loc][t]);
						lhs10.addTerm(-this.instLIRP.getDepot(lvl, loc).getCapacity(), y[lvl][loc]);
						this.LIRPSolver.addLe(lhs10, 0);
					}
					/* If we are at a clients level, take into account the incoming quantities through routes and the final customers demands (9) */
					else {
						IloLinearNumExpr lhs9 = this.LIRPSolver.linearNumExpr();
						lhs9.addTerm(-1, this.invLoc[lvl][loc][t]);
						double rhs9 = this.instLIRP.getClient(loc).getDemand(t);
						if(t == 0)
							rhs9 -= this.instLIRP.getClient(loc).getInitialInventory();
						else
							lhs9.addTerm(1, this.invLoc[lvl][loc][t - 1]);

						for (int r = 0; r < this.routes[lvl].length; r++) {
							lhs9.addTerm(1, q[lvl][loc][r][t]);
						}
						LIRPSolver.addEq(lhs9, rhs9);

						/* Stock capacity at the client or ensuring that the inventory is not greater than the sum of remaining demands (11) */
						double remainingDemand = this.instLIRP.getClient(loc).getCumulDemands(t + 1, this.instLIRP.getNbPeriods());
						IloLinearNumExpr lhs11 = this.LIRPSolver.linearNumExpr();
						lhs11.addTerm(1, this.invLoc[lvl][loc][t]);
						this.LIRPSolver.addLe(lhs11, Math.min(remainingDemand, this.instLIRP.getClient(loc).getCapacity()));
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

		this.y = new IloIntVar[this.instLIRP.getNbLevels() - 1][];
		this.q = new IloNumVar[this.instLIRP.getNbLevels()][][][];
		this.z = new IloIntVar[this.instLIRP.getNbLevels()][][];
		this.invLoc = new IloNumVar[this.instLIRP.getNbLevels()][][];

		for(int lvl = 0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			if(lvl < this.instLIRP.getNbLevels() - 1) {
				this.y[lvl] = this.LIRPSolver.boolVarArray(nbLocLvl);
			}
			this.z[lvl] = new IloIntVar[this.routes[lvl].length][];
			this.q[lvl] = new IloNumVar[nbLocLvl][this.routes[lvl].length][];
			this.invLoc[lvl] = new IloNumVar[nbLocLvl][]; 

			for(int loc = 0; loc < nbLocLvl; loc++) {
				invLoc[lvl][loc] =  this.LIRPSolver.numVarArray(this.instLIRP.getNbPeriods(), 0, Double.MAX_VALUE);
			}
			for(int r = 0; r < this.routes[lvl].length; r++) {
				this.z[lvl][r] = this.LIRPSolver.boolVarArray(this.instLIRP.getNbPeriods());
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
	private void solveMIP(boolean relax) throws IloException {

		IloNumVar obj = this.LIRPSolver.numVar(0, Double.MAX_VALUE, "obj"); // objective function

		/* Definition of the objective function */
		IloLinearNumExpr objexpr = this.LIRPSolver.linearNumExpr();

		for(int lvl = 0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			/* Fixed opening costs */
			if(lvl < this.instLIRP.getNbLevels() - 1) {
				for (int loc = 0; loc < this.instLIRP.getNbDepots(lvl); loc++) {
					objexpr.addTerm(this.instLIRP.getDepot(lvl, loc).getFixedCost(), this.y[lvl][loc]);
				}
			}

			for (int t = 0; t < this.instLIRP.getNbPeriods(); t++) {
				/* Delivering costs */
				for (int r = 0; r < this.routes[lvl].length; r++)
					objexpr.addTerm(this.routes[lvl][r].getCost(), this.z[lvl][r][t]);
				for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
					if(lvl < this.instLIRP.getNbLevels() - 1) {
						/* Holding cost incurred by the depots */
						objexpr.addTerm(this.instLIRP.getDepot(lvl, loc).getHoldingCost(), this.invLoc[lvl][loc][t]);
					}
					else {
						/* Holding cost incurred by the clients */
						objexpr.addTerm(this.instLIRP.getClient(loc).getHoldingCost(), this.invLoc[lvl][loc][t]);
					}
				}
			}
		}

		/*================
		 *   RESOLUTION 
		 =================*/
		this.LIRPSolver.addLe(objexpr, obj);
		this.LIRPSolver.addObjective(IloObjectiveSense.Minimize, obj);
		
		IloConversion[] convY = new IloConversion[y.length];
		IloConversion[][] convZ = new IloConversion[z.length][];

		IloLPMatrix lp = (IloLPMatrix) this.LIRPSolver.LPMatrixIterator().next();
		System.out.println(lp.getNumVars().toString());
        IloConversion conv = this.LIRPSolver.conversion(lp.getNumVars(),
                                                IloNumVarType.Float);

		if(relax) {
			System.out.println("Relaxing boolean constaints");

	         //IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();
	      
	         this.LIRPSolver.add(conv);

			/* If we want to solve the relaxed problem */
//			for(int lvl = 0; lvl < y.length; lvl++) {
//				convY[lvl] = this.LIRPSolver.conversion(y[lvl], IloNumVarType.Float);
//				this.LIRPSolver.add(convY[lvl]);
//				System.out.println("Type of localisation vars : " + this.y.getClass().toString());
//				convZ[lvl] = new IloConversion[z[lvl].length];
//				for(int rIter = 0; rIter < z[lvl].length; rIter++) {
//					convZ[lvl][rIter] = this.LIRPSolver.conversion(z[lvl][rIter], IloNumVarType.Float);
//					this.LIRPSolver.add(convZ[lvl][rIter]);
//				}
//			}
		}
		this.LIRPSolver.solve();
		if(relax) {
			System.out.print("Un-relaxing boolean constaints...");
			this.LIRPSolver.delete(conv);
//			/* Unrelax the variables */
//			for(int lvl = 0; lvl < y.length; lvl++) {
//				this.LIRPSolver.remove(convY[lvl]);
//				for(int rIter = 0; rIter < z[lvl].length; rIter++) {
//					this.LIRPSolver.remove(convZ[lvl][rIter]);
//				}
//			}
			System.out.println("done");

		}

		this.isSolved = true;
	}

	/**
	 * Creates a Solution object from the results of the solver on the LIRP problem considered
	 * @param printStreamSol	the stream on which to print the solution
	 * @return				the solution obtained from
	 */
	public Solution getSolution(boolean relax, double threshold) throws IloException {

		/*===============================
		 *     SAVE THE SOLVER OUTPUT
		=================================*/
		Solution sol  = new Solution(this.instLIRP, this.routes);

		if(!this.isSolved)
			this.solveMIP(relax);

		/* Get the best LB on the problem */
		double bestLB = this.LIRPSolver.getBestObjValue();

		if(!relax) {
			System.out.println(" Best LB : " + bestLB);
			if (this.LIRPSolver.getStatus().equals(IloCplex.Status.Infeasible)) {
				System.out.println("There is no solution");
			}
			else {
				System.out.println();
				System.out.println("===========  RESULTS  ===========");
				System.out.print("Status of the solver :   ");
				System.out.println(this.LIRPSolver.getStatus());
				System.out.print(" Best solution found :   ");
				/* Get the current integral solution */
				double bestFeasible = this.LIRPSolver.getObjValue();
				System.out.print(bestFeasible);
				System.out.println(" Best LB : ");
				System.out.print(bestLB);
			}
		}

		/*=======================
		 *    SOLUTION VALUES
			 ========================*/
		/* Save the status of depots (open/closed) */
		for(int lvl = 0; lvl < this.instLIRP.getNbLevels(); lvl++) {
			if(lvl < this.instLIRP.getNbLevels() - 1) {
				for(int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
					if (this.LIRPSolver.getValue(this.y[lvl][loc]) > threshold)
						sol.setOpenDepot(lvl, loc, true);
					else
						sol.setOpenDepot(lvl, loc, false);
				}
			}

			/* Save the quantities delivered to each location in each period */
			for (int t = 0; t < this.instLIRP.getNbPeriods(); t++){
				for(int r = 0; r < this.routes[lvl].length; r++) {
					if (this.LIRPSolver.getValue(this.z[lvl][r][t]) > threshold) {
						sol.setUsedRoute(lvl, r, t, true);
						for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++){
							double q = this.LIRPSolver.getValue(this.q[lvl][loc][r][t]);
							if(q > threshold) {
								sol.setDeliveryLocation(lvl, loc, r, t, q);
							}
							else {
								sol.setDeliveryLocation(lvl, loc, r, t, 0);
							}
						}
					}
					else {
						sol.setUsedRoute(lvl, r, t, false);
						for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++){
							double q = this.LIRPSolver.getValue(this.q[lvl][loc][r][t]);
							if(q > threshold) {
								sol.setDeliveryLocation(lvl, loc, r, t, q);
							}
							else {
								sol.setDeliveryLocation(lvl, loc, r, t, q);
							}
						}
					}
				}
				/* Save the inventory in each location in each period */
				for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++){
					if(this.LIRPSolver.getValue(this.invLoc[lvl][loc][t]) > threshold)
						sol.setInvLoc(lvl, loc, t, this.LIRPSolver.getValue(this.invLoc[lvl][loc][t]));
					else
						sol.setInvLoc(lvl, loc, t, 0);
				}

			}
		}

		sol.setStatus(this.LIRPSolver.getStatus().toString());
		sol.setLB(bestLB);

		if(!relax) {
			Checker.check(sol, this.instLIRP, this.routes);
		}

		System.out.print("Nb routes used : ");
		System.out.print(sol.collectUsedLoops().get(1).size());

		return sol;
	}
}