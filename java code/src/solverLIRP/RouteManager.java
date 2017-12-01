package solverLIRP;

import java.io.IOException;
import java.util.ArrayList;

import instanceManager.Client;
import instanceManager.Depot;
import instanceManager.Instance;
import instanceManager.Location;
import instanceManager.Parameters;
import instanceManager.Parameters;

public class RouteManager {

	/*
	 * ATTRIBUTES
	 */
	private Route[] directSD;		// Direct routes between the supplier and the depots
	private Route[] loopSD;			// Multi-stops routes from the supplier to the depots
	private Route[][] directDC;		// Direct routes between the depots and the clients (one set for each depot)
	private Route[][] loopDC;		// Multi-stops routes from the depots to the clients	(one set for each depot)

	/**
	 * Create a RouteManager object from an instance and the type of model under investigation
	 * @param instLIRP	the instance from which the set of routes is created
	 */
	public RouteManager(Instance instLIRP) throws IOException {
		/* Get the number of depots and the number of clients from the instance */
		int nbDepots = instLIRP.getNbDepots();
		int nbClients = instLIRP.getNbClients();
		
		/* Get the location of the supplier */
		Location supplier = instLIRP.getSupplier();
				
		/* Create direct routes for the instance */
		this.directSD = new Route[nbDepots];
		this.directDC = new Route[nbDepots][nbClients];
		for(int dIndex = 0; dIndex < nbDepots; dIndex++) {
			this.directSD[dIndex] = new Route(supplier, instLIRP.getDepot(dIndex));
			for(int cIndex = 0; cIndex < nbClients; cIndex++)
				directDC[dIndex][cIndex] = new Route(instLIRP.getDepot(dIndex), instLIRP.getClient(cIndex));
		}
		
		/* Get an upper bound on the maximum number of stops in a route */
		int maxNbStops = (int) Math.floor(Parameters.max_time_route/Parameters.stopping_time);
		
		/* If the stopping time is too long to include multi-stops routes, stop here */
		if(maxNbStops < 2) {
			loopSD = null;
			loopDC = null;
		}
		else {
			
		}
		ArrayList<Route> routesToAdd = new ArrayList<Route>();
	}

	/*
	 * ACCESSORS
	 */
	public getRoutes(Parameters.typeModel model) {
		switch (model) {
		case direct_direct:
			loopSD = null;
			loopDC = null;
			break;
		case direct_loop:
			loopSD = null;
			break;
		case loop_direct:
			loopDC = null;
			break;
		case loop_loop:
			break;
		default: 
			break;
		}
	}
}
