package solverLIRP;
import java.io.PrintStream;

import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import instanceManager.Instance;

//import javax.print.attribute.standard.PrinterLocation;

public final class Checker {
	public static boolean check(Solution sol, Instance instance, Route[] routesSD, Route[] routesDC, PrintStream printStreamSol) {

		int[][] Alpha = new int[instance.getNbClients()][routesDC.length]; // =1 if client i is in route r
		int[][] Beta = new int[instance.getNbDepots()][routesDC.length]; // = 1 if depot j is in route r
		int[][] Gamma = new int[instance.getNbDepots()][routesSD.length]; // = 1 if depot j is in route r
		int verbose=0;
		int CMAX = 4;

		/* Definition of parameters Alpha and Beta (for the depots-clients routes)*/
		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		for(int rIter = 0; rIter < routesDC.length; rIter++) {
			for(int cIter = 0; cIter < instance.getNbClients(); cIter++)
				Alpha[cIter][rIter] = routesDC[rIter].containsLocation(instance.getClient(cIter)) ? 1 : 0;
			for(int dIter = 0; dIter < instance.getNbDepots(); dIter++)
				Beta[dIter][rIter] = routesDC[rIter].containsLocation(instance.getDepot(dIter)) ? 1 : 0;
		}

		/* Definition of parameters Gamma (for the supplier-depots routes) */
		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		for(int rIter = 0; rIter < routesSD.length; rIter++) {
			for(int dIter = 0; dIter < instance.getNbDepots(); dIter++)
				Gamma[dIter][rIter] = routesSD[rIter].containsLocation(instance.getDepot(dIter)) ? 1 : 0;
		}

		/* The return value of the checker */
		boolean isFeasible = true;

		for (int t = 0; t < instance.getNbPeriods(); t++) {
			/* Check constraints on the route for depots */
			for(int dIter = 0; dIter < instance.getNbDepots(); dIter++) {
				/* Each depot is served by at most one route in every period (2) */
				int routeDepotConst = 0;
				for (int rIter = 0; rIter < routesDC.length; rIter++) 
					routeDepotConst += Beta[dIter][rIter] * sol.getDCRouteUse(rIter, t);
				if(routeDepotConst > 1) {
					System.out.println("ERROR, Constraint 2, depot " + dIter + ",  period "+t);
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 2, depot " + dIter + ",  period "+ t +"         " + routeDepotConst );
				}
			}

			/* Each client is served by at most one route in every period (3)*/
			for(int cIter = 0; cIter < instance.getNbClients(); cIter++) {
				int routeClientConst = 0;
				for (int rIter = 0; rIter < routesDC.length; rIter++)
					routeClientConst += Alpha[cIter][rIter] * sol.getDCRouteUse(rIter, t);
				if(routeClientConst > 1) {
					System.out.println("ERROR, Constraint 3, client " + cIter + ",  period "+ t);
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 3, client" + cIter + ",  period "+ t +"         " + routeClientConst );
				}
			}

			for(int dIter = 0; dIter < instance.getNbDepots(); dIter++) {
				for (int rIter = 0; rIter < routesSD.length; rIter++) {
					/* A supplier-depot route is used only if all depots on this route are opened (4)*/
					int routeSupplierConst = Gamma[dIter][rIter] * sol.getSDRouteUse(rIter, t);
					if(routeSupplierConst > 0 && !sol.isOpenDepot(dIter)) {
						System.out.println("ERROR, Constraint 4, depot " + dIter + ",  period "+ t);
						isFeasible = false;
					}
					else {
						if (verbose < 0)
							System.out.println("BINDING, Constraint 4, depot " + dIter + ",  period "+ t +"         " + routeSupplierConst );
					}
				}
			}

			/* Each route delivering customers can only departs from an opened depot (5)*/
			for (int rIter = 0; rIter < routesDC.length; rIter++) {
				int routeOpenDepot = 0;
				for(int dIter = 0; dIter < instance.getNbDepots(); dIter++) {
					if(sol.isOpenDepot(dIter))
						routeOpenDepot += Beta[dIter][rIter];
				}
				if(sol.getDCRouteUse(rIter, t) > routeOpenDepot) {
					System.out.println("ERROR, Constraint 5,  period "+ t);
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 5,  period "+ t +"         " + routeOpenDepot );
				}
			}

			/* Vehicle capacity on each delivery on each opened supplier-depot route (6)*/
			for (int rIter = 0; rIter < routesSD.length; rIter++) {
				double routeSumQDepots = 0;
				for(int dIter = 0; dIter < instance.getNbDepots(); dIter++)
					routeSumQDepots += sol.getDeliveryDepot(dIter, rIter, t);
				// TO MODIFY IN THE FINAL VERSION
				if(sol.getSDRouteUse(rIter, t) && routeSumQDepots < instance.getCapacityVehicle(0))
				expr6.addTerm(-instance.getCapacityVehicle(0), this.x[rIter][t]);
				this.LIRPSolver.addLe(expr6, 0);
			}


			/* Vehicle capacity on each delivery on each opened depot-client route (7)*/
			for (int rIter = 0; rIter < routesDC.length; rIter++) {
				IloLinearNumExpr expr7 = LIRPSolver.linearNumExpr();
				for(int cIter = 0; cIter < instance.getNbClients(); cIter++)
					expr7.addTerm(1, this.u[cIter][rIter][t]);
				// TO MODIFY IN THE FINAL VERSION
				expr7.addTerm(-this.LIRPInstance.getCapacityVehicle(0), this.x[rIter][t]);
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

		return isFeasible;
	}
}
