
/* test */

package solverLIRP;

import java.io.IOException;
import java.util.ArrayList;
import instanceManager.Instance;
import instanceManager.Parameters;

public class RouteManager {

	/*=====================
	 *      ATTRIBUTES
	 ======================*/
	private Instance instanceLIRP;	// The instance to which the routes apply
	private Route[] directSD;		// Direct routes between the supplier and the depots
	private Route[] directDC;		// Direct routes between the depots and the clients (one set for each depot)
	private Route[] loopSD;			// Multi-stops routes from the supplier to the depots
	private Route[] loopDC;		// Multi-stops routes from the depots to the clients	(one set for each depot)
	
	private boolean[] reachDepots;		//Array storing the depots that are reachable according to the maximum route length
	private boolean[] reachClients;		//Array storing the clients that are reachable according to the maximum route length


	/**
	 * Create a RouteManager object from an instance and the type of model under investigation
	 * @param instLIRP	the instance from which the set of routes is created
	 */
	public RouteManager(Instance instLIRP) throws IOException {
		/* Create direct routes for the instance */
		this.instanceLIRP = instLIRP;
		this.reachDepots = new boolean[this.instanceLIRP.getNbDepots()];
		this.reachClients = new boolean[this.instanceLIRP.getNbClients()];
		this.populateDirectRoutes();
		this.populateLoops();
		System.out.println("Route manager created.");
	}

//	/**
//	 * Create a new RouteManager object from an existing one, changing the multi-stops routes
//	 * @param rm			the RouteManager object from which the new RouteManager is created
//	 * @param loopSD		the indices at which to collect multi-stops routes between the supplier and the depots from rm to add them to the new route manager
//	 * @param loopDC		the indices at which to collect multi-stops routes between the depots and the clients from rm to add them to the new route manager
//	 */
//	private RouteManager(RouteManager rm, int[] loopSDIndices, int[] loopDCIndices) {
//		this.instanceLIRP = rm.getInstance();
//		this.directSD = rm.getDirectSDRoutes();
//		this.directDC = rm.getDirectDCRoutes();
//
//		this.loopSD = new ArrayList<Route>();
//		for(int routeSDIter = 0; routeSDIter < loopSDIndices.length; routeSDIter++) {
//			this.loopSD.add(rm.getSDRoute(routeSDIter));
//		}
//		this.loopDC = new ArrayList<Route>();
//		for(int routeDCIter = 0; routeDCIter < loopDCIndices.length; routeDCIter++) {
//			this.loopDC.add(rm.getDCRoute(routeDCIter));
//		}
//	}
//
//	/**
//	 * Creates a new RouteManager object using the direct routes of an existing one, but replacing its multi-stops routes with new ones
//	 * @param rm		the RouteManager object from which the new RouteManager is created
//	 * @param loopSD	the multi-stops routes between the supplier and the depots to add to the manager
//	 * @param loopDC	the multi-stops routes between the depots and the clients to add to the manager
//	 */
//	private RouteManager(RouteManager rm, ArrayList<Route> loopSD, ArrayList<Route> loopDC) {
//		this.instanceLIRP = rm.getInstance();
//		this.directSD = rm.getDirectSDRoutes();
//		this.directDC = rm.getDirectDCRoutes();
//
//		this.loopSD = loopSD;
//		this.loopDC = loopDC;
//	}

	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return	the instance to which the routes in the RouteManager object relate
	 */
	public Instance getInstance() {
		return this.instanceLIRP;
	}

	/**
	 * 
	 * @return	the ArrayList of direct routes from the supplier to the depots
	 */
	public Route[] getDirectSDRoutes() {
		return this.directSD;
	}

	/**
	 * 
	 * @return	the ArrayList of direct routes from the depots to the clients
	 */
	public Route[] getDirectDCRoutes() {
		return this.directDC;
	}

	/**
	 * 
	 * @return	the ArrayList of multi-stops routes from the supplier to the depots
	 */
	public Route[] getLoopSDRoutes(){

		return this.loopSD;
	}

	/**
	 * 
	 * @return	the ArrayList of multi-stops routes from the supplier to the depots
	 */
	public Route[] getLoopDCRoutes(){
		return this.loopDC;
	}
	
	/**
	 * 
	 * @return	the ArrayList of multi-stops routes from the supplier to the depots
	 */
	public int getNbLoopDC(){
		return this.loopDC.length;
	}

	/**
	 * 
	 * @return	an array containing of all Route objects in the route manager from the supplier to the depots
	 */
	public Route[] getSDRoutes(ArrayList<Integer> indices) {
		Route[] routesSD = new Route[this.directSD.length + indices.size()];

		for(int directSDIter = 0; directSDIter < this.directSD.length; directSDIter++) {
			routesSD[directSDIter] = this.directSD[directSDIter];
		}

		for(int loopSDIter : indices) {
			routesSD[this.directSD.length + loopSDIter] = this.loopSD[loopSDIter];
		}
		return routesSD;
	}

	/**
	 * 
	 * @return	an array containing of all Route objects in the route manager from depots to clients
	 */
	public Route[] getDCRoutes(ArrayList<Integer> indices) {
		Route[] routesDC = new Route[this.directDC.length + indices.size()];

		for(int directDCIter = 0; directDCIter < this.directDC.length; directDCIter++) {
			routesDC[directDCIter] = this.directDC[directDCIter];
		}
		for(int loopDCIter : indices) {
			routesDC[this.directDC.length + loopDCIter] = this.loopDC[loopDCIter];
		}
		return routesDC;
	}

	public boolean allReachable() {
		boolean allReachable = true;
		for(boolean isReachable : this.reachClients)
			allReachable = allReachable && isReachable;
		return allReachable;
	}
	
//	/**
//	 * 
//	 * @param rIndex		the index of the route of interest in the concatenation of directSD and loopSD
//	 * @return			the route between the supplier and a depot located at index rIndex
//	 */
//	public Route getSDRoute(int rIndex) {
//		if(rIndex < this.directSD.length)
//			return this.directSD[rIndex];
//		else if(rIndex < this.directSD.length + this.loopSD.length)
//			return this.loopSD[rIndex - this.directSD.length];
//		return null;
//	}
//
//	/**
//	 * 
//	 * @param rIndex		the index of the route of interest in the concatenation of directDC and loopDC
//	 * @return			the route between a depot and a client located at index rIndex
//	 */
//	public Route getDCRoute(int rIndex) {
//		if(rIndex < this.directDC.length)
//			return this.directDC[rIndex];
//		else if(rIndex < this.directDC.length + this.loopDC.length)
//			return this.loopDC[rIndex - this.directDC.length];
//		return null;
//	}

	/*
	 * METHODS
	 */
	/**
	 * Fill the direct routes arrays with the all valid direct routes for the instance considered
	 * @param instLIRP		the LIRP instance
	 * @throws IOException
	 */
	private void populateDirectRoutes() throws IOException {
		/* List to store the feasible routes as they are computed */
		ArrayList<Route> listSD = new ArrayList<Route>();
		ArrayList<Route> listDC = new ArrayList<Route>();
		
		for(int dIndex = 0; dIndex < this.instanceLIRP.getNbDepots(); dIndex++) {
			/* Create a new direct route from the supplier to the depot */
			Route dRoute = new Route(this.instanceLIRP, -1, dIndex);
			/* If its duration is lower than the maximum time allowed, add it to the list */
			if(dRoute.isValid()) {
				this.reachDepots[dIndex] = true;
				listSD.add(dRoute);
			}
			for(int cIndex = 0; cIndex < this.instanceLIRP.getNbClients(); cIndex++) {
				/* Create a new direct Route object between the current depot and client */
				Route cRoute = new Route(this.instanceLIRP, dIndex, cIndex);
				/* If its duration is lower than the maximum time allowed, add it to the list */
				if(cRoute.isValid()) {
					this.reachClients[cIndex] = true;
					listDC.add(cRoute);
				}
			}
		}
		this.directSD = listSD.toArray(new Route[listSD.size()]);
		this.directDC = listDC.toArray(new Route[listDC.size()]);
	}

	/**
	 * Fill the loop routes arrays with multi-stops routes for the instance considered
	 * @param instLIRP		the LIRP instance
	 * @throws IOException
	 */
	private void populateLoops() throws IOException {
		/* List to store the feasible routes as they are computed */
		ArrayList<Route> listSD = new ArrayList<Route>();
		ArrayList<Route> listDC = new ArrayList<Route>();

		/* Get an upper bound on the maximum number of stops in a route */
		int maxNbStops = (int) Math.floor(Parameters.max_time_route/Parameters.stopping_time);

		/* If the stopping time is small enough, build the multi-stops routes for this instance */
		if(maxNbStops > 1){
			/* Fill a list with depots candidates for insertion in loops */
			ArrayList<Integer> stopDCandidates = new ArrayList<Integer>();
			for(int dIter = 0; dIter < this.instanceLIRP.getNbDepots(); dIter++) {
				if(this.reachDepots[dIter])
					stopDCandidates.add(dIter);
			}
			/* Create loops with the different possible combinations of stops */
			for(int dIndex = 0; dIndex < stopDCandidates.size() - 1; dIndex++) {
				Route initSDRoute = new Route(this.instanceLIRP, -1, dIndex);
				listSD.addAll(computeAllRoutes(initSDRoute, new ArrayList<Integer>(stopDCandidates.subList(dIndex + 1, stopDCandidates.size())), maxNbStops - 1));
			}

			/* Fill a list with depots candidates for insertion in loops */
			ArrayList<Integer> stopCCandidates = new ArrayList<Integer>();
			for(int cIter = 0; cIter < this.instanceLIRP.getNbClients(); cIter++) {
				if(this.reachClients[cIter])
					stopCCandidates.add(cIter);
			}
			/* Create loops starting from each depot */
			for(int dIter = 0; dIter < this.instanceLIRP.getNbDepots(); dIter++) {
				/* Routes from the depot dIter are useful only if the depot is reachable from the supplier */
				if(this.reachDepots[dIter]) {
					/* Create loops with the different possible combinations of stops */
					for(int cIndex = 0; cIndex < stopCCandidates.size() - 1; cIndex++) {
						Route initDCRoute = new Route(this.instanceLIRP, dIter, cIndex);
						listDC.addAll(computeAllRoutes(initDCRoute, new ArrayList<Integer>(stopCCandidates.subList(cIndex + 1, stopCCandidates.size())), maxNbStops - 1));
					}
				}
			}
		}
		this.loopSD = listSD.toArray(new Route[listSD.size()]);
		this.loopDC = listDC.toArray(new Route[listDC.size()]);
	}

	/**
	 * 
	 * @param currentRoute		the current Route object that is considered as a basis to build new ones
	 * @param stopCandidates		the stops that can be added to currentRoute
	 * @param nbRemainingStops	the maximum number of stops that can be added to the currentRoute
	 * @return
	 * @throws IOException
	 */
	private ArrayList<Route> computeAllRoutes(Route currentRoute, ArrayList<Integer> stopCandidates, int nbRemainingStops) throws IOException {
		ArrayList<Route> routesToAdd = new ArrayList<Route>();
		/* If some stop candidates remain to extend the route, try to add them to the route */
		if(nbRemainingStops > 0 && !stopCandidates.isEmpty()) {
			for(int stopIter = 0; stopIter < stopCandidates.size(); stopIter++) {
				/* Create a new Route object by adding one stop among the candidates to currentRoute */
				Route routeCandidate = currentRoute.extend(stopCandidates.get(stopIter));
				/* If it is valid, add it to the set of routes to add and call recursively */
				if(routeCandidate.isValid()) {
					routesToAdd.add(routeCandidate);
					/* If the stop currently added is not the last of the list, call recursively with the remaining candidates */
					if(stopIter < stopCandidates.size() - 1) {
						ArrayList<Integer> newStopCandidates = new ArrayList<Integer>(stopCandidates.subList(stopIter + 1, stopCandidates.size()));
						routesToAdd.addAll(computeAllRoutes(routeCandidate, newStopCandidates, nbRemainingStops - 1));
					}
				}
			}
		}
		return routesToAdd;
	}
<<<<<<< HEAD
=======

	/**
	 * 
	 * @param splitParam	the maximum number of routes in a subset
	 * @return			an array of RouteManager objects with no more than splitParam routes in each loop arrays
	 */
	public RouteManager[] sampleRoutes(int splitParam){
		int nbManagers = (int) Math.ceil((this.loopSD.size() + this.loopDC.size()) / splitParam);
		RouteManager[] resultRManagers = new RouteManager[nbManagers];

		for (int i = 0; i < nbManagers - 1; i++){
			// The array containing the indices of the loop DC routes to add to the subset
			int[] routeDCSubset = new int[splitParam];// subset creation to fill routes with split parameter size 
			// Create an empty array for the index of SD loop routes to use
			int[] SDRouteSubset = new int[0];

			for (int j = 0; j < splitParam; j++){
				routeDCSubset[j] = i * splitParam + j;
			}
			resultRManagers[i] = new RouteManager(this, SDRouteSubset, routeDCSubset);
		}

		int diff = this.loopDC.size() - nbManagers * splitParam;
		if(diff ==  0) {
			int[] routeDCSubset = new int[splitParam];
			for (int j = 0; j < splitParam; j++){
				routeDCSubset[j] = (nbManagers - 1) * splitParam + j;
			}
			resultRManagers[nbManagers-1] = new RouteManager(this, new int[0], routeDCSubset);
		}
		else {
			int[] routeDCSubset = new int[splitParam];
			for (int j = 0; j < diff; j++){
				routeDCSubset[j] = (nbManagers - 1) * splitParam + j;
			}
			for(int j = 0; j < splitParam - diff; j++) {
				routeDCSubset[diff + j] = j;
			}
			resultRManagers[nbManagers-1] = new RouteManager(this, new int[0], routeDCSubset);
		}
		return resultRManagers;
	}
>>>>>>> c3f3a8615abe12dd4b26cc7589d113c61d52994d
}
