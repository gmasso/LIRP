
/* test */

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
	private Instance instanceLIRP;		// The instance to which the routes apply
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
		this.instanceLIRP = instLIRP;
		this.directSD = new ArrayList<Route>();
		this.directDC = new ArrayList<Route>();
		this.populateDirectRoutes(instLIRP);

		this.loopSD = new ArrayList<Route>();
		this.loopDC = new ArrayList<Route>();
		this.populateLoops(instLIRP);
	}

	/**
	 * Create a new RouteManager object from an existing one, changing the multi-stops routes
	 * @param rm			the RouteManager object from which the new RouteManager is created
	 * @param loopSD		the indices at which to collect multi-stops routes between the supplier and the depots from rm to add them to the new route manager
	 * @param loopDC		the indices at which to collect multi-stops routes between the depots and the clients from rm to add them to the new route manager
	 */
	private RouteManager(RouteManager rm, int[] loopSDIndices, int[] loopDCIndices) {
		this.instanceLIRP = rm.getInstance();
		this.directSD = rm.getDirectSDRoutes();
		this.directDC = rm.getDirectDCRoutes();

		this.loopSD = new ArrayList<Route>();
		for(int routeSDIter = 0; routeSDIter < loopSDIndices.length; routeSDIter++) {
			this.loopSD.add(rm.getSDRoute(routeSDIter));
		}
		this.loopDC = new ArrayList<Route>();
		for(int routeDCIter = 0; routeDCIter < loopDCIndices.length; routeDCIter++) {
			this.loopDC.add(rm.getDCRoute(routeDCIter));
		}
	}

	/**
	 * Creates a new RouteManager object using the direct routes of an existing one, but replacing its multi-stops routes with new ones
	 * @param rm		the RouteManager object from which the new RouteManager is created
	 * @param loopSD	the multi-stops routes between the supplier and the depots to add to the manager
	 * @param loopDC	the multi-stops routes between the depots and the clients to add to the manager
	 */
	private RouteManager(RouteManager rm, ArrayList<Route> loopSD, ArrayList<Route> loopDC) {
		this.instanceLIRP = rm.getInstance();
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
	 * @return	the instance to which the routes in the RouteManager object relate
	 */
	public Instance getInstance() {
		return this.instanceLIRP;
	}

	/**
	 * 
	 * @return	the ArrayList of direct routes from the supplier to the depots
	 */
	public ArrayList<Route> getDirectSDRoutes() {
		return this.directSD;
	}

	/**
	 * 
	 * @return	the ArrayList of direct routes from the depots to the clients
	 */
	public ArrayList<Route> getDirectDCRoutes() {
		return this.directDC;
	}

	/**
	 * 
	 * @return	the ArrayList of multi-stops routes from the supplier to the depots
	 */
	public ArrayList<Route> getLoopSDRoutes(){

		return this.loopSD;
	}

	/**
	 * 
	 * @return	the ArrayList of multi-stops routes from the supplier to the depots
	 */
	public ArrayList<Route> getLoopDCRoutes(){
		return this.loopDC;
	}

	/**
	 * 
	 * @return	an array containing of all Route objects in the route manager from the supplier to the depots
	 */
	public Route[] getSDRoutes() {
		Route[] routesSD = new Route[this.directSD.size() + this.loopSD.size()];

		for(int directSDIter = 0; directSDIter < this.directSD.size(); directSDIter++) {
			routesSD[directSDIter] = this.directSD.get(directSDIter);
		}

		for(int loopSDIter = 0; loopSDIter < this.loopSD.size(); loopSDIter++) {
			routesSD[this.loopSD.size() + loopSDIter] = this.loopSD.get(loopSDIter);
		}
		return routesSD;
	}

	/**
	 * 
	 * @return	an array containing of all Route objects in the route manager from depots to clients
	 */
	public Route[] getDCRoutes() {
		Route[] routesDC = new Route[this.directDC.size() + this.loopDC.size()];

		for(int directDCIter = 0; directDCIter < this.directDC.size(); directDCIter++) {
			routesDC[directDCIter] = this.directDC.get(directDCIter);
		}

		for(int loopDCIter = 0; loopDCIter < this.loopDC.size(); loopDCIter++) {
			routesDC[this.loopDC.size() + loopDCIter] = this.loopDC.get(loopDCIter);
		}

		return routesDC;
	}

	/**
	 * 
	 * @param rIndex		the index of the route of interest in the concatenation of directSD and loopSD
	 * @return			the route between the supplier and a depot located at index rIndex
	 */
	public Route getSDRoute(int rIndex) {
		if(rIndex < this.directSD.size())
			return this.directSD.get(rIndex);
		else if(rIndex < this.directSD.size() + this.loopSD.size())
			return this.loopSD.get(rIndex - this.directSD.size());

		return null;
	}

	/**
	 * 
	 * @param rIndex		the index of the route of interest in the concatenation of directDC and loopDC
	 * @return			the route between a depot and a client located at index rIndex
	 */
	public Route getDCRoute(int rIndex) {
		if(rIndex < this.directDC.size())
			return this.directDC.get(rIndex);
		else if(rIndex < this.directDC.size() + this.loopDC.size())
			return this.loopDC.get(rIndex - this.directDC.size());

		return null;
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
		int maxNbStops = (int) Math.floor(Parameters.max_time_route/Parameters.stopping_time);

		/* If the stopping time is small enough, build the multi-stops routes for this instance */
		if(maxNbStops > 1){
			ArrayList<Depot> stopDCandidates = new ArrayList<Depot>();
			for(int dIter = 0; dIter < nbDepots; dIter++) {
				stopDCandidates.add(instLIRP.getDepot(dIter));
			}
			for(int dIndex = 0; dIndex < stopDCandidates.size() - 1; dIndex++) {
				Route initSDRoute = new Route(instLIRP.getSupplier(), stopDCandidates.get(dIndex));
				this.loopSD.addAll(computeAllRoutes(initSDRoute, new ArrayList<Location>(stopDCandidates.subList(dIndex + 1, stopDCandidates.size())), maxNbStops - 1));
			}

			ArrayList<Client> stopCCandidates = new ArrayList<Client>();
			for(int cIter = 0; cIter < nbClients; cIter++) {
				stopCCandidates.add(instLIRP.getClient(cIter));
			}
			for(int dIter = 0; dIter < nbDepots; dIter++) {
				for(int cIndex = 0; cIndex < stopCCandidates.size() - 1; cIndex++) {
					Route initDCRoute = new Route(instLIRP.getDepot(dIter), stopCCandidates.get(cIndex));
					this.loopDC.addAll(computeAllRoutes(initDCRoute, new ArrayList<Location>(stopCCandidates.subList(cIndex + 1, stopCCandidates.size())), maxNbStops - 1));
				}
			}
		}
	}

	/**
	 * 
	 * @param currentRoute		the current Route object that is considered as a basis to build new ones
	 * @param stopCandidates		the stops that can be added to currentRoute
	 * @param nbRemainingStops	the maximum number of stops that can be added to the currentRoute
	 * @return
	 * @throws IOException
	 */
	private ArrayList<Route> computeAllRoutes(Route currentRoute, ArrayList<Location> stopCandidates, int nbRemainingStops) throws IOException {
		ArrayList<Route> routesToAdd = new ArrayList<Route>();
		/* If some stop candidates remain to extend the route, try to add them to the route */
		if(nbRemainingStops > 0 && !stopCandidates.isEmpty()) {
			for(int stopIter = 0; stopIter < stopCandidates.size(); stopIter++) {
				/* Create a new Route object by adding one stop among the candidates to currentRoute */
				Route routeCandidate = currentRoute.extend(stopCandidates.get(stopIter));
				/* If it is valid, add it to the set of routes to add and call recursively */
				if(routeCandidate.isValid()) {
					System.out.println("valid ");
					routesToAdd.add(routeCandidate);
					/* If the stop currently added is not the last of the list, call recursively with the remaining candidates */
					if(stopIter < stopCandidates.size() - 1) {
						ArrayList<Location> newStopCandidates = new ArrayList<Location>(stopCandidates.subList(stopIter + 1, stopCandidates.size()));
						routesToAdd.addAll(computeAllRoutes(routeCandidate, newStopCandidates, nbRemainingStops - 1));
					}
				}
			}
		}
		return routesToAdd;
	}

	/**
	 * 
	 * @param splitParam	the maximum number of routes in a subset
	 * @return			an array of RouteManager objects with no more than splitParam routes in each loop arrays
	 */
	public RouteManager[] sampleRoutes(int splitParam){
		int nbManagers = (int) Math.ceil((this.loopSD.size() + this.loopDC.size()) / splitParam);
		RouteManager[] resultRManagers = new RouteManager[nbManagers];

		return resultRManagers;
	}

}
