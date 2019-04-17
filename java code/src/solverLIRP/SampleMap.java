package solverLIRP;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

public class SampleMap extends HashMap<Integer, HashSet<RouteSet>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4551413446950814485L;

	/**
	 * 
	 * @param setOfLoops	The set of multi-stops routes at every level of the network
	 * @param subsetSizes	The size of the subsets to create a each level
	 * @return				A SampleMap object containing for each level a collection of subsets of multi-stops routes
	 */
	public SampleMap (RouteMap rMap, int[] subsetSizes) {

		for(int lvl = 0; lvl < subsetSizes.length; lvl++) {
			/* Sample the routes at each level and put it in the map */
			HashSet<RouteSet> routesSample = new HashSet<RouteSet>();
			if(subsetSizes[lvl] > 0) {
				routesSample.addAll(rMap.get(lvl).sample(subsetSizes[lvl]));
			}
			else {
				routesSample.add(rMap.get(lvl));
			}
			this.put(lvl, routesSample);
		}
	}
	
	/**
	 * Redefine the samples in each level of the SampleMap object
	 * @param rMap			A RouteMap object from which to create the samples
	 * @param subsetSizes	The samples sizes in each level
	 */
	public void reSample(RouteMap rMap, int[] subsetSizes) {
		this.clear();
		for(int lvl = 0; lvl < subsetSizes.length; lvl++) {
			/* Sample the routes at each level and put it in the map */
			HashSet<RouteSet> routesSample = new HashSet<RouteSet>();
			if(subsetSizes[lvl] > 0) {
				routesSample.addAll(rMap.get(lvl).sample(subsetSizes[lvl]));
			}
			else {
				routesSample.add(rMap.get(lvl));
			}
			this.put(lvl, routesSample);
		}
	}
	
	/**
	 * Add to a set of Route objects all the direct routes to locations of the next level that it does not reach
	 * @param lvlSamples	set of routes to check
	 * @param directMap	set of direct routes that can be added to complete setOfRoutes
	 */
	public void completeSamples(RouteMap directMap) {
		if(directMap.keySet().containsAll(this.keySet())) {
			for(int lvl : this.keySet()) {
				for(Route rDir : directMap.get(lvl)) {
					Iterator<RouteSet> setIter = this.get(lvl).iterator();
					while(setIter.hasNext()) {
						boolean reached = false;
						RouteSet currentSet = setIter.next(); 
						currentSet.addAll(directMap.get(lvl));
						Iterator<Route> rIter = currentSet.iterator();
						while(!reached && rIter.hasNext()) {
							reached = rIter.next().getStops().containsAll(rDir.getStops());
						}
						if(!reached) {
							System.out.println("Location " + rDir.getStops().toString() + " unreachable, adding it to the subset");
							System.out.println("Direct route from DC " + rDir.getStart().toString() + " to client " + rDir.getStops().toString() + "added to the pool.");
							currentSet.add(rDir);
						}
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
	 * 
	 * @param setOfMaps	A map between the levels and all the set of routes available for the level
	 * @return			A set of maps containing all the possible combinations of set of routes across all levels of the map
	 */
	public HashSet<RouteMap> getCombinations() {
		HashSet<RouteMap> previousCombos = new HashSet<RouteMap>();
		HashSet<RouteMap> currentCombos = new HashSet<RouteMap>();

		TreeSet<Integer> lvls = new TreeSet<Integer>(this.keySet());

		while(lvls.size() > 0) {
			previousCombos.clear();
			previousCombos.addAll(currentCombos);
			currentCombos.clear();
			int upLvl = lvls.pollFirst();
			Iterator<RouteSet> setIter = this.get(upLvl).iterator();
			while(setIter.hasNext()) {
				if(previousCombos.isEmpty()) {
					RouteMap mapToAdd = new RouteMap();

					mapToAdd.put(upLvl, new RouteSet());
					mapToAdd.get(upLvl).addAll(setIter.next());
					currentCombos.add(mapToAdd);
				}
				else {
					RouteSet currentSet = setIter.next();
					for(RouteMap mapToModify : previousCombos) {
						RouteMap mapToAdd = new RouteMap();
						for(int lvl : mapToModify.keySet()) {
							mapToAdd.put(lvl, new RouteSet());
							mapToAdd.get(lvl).addAll(mapToModify.get(lvl));
						}
						mapToAdd.put(upLvl, new RouteSet());
						mapToAdd.get(upLvl).addAll(currentSet);
						currentCombos.add(mapToAdd);
					}
				}
			}
		}

		return currentCombos;
	}

}
