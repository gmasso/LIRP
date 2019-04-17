package solverLIRP;

import java.io.IOException;
import java.util.HashSet;

import ilog.concert.IloException;
import instanceManager.Instance;
import tools.Config;

public final class RSH {

	private RSH() {}

	/**
	 * Returns a solution to an LIRP instance by applying (or not) RSH on its different levels
	 * @param instLIRP	The instance to solve
	 * @param rm		The RouteManager for this instance
	 * @param withLoops	Indicators for each level if it includes loops or not
	 * @param rSplit	The split parameters for the routes at each level (no split if the parameter is equal to 0)
	 * @param lm		The location manager if we pre-process an assignement for each location to a DC
	 * @param presolve	If we start with a quick and dirty resolution of the problem to start extracting some routes
	 * @return			A solution to the LIRP problem
	 * @throws IloException
	 */
	public static Solution computeSolution(Instance instLIRP, RouteManager rm, boolean[] withLoops, int[] rSplit, LocManager lm, boolean presolve)
			throws IloException {

		boolean noSampling = true;
		int lvlIter = 0;
		while(noSampling && lvlIter < instLIRP.getNbLevels()) {
			noSampling = (rSplit[lvlIter] < 1);
			lvlIter++;
		}
		if(noSampling) {
			RouteMap rMap = new RouteMap();
			for(int lvl = 0; lvl < instLIRP.getNbLevels(); lvl++) {
				rMap.put(lvl, rm.getAllRoutesOfLvl(lvl));
			}
			long startChrono = System.currentTimeMillis();

			Solver solverLIRP = new Solver(instLIRP, rMap, null, Config.NOSPLIT_TILIM);
			Solution NoSplitSol = solverLIRP.getSolution(false, Config.EPSILON);

			long stopChrono = System.currentTimeMillis();

			NoSplitSol.setSolvingTime(stopChrono - startChrono);
			return NoSplitSol;
		}
		else {
			return RSHSol(instLIRP, rm, withLoops, rSplit, lm, presolve);
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
	private static Solution RSHSol(Instance instLIRP, RouteManager rm, boolean[] withLoops, int[] rSplit, LocManager lm, boolean presolve) throws IloException {
		/*
		 * ======================================================= 
		 * Create two HashMaps of Routes objects : the first one 
		 * contains the direct routes for each level, the second 
		 * one contains the loop routes for each level
		 * ======================================================= 
		 */
		RouteMap setOfDirect = rm.getAllDirects();
		RouteMap setOfRoutes = rm.getAllRoutes(withLoops);
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
		/* Total time spent on partial solutions */
		double totalTime = 0;

		long startChrono = System.currentTimeMillis();
		RouteMap rMap = preProcess(instLIRP, setOfDirect, setOfRoutes, lm, presolve);
		totalTime += System.currentTimeMillis() - startChrono;

		/* Create dumb solutions to store the intermediate results */
		Solution currentSol = getSampleSol(instLIRP, setOfDirect, rMap, subsetSizes, timeLimit);
		Solution bestSol = new Solution();

		/* Total time spent on partial solutions */
		totalTime += currentSol.getSolvingTime();

		/* As long as we have some time available, re-apply the algorithm to find other solutions */
		while(bestSol.getObjVal() < 0 || (totalTime < timeLimit && currentSol != null && currentSol.getObjVal() < bestSol.getObjVal())) {
			bestSol = currentSol;
			if(timeLimit - totalTime < Config.MAIN_TILIM) {
				currentSol = null;
			}
			else {
				/* Get a new current solution using a new sampling of the loop routes */
				currentSol = getSampleSol(instLIRP, setOfDirect, rMap, subsetSizes, timeLimit - totalTime);
			}
			if(currentSol != null)
				totalTime += currentSol.getSolvingTime();
			System.out.println("Total Time elapsed : " + totalTime);
		}
		return bestSol;
	}

	/**
	 * 
	 * @param instLIRP	The instance of the LIRP problem to solve
	 * @param directMap	The RouteMap object containing all the direct routes
	 * @param rMap		The RouteMap object containing all the multi-stops routes
	 * @param lm		The LocManager object assigning locations to DCs of the upper level
	 * @param presolve	Boolean indicating if the pre-processing solves the linear relaxation to further shrink the set of routes available
	 * @return			A RouteMap object containing the multi-stops routes to use throughout the resolution
	 */
	private static RouteMap preProcess(Instance instLIRP, RouteMap directMap, RouteMap rMap, LocManager lm, boolean presolve) {
		try {
			RouteMap filteredRoutes = rMap.filterRoutes(lm);
			/* If the presolve option is activated, solve the problem without sampling first and to extract the routes used and 
			 * reduce the pool of routes from which to sample from 
			 */
			if(presolve)
				/* If we solve a relaxation of the problem, collect the multi-stops routes used in the relaxed solutions */
				return collectMSRoutes(instLIRP, rMap, filteredRoutes, Config.NOSPLIT_TILIM, Config.PRESOLVE_TILIM, presolve);
			else
				return filteredRoutes;
		}
		catch (IOException ioe) {
			System.out.println("ERR: Problem while filtering the routes");
			System.out.println(ioe.getMessage());
			System.exit(1);
		}

		return rMap;
	}

	/**
	 * Compute a solution to the original LIRP instance using the a specified set of routes
	 * @param instLIRP		The instance of the LIRP problem considered
	 * @param directMap		The RouteMap object containing the direct routes
	 * @param rMap			The RouteMap object containing the multi-stops routes
	 * @param subsetSizes	The samples size in each level
	 * @param remainingTime	The time remaining in the allowed resolution time
	 * @return				A Solution object to the LIRP
	 */
	private static Solution getSampleSol(Instance instLIRP, RouteMap directMap, RouteMap rMap, int[] subsetSizes, double remainingTime){
		double totalTime = 0;

		/* Create a HashMap to store the subsets of routes at each level */
		SampleMap lvlSamples = new SampleMap(rMap, subsetSizes);
		int nbSubsets = 1;
		int previousNbRoutes = 0;
		/* The total number of subset is the number of possible combinations between all the subset of routes */
		for(int lvl = 0; lvl < instLIRP.getNbLevels(); lvl++) {
			previousNbRoutes += rMap.get(lvl).size();
			nbSubsets *= lvlSamples.get(lvl).size();
		}

		lvlSamples.completeSamples(directMap);

		/* Get all the possible combinations of route samples from each level */
		HashSet<RouteMap> setOfMapRoutes = lvlSamples.getCombinations();

		/* Routes object that are candidate for the final solution */
		RouteMap selectedRoutes = new RouteMap();
		for(int lvl: directMap.keySet()) {
			selectedRoutes.put(lvl, new RouteSet());
			selectedRoutes.get(lvl).addAll(directMap.get(lvl));
		}
		/* If the number of subsets is 1, add all the multi-stops routes to the set of available route objects to compute the final solution */
		if(nbSubsets == 1) {
			for(int lvl: selectedRoutes.keySet())
				selectedRoutes.get(lvl).addAll(rMap.get(lvl));
		}

		int nbIterations = 0;
		int nbRoutes = 0;

		while(nbSubsets > 1 && nbRoutes < previousNbRoutes && totalTime + Config.MAIN_TILIM < remainingTime) {
			/* Set of all loops used in at least one of the intermediate solutions */
			RouteMap collectedRoutes = new  RouteMap();
			nbIterations++;
			System.out.println("========================================");
			System.out.println("== Iteration " + nbIterations + ": Solving with " + setOfMapRoutes.size() + " subsets of routes ==");
			System.out.println("========================================");
			double partialTiLim = Math.max((remainingTime - Config.MAIN_TILIM) / (1.5 * Config.RECOMPUTE * setOfMapRoutes.size()), Config.AUX_TILIM);
			for(RouteMap availRoutes : setOfMapRoutes) {
				long startChrono = System.currentTimeMillis();
				RouteMap eliteRoutes = collectMSRoutes(instLIRP, directMap, availRoutes, remainingTime - totalTime, partialTiLim, false);
				for(int lvl : eliteRoutes.keySet()) {
					if(collectedRoutes.get(lvl) != null) {
						collectedRoutes.get(lvl).addAll(eliteRoutes.get(lvl));
					}
					else {
						collectedRoutes.put(lvl, new RouteSet());
						collectedRoutes.get(lvl).addAll(eliteRoutes.get(lvl));
					}
				}
				long stopChrono = System.currentTimeMillis();
				totalTime += stopChrono - startChrono;
			}

			/* Re-sample from the set of collected loops */
			lvlSamples.reSample(collectedRoutes, subsetSizes);
			/* Complete the subsets to reach all locations */
			lvlSamples.completeSamples(directMap);
			/* Get all the possible combinations of route samples from each level */
			setOfMapRoutes = lvlSamples.getCombinations();
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
				Solution sampleSol = solverLIRP.getSolution(false, Config.EPSILON);

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

		return null;
	}

	/**
	 * Returns the routes of each level used in a solution for the LIRP 
	 * @param instLIRP		The instance of LIRP to solve
	 * @param directMap		The RouteMap object containing the direct routes in each level
	 * @param availLoopsMap	The RouteMap object containing all the routes available in each level
	 * @param timeLeft		The time left for solving the problem
	 * @param partialTiLim	The time left for solving the partial problem
	 * @return				A RouteMap object containing the multi-stops routes routes used in each level
	 */
	private static RouteMap collectMSRoutes(Instance instLIRP, RouteMap directMap, RouteMap availLoopsMap, double timeLeft, double partialTiLim, boolean presolve){
		RouteMap allUsedRoutes = new RouteMap();
		/* Create a map of available routes after filtering the routes in mapLoops */
		int computeIter = 0;
		int nbComput = Config.RECOMPUTE;
		/* If the objective is to collect a first set of routes from the linear relaxation, set the time limit 
		 * for each resolution based on the number of presolve */
		if(presolve) {
			nbComput = Config.PRESOLVE_NB;
			partialTiLim *= 1.0/nbComput; 
		}

		double updatedTimeLeft = timeLeft;
		/* Compute solutions sequentially by removing used routes from one iteration to the next */
		while (computeIter < nbComput) {
			Solution partialSol = null;
			/* If the remaining time is enough to compute a solution to a subproblem, 
			 * create a problem with a subset of available loops */
			if(Config.MAIN_TILIM < updatedTimeLeft) {
				/* Complete the map of multi-stops routes with direct routes for unreachable clients */
				availLoopsMap.completeMap(directMap);
				partialSol = getPartialSol(instLIRP, availLoopsMap, partialTiLim, presolve);
				updatedTimeLeft -= partialSol.getSolvingTime();
			}
			/* If we have computed a partial solution, add the multi-stops routes used in this solution to the collected ones */
			if (partialSol != null) {
				RouteMap usedLoops = partialSol.collectUsedLoops();
				for(int lvl : availLoopsMap.keySet()) {
					if(allUsedRoutes.get(lvl) != null) {
						allUsedRoutes.get(lvl).addAll(usedLoops.get(lvl));
					}
					else {
						allUsedRoutes.put(lvl, new RouteSet());
						allUsedRoutes.get(lvl).addAll(usedLoops.get(lvl));
					}

					/* Remove the collected multi-stops routes from the available multi-stops routes for the next iteration */
					availLoopsMap.get(lvl).removeAll(usedLoops.get(lvl));
				}
				//availLoopsMap.completeMap(directMap);
			}
			/* If there is no partial solution because of a lack of time,
			 * add all the remaining loops to the collected ones
			 */
			else {
				for(int lvl: availLoopsMap.keySet()) {
					if(allUsedRoutes.get(lvl) != null) {
						allUsedRoutes.get(lvl).addAll(availLoopsMap.get(lvl));
					}
					else {
						allUsedRoutes.put(lvl, new RouteSet());
						allUsedRoutes.get(lvl).addAll(availLoopsMap.get(lvl));
					}
				}
			}
			computeIter++;
		}

		return allUsedRoutes;
	}

	/**
	 * Compute a partial solution based on a set of available routes
	 * @param instLIRP		The instance of the LIRP problem considered
	 * @param routesLvls	The set of available routes for each level of the network
	 * @return				A pair containing the solving time and the set of routes used in the solution (from the set of available ones)
	 */
	private static Solution getPartialSol(Instance instLIRP, RouteMap routesLvls, double timeLim, boolean presolve){

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
}
