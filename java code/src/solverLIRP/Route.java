package solverLIRP;

import java.io.IOException;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import instanceManager.Instance;
import instanceManager.Location;
import tools.Config;

public class Route {

	/*======================
	 *      ATTRIBUTES
	 =======================*/
	private Instance instLIRP; 				// The instance from which the stops of the route are collected
	
	private int lvl;
	private int start; 						// The start of the route (index of depot in model 1, -1 in model 2 to refer to the supplier)
	private LinkedHashSet<Integer> stops; 	// Permutation of the stops indices corresponding to the best route
	
	private double travelTime; 				// The duration of the route (travel and stopping time)
	private double stopTime; 				// The total time spent delivering the stops
	private double cost; 					// The cost of the route
	private boolean isDummy;

	/*======================
	 *      CONSTRUCTOR 
	 =======================*/
	/**
	 * Constructor of the route from the coordinates of a start point and a list of points
	 * @param start, points
	 * @throws IOException
	 */
	private Route(Instance instLIRP, int lvl, int start, LinkedHashSet<Integer> stops) throws IOException {
		this.instLIRP = instLIRP;
		this.lvl = lvl; 
		/* If the start index is negative and the route does not serves the first level, 
		 * mark it as dummy */
		this.isDummy =  (start < 0 && lvl > 0);
		this.start = start;
		this.stops = new LinkedHashSet<Integer>(stops);
		ArrayList<Integer> startingPermutation = new ArrayList<Integer>(stops);
		this.stops = bruteForceFindBestRoute(new ArrayList<Integer>(), startingPermutation);
		this.travelTime = computeTravelTime();
		this.stopTime = Config.STOPPING_TIME * this.stops.size();
		this.cost = Config.FIXED_COST_ROUTE + Config.COST_KM * Config.AVG_SPEED * this.travelTime;

		if(this.isDummy) {
			this.addDummyCost();
		}
	}

	/**
	 * Create a Route object from the coordinates of a start point and a stop point (direct route only)
	 * @param start, stop
	 * @throws IOException
	 */
	public Route(Instance instLIRP, int lvl, int start, int index) throws IOException {
		this.instLIRP = instLIRP;

		this.lvl = lvl; 
		/* If the start index is negative and the route does not serves the first level, 
		 * mark it as dummy */
		this.isDummy =  (start < 0 && lvl > 0);

		this.start = start;
		this.stops = new LinkedHashSet<Integer>();
		this.stops.add(index);
		this.travelTime = computeTravelTime();
		/* If the route is a dummy route, add the maximum time allowed for a route to the travel duration */
		if(this.isDummy) {
			this.travelTime += Config.MAX_TIME_ROUTE;
		}
		this.stopTime = Config.STOPPING_TIME;
		this.cost = Config.FIXED_COST_ROUTE + Config.COST_KM * Config.AVG_SPEED * this.travelTime;
		/* If the route visits a DC, add its potential ordering cost to its total cost */
		if(this.lvl < this.instLIRP.getNbLevels() - 1)
			this.cost += this.instLIRP.getDepot(lvl, index).getOrderingCost();
		/* If this route is dummy, add to its cost the fixed opening costs of all the depots of the instance */
		if(this.isDummy) {
			this.addDummyCost();
		}
	}
	
	/**
	 * Creates a new Route Object with the same attributes as another one
	 * @param routeToCopy 	The route from which to get the attributes
	 */
	public Route(Route routeToCopy) {
		this.instLIRP = routeToCopy.instLIRP;
		this.cost = routeToCopy.cost;
		this.lvl = routeToCopy.lvl;
		this.isDummy = routeToCopy.isDummy;
		this.start = routeToCopy.start;
		this.stops = new LinkedHashSet<Integer>(routeToCopy.stops);
		this.stopTime = routeToCopy.stopTime;
		this.travelTime = routeToCopy.travelTime;
	}

	/*
	 * ACCESSORS 
	 */
	/**
	 * 
	 * @return	the length of the route
	 */
	public int getNbStops() {
		return this.stops.size();
	}

	/**
	 * 
	 * @return	the location at the start of the route
	 */
	public Location getStart() {
		if(this.lvl > 0)
			return this.instLIRP.getDepot(this.lvl - 1, start);
		
		return this.instLIRP.getSupplier();

	}

	/**
	 * 
	 * @return	the location at the start of the route
	 */
	public int getStartIndex() {
			return start;
	}
	/**
	 * 
	 * @return	the array of locations corresponding to the stops
	 */
	public LinkedHashSet<Integer> getStops() {
		return this.stops;
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public Location getStop(int index) {
		if(this.stops.contains(index)) {
			if(this.lvl < this.instLIRP.getNbLevels() - 1 && index < this.instLIRP.getNbDepots(this.lvl))
				return this.instLIRP.getDepot(this.lvl, index);
			else if(this.lvl == this.instLIRP.getNbLevels() - 1 && index < this.instLIRP.getNbClients())
				return this.instLIRP.getClient(index);
		}
		return null;
	}

	/**
	 * 
	 * @return	An iterator on the stops of the route
	 */
	public Iterator<Integer> stopIterator(){
		return this.stops.iterator();
	}
	
//	/**
//	 * Get the position of a given stop
//	 * @param stopIndex	the index of a location of interest along the route
//	 * @return			the index of the corresponding stop in the sequence
//	 */
//	public int getPositionInRoute(int stopIndex) {
//		return stops.indexOf(stopIndex);
//	}

	/**
	 * 
	 * @return	the cost of the route given its stops sequence
	 */
	public double getCost() {
		return this.cost;
	}

	/**
	 * 
	 * @return	the duration of the route given its stops sequence
	 */
	public double getDuration() {
		return this.travelTime + this.stopTime;
	}

	/**
	 * 
	 * @return	the duration of the route given its stops sequence
	 */
	public int getMaxStop() {
		int maxStop = this.stops.size();
		for(int stopId : this.stops) {
			if(stopId > maxStop) {
				maxStop = stopId;
			}
		}
		return maxStop;
	}
	
	/**
	 * 
	 * @return	true if the Route object is dummy
	 */
	public boolean isDummy() {
		return this.isDummy;
	}
	
	/*
	 * MUTATORS
	 */
	/**
	 * Increase the cost of a route with a fixed ordering cost
	 * @param OC
	 */
	public void addToCost(double OC) {
		this.cost += OC;
	}

	/**
	 *  Add a dummy (expensive) cost to the route
	 */
	private void addDummyCost() {
		/* Increase the cost of the route with the fixed opening costs of all the depots of the instance... */
		for(int l = 0; l < this.instLIRP.getNbLevels() - 1; l++) {
			for(int d = 0; d < this.instLIRP.getNbDepots(l); d++) {
				this.cost += this.instLIRP.getDepot(l, d).getFixedCost();
			}
			/* ...and the holding cost corresponding to keeping all the demands for the entire planning horizon */
			double hc = (lvl < this.instLIRP.getNbLevels() - 1) ? this.instLIRP.getDepot(this.lvl, 0).getHoldingCost() : this.instLIRP.getClient(0).getHoldingCost();
			for(int c = 0; c < this.instLIRP.getNbClients(); c++) {
				this.cost += hc * this.instLIRP.getClient(c).getCumulDemands(0, this.instLIRP.getNbPeriods()); 
			}
		}
	}
	
	/*
	 * METHODS
	 */
	/**
	 * 
	 * @return	True if the duration of the route is less than the maximum time allowed 
	 */
	public boolean isValid() {
		return (this.start < 0 && this.lvl > 0) || (this.travelTime + this.stopTime) < Config.MAX_TIME_ROUTE;
	}
	/**
	 * Check if the start attribute of the Route object is equal to a given index
	 * @param index	the index of the location (-1 for the supplier)
	 * @return		true if the route starts from the depot corresponding to index (or -1 if it starts from the supplier), false otherwise
	 */
	public boolean hasStart(int index) {
		return this.start == index;
	}
	
	/**
	 * Check if the Route object contains a given location in its stops
	 * @param index	the index of the location on the corresponding level
	 * @return		true if the route contains the location, false otherwise
	 */
	public boolean containsStop(int index) {
		return this.stops.contains(index);
	}
	
	/**
	 * Check if the contains a given location
	 * @param loc	the location of interest
	 * @return		true if the route contains the location, false otherwise
	 */
	public boolean containsLocation(int lvl, Location loc) {
		boolean inRoute = false;
		/* Start by checking if loc is the starting point of the route */
		if(lvl > 0) {
			/* If the start of the route is a dc */
			inRoute =  (this.instLIRP.getDepot(lvl - 1, start) == loc);
		}
		else {
			/* If the start of the route is the supplier */
			inRoute =  (this.instLIRP.getSupplier() == loc);
		}
		Iterator<Integer> stopsIter = this.stops.iterator();
		/* Loop through the stops and update inRoute if one corresponds to loc */
		while(!inRoute && stopsIter.hasNext()) {
			if(lvl < this.instLIRP.getNbLevels() - 1)
				inRoute = (loc == this.instLIRP.getDepot(lvl, stopsIter.next()));
			else
				inRoute = (loc == this.instLIRP.getClient(stopsIter.next()));
		}
		return inRoute;
	}

	/**
	 * Create a new Route object by adding a new stop to this route
	 * @param stop	the new stop to be added
	 * @return		a new Route object extending the current one with the addition of stop to the loop
	 * @throws IOException
	 */
	public Route extend(int stopIndex) throws IOException {
		/* Check that the route does not already contains the stop that is about to be added */
		if(this.containsStop(stopIndex)){
			System.out.println("Trying to add a stop to a route that already contains it");
			System.exit(1);
		}

		/* Create a new array to list the stops of the new Route object */
		LinkedHashSet<Integer> newStops = new LinkedHashSet<Integer> (this.stops);
		if(this.getLB(stopIndex) < Config.MAX_TIME_ROUTE) {
			newStops.add(stopIndex);
			return new Route(this.instLIRP, this.lvl, this.start, newStops);
		}
		else {
			Route fakeRoute = new Route(this.instLIRP, this.lvl, this.start, stopIndex); 
			fakeRoute.stops.addAll(this.stops);
			fakeRoute.travelTime = this.computeDuration(fakeRoute.getStops());
			fakeRoute.stopTime += this.stopTime;
			return fakeRoute;
		}
	}

	/**
	 * Compute the best permutation of the stops to minimize the cost of the route (brute force)
	 * @param partialPermutation	the current permutation of the stops in the route
	 * @param indicesNotInRoute	the indices that have not been included in the route yet
	 * @return
	 */
	private LinkedHashSet<Integer> bruteForceFindBestRoute(ArrayList<Integer> partialPermutation, ArrayList<Integer> indicesNotInRoute)
	{
		/* Create a candidate for best route starting with the stops in r */
		LinkedHashSet<Integer> bestRoute = new LinkedHashSet<Integer>(partialPermutation);
		/* Add all the remaining stops to the candidate as they appear in the stopsNotInRoute */
		bestRoute.addAll(indicesNotInRoute);
		double bestCost = computeDuration(bestRoute);

		/* If the start of the route is already greater than the maximum time allowed, 
		 stop computations and return the current candidate */
		if(computeDuration(partialPermutation) + Config.STOPPING_TIME * partialPermutation.size() > Config.MAX_TIME_ROUTE)
			return bestRoute;

		/* If there are stops that are not included in the route, try to add them one by one */
		if(!indicesNotInRoute.isEmpty()) {
			int nextStop = 0;
			/* Loop through the possible next stops along the route */
			while(nextStop < indicesNotInRoute.size()) {
				int justRemoved = indicesNotInRoute.remove(0);
				ArrayList<Integer> newPartialPermutation = new ArrayList<Integer>(partialPermutation);
				newPartialPermutation.add(justRemoved);

				/* Compute the best remaining route */
				LinkedHashSet<Integer> permutationCandidate = bruteForceFindBestRoute(newPartialPermutation, indicesNotInRoute);
				double costCandidate = computeDuration(permutationCandidate) + Config.STOPPING_TIME * permutationCandidate.size();
				/* Compare its cost with the current best cost and update the best route if necessary */
				if(costCandidate < Config.MAX_TIME_ROUTE && costCandidate < bestCost) {
					bestRoute = permutationCandidate;
					bestCost = costCandidate;
				}
				/* Replace the stop that has just been extracted at the end of the list */
				indicesNotInRoute.add(justRemoved);
				nextStop++;
			}
		}
		return bestRoute;
	}

	/**
	 * Compute a lower bound if a new stop is added to the current set of stops
	 * @param newStop	the new stop to add to the route
	 * @return			the lower bound on the duration of the road (time for the current route or longest triangle route between the start, the new stop and any stop of the existing route, plus the stop durations)
	 */
	private double getLB(int newStop) {
		/* Initialize the lower bound to the cumulative duration of the stops */
		double travelDuration = this.travelTime;
		Location newLoc = (this.start < 0) ? this.instLIRP.getDepot(0, newStop):this.instLIRP.getClient(newStop);

		Iterator<Integer> stop1 = this.stops.iterator();
		while(stop1.hasNext()) {
			int stop1Index = stop1.next();
			Location loc1 = (this.lvl < this.instLIRP.getNbLevels() - 1) ? this.instLIRP.getDepot(this.lvl, stop1Index) : this.instLIRP.getClient(stop1Index);
			travelDuration = Math.max((this.getStart().getDistance(loc1) + this.getStart().getDistance(newLoc) + loc1.getDistance(newLoc)) / Config.AVG_SPEED, travelDuration);
		}

		return this.stopTime + Config.STOPPING_TIME + travelDuration;
	}

	/**
	 * Compute the cost of the route associated with a given permutation
	 * @param stopsIndices	the permutation of the stops
	 * @return				the cost incurred by the sequence stopsIndices
	 */
	private double computeDuration(Collection<Integer> stopsIndices) {
		// Start with a time of 0 for the route
		double travelTime = 0;
		// Set the current stop at the starting point
		Location currentStop = this.getStart();

		Location nextStop;
		Iterator<Integer> indexIterator = stopsIndices.iterator();
		// Iterate through the indices of the stops and increment the cost with the time necessary to reach the next stop and the average time spent at the stop 
		while (indexIterator.hasNext()) {
			nextStop = (this.start < 0) ? this.instLIRP.getDepot(0, indexIterator.next()):this.instLIRP.getClient(indexIterator.next());
			travelTime += currentStop.getDistance(nextStop) / Config.AVG_SPEED;
			currentStop = nextStop;
		}
		// Add the time to return to the starting point of the route
		travelTime += currentStop.getDistance(this.getStart()) / Config.AVG_SPEED;

		return travelTime;
	}
	
	/**
	 * Compute the cost of the route associated with a given permutation
	 * @param stopsIndices	the permutation of the stops
	 * @return				the cost incurred by the sequence stopsIndices
	 */
	private double computeTravelTime() {
		/* Start with a time of 0 for the route */
		double travelTime = 0;
		/* Set the current stop at the starting point */
		Location currentStop = this.getStart();

		Location nextStop;
		Iterator<Integer> indexIterator = this.stops.iterator();
		/* Iterate through the indices of the stops and increment the cost with the time necessary to reach the next stop and the average time spent at the stop */
		while (indexIterator.hasNext()) {
			nextStop = this.instLIRP.getClient(indexIterator.next());
			travelTime += currentStop.getDistance(nextStop) / Config.AVG_SPEED;
			currentStop = nextStop;
		}
		/* Add the time to return to the starting point of the route */
		travelTime += currentStop.getDistance(this.getStart()) / Config.AVG_SPEED;

		return travelTime;
	}
	
	/**
	 * 
	 * @return	a JSON object describing the Route object
	 * @throws IOException
	 */
	public JSONObject getJSONRoute() throws IOException {
		// Create a JSON Object to describe the depots map
		JSONObject jsonRoute = new JSONObject();
		jsonRoute.put("start", this.start);
		jsonRoute.put("stops", new JSONArray(this.stops));
		jsonRoute.put("cost", ((double) Math.floor(this.cost * 1000)) / 1000.0);
		jsonRoute.put("duration", ((double) Math.floor((this.travelTime + this.stopTime) * 1000)) / 1000.0);
		if(this.travelTime + this.stopTime > Config.MAX_TIME_ROUTE) {
			System.out.println("route too long (start " + this.start + ")");
		}
		
		return jsonRoute;

	}
}


