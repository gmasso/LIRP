package solverLIRP;

import java.io.IOException;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import instanceManager.Instance;
import instanceManager.Location;
import tools.Parameters;

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
		LinkedHashSet<Integer> startingPermutation = new LinkedHashSet<Integer>(stops);
		this.stops = bruteForceFindBestRoute(new ArrayList<Integer>(), startingPermutation);
		this.travelTime = computeDuration();
		this.stopTime = Parameters.stopping_time * this.stops.size();
		this.cost = Parameters.fixed_cost_route + Parameters.cost_km * Parameters.avg_speed * this.travelTime;
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
		this.travelTime = computeDuration();
		if(this.lvl < 0) {
			this.travelTime += Parameters.max_time_route;
		}
		this.stopTime = Parameters.stopping_time;
		this.cost = Parameters.fixed_cost_route + Parameters.cost_km * Parameters.avg_speed * this.travelTime;
		if(this.start == -1)
			this.cost += this.instLIRP.getDepot(-1, index).getOrderingCost();
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
		if(this.start < 0)
			return this.instLIRP.getSupplier();
		return this.instLIRP.getDepot(0, start);
	}

	/**
	 * 
	 * @return	the array of locations corresponding to the stops
	 */
	public LinkedHashSet<Integer> getStops() {
		return this.stops;
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

	/*
	 * METHODS
	 */
	/**
	 * 
	 * @return	True if the duration of the route is less than the maximum time allowed 
	 */
	public boolean isValid() {
		return (this.lvl < 0) || (this.travelTime + this.stopTime) < Parameters.max_time_route;
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
	public boolean containsLocation(Location loc) {
		// Start by checking if loc is the starting point of the route
		boolean inRoute = (this.start < 0 ) ? (this.instLIRP.getSupplier() == loc) : (this.instLIRP.getDepot(0, start) == loc);
		Iterator<Integer> stopsIter = this.stops.iterator();
		// Loop through the stops and update inRoute if one corresponds to loc
		while(!inRoute && stopsIter.hasNext()) {
			if(start < 0)
				inRoute = (loc == this.instLIRP.getDepot(0, stopsIter.next()));
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
		LinkedHashSet<Integer> newStops = new LinkedHashSet<Integer> (stops);
		if(this.getLB(stopIndex) < Parameters.max_time_route) {
			newStops.add(stopIndex);
			return new Route(this.instLIRP, this.lvl, this.start, newStops);
		}
		else {
			Route fakeRoute = new Route(this.instLIRP, this.lvl, this.start, stopIndex); 
			fakeRoute.stops.addAll(this.stops);
			fakeRoute.travelTime = fakeRoute.computeDuration();
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
	private LinkedHashSet<Integer> bruteForceFindBestRoute(ArrayList<Integer> partialPermutation, LinkedHashSet<Integer> indicesNotInRoute)
	{
		/* Create a candidate for best route starting with the stops in r */
		LinkedHashSet<Integer> bestRoute = new LinkedHashSet<Integer>(partialPermutation);
		/* Add all the remaining stops to the candidate as they appear in the stopsNotInRoute */
		bestRoute.addAll(indicesNotInRoute);
		double bestCost = computeDuration(bestRoute);

		/* If the start of the route is already greater than the maximum time allowed, 
		 stop computations and return the current candidate */
		if(computeDuration(partialPermutation) + Parameters.stopping_time * partialPermutation.size() > Parameters.max_time_route)
			return bestRoute;

		/* If there are stops that are not included in the route, try to add them one by one */
		if(!indicesNotInRoute.isEmpty())
		{
			/* Loop through the possible next stops along the route */
			Iterator<Integer> id = indicesNotInRoute.iterator();
			while(id.hasNext()) {
				int justRemoved = id.next();
				indicesNotInRoute.remove(justRemoved);
				ArrayList<Integer> newPartialPermutation = new ArrayList<Integer>(partialPermutation);
				newPartialPermutation.add(justRemoved);

				// Compute the best remaining route
				LinkedHashSet<Integer> permutationCandidate = bruteForceFindBestRoute(newPartialPermutation, indicesNotInRoute);
				double costCandidate = computeDuration(permutationCandidate) + Parameters.stopping_time * permutationCandidate.size();
				// Compare its cost with the current best cost and update the best route if necessary
				if(costCandidate < Parameters.max_time_route && costCandidate < bestCost) {
					bestRoute = permutationCandidate;
					bestCost = costCandidate;
				}
				/* Replace the stop that has just been extracted at the end of the list */
				indicesNotInRoute.add(justRemoved);
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
			Location loc1 = (this.start < 0) ? this.instLIRP.getDepot(0, stop1Index):this.instLIRP.getClient(stop1Index);
			travelDuration = Math.max((this.getStart().getDistance(loc1) + this.getStart().getDistance(newLoc) + loc1.getDistance(newLoc)) / Parameters.stopping_time, travelDuration);
		}

		return this.stopTime + Parameters.stopping_time + travelDuration;
	}

	/**
	 * Compute the cost of the route associated with a given permutation
	 * @param stopsIndices	the permutation of the stops
	 * @return				the cost incurred by the sequence stopsIndices
	 */
	private double computeDuration(Collection<Integer> stopsIndices) {
		// Start with a time of 0 for the route
		double travelTime = 0;
		double stopsTime = Parameters.stopping_time * stopsIndices.size();
		// Set the current stop at the starting point
		Location currentStop = this.getStart();

		Location nextStop;
		Iterator<Integer> indexIterator = stopsIndices.iterator();
		// Iterate through the indices of the stops and increment the cost with the time necessary to reach the next stop and the average time spent at the stop 
		while (travelTime + stopsTime < Parameters.max_time_route && indexIterator.hasNext()) {
			nextStop = (this.start < 0) ? this.instLIRP.getDepot(0, indexIterator.next()):this.instLIRP.getClient(indexIterator.next());
			travelTime += currentStop.getDistance(nextStop) / Parameters.avg_speed;
			currentStop = nextStop;
		}
		// Add the time to return to the starting point of the route
		travelTime += currentStop.getDistance(this.getStart()) / Parameters.avg_speed;

		return travelTime;
	}
	
	/**
	 * Compute the cost of the route associated with a given permutation
	 * @param stopsIndices	the permutation of the stops
	 * @return				the cost incurred by the sequence stopsIndices
	 */
	private double computeDuration() {
		// Start with a time of 0 for the route
		double travelTime = 0;
		double stopsTime = Parameters.stopping_time * this.stops.size();
		// Set the current stop at the starting point
		Location currentStop = this.getStart();

		Location nextStop;
		Iterator<Integer> indexIterator = this.stops.iterator();
		// Iterate through the indices of the stops and increment the cost with the time necessary to reach the next stop and the average time spent at the stop 
		while (travelTime + stopsTime < Parameters.max_time_route && indexIterator.hasNext()) {
			nextStop = this.instLIRP.getClient(indexIterator.next());
			travelTime += currentStop.getDistance(nextStop) / Parameters.avg_speed;
			currentStop = nextStop;
		}
		// Add the time to return to the starting point of the route
		travelTime += currentStop.getDistance(this.getStart()) / Parameters.avg_speed;

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
		
		return jsonRoute;

	}
}


