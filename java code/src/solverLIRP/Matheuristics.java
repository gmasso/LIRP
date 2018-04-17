package solverLIRP;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import ilog.concert.IloException;
import instanceManager.Instance;
import instanceManager.Location;
import tools.Parameters;

public final class Matheuristics {

	private Matheuristics() {}

	public static Solution computeSolution(Instance instLIRP, RouteManager rm, boolean[] withLoops, int[] rSplit, LocManager lm)
			throws IloException {

		boolean noSampling = true;
		int lvlIter = 0;
		while(noSampling && lvlIter < instLIRP.getNbLevels()) {
			noSampling = (rSplit[lvlIter] < 1);
			lvlIter++;
		}
		if(noSampling) {
			HashMap<Integer, LinkedHashSet<Route>> mapRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
			for(int lvl = 0; lvl < instLIRP.getNbLevels(); lvl++) {
				mapRoutes.put(lvl, new LinkedHashSet<Route>(rm.getAllRoutesOfLvl(lvl)));
			}

			Solver solverLIRP = new Solver(instLIRP, mapRoutes, null, Parameters.noSplitTiLimit);
			return solverLIRP.getSolution();
		}
		else {
			return routeSamplingSol(instLIRP, rm, withLoops, rSplit, lm);
		}

	}

	/**
	 * Solves the LIRP problem on instance inst using the route sampling method,
	 * starting from
	 * 
	 * @param inst
	 * @param rm
	 * @param loopRoutes
	 * @return
	 */
	private static Solution routeSamplingSol(Instance instLIRP, RouteManager rm, boolean[] withLoops, int[] rSplit, LocManager lm) throws IloException {

		/*
		 * ======================================================= 
		 * Apply the route sampling algorithm: If the split 
		 * parameter is big enough, the set of routes will not be 
		 * split 
		 * =======================================================
		 */
		HashMap<Integer, LinkedHashSet<Route>> setOfRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
		int[] subsetSizes = new int[instLIRP.getNbLevels()];
		for(int lvl = 0; lvl < instLIRP.getNbLevels(); lvl++) {
			if(withLoops[lvl]) {
				LinkedHashSet<Route> lvlRoutes = new LinkedHashSet<Route>();
				int nbStops = 1;
				while(rm.getNbRoutesOfType(lvl, nbStops) > 0){
					lvlRoutes.addAll(rm.getAllRoutesOfType(lvl, nbStops));
					nbStops++;
				}
				/*
				 * ======================================================= 
				 * If we are solving the problem using cplex directly for 
				 * the current level, set the split parameter to the total 
				 * number of routes 
				 * =======================================================
				 */
				subsetSizes[lvl] = (rSplit[lvl] > 0) ? rSplit[lvl] : lvlRoutes.size();
				setOfRoutes.put(lvl,  lvlRoutes);
			}
			else {
				setOfRoutes.put(lvl, new LinkedHashSet<Route>());
			}
		}

		/*
		 * ========================================================= 
		 * Create dumb solutions to store the intermediate results
		 * =========================================================
		 */
		Solution bestSol = new Solution();
		Solution currentSol = new Solution();

		HashMap<Integer, LinkedHashSet<Route>> routesCandidates = new HashMap<Integer, LinkedHashSet<Route>>();

		while (bestSol.getObjVal() < 0 || currentSol.getObjVal() < bestSol.getObjVal()) {
			bestSol = currentSol;
			System.out.println("Best solution so far : " + bestSol.getObjVal());

			int nbSubsets = 1;
			HashMap<Integer, HashSet<LinkedHashSet<Route>>> mapSample= new HashMap<Integer, HashSet<LinkedHashSet<Route>>>();
			for(int lvl = 0; lvl < instLIRP.getNbLevels(); lvl++) {
				if(routesCandidates.containsKey(lvl)) {
					setOfRoutes.get(lvl).removeAll(routesCandidates.get(lvl));
				}
				HashSet<LinkedHashSet<Route>> rSample = sampleRoutes(setOfRoutes.get(lvl), subsetSizes[lvl]);
				nbSubsets *= rSample.size();
				mapSample.put(lvl, rSample);
			}
			HashSet<HashMap<Integer, LinkedHashSet<Route>>> setOfMapping = getCombinations(mapSample);

			System.out.println("========================================");
			System.out.println("== Solving with " + setOfMapping.size() + " subsets of routes ==");
			System.out.println("========================================");

			while(nbSubsets > 1) {
				HashMap<Integer, LinkedHashSet<Route>> collectedRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
				setOfMapping = getCombinations(mapSample);
				for(HashMap<Integer, LinkedHashSet<Route>> mapRoutes : setOfMapping) {
					int computeIter = 0;
					HashMap<Integer, LinkedHashSet<Route>> firstWave = new HashMap<Integer, LinkedHashSet<Route>>();
					HashMap<Integer, LinkedHashSet<Route>> secondWave = mapRoutes;
					for(int lvl = 0; lvl < instLIRP.getNbLevels() - 1; lvl++) {
						try {
							LinkedHashSet<Route> fRoutes = filterRoutes(lm, lvl, secondWave.get(lvl));
							firstWave.put(lvl, fRoutes);
							secondWave.get(lvl).removeAll(fRoutes);
						}
						catch (IOException ioe) {
							System.out.println("ERR: Problem while filtering the routes");
						}
					}
					while (computeIter < Parameters.recompute) {
						Solver solverLIRP;
						if(computeIter < 1) {
							solverLIRP = new Solver(instLIRP, firstWave, null, Parameters.auxTimeLimit);
						}
						else {
							solverLIRP = new Solver(instLIRP, secondWave, null, Parameters.auxTimeLimit);
						}
						Solution partialSol = solverLIRP.getSolution();
						if (partialSol != null) {
							HashMap<Integer, LinkedHashSet<Route>> usedRoutes = partialSol.collectUsedRoutes();
							for(int lvl: mapRoutes.keySet()) {
								if(collectedRoutes.get(lvl) != null)
									collectedRoutes.get(lvl).addAll(usedRoutes.get(lvl));
								else
									collectedRoutes.put(lvl, usedRoutes.get(lvl));
								/* Remove the collected routes to the first wave and add the remaining ones to the second wave */
								firstWave.get(lvl).removeAll(collectedRoutes.get(lvl));
								secondWave.get(lvl).addAll(firstWave.get(lvl));
								/* Add the direct routes to the second wave */
								secondWave.get(lvl).addAll(rm.getAllRoutesOfType(lvl, 1));
								/* Add the dummy routes to ensure feasibility */
								secondWave.get(lvl).addAll(rm.getAllRoutesOfType(lvl, 0));
							}
							computeIter++;
						}
					}
				}

				nbSubsets = 1;
				for(int lvl = 0; lvl < instLIRP.getNbLevels(); lvl++) {
					setOfRoutes.get(lvl).removeAll(routesCandidates.get(lvl));
					HashSet<LinkedHashSet<Route>> rSample = sampleRoutes(setOfRoutes.get(lvl), subsetSizes[lvl]);
					nbSubsets *= rSample.size();
					mapSample.put(lvl, rSample);
				}
			}

			for(int lvl = 0; lvl < instLIRP.getNbLevels(); lvl++) {
				for(LinkedHashSet<Route> solRoutes: mapSample.get(lvl))
					if(routesCandidates.containsKey(lvl)) {
						routesCandidates.get(lvl).addAll(solRoutes);
					}
					else {
						routesCandidates.put(lvl, solRoutes);
					}
			}
			Solver solverLIRP = new Solver(instLIRP, routesCandidates, null, Parameters.mainTimeLimit);
			System.out.println("done!");

			/* Call the method from the solver */
			currentSol = solverLIRP.getSolution();
			currentSol.computeObjValue();
		}

		return bestSol;
	}

	/**
	 * 
	 * @param loopIndices
	 *            the indices of routes from which we form the subsets
	 * @param subsetSizes
	 *            the desired size for the subsets produced
	 * @return a double-entry containing the indices of the routes contained in each
	 *         subset
	 */
	private static HashSet<LinkedHashSet<Route>> sampleRoutes(LinkedHashSet<Route> setOfRoutes, int subsetSizes) {
		int nbSubsets = (int) Math.ceil(((double) setOfRoutes.size()) / subsetSizes);
		HashSet<LinkedHashSet<Route>> rSubsets = new HashSet<LinkedHashSet<Route>>();
		ArrayList<Route> listOfRoutes = new ArrayList<Route>(setOfRoutes);

		/*
		 * If there is only one subset, return directly the set of indices taken as
		 * input
		 */
		if (nbSubsets < 2) {
			rSubsets.add(setOfRoutes);
			return rSubsets;
		}

		/* Shuffle code */
		int[] permutDC = new int[nbSubsets * subsetSizes];
		/* Shuffle the array at random */
		Random rnd = ThreadLocalRandom.current();
		/* Fill an array with the indices of possible routes */
		for (int loopIndex = 0; loopIndex < listOfRoutes.size(); loopIndex++)
			permutDC[loopIndex] = loopIndex;
		for (int i = listOfRoutes.size(); i > 0; i--) {
			int index = rnd.nextInt(i);
			int permutVal = permutDC[index];
			permutDC[index] = permutDC[i - 1];
			permutDC[i - 1] = permutVal;
		}

		/* Complete the remaining indices at the end */
		/* Define a range from which we draw the additional values */
		int rangeSelect = subsetSizes;
		/* Define the first index for which we do not have a route index */
		int missingIndex = nbSubsets * subsetSizes;
		while (missingIndex > listOfRoutes.size()) {
			/* Select a complete subset at random */
			int subsetRand = rnd.nextInt(nbSubsets - 1);
			/* Select the index of the element to copy from this subset at random */
			int swapIndex = rnd.nextInt(rangeSelect);

			/* Select an element to copy in the subset subsetRand at position swapIndex */
			int copyID = permutDC[subsetRand * subsetSizes + swapIndex];

			/*
			 * Swap the element to copy with the last one available in the selected subset
			 * (this ensures it will not be selected in a subsequent iteration)
			 */
			permutDC[subsetRand * subsetSizes + swapIndex] = permutDC[subsetRand * subsetSizes + rangeSelect - 1];
			permutDC[subsetRand * subsetSizes + rangeSelect - 1] = copyID;

			/* Add the item to copy t of the missing indices */
			permutDC[missingIndex - 1] = copyID;

			/* Increment the iterators */
			rangeSelect--;
			missingIndex--;
		}

		/* Copy the permutation to the result array */
		for (int subsetIndex = 0; subsetIndex < nbSubsets; subsetIndex++) {
			LinkedHashSet<Route> currentSubset = new LinkedHashSet<Route>();
			for (int idIter = 0; idIter < subsetSizes; idIter++) {
				currentSubset.add(listOfRoutes.get(permutDC[subsetIndex * subsetSizes + idIter]));
			}
			rSubsets.add(currentSubset);
		}

		return rSubsets;
	}


	/**
	 * Create a RouteManager object from an instance and the type of model under investigation
	 * @param instLIRP	the instance from which the set of routes is created
	 */
	private static LinkedHashSet<Route> filterRoutes(LocManager lm, int lvl, LinkedHashSet<Route> setOfRoutes) throws IOException {
		if(lm == null) {
			return setOfRoutes;
		}
		/* Select a random number of depots among the depots available */
		int maxNbDC = (int) Math.floor(0.6 * (lm.getInstance().getNbLocations(lvl)));
		HashSet<Location> dSelect = lm.depotSelect(Parameters.rand.nextInt(maxNbDC) + 1);
		LinkedHashSet<Route> rFilter = new LinkedHashSet<Route>();

		Iterator<Route> rIter = setOfRoutes.iterator();
		while(rIter.hasNext()) {
			Route currentRoute = rIter.next();
			Location rStart = currentRoute.getStart();
			if((rStart == lm.getInstance().getSupplier()) || (dSelect.contains(rStart))) {
				rFilter.add(currentRoute);
			}
		}
		return rFilter;
	}

	/**
	 * 
	 * @param setOfMaps	A map between the levels and all the set of routes available for the level
	 * @return			A set of maps containing all the possible combinations of set of routes across all levels of the map
	 */
	private static HashSet<HashMap<Integer, LinkedHashSet<Route>>> getCombinations(HashMap<Integer, HashSet<LinkedHashSet<Route>>> setOfMaps) {
		HashSet<HashMap<Integer, LinkedHashSet<Route>>> rCombos = new HashSet<HashMap<Integer, LinkedHashSet<Route>>>();

		TreeSet<Integer> lvls = new TreeSet<Integer>(setOfMaps.keySet());

		boolean firstIter = true;
		while(lvls.size() > 0) {
			int upLvl = lvls.pollFirst();
			Iterator<LinkedHashSet<Route>> setIter = setOfMaps.get(upLvl).iterator();
			while(setIter.hasNext()) {
				if(firstIter) {
					HashMap<Integer, LinkedHashSet<Route>> mapToAdd = new HashMap<Integer, LinkedHashSet<Route>>();
					mapToAdd.put(upLvl, setIter.next());
					rCombos.add(mapToAdd);
				}
				else {
					LinkedHashSet<Route> currentSet = setIter.next();
					for(HashMap<Integer, LinkedHashSet<Route>> mapToModify : rCombos) {
						mapToModify.put(upLvl, currentSet);
					}
				}
			}
			firstIter = false;
		}

		return rCombos;
	}

	private HashMap<Integer, LinkedHashSet<Route>> cleanMapRoutes(HashMap<Integer, LinkedHashSet<Route>> mapRoutes) {
		HashMap<Integer, LinkedHashSet<Route>> cleanMap = mapRoutes;
		
		return cleanMap;

	}


}
