package solverLIRP;

import java.io.IOException;
import java.util.*;

import instanceManager.Location;
import instanceManager.Parameters;

public class Route {

	private Location start; // The start of the route (depot in model 1, supplier in model 2)
	private Location[] stops; // Coordinates of the stopping points along the route (clients in model 1, depots in model 2)
	private ArrayList<Integer> stopsPermutation; // Permutation of the stops indices corresponding to the best route
	private double cost; // the cost of the route


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
		this.stopsPermutation = bruteForceFindBestRoute(new ArrayList<Integer>(), startingPermutation);
		this.cost = computeCost(this.stopsPermutation);
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
	 * Check if the contains a given location
	 * @param loc	the location of interest
	 * @return		true if the route contains the location, false otherwise
	 */
	public boolean containsLocation(Location loc) {
		// Start by checking if loc is the starting point of the route
		boolean inRoute = (loc==this.start);
		int stopsIter = 0;
		// Loop through the stops and update inRoute if one corresponds to loc
		while(!inRoute && stopsIter < stops.length) {
			inRoute = (loc == stops[stopsIter]);
		}
		return inRoute;
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

	/*
	 * METHODS
	 */	
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
		double bestCost = computeCost(bestRoute);

		// If the start of the route is already greater than the maximum time allowed, 
		// stop computations and return the current candidate
		if(computeCost(partialPermutation) > Parameters.max_time_route)
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
				double costCandidate = computeCost(permutationCandidate);
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
	
	// Compute the cost of the route for the sequence of stops contained in routeStops
	/**
	 * Compute the cost of the route associated with a given permutation
	 * @param stopsIndices	the permutation of the stops
	 * @return				the cost incurred by the sequence stopsIndices
	 */
	private double computeCost(ArrayList<Integer> stopsIndices) {
		// Start with a time of 0 for the route
		double time_route = 0;
		// Set the current stop at the starting point
		Location currentStop = this.start;
		
		int nextIndex;
		Location nextStop;
        Iterator<Integer> indexIterator = stopsIndices.iterator();
		// Iterate through the indices of the stops and increment the cost with the time necessary to reach the next stop and the average time spent at the stop 
        while (time_route < Parameters.max_time_route && indexIterator.hasNext()) {
        	nextIndex = indexIterator.next();
        	nextStop = this.stops[nextIndex];
			time_route += Parameters.stopping_time + Parameters.avg_speed * currentStop.getDistance(nextStop);
			currentStop = nextStop;
        }
        // Add the time to return to the starting point of the route
		time_route += Parameters.avg_speed * currentStop.getDistance(this.start);
		return time_route;
	}
}


