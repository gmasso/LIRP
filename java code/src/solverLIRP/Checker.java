package solverLIRP;
import java.io.PrintStream;

import instanceManager.Instance;
import tools.Parameters;

//import javax.print.attribute.standard.PrinterLocation;

public final class Checker {
	public static boolean check(Solution sol, Instance instance, Route[][] routes, PrintStream printStreamSol) {

		int[][] Alpha = new int[instance.getNbClients()][routes[1].length]; // =1 if client i is in route r
		int[][] Beta = new int[instance.getNbDepots(0)][routes[1].length]; // = 1 if depot j is in route r
		int[][] Gamma = new int[instance.getNbDepots(0)][routes[0].length]; // = 1 if depot j is in route r
		int verbose=0;

		/* Definition of parameters Alpha and Beta (for the depots-clients routes)*/
		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		for(int rIter = 0; rIter < routes[1].length; rIter++) {
			for(int cIter = 0; cIter < instance.getNbClients(); cIter++)
				Alpha[cIter][rIter] = routes[1][rIter].containsStop(cIter) ? 1 : 0;
			for(int dIter = 0; dIter < instance.getNbDepots(0); dIter++)
				Beta[dIter][rIter] = routes[1][rIter].containsLocation(instance.getDepot(0, dIter)) ? 1 : 0;
		}

		/* Definition of parameters Gamma (for the supplier-depots routes) */
		/* Fill the two arrays by checking for each route which clients and which depots it contains */
		for(int rIter = 0; rIter < routes[0].length; rIter++) {
			for(int dIter = 0; dIter < instance.getNbDepots(0); dIter++)
				Gamma[dIter][rIter] = routes[0][rIter].containsStop(dIter) ? 1 : 0;
		}

		/* The return value of the checker */
		boolean isFeasible = true;

		for (int t = 0; t < instance.getNbPeriods(); t++) {
			/* Check constraints on the route for depots */
			for(int dIter = 0; dIter < instance.getNbDepots(0); dIter++) {
				/* Each depot is served by at most one route in every period (2) */
				int routeDepotConst = 0;
				for (int rIter = 0; rIter < routes[0].length; rIter++) 
					if(sol.isUsedRoute(0, rIter, t))
						routeDepotConst += Gamma[dIter][rIter] ;
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
				for (int rIter = 0; rIter < routes[1].length; rIter++)
					if(sol.isUsedRoute(1, rIter, t))
						routeClientConst += Alpha[cIter][rIter];
				if(routeClientConst > 1) {
					System.out.println("ERROR, Constraint 3, client " + cIter + ",  period "+ t);
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 3, client" + cIter + ",  period "+ t +"         " + routeClientConst );
				}
			}

			for(int dIter = 0; dIter < instance.getNbDepots(0); dIter++) {
				for (int rIter = 0; rIter < routes[0].length; rIter++) {
					/* A supplier-depot route is used only if all depots on this route are opened (4)*/
					int routeSupplierConst = 0;
					if(sol.isUsedRoute(0, rIter, t))
						routeSupplierConst = Gamma[dIter][rIter];
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
			for (int rIter = 0; rIter < routes[1].length; rIter++) {
				int routeOpenDepot = 0;
				for(int dIter = 0; dIter < instance.getNbDepots(0); dIter++) {
					if(sol.isOpenDepot(dIter))
						routeOpenDepot += Beta[dIter][rIter];
				}
				if(sol.isUsedRoute(1, rIter, t) && routeOpenDepot < 1) {
					System.out.println("ERROR, Constraint 5,  period "+ t);
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 5,  period "+ t +"         " + routeOpenDepot );
				}
			}

			/* Vehicle capacity on each delivery on each opened supplier-depot route (6)*/
			for (int rIter = 0; rIter < routes[0].length; rIter++) {
				double routeSumQDepots = 0;
				for(int dIter = 0; dIter < instance.getNbDepots(0); dIter++)
					routeSumQDepots += sol.getDeliveryDepot(dIter, rIter, t);
				// TO MODIFY IN THE FINAL VERSION
				if((!sol.isUsedRoute(0, rIter, t) && routeSumQDepots > 0) || (sol.isUsedRoute(0, rIter, t) && routeSumQDepots > instance.getCapacityVehicle(0))) {
					System.out.println("ERROR, Constraint 6, period "+ t);
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 6,  period "+ t +"         " + routeSumQDepots );
				}
			}


			/* Vehicle capacity on each delivery on each opened depot-client route (7)*/
			for (int rIter = 0; rIter < routes[1].length; rIter++) {
				double routeSumQClients = 0;
				for(int cIter = 0; cIter < instance.getNbClients(); cIter++)
					routeSumQClients += sol.getDeliveryClient(cIter, rIter, t);
				// TO MODIFY IN THE FINAL VERSION
				if((!sol.isUsedRoute(1, rIter, t) && routeSumQClients > 0) || (sol.isUsedRoute(1, rIter, t) && routeSumQClients > instance.getCapacityVehicle(0))) {
					System.out.println("ERROR, Constraint 7, period "+ t);
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 7,  period "+ t +"         " + routeSumQClients );
				}
			}

			/* Flow conservation at the depots (8) */
			for(int dIter = 0; dIter < instance.getNbDepots(0); dIter++) {
				double lastInvDepot;
				if (t==0)
					lastInvDepot = instance.getDepot(0, dIter).getInitialInventory();
				else
					lastInvDepot = sol.getStockDepot(dIter, t - 1);
				
				for(int rSDIter = 0; rSDIter < routes[0].length; rSDIter++)
					lastInvDepot += sol.getDeliveryDepot(dIter, rSDIter, t);
				
				double newInvDepot = sol.getStockDepot(dIter, t);
				for (int rDCIter = 0; rDCIter < routes[1].length; rDCIter++) {
					for (int cIter = 0; cIter < instance.getNbClients(); cIter++)
						newInvDepot += Beta[dIter][rDCIter] * sol.getDeliveryClient(cIter,  rDCIter,  t);
				}
				if((lastInvDepot > newInvDepot + Parameters.epsilon) || (lastInvDepot < newInvDepot - Parameters.epsilon)) {
					System.out.println("ERROR, Constraint 8, period "+ t + ": diff " + (newInvDepot - lastInvDepot));
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 8,  period "+ t +"         " + (newInvDepot - lastInvDepot));
				}
			}

			/* Flow conservation at the clients (9) */
			for(int cIter = 0; cIter < instance.getNbClients(); cIter++) {
				double lastInvClient;
				if (t==0)
					lastInvClient = instance.getClient(cIter).getInitialInventory();
				else
					lastInvClient = sol.getStockClient(cIter, t - 1);
				for(int rIter = 0; rIter < routes[1].length; rIter++)
					lastInvClient += sol.getDeliveryClient(cIter, rIter, t);
				double newInvClient = sol.getStockClient(cIter, t) + instance.getClient(cIter).getDemand(t);
				if((lastInvClient > newInvClient + Parameters.epsilon) || (lastInvClient < newInvClient - Parameters.epsilon)) {
					System.out.println("ERROR, Constraint 9, period "+ t+ ": diff " + (newInvClient - lastInvClient));
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 9,  period "+ t +"         " + (newInvClient - lastInvClient));
				}
			}

			/* Stock capacity at the client or ensuring that the inventory is not greater than the sum of remaining demands (10) */
			// IS THIS LAST ASPECT OF THE CONSTRAINT REALLY USEFUL? 
			for (int cIter = 0; cIter < instance.getNbClients(); cIter++) {
				double ub = Math.min(instance.getClient(cIter).getCumulDemands(t+1, instance.getNbPeriods()), instance.getClient(cIter).getCapacity());
				double currentInvClient = sol.getStockClient(cIter, t);
				if(currentInvClient > ub + Parameters.epsilon) {
					System.out.println("ERROR, Constraint 10, client " + cIter + " in period "+ t);
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 10, client " + cIter + " in  period " + t + "         " + currentInvClient);
				}
			}

			/* Capacity constraints at depots (11) */
			for (int dIter = 0; dIter < instance.getNbDepots(0); dIter++) {
				double currentInvDepot = sol.getStockDepot(dIter,  t);
				if(currentInvDepot > instance.getDepot(0, dIter).getCapacity() + Parameters.epsilon) {
					System.out.println("ERROR, Constraint 11, depot " + dIter + " in period "+ t);
					isFeasible = false;
				}
				else {
					if (verbose < 0)
						System.out.println("BINDING, Constraint 10, depot " + dIter + " in  period " + t + "         " + currentInvDepot);
				}
			}
		}
		return isFeasible;
	}
}
