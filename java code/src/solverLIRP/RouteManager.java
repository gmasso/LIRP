
/* test */

package solverLIRP;

import java.io.IOException;
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
	private Instance instLIRP;													// The instance to which the routes apply
	private HashMap<Integer, HashMap<Integer, LinkedHashSet<Route>>> routes; 	// A route is referenced with its level (0 : supplier-depot, 1 : depot-client)
	// For a given level, routes are ordered according to their number of stops
	private int[] nbRoutesLvl;													// Total number of routes at each level (0: Supplier to depots, 1: Depots to clients)

	/**
	 * Create a RouteManager object from an instance and the type of model under investigation
	 * @param instLIRP	the instance from which the set of routes is created
	 */
	public RouteManager(Instance instLIRP) throws IOException {
		/* Create direct routes for the instance */
		this.instLIRP = instLIRP;
		this.routes = new HashMap<Integer, HashMap<Integer, LinkedHashSet<Route>>>();

		/* Create HashMaps to store the sets of routes at each level */
		this.nbRoutesLvl = new int[Parameters.nb_levels];
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			this.routes.put(lvl, new HashMap<Integer, LinkedHashSet<Route>>());
			this.nbRoutesLvl[lvl] = 0;
		}
	}
	
	/**
	 * Create a RouteManager object from an instance and the type of model under investigation
	 * @param instLIRP		The instance from which the set of routes is created
	 * @param onlyDirect	If we only want to generate direct routes and modify instLIRP so that all clients are reachable
	 * @throws IOException
	 */
	public void initialize(boolean onlyDirect) throws IOException {
		this.populateDirect();
		if(!onlyDirect) {
			/* Populate loops at both levels */
			for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
				this.populateLoops(lvl, 1);
			}
		}
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
		if(lvl > -1 && lvl < Parameters.nb_levels) {
			if(this.routes.get(lvl).containsKey(nbStops)) {
				return this.routes.get(lvl).get(nbStops).size();
			}
		}
		return 0;
	}

	/*
	 * METHODS
	 */
	/**
	 * Fill the direct routes arrays with all the valid direct routes for the instance considered. 
	 * If a location is not reachable from an upper level, re-position this location on the map.
	 * @param instLIRP		the LIRP instance
	 * @throws IOException
	 */
	private void populateDirect() throws IOException {
		/* Loop through the different levels */
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			/* Create a LinkedHashSet to store the feasible routes on the current level as they are computed */
			LinkedHashSet<Route> directLvl = new LinkedHashSet<Route>();
			LinkedHashSet<Route> dummyLvl = new LinkedHashSet<Route>();
			int sIndex = 0;
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			/* Fill the direct routes first and test that each location of the level is reachable from at least one site on the upper level */
			while(sIndex < nbLocLvl) {
				int nbUpperSites = this.instLIRP.getNbDepots(lvl - 1);
				int uIndex = (lvl == 0) ? -1 : 0;
				boolean reachable = false;
				while(uIndex < nbUpperSites) {
					/* Create a new direct route from the current upper site (uIndex) to the current lower site (sIndex) */
					Route directR = new Route(this.instLIRP, lvl, uIndex, sIndex);
					/* If its duration is lower than the maximum time allowed, add it to the list and mark the lower site as reachable*/
					if(directR.isValid()) {
						reachable = true;
						directLvl.add(directR);
						this.nbRoutesLvl[lvl] += 1;
					}
					uIndex++;
				}
				/* If the site is reachable, go to the next site index */
				if(reachable) {
					if(lvl > 0)
						dummyLvl.add(new Route(this.instLIRP, lvl, -1, sIndex));
					sIndex++;
				}
				/* Otherwise, re-position the site randomly and check if the new location is reachable from an upper site */
				else {
					if(lvl == Parameters.nb_levels - 1)
						this.instLIRP.drawClient(sIndex);
					else
						this.instLIRP.drawDepot(lvl, sIndex);
				}
			}
			/* Add the direct routes to the corresponding level in the HashMap */
			this.routes.get(lvl).put(1, directLvl);
			/* NB : the dummy routes are stored as routes of length 0 */
			this.routes.get(lvl).put(0, dummyLvl);
		}
	}

	/**
	 * Fill the loop routes HashSets at a specific level
	 * @param lvl			the level at which we want to create the loop routes
	 * @param nbStops		the number of stops of the starting routes to extend
	 * @throws IOException
	 */
	private void populateLoops(int lvl, int nbStops) throws IOException {
		/* HashSet to store the loop routes with nbStops+1 stops at level lvl */
		LinkedHashSet<Route> loopsLvl = new LinkedHashSet<Route>();

		/* Fill an array list with the potential stop candidates to add to the loop routes */
		int finalIndex = 0;
		if(lvl == Parameters.nb_levels - 1)
			finalIndex = this.instLIRP.getNbClients();
		else
			finalIndex = this.instLIRP.getNbDepots(lvl);

		/* If the stopping time is small enough, build the multi-stops routes for this instance */
		/* Start from each of the existing direct routes */
		for(Route startRoute : this.routes.get(lvl).get(nbStops)) {
			/* If the route does not start from the dummy depot and it is possible to add a stop */
			if(!startRoute.isDummy() && startRoute.getDuration() + Parameters.stopping_time < Parameters.max_time_route) {
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
		/* If some routes have been found for this number of stops, add it to the HashMap */
		if(!loopsLvl.isEmpty()) {
			this.routes.get(lvl).put(nbStops + 1, loopsLvl);
			populateLoops(lvl, nbStops + 1);
		}
	}
	
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
	public void writeToJSONFile(String filename) throws IOException {
		System.out.println(filename);
		JSONParser.writeJSONToFile(this.getJSONRM(), filename);
	}
}
