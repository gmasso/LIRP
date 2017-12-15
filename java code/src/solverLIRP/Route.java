package solverLIRP;

import java.io.IOException;
import java.util.*;

import instanceManager.Client;
import instanceManager.Depot;
import instanceManager.Location;
import instanceManager.Parameters;

public class Route {

	private Location start; // The start of the route (depot in model 1, supplier in model 2)
	private Location[] stops; // Coordinates of the stopping points along the route (clients in model 1, depots in model 2)
	private ArrayList<Integer> stopsPermutation; // Permutation of the stops indices corresponding to the best route
	private double travelTime; // The duration of the route (travel and stopping time)
	private double stopTime; // The total time spent delivering the stops
	private double cost; // The cost of the route


	/*
	 * CONSTRUCTOR 
	 */
	/**
	 * Constructor of the route from the coordinates of a start point and a list of points
	 * @param start, points
	 * @throws IOException
	 */
	public Route(Location start, Location[] stops) throws IOException {
		this.start = start;
		this.stops = stops;
		ArrayList<Integer> startingPermutation = new ArrayList<Integer>();
		for(int i=0; i < stops.length; i++)
			startingPermutation.add(i);

		if(this.getLB() > Parameters.max_time_route)
			this.stopsPermutation = startingPermutation;
		else
			this.stopsPermutation = bruteForceFindBestRoute(new ArrayList<Integer>(), startingPermutation);

		this.travelTime = computeDuration(this.stopsPermutation);
		this.stopTime = Parameters.stopping_time * this.stops.length;
		System.out.println(this.stopTime + ", " + this.travelTime);
		this.cost = Parameters.fixed_cost_route + Parameters.cost_km * Parameters.avg_speed * this.travelTime;
	}

	/**
	 * Create a Route object from the coordinates of a start point and a stop point (direct route only)
	 * @param start, stop
	 * @throws IOException
	 */
	public Route(Location start, Depot stop) throws IOException {
		this.start = start;
		this.stops = new Location[1];
		this.stops[0] = stop;
		this.stopsPermutation = new ArrayList<Integer>();
		this.stopsPermutation.add(0); 
		this.travelTime = computeDuration(this.stopsPermutation);
		this.stopTime = Parameters.stopping_time * this.stops.length;
		this.cost = stop.getOrderingCost() + Parameters.fixed_cost_route + Parameters.cost_km * Parameters.avg_speed * this.travelTime;
	}

	/**
	 * Create a Route object from the coordinates of a start point and a stop point (direct route only)
	 * @param start, stop
	 * @throws IOException
	 */
	public Route(Location start, Client stop) throws IOException {
		this.start = start;
		this.stops = new Location[1];
		this.stops[0] = stop;
		this.stopsPermutation = new ArrayList<Integer>();
		this.stopsPermutation.add(0); 
		this.travelTime = computeDuration(this.stopsPermutation);
		this.stopTime = Parameters.stopping_time * this.stops.length;
		this.cost = Parameters.fixed_cost_route + Parameters.cost_km * Parameters.avg_speed * this.travelTime;
	}

	/*
	 * ACCESSORS 
	 */
	/**
	 * 
	 * @return	the length of the route
	 */
	public int getNbStops() {
		return this.stops.length;
	}

	/**
	 * 
	 * @return	the location at the start of the route
	 */
	public Location getStart() {
		return this.start;
	}

	/**
	 * 
	 * @return	the array of locations corresponding to the stops
	 */
	public Location[] getStops() {
		return this.stops;
	}

	/**
	 * Get the position of a given stop
	 * @param stopIndex	the index of a location of interest along the route
	 * @return			the index of the corresponding stop in the sequence
	 */
	public int getPositionInRoute(int stopIndex) {
		return stopsPermutation.indexOf(stopIndex);
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
	 * Check if the contains a given location
	 * @param loc	the location of interest
	 * @return		true if the route contains the location, false otherwise
	 */
	public boolean containsLocation(Location loc) {
		// Start by checking if loc is the starting point of the route
		boolean inRoute = (loc == this.start);
		int stopsIter = 0;
		// Loop through the stops and update inRoute if one corresponds to loc
		while(!inRoute && stopsIter < stops.length) {
			inRoute = (loc == stops[stopsIter]);
			stopsIter++;
		}
		return inRoute;
	}

	/**
	 * Create a new Route object by adding a new stop to this route
	 * @param stop	the new stop to be added
	 * @return		a new Route object extending the current one with the addition of stop to the loop
	 * @throws IOException
	 */
	public Route extend(Location stop) throws IOException {
		/* Check that the route does not already contains the stop that is about to be added */
		if(this.containsLocation(stop)){
			System.out.println("Trying to add a stop to route that already contains it");
			System.exit(1);
		}

		/* Create a new array to list the stops of the new Route object */
		Location[] newStops = new Location[this.stops.length + 1];
		for(int stopsIter = 0; stopsIter < this.stops.length; stopsIter++)
			newStops[stopsIter] = this.stops[stopsIter];
		newStops[this.stops.length] = stop;
		return new Route(this.start, newStops);
	}

	/**
	 * Compute the best permutation of the stops to minimize the cost of the route (brute force)
	 * @param partialPermutation	the current permutation of the stops in the route
	 * @param indicesNotInRoute	the indices that have not been included in the route yet
	 * @return
	 */
	private ArrayList<Integer> bruteForceFindBestRoute(ArrayList<Integer> partialPermutation, ArrayList<Integer> indicesNotInRoute)
	{
		// Create a candidate for best route starting with the stops in r
		ArrayList<Integer> bestRoute = new ArrayList<Integer>(partialPermutation);
		// Add all the remaining stops to the candidate as they appear in the stopsNotInRoute
		bestRoute.addAll(indicesNotInRoute);
		double bestCost = computeDuration(bestRoute);

		// If the start of the route is already greater than the maximum time allowed, 
		// stop computations and return the current candidate
		if(computeDuration(partialPermutation) + Parameters.stopping_time * partialPermutation.size() > Parameters.max_time_route)
			return bestRoute;

		// If there are stops that are not included in the route, try to add them one by one
		if(!indicesNotInRoute.isEmpty())
		{
			// Loop through the possible next stops along the route
			for(int i = 0; i<indicesNotInRoute.size(); i++)
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
				// Replace the stop that has just been extracted at the end of the list
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
		double stopsTime = this.stops.length * Parameters.stopping_time;
		double travelDist = 0;

		for(int stop1 = 0; stop1 < this.stops.length - 1; stop1++)
			for(int stop2 = stop1 + 1; stop2 < this.stops.length; stop2++) {
				double td = this.start.getDistance(this.stops[stop1]) + this.start.getDistance(this.stops[stop2]) + this.stops[stop1].getDistance(this.stops[stop2]);
				travelDist = Math.max(td, travelDist);
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
		Location currentStop = this.start;

		int nextIndex;
		Location nextStop;
		Iterator<Integer> indexIterator = stopsIndices.iterator();
		// Iterate through the indices of the stops and increment the cost with the time necessary to reach the next stop and the average time spent at the stop 
		while (travelTime + stopsTime < Parameters.max_time_route && indexIterator.hasNext()) {
			nextIndex = indexIterator.next();
			nextStop = this.stops[nextIndex];
			travelTime += currentStop.getDistance(nextStop) / Parameters.avg_speed;
			currentStop = nextStop;
		}
		// Add the time to return to the starting point of the route
		travelTime += currentStop.getDistance(this.start) / Parameters.avg_speed;


		return travelTime;
	}
}


