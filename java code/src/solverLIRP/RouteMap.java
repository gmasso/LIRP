package solverLIRP;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import instanceManager.Location;
import tools.Config;

public class RouteMap extends HashMap<Integer, RouteSet>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1468663089627768419L;
	
	public RouteMap() {}
	
	/**
	 * Create a new RouteMap object that contains all the elements of an other one
	 * @param rMap	The RouteMap object from which to extract elements
	 */
	public RouteMap(RouteMap rMap) {
		this.putAll(rMap);
	}
	
	/**
	 * Complete the routes of each level with direct routes to ensure that each location is attainable from the previous one	
	 * @param directMap	The map of direct routes available
	 */
	public void completeMap(RouteMap directMap) {
		if(directMap.keySet().containsAll(this.keySet())) {
			for(int lvl : this.keySet()) {
				for(Route rDir : directMap.get(lvl)) {
					Iterator<Route> rIter = this.get(lvl).iterator();
					boolean reached = false;
					while(!reached && rIter.hasNext()) {
						reached = rIter.next().getStops().containsAll(rDir.getStops());
					}
					if(!reached) {
						System.out.println("Location " + rDir.getStops().toString() + " at level " + lvl + " unreachable, adding it to the subset");
						System.out.println("Direct route from DC " + rDir.getStart().toString() + " to client " + rDir.getStops().toString() + "added to the pool.");
						this.get(lvl).add(rDir);
					}
				}
			}
		}
		else {
			System.out.println("ERR while completing a set of routes with direct ones : levels don't match");
			System.exit(1);
		}
	}

	/**
	 * Filter the routes that are available or not according to an allocation
	 * @param lm			The LocManager object deciding which location is allocated to which DCs at the upper level
	 * @return				A RouteMap object containing only the filtered route, i.e. routes that stops only at locations allocated to its starting point
	 * @throws IOException
	 */
	public RouteMap filterRoutes(LocManager lm) throws IOException {
		if(lm == null) {
			return this;
		}
		RouteMap filteredRoutes = new RouteMap();
		for(int lvl : this.keySet()) {
			/* Select a random number of depots among the depots available */
			int maxNbDC = (int) Math.ceil(0.6 * (lm.getInstance().getNbLocations(lvl)));
			HashMap<Location, HashSet<Location>> locAlloc = lm.assignLocations(Config.RAND.nextInt(maxNbDC) + 1);
			RouteSet rFilter = new RouteSet();

			/* Loop through the routes and keep only the routes that start from the selected depots */
			Iterator<Route> rIter = this.get(lvl).iterator();
			while(rIter.hasNext()) {
				Route currentRoute = rIter.next();
				Location rStart = currentRoute.getStart();
				if(rStart == lm.getInstance().getSupplier()) {
					rFilter.add(currentRoute);
				}
				else if(locAlloc.containsKey(rStart)) {
					boolean allAlloc = true;
					Iterator<Integer> stopIter = currentRoute.stopIterator();
					while(allAlloc && stopIter.hasNext())
						allAlloc = locAlloc.get(rStart).contains(currentRoute.getStop(stopIter.next()));
					if(allAlloc) {
						rFilter.add(currentRoute);
					}
				}
			}
			filteredRoutes.put(lvl, rFilter);
		}
		return filteredRoutes;
	}
}
