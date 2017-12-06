package solverLIRP;

import java.io.IOException;
import java.util.ArrayList;

import instanceManager.Client;
import instanceManager.Depot;
import instanceManager.Instance;
import instanceManager.Location;
import instanceManager.Parameters;

public class RouteManager {

	/*
	 * ATTRIBUTES
	 */
	private ArrayList<Route> directSD;	// Direct routes between the supplier and the depots
	private ArrayList<Route> loopSD;		// Multi-stops routes from the supplier to the depots
	private ArrayList<Route> directDC;	// Direct routes between the depots and the clients (one set for each depot)
	private ArrayList<Route> loopDC;		// Multi-stops routes from the depots to the clients	(one set for each depot)

	/**
	 * Create a RouteManager object from an instance and the type of model under investigation
	 * @param instLIRP	the instance from which the set of routes is created
	 */
	public RouteManager(Instance instLIRP) throws IOException {
		/* Create direct routes for the instance */
		this.directSD = new ArrayList<Route>();
		this.directDC = new ArrayList<Route>();
		this.populateDirectRoutes(instLIRP);
		this.populateLoops(instLIRP);
	}
	
	public RouteManager(RouteManager rm, ArrayList<Route> loopSD, ArrayList<Route> loopDC) {
		this.directSD = rm.getDirectSDRoutes();
		this.directDC = rm.getDirectDCRoutes();
		
		this.loopSD = loopSD;
		this.loopDC = loopDC;
	}

	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return	the ArrayList of direct routes from the supplier to the depots
	 */
	private ArrayList<Route> getDirectSDRoutes() {
		return this.directSD;
	}
	
	/**
	 * 
	 * @return	the ArrayList of direct routes from the depots to the clients
	 */
	private ArrayList<Route> getDirectDCRoutes() {
		return this.directDC;
	}
	
	/**
	 * 
	 * @return	the ArrayList of multi-stops routes from the supplier to the depots
	 */
	private ArrayList<Route> getLoopSDRoutes(){
		
		return this.loopSD;
	}
	/**
	 * 
	 * @return	the ArrayList of multi-stops routes from the supplier to the depots
	 */
	private ArrayList<Route> getLoopDCRoutes(){
		return this.loopDC;
	}

	/*
	 * METHODS
	 */
	/**
	 * Fill the Route ArrayList with the all valid direct routes for the instance considered
	 * @param instLIRP		the LIRP instance
	 * @throws IOException
	 */
	private void populateDirectRoutes(Instance instLIRP) throws IOException {
		/* Get the number of depots and the number of clients from the instance */
		int nbDepots = instLIRP.getNbDepots();
		int nbClients = instLIRP.getNbClients();
		
		for(int dIndex = 0; dIndex < nbDepots; dIndex++) {
			/* Create a new direct route from the supplier to the depot */
			Route dRoute = new Route(instLIRP.getSupplier(), instLIRP.getDepot(dIndex));
			/* If its duration is lower than the maximum time allowed, add it to the list */
			if(dRoute.isValid())
				this.directSD.add(dRoute);

			for(int cIndex = 0; cIndex < nbClients; cIndex++) {
				/* Create a new direct Route object between the current depot and client */
				Route cRoute = new Route(instLIRP.getDepot(dIndex), instLIRP.getClient(cIndex));
				/* If its duration is lower than the maximum time allowed, add it to the list */
				if(cRoute.isValid())
					this.directDC.add(cRoute);
			}
		}
	}
	
	/**
	 * Fill the loop ArrayList of Route objects with multi-stops routes for the instance considered
	 * @param instLIRP		the LIRP instance
	 * @throws IOException
	 */
	private void populateLoops(Instance instLIRP) throws IOException {
		/* Get the number of depots and the number of clients from the instance */
		int nbDepots = instLIRP.getNbDepots();
		int nbClients = instLIRP.getNbClients();
		
		/* Get an upper bound on the maximum number of stops in a route */
		int maxNbStops = (int) Math.ceil(Parameters.max_time_route/Parameters.stopping_time);
		
		/* If the stopping time is too long to include multi-stops routes, stop here */
		if(maxNbStops < 3) {
			loopSD = null;
			loopDC = null;
		}
		
		/* If the stopping time is small enough, build the multi-stops routes for this instance */
		else {
			for(int nbStops = 2; nbStops < maxNbStops; nbStops++) {
				/* Compute the remaining time that can be used to travel */
				double remainingTime = Parameters.max_time_route - nbStops * Parameters.stopping_time;
				
				ArrayList<Route> routesDCToAdd = new ArrayList<Route>();

				for (int cIndex = 0; cIndex < nbClients; cIndex++) {
					
				}
			}
		}
	}
	
	private ArrayList<Location> computeStops(Route currentLoop, ArrayList<Location> stopCandidates, int nbRemainingStops, int startingIndex, int endIndex) {
		
		if(nbRemainingStops < 2) {
			for(remainingStops
		}
		if(endIndex - startingIndex < remainingStops)
			return null;
		else if(endIndex - startingIndex == remainingStops) {
			for(int index = remainingStops;  index > 0; index--) {
				assignedStops[assignedStops.length - index] = endIndex - index;
			}
			return assignedStops;
		}
		else {

		}	
		return new int[10];
	}
	
	/**
	 * 
	 * @param splitParam	the maximum number of routes in a subset
	 * @return			an array of RouteManager objects with no more than splitParam routes in each loop arrays
	 */
	public RouteManager[] sampleRoutes(int splitParam){
		int nbManagers = (int) Math.ceil(this.loopSD.length / splitParam);
		RouteManager[] resultRManagers = new RouteManager[nbManagers];
		
		return resultRManagers;
	}

}
