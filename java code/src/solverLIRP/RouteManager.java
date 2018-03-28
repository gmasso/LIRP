
/* test */

package solverLIRP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import instanceManager.Instance;
import tools.JSONParser;
import tools.Parameters;

public class RouteManager {

	/*=====================
	 *      ATTRIBUTES
	 ======================*/
	private Instance instLIRP;												// The instance to which the routes apply
	private HashMap<Integer, HashMap<Integer, LinkedHashSet<Route>>> routes; 	// A route is referenced with its level (0 : supplier-depot, 1 : depot-client)
	// For a given level, routes are ordered according to their number of stops
	private int[] nbRoutesLvl;												// Total number of routes at each level (0: Supplier to depots, 1: Depots to clients)

	/**
	 * Create a RouteManager object from an instance and the type of model under investigation
	 * @param instLIRP	the instance from which the set of routes is created
	 */
	public RouteManager(Instance instLIRP) throws IOException {
		/* Create direct routes for the instance */
		this.instLIRP = instLIRP;
		this.routes = new HashMap<Integer, HashMap<Integer, LinkedHashSet<Route>>>();

		/* Create HashMaps to store the sets of routes at level 0 and 1 */
		this.routes.put(0, new HashMap<Integer, LinkedHashSet<Route>>());
		this.routes.put(1, new HashMap<Integer, LinkedHashSet<Route>>());
		this.nbRoutesLvl = new int[Parameters.nb_levels];
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			this.nbRoutesLvl[lvl] = 0;
		}

		this.populateDirect();
		/* Populate loops at both levels */
		this.populateLoops(0, 1);
		this.populateLoops(1, 1);

		System.out.println("Route manager created.");
	}

	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return	the instance to which the routes in the RouteManager object relate
	 */
	public Instance getInstance() {
		return this.instLIRP;
	}

	/**
	 * 
	 * @return	the ArrayList of direct routes from the supplier to the depots
	 */
	public LinkedHashSet<Route> getAllRoutesOfType(int lvl, int nbStops) {
		return this.routes.get(lvl).get(nbStops);
	}

	/**
	 * 
	 * @return	the ArrayList of multi-stops routes from the supplier to the depots
	 */
	public int getNbRoutesOfType(int lvl, int nbStops){
		return this.routes.get(lvl).get(nbStops).size();
	}

	//	/**
	//	 * 
	//	 * @return	an array containing of all Route objects in the route manager from the supplier to the depots
	//	 */
	//	public HashSet<Route> extractSubsetRoutesOfTypes(int lvlKey, int nbStops, HashSet<Integer> indices) {
	//
	//		HashSet<Route> routesLvl0 = new HashSet<Route>(); //[indices.size() + indices.size()];
	//
	//		int routeIndex = 0;
	//		for(Route route : this.routes.get(lvlKey).get(nbStops)) {
	//			if(indices.contains(routeIndex))
	//				routesSD.add(route);
	//			routeIndex++;	
	//		}
	//
	//
	//		for(int loopSDIter : indices) {
	//			routesSD[this.directSD.length + loopSDIter] = this.loopSD[loopSDIter];
	//		}
	//		return routesSD;
	//	}
	//
	//	/**
	//	 * 
	//	 * @return	an array containing of all Route objects in the route manager from depots to clients
	//	 */
	//	public Route[] getDCRoutes(ArrayList<Integer> indices) {
	//		Route[] routesDC = new Route[this.directDC.length + indices.size()];
	//
	//		for(int directDCIter = 0; directDCIter < this.directDC.length; directDCIter++) {
	//			routesDC[directDCIter] = this.directDC[directDCIter];
	//		}
	//		int pos = 0;
	//		for(int loopDCIter : indices) {
	//			routesDC[this.directDC.length + pos] = this.loopDC[loopDCIter];
	//			pos++;
	//		}
	//		return routesDC;
	//	}
	//

	/*
	 * METHODS
	 */
	/**
	 * Fill the direct routes arrays with the all valid direct routes for the instance considered
	 * @param instLIRP		the LIRP instance
	 * @throws IOException
	 */
	private void populateDirect() throws IOException {
		/* Linked Hashsets to store the feasible routes on the two levels as they are computed */
		LinkedHashSet<Route> directLvl0 = new LinkedHashSet<Route>();
		LinkedHashSet<Route> directLvl1 = new LinkedHashSet<Route>();
		int dIndex = 0;
		/* Fill the direct routes first in order to test the reachability of each location */
		while(dIndex < this.instLIRP.getNbDepots(0)) {
			/* Create a new direct route from the supplier to the depot */
			Route direct0 = new Route(this.instLIRP, -1, dIndex);
			/* If its duration is lower than the maximum time allowed, add it to the list */
			if(direct0.isValid()) {
				directLvl0.add(direct0);
				this.nbRoutesLvl[0] += 1;
				int cIndex = 0;
				while(cIndex < this.instLIRP.getNbClients()) {
					/* Create a new direct Route object between the current depot and client */
					Route direct1 = new Route(this.instLIRP, dIndex, cIndex);
					/* If its duration is lower than the maximum time allowed, add it to the list */
					if(direct1.isValid()) {
						directLvl1.add(direct1);
						this.nbRoutesLvl[1] += 1;
						cIndex++;
					}
					/* Otherwise, we have found an unreachable client: The instance is not valid and we draw a new location for the unreachable client*/
					else {
						this.instLIRP.drawClient(cIndex);
					}

				}
				/* Add the routes containing nbStops to the level 1 HashMap */
				if(!directLvl1.isEmpty()) {
					this.routes.get(1).put(1, directLvl1);
				}
				dIndex++;
			}
			/* Otherwise, we have found an unreachable depot: The instance is not valid and we draw a new location for the unreachable depot*/
			else {
				this.instLIRP.drawDepot(0, dIndex);
			}
			/* Add the routes containing nbStops to the level 0 HashMap */
			if(!directLvl0.isEmpty()) {
				this.routes.get(0).put(1, directLvl0);
			}
		}
	}

	/**
	 * Fill the loop routes HashSets at a specific level
	 * @param lvl			the level at which we want to create the loop routes
	 * @throws IOException
	 */
	/**
	 * Fill the loop routes HashSets at a specific level
	 * @param lvl			the level at which we want to create the loop routes
	 * @param nbStops		the number of stops of the starting routes to extend
	 * @throws IOException
	 */
	private void populateLoops(int lvl, int nbStops) throws IOException {
		/* HashSet to store the loop routes with nbStops+1 stops at level lvl*/
		LinkedHashSet<Route> loopsLvl = new LinkedHashSet<Route>();

		/* Fill an array list with the potential stop candidates to add to the loop routes */
		int finalIndex = 0;
		if(lvl > 0)
			finalIndex = this.instLIRP.getNbClients();
		else
			finalIndex = this.instLIRP.getNbDepots(0);

		/* If the stopping time is small enough, build the multi-stops routes for this instance */
		/* Start from each of the existing direct routes */
		for(Route startRoute : this.routes.get(lvl).get(nbStops)) {
			/* If it is possible to add a stop */
			if(startRoute.getDuration() + Parameters.stopping_time < Parameters.max_time_route) {
				/* Start at the maximum last possible stop */
				int stopToAdd = finalIndex - 1;
				int maxStop = startRoute.getMaxStop();
				while(stopToAdd > maxStop) {
					/* Create a new route candidate by adding the stop to the current route */
					Route routeCandidate = startRoute.extend(stopToAdd);
					/* Add it to the set of possible routes if it is valid */
					if(routeCandidate.isValid()) {
						loopsLvl.add(routeCandidate);
						this.nbRoutesLvl[lvl] += 1;
					}
					stopToAdd--;
				}
			}
		}
		if(!loopsLvl.isEmpty()) {
			this.routes.get(lvl).put(nbStops + 1, loopsLvl);
			populateLoops(lvl, nbStops + 1);
		}
	}

	
	
	/**
	 * 
	 * @param loopSD        routes from the supplier to depots
	 * @param Allocation    matrix with customer allocation. Depot with 0 client = not selected
	 * @return list of SD routes: SD routes starting from an unselected depot are filtered
	 * @throws IOException
	 */
	private HashMap<Integer, HashMap<Integer, LinkedHashSet<Route>>> filterRoutes(ArrayList<Route> loopSD, int[][] Allocation) throws IOException {

		int nd = this.instLIRP.getNbDepots(0); 
		int nc = this.instLIRP.getNbClients(); 

		ArrayList<Route> filteredSD = new ArrayList<Route>();
		for (int d=0; d<nd;d++){
			for (int c=0; c<nc;c++) {
				if (Allocation[c][d] == 1) {
					Route r = new Route(this.instLIRP, -1, d); // create a new SD route r from s to d
					filteredSD.add(r);
					c=nc; // to exit the for loop
				}
			}
		}
		return filteredSD;
	}


	/**
	 * 
	 * @param loopDC        routes from depots to clients
	 * @param Allocation    matrix with customer allocation. Depot with 0 client = not selected
	 * @return filtered list of DC routes. 
	 * DC route starting from an unselected depot are filtered
	 * DC Routes with all clients not allocated to d are filtered
	 * @throws IOException
	 */

	private LinkedHashSet<Route> filterRoutes(int ArrayList<Route> loopDC, int[][] Allocation) throws IOException {

		int keep; // indicates if a route must be kept or not in the filtered list

		ArrayList<Route> filteredDC = new ArrayList<Route>();

		for (int r=0; r<loopDC.size();r++) {
			keep=1;
			Route currentRoute = loopDC.get(r);
			ArrayList<Integer> stops = currentRoute.getStops();
			int d = stops.get(0); // check if it is the depot index OR the index of first client
			int rsize = currentRoute.getNbStops();
			for (int i=0;i<rsize; i++) {
				int c = stops.get(i); // index of client number i on route r
				if (Allocation[c][d]==0){
					keep = 0;
					i=rsize; // to directly check next 
				}
			}
			if (keep==1) { // in this case, all clients are allocated to the depot
				filteredDC.add(currentRoute);
			}
		}
		return filteredDC;
	}

	//			/* Build additional routes from every existing shorter route */
	//			for(Route startRouteLvl0 : this.routes.get(0).get(nbStops - 1)) {
	//				for(int stopIter = 0; stopIter < stopCandidates.size(); stopIter++) {
	//					/* Create a new Route object by adding one stop among the candidates to currentRoute */
	//					Route routeCandidate = currentRoute.extend(stopCandidates.get(stopIter));
	//					/* If it is valid, add it to the set of routes to add and call recursively */
	//					if(routeCandidate.isValid()) {
	//			}
	//			/* Fill a list with depots candidates for insertion in loops */
	//			ArrayList<Integer> stopDCandidates = new ArrayList<Integer>();
	//			for(int dIter = 0; dIter < this.instLIRP.getNbDepots(0); dIter++) {
	//				stopDCandidates.add(dIter);
	//			}
	//			/* Create loops with the different possible combinations of stops */
	//			for(int dIndex = 0; dIndex < stopDCandidates.size() - 1; dIndex++) {
	//				Route initSDRoute = new Route(this.instLIRP, -1, dIndex);
	//				loopsLvl0.addAll(computeAllRoutes(initSDRoute, new ArrayList<Integer>(stopDCandidates.subList(dIndex + 1, stopDCandidates.size())), maxNbStops - 1));
	//			}
	//
	//			/* Fill a list with depots candidates for insertion in loops */
	//			ArrayList<Integer> stopCCandidates = new ArrayList<Integer>();
	//			for(int cIter = 0; cIter < this.instLIRP.getNbClients(); cIter++) {
	//				stopCCandidates.add(cIter);
	//			}
	//			/* Create loops starting from each depot */
	//			for(int dIter = 0; dIter < this.instLIRP.getNbDepots(0); dIter++) {
	//				/* Routes from the depot dIter are useful only if the depot is reachable from the supplier */
	//				/* Create loops with the different possible combinations of stops */
	//				for(int cIndex = 0; cIndex < stopCCandidates.size() - 1; cIndex++) {
	//					Route initDCRoute = new Route(this.instLIRP, dIter, cIndex);
	//					loopsLvl1.addAll(computeAllRoutes(initDCRoute, new ArrayList<Integer>(stopCCandidates.subList(cIndex + 1, stopCCandidates.size())), maxNbStops - 1));
	//				}
	//			}
	//		}
	//	}

	//
	//	/**
	//	 * 
	//	 * @param currentRoute		the current Route object that is considered as a basis to build new ones
	//	 * @param stopCandidates		the stops that can be added to currentRoute
	//	 * @param nbRemainingStops	the maximum number of stops that can be added to the currentRoute
	//	 * @return
	//	 * @throws IOException
	//	 */
	//	private ArrayList<Route> computeAllRoutes(Route currentRoute, ArrayList<Integer> stopCandidates, int nbRemainingStops) throws IOException {
	//		ArrayList<Route> routesToAdd = new ArrayList<Route>();
	//		/* If some stop candidates remain to extend the route, try to add them to the route */
	//		if(nbRemainingStops > 0 && !stopCandidates.isEmpty()) {
	//			for(int stopIter = 0; stopIter < stopCandidates.size(); stopIter++) {
	//				/* Create a new Route object by adding one stop among the candidates to currentRoute */
	//				Route routeCandidate = currentRoute.extend(stopCandidates.get(stopIter));
	//				/* If it is valid, add it to the set of routes to add and call recursively */
	//				if(routeCandidate.isValid()) {
	//					routesToAdd.add(routeCandidate);
	//					/* If the stop currently added is not the last of the list, call recursively with the remaining candidates */
	//					if(stopIter < stopCandidates.size() - 1) {
	//						ArrayList<Integer> newStopCandidates = new ArrayList<Integer>(stopCandidates.subList(stopIter + 1, stopCandidates.size()));
	//						routesToAdd.addAll(computeAllRoutes(routeCandidate, newStopCandidates, nbRemainingStops - 1));
	//					}
	//				}
	//			}
	//		}
	//		return routesToAdd;
	//	}

	/**
	 * 
	 * @return	a JSON object containing the RouteManager object
	 * @throws IOException
	 */
	public JSONObject getJSONRM() throws IOException {
		// Create a JSON Object to describe the depots map
		JSONObject jsonRM = new JSONObject();
		jsonRM.put("instance id", this.instLIRP.getID());

		JSONObject jsonLvls = new JSONObject();
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			//JSONArray jsonLvlRoutes = new JSONArray();
			Iterator<Map.Entry<Integer, LinkedHashSet<Route>>> mapRouteIter = this.routes.get(lvl).entrySet().iterator();
			JSONObject jsonLvlRoutes = new JSONObject();
			while (mapRouteIter.hasNext()) {
				Map.Entry<Integer, LinkedHashSet<Route>> setPair = mapRouteIter.next();
				JSONArray jsonRouteArray = new JSONArray();
				Iterator<Route> routesIter = setPair.getValue().iterator();
				while(routesIter.hasNext()) {
					jsonRouteArray.put(routesIter.next().getJSONRoute());
				}
				jsonLvlRoutes.put(setPair.getKey().toString(), jsonRouteArray);
			}
			jsonLvls.put(String.valueOf(lvl), jsonLvlRoutes);

		}
		jsonRM.put("routes", jsonLvls);

		return jsonRM;
	}

	/**
	 * Write the JSON object of this map to a file
	 * @param filename	the destination file
	 * @throws IOException
	 */
	protected void writeToJSONFile(String filename) throws IOException {
		System.out.println(filename);
		JSONParser.writeJSONToFile(this.getJSONRM(), filename);
	}
}
