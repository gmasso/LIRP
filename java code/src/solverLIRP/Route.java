package solverLIRP;

import java.io.IOException;
import java.util.*;

import instanceManager.Instance;
import instanceManager.Location;
import instanceManager.Parameters;

public class Route {

	/*======================
	 *      ATTRIBUTES
	 =======================*/
	private Instance instLIRP; 			// The instance from which the stops of the route are collected
	private int start; 					// The start of the route (index of depot in model 1, -1 in model 2 to refer to the supplier)
	//private Location[] stops; 			// Coordinates of the stopping points along the route (clients in model 1, depots in model 2)
	private ArrayList<Integer> stops; 	// Permutation of the stops indices corresponding to the best route
	private double travelTime; 			// The duration of the route (travel and stopping time)
	private double stopTime; 			// The total time spent delivering the stops
	private double cost; 				// The cost of the route


	/*======================
	 *      CONSTRUCTOR 
	 =======================*/
	/**
	 * Constructor of the route from the coordinates of a start point and a list of points
	 * @param start, points
	 * @throws IOException
	 */
	public Route(Instance instLIRP, int start, ArrayList<Integer> stops) throws IOException {
		this.instLIRP = instLIRP;
		this.start = start;
		this.fillStops(stops);
	}

	/**
	 * Create a Route object from the coordinates of a start point and a stop point (direct route only)
	 * @param start, stop
	 * @throws IOException
	 */
	public Route(Instance instLIRP, int start, int index) throws IOException {
		this.instLIRP = instLIRP;
		this.start = start;
		this.stops = new ArrayList<Integer>();
		this.stops.add(index);
		this.travelTime = computeDuration();
		this.stopTime = Parameters.stopping_time;
		this.cost = Parameters.fixed_cost_route + Parameters.cost_km * Parameters.avg_speed * this.travelTime;
		if(this.start < 0)
			this.cost += this.instLIRP.getDepot(index).getOrderingCost();
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
		return this.instLIRP.getDepot(start);
	}

	/**
	 * 
	 * @return	the array of locations corresponding to the stops
	 */
	public ArrayList<Integer> getStops() {
		return this.stops;
	}

	/**
	 * Get the position of a given stop
	 * @param stopIndex	the index of a location of interest along the route
	 * @return			the index of the corresponding stop in the sequence
	 */
	public int getPositionInRoute(int stopIndex) {
		return stops.indexOf(stopIndex);
	}

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
	 * @return	true if the route links the supplier with depots, false if it links a depot with clients
	 */
	public boolean isSD() {
		return this.start < 0;
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
		return (this.travelTime + this.stopTime) < Parameters.max_time_route;
	}
	/**
	 * Check if the Route object contains a given location in its stops
	 * @param index	the index of the location on the corresponding layer
	 * @return		true if the route contains the location, false otherwise
	 */
	public boolean containsStop(int index) {
		boolean inRoute = false;
		Iterator<Integer> stopsIter = this.stops.iterator();
		// Loop through the stops and update inRoute if the list of stops contains the index of interest
		while(!inRoute && stopsIter.hasNext()) {
			inRoute = (index == stopsIter.next());
		}
		return inRoute;
	}
	
	/**
	 * Check if the contains a given location
	 * @param loc	the location of interest
	 * @return		true if the route contains the location, false otherwise
	 */
	public boolean containsLocation(Location loc) {
		// Start by checking if loc is the starting point of the route
		boolean inRoute = (this.start < 0 ) ? false : (this.instLIRP.getDepot(start) == loc);
		Iterator<Integer> stopsIter = this.stops.iterator();
		// Loop through the stops and update inRoute if one corresponds to loc
		while(!inRoute && stopsIter.hasNext()) {
			if(start < 0)
				inRoute = (loc == this.instLIRP.getDepot(stopsIter.next()));
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
		ArrayList<Integer> newStops = new ArrayList<Integer> (stops);
		newStops.add(stopIndex);
		return new Route(this.instLIRP, this.start, newStops);
	}

	/**
	 * Compute the best permutation of the stops to minimize the cost of the route (brute force)
	 * @param partialPermutation	the current permutation of the stops in the route
	 * @param indicesNotInRoute	the indices that have not been included in the route yet
	 * @return
	 */
	private ArrayList<Integer> bruteForceFindBestRoute(ArrayList<Integer> partialPermutation, ArrayList<Integer> indicesNotInRoute)
	{
		/* Create a candidate for best route starting with the stops in r */
		ArrayList<Integer> bestRoute = new ArrayList<Integer>(partialPermutation);
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
			for(int i = 0; i < indicesNotInRoute.size(); i++)
			{
				int justRemoved = indicesNotInRoute.remove(0);
				ArrayList<Integer> newPartialPermutation = new ArrayList<Integer>(partialPermutation);
				newPartialPermutation.add(justRemoved);

				// Compute the best remaining route
				ArrayList<Integer> permutationCandidate = bruteForceFindBestRoute(newPartialPermutation, indicesNotInRoute);
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
	 * 
	 * @return	the lower bound on the duration of the road (longest triangle route between the start and two stops, plus the stop durations)
	 */
	private double getLB() {
		/* Initialize the lower bound to the cumulative duration of the stops */
		double stopsTime = this.stops.size() * Parameters.stopping_time;
		double travelDist = 0;

		int stop1 = 0;
		while(stop1 < this.stops.size()) {
			for(int stop2 = stop1 + 1; stop2 < this.stops.size(); stop2++) {
				Location loc1;
				Location loc2;
				if(this.start < 0) {
					loc1 = this.instLIRP.getDepot(this.stops.get(stop1));
					loc2 = this.instLIRP.getDepot(this.stops.get(stop2));
				}
				else{
					loc1 = this.instLIRP.getClient(this.stops.get(stop1));
					loc2 = this.instLIRP.getClient(this.stops.get(stop2));
				}
				travelDist = Math.max(this.getStart().getDistance(loc1) + this.getStart().getDistance(loc2) + loc1.getDistance(loc2), travelDist);
			}
			stop1++;
		}

		return stopsTime + travelDist / Parameters.avg_speed;
	}

	/**
	 * Compute the cost of the route associated with a given permutation
	 * @param stopsIndices	the permutation of the stops
	 * @return				the cost incurred by the sequence stopsIndices
	 */
	private double computeDuration(ArrayList<Integer> stopsIndices) {
		// Start with a time of 0 for the route
		double travelTime = 0;
		double stopsTime = Parameters.stopping_time * stopsIndices.size();
		// Set the current stop at the starting point
		Location currentStop = this.getStart();

		Location nextStop;
		Iterator<Integer> indexIterator = stopsIndices.iterator();
		// Iterate through the indices of the stops and increment the cost with the time necessary to reach the next stop and the average time spent at the stop 
		while (travelTime + stopsTime < Parameters.max_time_route && indexIterator.hasNext()) {
			nextStop = (this.start < 0) ? this.instLIRP.getDepot(indexIterator.next()):this.instLIRP.getClient(indexIterator.next());
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
	 * A methods that fill the attribute of the stops with the permutation that achieves the minimum cost, and fill the time and costs associated with the resulting route
	 * @param stops
	 */
	private void fillStops(ArrayList<Integer> stops) {
		this.stops = new ArrayList<Integer>(stops);
		ArrayList<Integer> startingPermutation = new ArrayList<Integer>(stops);

		if(this.getLB() < Parameters.max_time_route)
			this.stops = bruteForceFindBestRoute(new ArrayList<Integer>(), startingPermutation);

		this.travelTime = computeDuration();
		this.stopTime = Parameters.stopping_time * this.stops.size();
		this.cost = Parameters.fixed_cost_route + Parameters.cost_km * Parameters.avg_speed * this.travelTime;
	}
}


