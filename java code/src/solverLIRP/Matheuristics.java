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
import tools.Config;

public final class Matheuristics {

	private Matheuristics() {}

	public static Solution computeSolution(Instance instLIRP, RouteManager rm, boolean[] withLoops, int[] rSplit, LocManager lm, boolean presolve)
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
				mapRoutes.put(lvl, rm.getAllRoutesOfLvl(lvl));
			}
			long startChrono = System.currentTimeMillis();

			Solver solverLIRP = new Solver(instLIRP, mapRoutes, null, Config.NOSPLIT_TILIM);
			Solution NoSplitSol = solverLIRP.getSolution(false, Config.EPSILON);

			long stopChrono = System.currentTimeMillis();

			NoSplitSol.setSolvingTime(stopChrono - startChrono);
			return NoSplitSol;
		}
		else {
			return routeSamplingSol(instLIRP, rm, withLoops, rSplit, lm, presolve);
		}

	}

	/**
	 * Solves an LIRP problem using the route sampling method
	 * @param instLIRP		The instance to solve
	 * @param rm			The RouteManager object containing the routes available for the instance
	 * @param withLoops	 	Indicator for each level of the network if multi-stops routes are allowed or not
	 * @param rSplit		The split parameter for loop routes at each level
	 * @param lm			The LocManager object to use to link location with sources from the upper level
	 * @return				A solution to the LIRP problem corresponding to the instance instLIRP
	 * @throws IloException
	 */
	private static Solution routeSamplingSol(Instance instLIRP, RouteManager rm, boolean[] withLoops, int[] rSplit, LocManager lm, boolean presolve) throws IloException {
		/*
		 * ======================================================= 
		 * Create two HashMaps of Routes objects : the first one 
		 * contains the direct routes for each level, the second 
		 * one contains the loop routes for each level
		 * ======================================================= 
		 */
		HashMap<Integer, LinkedHashSet<Route>> setOfDirect = getAllDirects(rm);
		HashMap<Integer, LinkedHashSet<Route>> setOfRoutes = getAllRoutes(rm, withLoops);
		/* The total time available to solve the instance is the same as the solver time without sampling */
		double timeLimit = Config.NOSPLIT_TILIM;

		int[] subsetSizes = new int[instLIRP.getNbLevels()];

		for(int lvl = 0; lvl < subsetSizes.length; lvl++) {
			/* If the split parameter of this level is greater than 0, 
			 * set the size of subset of routes for this level accordingly
			 * and set a maximum time spent on partial solutions of at 
			 * most 67% of the time allowed for the solver with no splitting
			 */
			if(rSplit[lvl] > 0) {
				subsetSizes[lvl] = rSplit[lvl];
			}
			/* Otherwise, we create only one subset containing all the routes */
			else {
				subsetSizes[lvl] = setOfRoutes.get(lvl).size();
			}
		}
		
		/* Create dumb solutions to store the intermediate results */
		Solution currentSol = computeSampleSol(instLIRP, setOfDirect, setOfRoutes, subsetSizes, lm, timeLimit, presolve);
		Solution bestSol = new Solution();

		/* Total time spent on partial solutions */
		double totalTime = currentSol.getSolvingTime();

		/* As long as we have some time available, re-apply the algorithm to find other solutions */
		while(bestSol.getObjVal() < 0 || (totalTime < timeLimit && currentSol != null && currentSol.getObjVal() < bestSol.getObjVal())) {
			bestSol = currentSol;
			if(timeLimit - totalTime < Config.MAIN_TILIM) {
				currentSol = null;
			}
			else {
				/* Get a new current solution using a new sampling of the loop routes */
				currentSol = computeSampleSol(instLIRP, setOfDirect, setOfRoutes, subsetSizes, lm, timeLimit - totalTime, false);
			}
			if(currentSol != null)
				totalTime += currentSol.getSolvingTime();
			System.out.println("Total Time elapsed : " + totalTime);
		}
		return bestSol;
	}

	/**
	 * Compute a solution to the original LIRP instance using the route sampling algorithm
	 * @param instLIRP		The instance of the LIRP problem considered
	 * @param routesLvls	The set of available routes for each level of the network
	 * @return				A pair containing the solving time and the set of routes used in the solution (from the set of available ones)
	 */
	private static Solution computeSampleSol(Instance instLIRP, HashMap<Integer, LinkedHashSet<Route>> setOfDirect, HashMap<Integer, LinkedHashSet<Route>> setOfRoutes, int[] subsetSizes, LocManager lm, double remainingTime, boolean presolve){
		try {
			HashMap<Integer, LinkedHashSet<Route>> filteredRoutes = filterRoutes(lm, setOfRoutes);
			double totalTime = 0;
			/* If the presolve option is activated, solve the problem without sampling first and to extract the routes used and 
			 * reduce the pool of routes from which to sample from 
			 */
			if(presolve) {
				long startChrono = System.currentTimeMillis();
				filteredRoutes = collectRoutes(instLIRP, setOfDirect, filteredRoutes, remainingTime - totalTime, Config.PRESOLVE_TILIM, presolve);
				long stopChrono = System.currentTimeMillis();
				totalTime += stopChrono - startChrono;
			}
			System.out.println();
			System.out.println("NUMBER OF ROUTES COLLECTED FROM PRESOLVE : " + filteredRoutes.size());
			System.out.println();

			
			/* Create a HashMap to store the subsets of routes at each level */
			HashMap<Integer, HashSet<LinkedHashSet<Route>>> lvlSamples = computeSamples(filteredRoutes, subsetSizes);
			int nbSubsets = 1;
			int previousNbRoutes = 0;
			/* The total number of subset is the number of possible combinations between all the subset of routes */
			for(int lvl = 0; lvl < instLIRP.getNbLevels(); lvl++) {
				previousNbRoutes += setOfRoutes.get(lvl).size();
				nbSubsets *= lvlSamples.get(lvl).size();
			}

			completeSubsets(lvlSamples, setOfDirect);

			/* Get all the possible combinations of route samples from each level */
			HashSet<HashMap<Integer, LinkedHashSet<Route>>> setOfMapRoutes = getCombinations(lvlSamples);

			/* Routes object that are candidate for the final solution */
			HashMap<Integer, LinkedHashSet<Route>> selectedRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
			for(int lvl: setOfDirect.keySet()) {
				selectedRoutes.put(lvl, new LinkedHashSet<Route>());
				selectedRoutes.get(lvl).addAll(setOfDirect.get(lvl));
			}
			/* If the number of subsets is 1, add all the multi-stops routes to the set of available route objects to compute the final solution */
			if(nbSubsets == 1) {
				for(int lvl: selectedRoutes.keySet())
					selectedRoutes.get(lvl).addAll(setOfRoutes.get(lvl));
			}

			int nbIterations = 0;
			int nbRoutes = 0;
			
			while(nbSubsets > 1 && nbRoutes < previousNbRoutes && totalTime + Config.MAIN_TILIM < remainingTime) {
				/* Set of all loops used in at least one of the intermediate solutions */
				HashMap<Integer, LinkedHashSet<Route>> collectedRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
				nbIterations++;
				System.out.println("========================================");
				System.out.println("== Iteration " + nbIterations + ": Solving with " + setOfMapRoutes.size() + " subsets of routes ==");
				System.out.println("========================================");
				double partialTiLim = Math.max((remainingTime - Config.MAIN_TILIM) / (1.5 * Config.RECOMPUTE * setOfMapRoutes.size()), Config.AUX_TILIM);
				for(HashMap<Integer, LinkedHashSet<Route>> availRoutes : setOfMapRoutes) {
					long startChrono = System.currentTimeMillis();
					HashMap<Integer, LinkedHashSet<Route>> eliteRoutes = collectRoutes(instLIRP, setOfDirect, availRoutes, remainingTime - totalTime, partialTiLim, false);
					for(int lvl : eliteRoutes.keySet()) {
						if(collectedRoutes.get(lvl) != null) {
							collectedRoutes.get(lvl).addAll(eliteRoutes.get(lvl));
						}
						else {
							collectedRoutes.put(lvl, new LinkedHashSet<Route>());
							collectedRoutes.get(lvl).addAll(eliteRoutes.get(lvl));
						}
					}
					long stopChrono = System.currentTimeMillis();
					totalTime += stopChrono - startChrono;
				}

				/* Re-sample from the set of collected loops */
				lvlSamples = computeSamples(collectedRoutes, subsetSizes);
				/* Complete the subsets to reach all locations */
				completeSubsets(lvlSamples, setOfDirect);
				/* Get all the possible combinations of route samples from each level */
				setOfMapRoutes = getCombinations(lvlSamples);
				/* Compute the number of subsets left  after this iteration */
				nbSubsets = 1;
				previousNbRoutes = nbRoutes;
				nbRoutes = 0;
				/* The total number of subset is the number of possible combinations between all the subset of routes */
				for(int lvl = 0; lvl < instLIRP.getNbLevels(); lvl++) {
					nbRoutes += collectedRoutes.get(lvl).size();
					nbSubsets *= lvlSamples.get(lvl).size();
				}

				/* If this is the last computation of partial solution, keep all the collected multi-stops routes for the final computation */
				if(nbSubsets == 1) {
					for(int lvl: selectedRoutes.keySet())
						selectedRoutes.get(lvl).addAll(collectedRoutes.get(lvl));
				}
			}

			try {
				long startChrono = System.currentTimeMillis();
				if(remainingTime - totalTime > Config.AUX_TILIM) {
					/* Use the set of collected routes to solve the instance */
					Solver solverLIRP = new Solver(instLIRP, selectedRoutes, null, remainingTime - totalTime);
					/* Call the method from the solver */
					Solution sampleSol = solverLIRP.getSolution(presolve, Config.EPSILON);

					long stopChrono = System.currentTimeMillis();
					sampleSol.setSolvingTime(stopChrono - startChrono);

					sampleSol.computeObjValue();

					totalTime += sampleSol.getSolvingTime();
					System.out.println("Setting the solving time to " + totalTime);
					sampleSol.setSolvingTime(((long) (totalTime * 1000)));
					return sampleSol;
				}
			}
			catch(IloException iloe) {
				System.out.println("ERR: Problem while solving the problem");
				System.out.println(iloe.getMessage());
				System.exit(1);
			}

		}

		catch (IOException ioe) {
			System.out.println("ERR: Problem while filtering the routes");
			System.out.println(ioe.getMessage());
			System.exit(1);
		}
		return null;
	}

	/**
	 * 
	 * @param instLIRP
	 * @param setOfDirect
	 * @param availRoutes
	 * @param timeLeft
	 * @param partialTiLim
	 * @return
	 */
	private static HashMap<Integer, LinkedHashSet<Route>> collectRoutes(Instance instLIRP, HashMap<Integer, LinkedHashSet<Route>> setOfDirect, HashMap<Integer, LinkedHashSet<Route>> availRoutes, double timeLeft, double partialTiLim, boolean presolve){
		HashMap<Integer, LinkedHashSet<Route>> allUsedRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
		/* Create a map of available routes after filtering the routes in mapLoops */
		int computeIter = 0;
		int nbComput = Config.RECOMPUTE;
		if(presolve) {
			nbComput = Config.PRESOLVE_NB;
			partialTiLim *= 1.0/nbComput; 
		}
		while (computeIter < nbComput) {
			Solution partialSol = null;
			/* If the remaining time is enough to compute a solution to a subproblem, 
			 * create a problem with a subset of available loops */
			if(Config.MAIN_TILIM < timeLeft) {
				if(presolve) 
					System.out.println("Computing a first solution using " + availRoutes.get(1).size() + " routes");
				else 
					System.out.println("Computing a partial solution using " + availRoutes.get(1).size() + " routes");
				partialSol = getPartialSol(instLIRP, availRoutes, partialTiLim, presolve);
			}
			/* If we have computed a partial solution, add the routes used in this solution to the collected ones */
			if (partialSol != null) {
				HashMap<Integer, LinkedHashSet<Route>> usedRoutes = partialSol.collectUsedRoutes();
				for(int lvl: availRoutes.keySet()) {
					if(allUsedRoutes.get(lvl) != null) {
						allUsedRoutes.get(lvl).addAll(usedRoutes.get(lvl));
					}
					else {
						allUsedRoutes.put(lvl, new LinkedHashSet<Route>());
						allUsedRoutes.get(lvl).addAll(usedRoutes.get(lvl));
					}

					/* Remove the collected multi-stops routes from the available multi-stops routes for the next iteration */
					availRoutes.get(lvl).removeAll(usedRoutes.get(lvl));
				}
				completeSubset(availRoutes, setOfDirect);
			}
			/* If there is no partial solution because of a lack of time,
			 * add all the remaining loops to the collected ones
			 */
			else {
				for(int lvl: availRoutes.keySet()) {
					allUsedRoutes.get(lvl).addAll(availRoutes.get(lvl));
				}
			}
			computeIter++;
		}
		
		return allUsedRoutes;
	}
	
	/**
	 * 
	 * @param setOfRoutes
	 * @param setOfDirect
	 */
	private static void completeSubset(HashMap<Integer, LinkedHashSet<Route>> setOfRoutes, HashMap<Integer, LinkedHashSet<Route>> setOfDirect) {
		if(setOfDirect.keySet().containsAll(setOfRoutes.keySet())) {
			for(int lvl : setOfRoutes.keySet()) {
				for(Route rDir : setOfDirect.get(lvl)) {
					Iterator<Route> rIter = setOfRoutes.get(lvl).iterator();
					boolean reached = false;
					while(!reached && rIter.hasNext()) {
						reached = rIter.next().getStops().containsAll(rDir.getStops());
					}
					if(!reached) {
						System.out.println("Location " + rDir.getStops().toString() + " at level " + lvl + " unreachable, adding it to the subset");
						System.out.println("Direct route from DC " + rDir.getStart().toString() + " to client " + rDir.getStops().toString() + "added to the pool.");
						setOfRoutes.get(lvl).add(rDir);
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
	 * Add to a set of Route objects all the direct routes to locations of the next level that it does not reach
	 * @param lvlSamples	set of routes to check
	 * @param setOfDirect	set of direct routes that can be added to complete setOfRoutes
	 */
	private static void completeSubsets(HashMap<Integer, HashSet<LinkedHashSet<Route>>> lvlSamples, HashMap<Integer, LinkedHashSet<Route>> setOfDirect) {
		if(setOfDirect.keySet().containsAll(lvlSamples.keySet())) {
			for(int lvl : lvlSamples.keySet()) {
				for(Route rDir : setOfDirect.get(lvl)) {
					Iterator<LinkedHashSet<Route>> setIter = lvlSamples.get(lvl).iterator();
					while(setIter.hasNext()) {
						boolean reached = false;
						LinkedHashSet<Route> currentSet = setIter.next(); 
						currentSet.addAll(setOfDirect.get(lvl));
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
	 * Compute a partial solution based on a set of available routes
	 * @param instLIRP		The instance of the LIRP problem considered
	 * @param routesLvls	The set of available routes for each level of the network
	 * @return				A pair containing the solving time and the set of routes used in the solution (from the set of available ones)
	 */
	private static Solution getPartialSol(Instance instLIRP, HashMap<Integer, LinkedHashSet<Route>> routesLvls, double timeLim, boolean presolve){

		long startChrono = System.currentTimeMillis();
		try {
			Solver solverLIRP = new Solver(instLIRP, routesLvls, null, timeLim);
			Solution partialSol; 
			if(presolve) 
				partialSol = solverLIRP.getSolution(true, Config.ACCEPT_TS);
			else 
				partialSol = solverLIRP.getSolution(false, Config.EPSILON);

			long stopChrono = System.currentTimeMillis();

			partialSol.setSolvingTime(stopChrono - startChrono);
			return partialSol;
		} catch (IloException iloe) {
			System.out.println("ERR while trying to solve a subproblem: " + iloe.getMessage());
			System.exit(1);
		}

		return null;
	}

	/**
	 * 
	 * @param setOfLoops	The set of multi-stops routes at every level of the network
	 * @param subsetSizes	The size of the subsets to create a each level
	 * @return				A HashMap containing for each level a collection of subsets of multi-stops routes
	 */
	private static HashMap<Integer, HashSet<LinkedHashSet<Route>>> computeSamples(HashMap<Integer, LinkedHashSet<Route>> setOfLoops, int[] subsetSizes) {

		HashMap<Integer, HashSet<LinkedHashSet<Route>>> lvlSamples = new HashMap<Integer, HashSet<LinkedHashSet<Route>>>();
		for(int lvl = 0; lvl < subsetSizes.length; lvl++) {
			/* Sample the routes at each level and put it in the map */
			HashSet<LinkedHashSet<Route>> routesSample = new HashSet<LinkedHashSet<Route>>();
			if(subsetSizes[lvl] > 0) {
				routesSample.addAll(sampleRoutes(setOfLoops.get(lvl), subsetSizes[lvl]));
			}
			else {
				routesSample.add(setOfLoops.get(lvl));
			}
			lvlSamples.put(lvl, routesSample);
		}

		return lvlSamples;
	}


	/**
	 * Return a set of multi-stops for each level of the network
	 * @param rm		The RouteManager object containing the routes for the instance
	 * @param withLoops	An array indicating for each level if multi-stops routes are considered of not
	 * @return			A HashMap containing the multi-stops routes at every level of the network
	 */
	private static HashMap<Integer, LinkedHashSet<Route>> getAllRoutes(RouteManager rm, boolean[] withLoops){
		HashMap<Integer, LinkedHashSet<Route>> setOfRoutes = new HashMap<Integer, LinkedHashSet<Route>>(getAllDirects(rm));
		for(int lvl = 0; lvl < rm.getInstance().getNbLevels(); lvl++) {
			if(withLoops[lvl]) {
				LinkedHashSet<Route> lvlRoutes = new LinkedHashSet<Route>();
				int nbStops = 2;
				while(rm.getNbRoutesOfType(lvl, nbStops) > 0){
					lvlRoutes.addAll(rm.getAllRoutesOfType(lvl, nbStops));
					nbStops++;
				}
				setOfRoutes.get(lvl).addAll(lvlRoutes);
			}
		}
		return setOfRoutes;
	}

	/**
	 * 
	 * @param rm	The RouteManager object from which direct routes are collected
	 * @return		A HashMap object containing the set of direct routes for each level
	 */
	private static HashMap<Integer, LinkedHashSet<Route>> getAllDirects(RouteManager rm){
		HashMap<Integer, LinkedHashSet<Route>> setOfDirects = new HashMap<Integer, LinkedHashSet<Route>>();
		for(int lvl = 0; lvl < rm.getInstance().getNbLevels(); lvl++) {
			setOfDirects.put(lvl, new LinkedHashSet<Route>());
			setOfDirects.get(lvl).addAll(rm.getAllRoutesOfType(lvl, 1));
		}
		return setOfDirects;
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
	 * Filter the routes that are available or not according to an allocation
	 * @param lm			A LocManager object indicating the allocation of locations to one or several DCs of the upper level
	 * @param lvl			The level at which the routes have to be filtered
	 * @param setOfRoutes	The set of Route objects on which to apply the the filter
	 * @return				A filtered set of routes: Every route in the returned set only contains routes involving locations that are assigned to the start of the route
	 * @throws IOException
	 */
	private static HashMap<Integer, LinkedHashSet<Route>> filterRoutes(LocManager lm, HashMap<Integer, LinkedHashSet<Route>> setOfLvlRoutes) throws IOException {
		if(lm == null) {
			return setOfLvlRoutes;
		}
		HashMap<Integer, LinkedHashSet<Route>> filteredRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
		for(int lvl : setOfLvlRoutes.keySet()) {
			/* Select a random number of depots among the depots available */
			int maxNbDC = (int) Math.ceil(0.6 * (lm.getInstance().getNbLocations(lvl)));
			HashMap<Location, HashSet<Location>> locAlloc = lm.assignLocations(Config.RAND.nextInt(maxNbDC) + 1);
			LinkedHashSet<Route> rFilter = new LinkedHashSet<Route>();

			/* Loop through the routes and keep only the routes that start from the selected depots */
			Iterator<Route> rIter = setOfLvlRoutes.get(lvl).iterator();
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

	/**
	 * 
	 * @param setOfMaps	A map between the levels and all the set of routes available for the level
	 * @return			A set of maps containing all the possible combinations of set of routes across all levels of the map
	 */
	private static HashSet<HashMap<Integer, LinkedHashSet<Route>>> getCombinations(HashMap<Integer, HashSet<LinkedHashSet<Route>>> setOfMaps) {
		HashSet<HashMap<Integer, LinkedHashSet<Route>>> previousCombos = new HashSet<HashMap<Integer, LinkedHashSet<Route>>>();
		HashSet<HashMap<Integer, LinkedHashSet<Route>>> currentCombos = new HashSet<HashMap<Integer, LinkedHashSet<Route>>>();

		TreeSet<Integer> lvls = new TreeSet<Integer>(setOfMaps.keySet());

		while(lvls.size() > 0) {
			previousCombos.clear();
			previousCombos.addAll(currentCombos);
			currentCombos.clear();
			int upLvl = lvls.pollFirst();
			Iterator<LinkedHashSet<Route>> setIter = setOfMaps.get(upLvl).iterator();
			while(setIter.hasNext()) {
				if(previousCombos.isEmpty()) {
					HashMap<Integer, LinkedHashSet<Route>> mapToAdd = new HashMap<Integer, LinkedHashSet<Route>>();

					mapToAdd.put(upLvl, new LinkedHashSet<Route>());
					mapToAdd.get(upLvl).addAll(setIter.next());
					currentCombos.add(mapToAdd);
				}
				else {
					LinkedHashSet<Route> currentSet = setIter.next();
					for(HashMap<Integer, LinkedHashSet<Route>> mapToModify : previousCombos) {
						HashMap<Integer, LinkedHashSet<Route>> mapToAdd = new HashMap<Integer, LinkedHashSet<Route>>();
						for(int lvl : mapToModify.keySet()) {
							mapToAdd.put(lvl, new LinkedHashSet<Route>());
							mapToAdd.get(lvl).addAll(mapToModify.get(lvl));
						}
						mapToAdd.put(upLvl, new LinkedHashSet<Route>());
						mapToAdd.get(upLvl).addAll(currentSet);
						currentCombos.add(mapToAdd);
					}
				}
			}
		}

		return currentCombos;
	}
}
