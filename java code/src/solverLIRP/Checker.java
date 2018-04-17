package solverLIRP;

import instanceManager.Instance;
import tools.Parameters;

public final class Checker {
	public static boolean check(Solution sol, Instance instLIRP, Route[][] routes) {
		/* Definition of parameters Alpha and Beta*/
		/* Alpha_lir = 1 if location i of level l is visited by route r */
		int[][][] Alpha = new int[Parameters.nb_levels][][]; 
		/* Beta_lir = 1 if route r with stops at level l starts from location j of level l - 1 */
		int[][][] Beta = new int[Parameters.nb_levels][][];
		/* Fill the two arrays by checking for each route which locations it visits and from which depots it starts */
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			int nbLocLvl = instLIRP.getNbLocations(lvl);
			Alpha[lvl] = new int[nbLocLvl][routes[lvl].length];							// = 1 if route r stops in location i
			/* Beta[lvl] is allocated only if we consider routes stopping at a level >= 1 */
			if(lvl > 0)
				Beta[lvl] = new int[instLIRP.getNbDepots(lvl - 1)][routes[lvl].length];	// = 1 if route r starts from depot j
			for(int rIndex = 0; rIndex < routes[lvl].length; rIndex++) {
				for(int stop = 0; stop < nbLocLvl; stop++)
					Alpha[lvl][stop][rIndex] = routes[lvl][rIndex].containsStop(stop) ? 1 : 0;
				if(lvl > 0) {
					for(int start = 0; start < instLIRP.getNbDepots(lvl - 1); start++)
						Beta[lvl][start][rIndex] = routes[lvl][rIndex].hasStart(start) ? 1 : 0;
				}
			}
		}

		/* The return value of the checker */
		boolean isFeasible = true;

		for (int t = 0; t < instLIRP.getNbPeriods(); t++) {
			/*         =========
			 * Constraints on routes usage
		           =========          */
			for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
				/* Get the number of locations at this level and at the upper level */
				int nbLocLvl = instLIRP.getNbLocations(lvl);
				int nbLocUp = instLIRP.getNbLocations(lvl - 1); 

				/* Each location is served by at most one route in every period (2-3) */
				for(int loc = 0; loc < nbLocLvl; loc++) {
					double lhs23 = 0;
					for (int r = 0; r < routes[lvl].length; r++)
						if(sol.isUsedRoute(lvl, r, t)) {
							lhs23 += Alpha[lvl][loc][r];
						}
					/* If the location is a dc (constraint (3)), the rhs uses the boolean variable y_{j} to check that the location is open */
					if(lvl < Parameters.nb_levels - 1) {
						double rhs23 = sol.isOpenDepot(lvl, loc) ? 1 : 0;
						if(lhs23 > rhs23) {
							isFeasible = false;
							System.out.println("ERR in constraint (2), depot " + loc + " at level " + lvl + " in period " + t);
						}
					}
					else {
						if(lhs23 > 1) {
							isFeasible = false;
							System.out.println("ERR in constraint (3), client " + loc + " in period " + t);
						}
					}
				}

				/* Constraints on resources available in period t */

				/* The total number of routes used on a given level must be lower than the number of vehicles available for this level (5) */
				int lhs5 = 0;

				for (int r = 0; r < routes[lvl].length; r++) {
					if(lvl > 0) {
						/* Each active route must start from an open depot (4) */
						int lhs4 = sol.isUsedRoute(lvl, r, t) ? 1 : 0;
						int rhs4 = 0;
						for(int locUp = 0; locUp < nbLocUp; locUp++) {
							if(sol.isOpenDepot(lvl - 1, locUp)) {
								rhs4 += Beta[lvl][locUp][r];
							}
						}
						if(lhs4 > rhs4) {
							isFeasible = false;
							System.out.println("ERR in constraint (4) at level " + lvl + " in period " + t);
						}
					}

					/* The quantity delivered to the locations of the level through a given route cannot exceed the capacity of a vehicle (6) */
					double lhs6 = 0;
					double rhs6 = sol.isUsedRoute(lvl, r, t) ? instLIRP.getCapacityVehicle(lvl) : 0 ;
					for(int loc = 0; loc < nbLocLvl; loc++) {
						lhs6 += sol.getQuantityDelivered(lvl, loc, r, t);
					}
					if(lhs6 > rhs6 + Parameters.epsilon) {
						isFeasible = false;
						System.out.println("ERR in constraint (6), trying to deliver a quantity of " + lhs6 + " at level " + lvl + " in period " + t + ", while the total quandity available is only " + rhs6);
					}
					if(sol.isUsedRoute(lvl, r, t)) {
						lhs5++;
					}
				}
				if(lhs5 > instLIRP.getNbVehicles(lvl)) {
					isFeasible = false;
					System.out.println("ERR in constraint (5), trying to use " + lhs5 + " routes at level " + lvl + " in period " + t + " with only " + instLIRP.getNbVehicles(lvl) + " vehicles.");
				}

				/*         ======
				 * Constraints on inventory
			           ======          */
				/* Flow conservation (7-8) */
				for(int loc = 0; loc < nbLocLvl; loc++) {
					/* If we are at a dc level, take into account the incoming and outgoing quantities through routes (7) */
					if(lvl < Parameters.nb_levels - 1) {
						int nbLocDown = instLIRP.getNbLocations(lvl + 1);
						double lhs7 = 0;
						lhs7 += sol.getInvLoc(lvl, loc, t);
						double rhs7 = 0;
						if(t == 0)
							rhs7 = instLIRP.getDepot(lvl, loc).getInitialInventory();
						else
							rhs7 += sol.getInvLoc(lvl, loc, t - 1);
						for (int r = 0; r < routes[lvl].length; r++) {
							rhs7 += sol.getQuantityDelivered(lvl,  loc, r, t);
						}
						for (int rDown = 0; rDown < routes[lvl + 1].length; rDown++) {
							if(Beta[lvl + 1][loc][rDown] > 0) {
								for(int locDown = 0; locDown < nbLocDown; locDown++) {
									lhs7 +=  sol.getQuantityDelivered(lvl + 1, locDown, rDown, t);
								}
							}
						}
						if(lhs7 < rhs7 - Parameters.epsilon || lhs7 > rhs7 + Parameters.epsilon) {
							isFeasible = false;
							System.out.println("ERR in constraint (7) in the flow conservation of location " + loc + " at level " + lvl + " in period " + t);
						}

						/* Capacity constraints at depots (9) */
						double rhs9 = sol.isOpenDepot(lvl,  loc) ? instLIRP.getDepot(lvl, loc).getCapacity() : 0; 
						if(sol.getInvLoc(lvl,  loc,  t) > rhs9 + Parameters.epsilon) {
							isFeasible = false;
							System.out.println("ERR in constraint (9), inventory at location " + loc + " at level " + lvl + " in period " + t + " violates capacity constraint (max " + instLIRP.getDepot(lvl, loc).getCapacity() +")"); ;
						}
					}
					/* If we are at a clients level, take into account the incoming  quantities through routes and the final customers demands (8) */
					else {
						double lhs8 = sol.getInvLoc(lvl, loc, t) + instLIRP.getClient(loc).getDemand(t);
						double rhs8 = 0;
						if(t == 0)
							rhs8 += instLIRP.getClient(loc).getInitialInventory();
						else
							rhs8 += sol.getInvLoc(lvl, loc, t - 1);

						for (int r = 0; r < routes[lvl].length; r++) {
							rhs8 += sol.getQuantityDelivered(lvl, loc, r, t);
						}
						if(lhs8 < rhs8 - Parameters.epsilon || lhs8 > rhs8 + Parameters.epsilon) {
							isFeasible = false;
							System.out.println("ERR in constraint (8) in the flow conservation of client " + loc + " at level " + lvl + " in period " + t);
						}

						/* Stock capacity at the client or ensuring that the inventory is not greater than the sum of remaining demands (10) */
						double remainingDemand = instLIRP.getClient(loc).getCumulDemands(t + 1, instLIRP.getNbPeriods());
						if(sol.getInvLoc(lvl,  loc,  t) > Math.min(remainingDemand, instLIRP.getClient(loc).getCapacity()) + Parameters.epsilon) {
							isFeasible = false;
							System.out.println("ERR in constraint (10), inventory at client " + loc + " in period " + t + " violates capacity constraint (max " + instLIRP.getClient(loc).getCapacity() +")"); ;
						}
					}
				}
			}
		}
		return isFeasible;
	}
}
