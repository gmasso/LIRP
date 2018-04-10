package solverLIRP;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import ilog.concert.IloException;
import instanceManager.Instance;
import instanceManager.Location;
import tools.Parameters;

public final class Matheuristics {

	private Matheuristics() {}

	public static Solution computeSolution(Instance inst, RouteManager rm, int[] rSplit, LocManager lm, PrintStream printStreamSol)
			throws IloException {

		boolean noSampling = true;
		int lvlIter = 0;
		while(noSampling && lvlIter < Parameters.nb_levels) {
			noSampling = (rSplit[lvlIter] < 1);
			lvlIter++;
		}
		if(noSampling) {
			return routeSamplingSol(inst, rm, rSplit, printStreamSol);
		}

		HashMap<Integer, LinkedHashSet<Route>> mapRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			mapRoutes.put(lvl, new LinkedHashSet<Route>(rm.getAllRoutesOfType(lvl, 1)));
		}
		
		Solver solverLIRP = new Solver(inst, mapRoutes, null, true);
		return solverLIRP.getSolution(printStreamSol);
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
	private static Solution routeSamplingSol(Instance inst, RouteManager rm, int[] rSplit, PrintStream printStreamSol) throws IloException {

		/*
		 * ======================================================= 
		 * Apply the route sampling algorithm: If the split 
		 * parameter is big enough, the set of routes will not be 
		 * split 
		 * =======================================================
		 */
		HashMap<Integer, LinkedHashSet<Route>> setOfRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
		int[] subsetSizes = new int[Parameters.nb_levels];
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
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
			for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
				setOfRoutes.get(lvl).removeAll(routesCandidates.get(lvl));
				HashSet<LinkedHashSet<Route>> rSample = sampleRoutes(setOfRoutes.get(lvl), subsetSizes[lvl]);
				nbSubsets *= rSample.size();
				mapSample.put(lvl, rSample);
			}
			
			System.out.println("========================================");
			System.out.println("== Solving with 	" + mapSample.values().size() + " subsets of routes ==");
			System.out.println("========================================");

			while (nbSubsets > 1) {
				HashMap<Integer, LinkedHashSet<Route>> collectedRoutes = new HashMap<Integer, LinkedHashSet<Route>>();
				for (HashMap<Integer, LinkedHashSet<Route>> mapRoutes : getCombinations(mapSample)) {
					int computeIter = 0;
					while (computeIter < Parameters.recompute) {
						Solver solverLIRP = new Solver(inst, mapRoutes, null, false);
						Solution partialSol = solverLIRP.getSolution(printStreamSol);
						if (partialSol != null) {
							HashMap<Integer, LinkedHashSet<Route>> usedRoutes = partialSol.collectUsedRoutes();
							for(int lvl: mapRoutes.keySet()) {
								if(collectedRoutes.get(lvl) != null)
									collectedRoutes.get(lvl).addAll(usedRoutes.get(lvl));
								else
									collectedRoutes.put(lvl, usedRoutes.get(lvl));
								mapRoutes.get(lvl).removeAll(collectedRoutes.get(lvl));
							}
							computeIter++;
						}
					}
				}
				nbSubsets = 1;
				for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
					setOfRoutes.get(lvl).removeAll(routesCandidates.get(lvl));
					HashSet<LinkedHashSet<Route>> rSample = sampleRoutes(setOfRoutes.get(lvl), subsetSizes[lvl]);
					nbSubsets *= rSample.size();
					mapSample.put(lvl, rSample);
				}
			}
			
			for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
				for(LinkedHashSet<Route> solRoutes: mapSample.get(lvl))
					routesCandidates.get(lvl).addAll(solRoutes);
			}
			Solver solverLIRP = new Solver(inst, routesCandidates, null, true);
			System.out.println("done!");

			// Call the method from the solver
			currentSol = solverLIRP.getSolution(printStreamSol);
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
	public LinkedHashSet<Route> filterRoutes(LocManager lm, int lvl, LinkedHashSet<Route> setOfRoutes) throws IOException {
		/* Select a random number of depots among the depots available */
		int maxNbDC = (int) Math.floor(0.6 * (lm.getInstance().getNbLocations(lvl)));
		HashSet<Location> dSelect = lm.depotSelect(Parameters.rand.nextInt(maxNbDC) + 1);
		LinkedHashSet<Route> rFilter = new LinkedHashSet<Route>();
		
		Iterator<Route> rIter = setOfRoutes.iterator();
		while(rIter.hasNext()) {
			Route currentRoute = rIter.next();
			Location rStart = currentRoute.getStart();
			if((rStart == lm.getInstance().getDummy()) || (dSelect.contains(rStart))) {
				rFilter.add(currentRoute);
			}
		}
		return rFilter;
	}
	
	//	/**
	//	 * 
	//	 * @param Allocation  allocation matrix
	//	 * @return list of selected depot indices
	//	 * @throws IOException
	//	 */
	//	private LinkedHashSet<Integer> getListOfSelectedDepots(int[][] Allocation)throws IOException 
	//	{
	//		LinkedHashSet<Integer> S = new LinkedHashSet<Integer>();
	//		int nd = this.instLIRP.getNbDepots(0); 
	//		int nc = this.instLIRP.getNbClients(); 
	//		
	//		// If depot d has at least one client allocated to it, we consider it is selected, otherwise not. 
	//		for (int d=0; d<nd;d++){
	//			for (int c=0; c<nc;c++) {
	//				if (Allocation[c][d] ==1)
	//					S.add(d);
	//				c=nc; // if at least one client is allocated to depot d, depot d exists and we can check next depot
	//			}
	//		}
	//		return S;
	//	}
	//	
	//	/**
	//	 * @param : d : depot index
	//	 * @param Allocation  allocation matrix
	//	 * @return list of clients allocated to d
	//	 * @throws IOException
	//	 * **/
	//	 
	//		private LinkedHashSet<Integer> getListOfAllocatedClients(int d, int[][] Allocation)throws IOException 
	//		{
	//			LinkedHashSet<Integer> AAA = new LinkedHashSet<Integer>();
	//			int nc = this.instLIRP.getNbClients(); 
	//			
	//			for (int c=0; c<nc;c++) {
	//					if (Allocation[c][d] == 1) AAA.add(c);
	//			}
	//			return AAA;
	//		}
	//	 
	//	
	//	
	//	
	//	/**
	//	 * 
	//	 * @param loopSD        routes from the supplier to depots
	//	 * @param Allocation    matrix with customer allocation. Depot with 0 client = not selected
	//	 * @return list of SD routes: SD routes starting from an unselected depot are filtered
	//	 * @throws IOException
	//	 */
	//
	//	// GUILLAUME
	//	//private HashMap<Integer, HashMap<Integer, LinkedHashSet<Route>>> filterSDRoutes(LinkedHashSet<Route> loopSD, int[][] Allocation) throws IOException {
	//
	//	// OLIVIER
	//	LinkedHashSet<Route> filterSDRoutes(LinkedHashSet<Route> loopSD, int[][] Allocation) throws IOException {
	//		
	//		LinkedHashSet<Route> filteredSD = new LinkedHashSet<Route>();
	//		LinkedHashSet<Integer> S =  getListOfSelectedDepots(Allocation);
	//	
	//		for (int d=0; d<S.size();d++){
	//			Route r = new Route(this.instLIRP, -1, S.get(d)); // create a new SD route r from s to d
	//			filteredSD.add(r);
	//		}
	//		return filteredSD;
	//	}
	//
	//
	//	/**
	//	 * 
	//	 * @param loopDC        routes from depots to clients
	//	 * @param Allocation    matrix with customer allocation. 
	//	 * @return filtered list of DC routes. 
	//	 * DC route starting from an unselected depot are filtered
	//	 * @throws IOException
	//	 */
	//
	//		// OLIVIER
	//	private LinkedHashSet<Route> filterDCRoutes(LinkedHashSet<Route> loopDC, int[][] Allocation) throws IOException {
	//
	//		int keep; // indicates if a route must be kept or not in the filtered list
	//		LinkedHashSet<Route> filteredDC = new LinkedHashSet<Route>();
	//		LinkedHashSet<Integer> S =  getListOfSelectedDepots(Allocation);
	//		
	//		for (int itr=0; itr<loopDC.size();itr++) {
	//			Route r = loopDC.get(itr);
	//			instanceManager.Location rdep = r.getStart();
	//			// Ici ca risque de ne pas fonctionner car rdep est de type Location et S continet des entiers (a v�rifier)
	//			if (S.contains(rdep)){
	//					filteredDC.add(r);
	//			}
	//		}
	//		return filteredDC;
	//	}
	//	
	//	
	//	
	//	
	//	/**
	//	 * 
	//	 * @param loopDC        routes from depots to clients
	//	 * @param Allocation    matrix with customer allocation. 
	//	 * @return filtered list of DC routes. 
	//	 * DC Routes where all clients not preAllocated to d are filtered
	//	 * @throws IOException
	//	 */
	//
	//	// GUILLAUME
	//	//private LinkedHashSet<Route> filterRoutes(LinkedHashSet<Route> loopDC, int[][] Allocation) throws IOException {
	//
	//	// OLIVIER
	//	private LinkedHashSet<Route> filterRoutes(LinkedHashSet<Route> loopDC, int[][] Allocation) throws IOException {
	//
	//		int keep; // indicates if a route must be kept or not in the filtered list
	//		LinkedHashSet<Integer> S =  getListOfSelectedDepots(Allocation);
	//		LinkedHashSet<Route> filtered = new LinkedHashSet<Route>();
	//
	//		for (int itr=0; itr<loopDC.size();itr++) {
	//			keep=1;
	//			Route r = loopDC.get(itr);
	//			LinkedHashSet<Integer> AAA = getListOfAllocatedClients(itr, Allocation);
	//			if (r.containsAll(AAA)) { // all clients are allocated to the depot of route r
	//				filtered.add(r);
	//			}
	//		}
	//		return filtered;
	//	}



	//			/* Build additional routes from every existing shorter route */
	//			for(Route startRouteLvl0 : this.routes.get(0).get(nbStops - 1)) {
	//				for(int stopIter = 0; stopIter < stopCandidates.size(); stopIter++) {
	//					/* Create a new Route object by adding one stop among the candidates to currentRoute */
	//					Route routeCandidate = currentRoute.extend(stopCandidates.get(stopIter));
	//					/* If it is valid, add it to the set of routes to add and call recursively */
	//					if(routeCandidate.isValid()) {
	//			}
	//			/* Fill a list with depots candidates for insertion in loops */
	//			LinkedHashSet<Integer> stopDCandidates = new LinkedHashSet<Integer>();
	//			for(int dIter = 0; dIter < this.instLIRP.getNbDepots(0); dIter++) {
	//				stopDCandidates.add(dIter);
	//			}
	//			/* Create loops with the different possible combinations of stops */
	//			for(int dIndex = 0; dIndex < stopDCandidates.size() - 1; dIndex++) {
	//				Route initSDRoute = new Route(this.instLIRP, -1, dIndex);
	//				loopsLvl0.addAll(computeAllRoutes(initSDRoute, new LinkedHashSet<Integer>(stopDCandidates.subList(dIndex + 1, stopDCandidates.size())), maxNbStops - 1));
	//			}
	//
	//			/* Fill a list with depots candidates for insertion in loops */
	//			LinkedHashSet<Integer> stopCCandidates = new LinkedHashSet<Integer>();
	//			for(int cIter = 0; cIter < this.instLIRP.getNbClients(); cIter++) {
	//				stopCCandidates.add(cIter);
	//			}
	//			/* Create loops starting from each depot */
	//			for(int dIter = 0; dIter < this.instLIRP.getNbDepots(0); dIter++) {
	//				/* Routes from the depot dIter are useful only if the depot is reachable from the supplier */
	//				/* Create loops with the different possible combinations of stops */
	//				for(int cIndex = 0; cIndex < stopCCandidates.size() - 1; cIndex++) {
	//					Route initDCRoute = new Route(this.instLIRP, dIter, cIndex);
	//					loopsLvl1.addAll(computeAllRoutes(initDCRoute, new LinkedHashSet<Integer>(stopCCandidates.subList(cIndex + 1, stopCCandidates.size())), maxNbStops - 1));
	//				}
	//			}
	//		}
	//	}

	//
	//	/**
	//	 * 
	//	 * @param currentRoute		the current Route object that is considered as a basis to build new ones
	//	 * @param stopCandidates		the stops that can be added to currentRoute
	//	 * @param nbRemainingStops	the maximum number of stops that can be added to the currentRoute
	//	 * @return
	//	 * @throws IOException
	//	 */
	//	private LinkedHashSet<Route> computeAllRoutes(Route currentRoute, LinkedHashSet<Integer> stopCandidates, int nbRemainingStops) throws IOException {
	//		LinkedHashSet<Route> routesToAdd = new LinkedHashSet<Route>();
	//		/* If some stop candidates remain to extend the route, try to add them to the route */
	//		if(nbRemainingStops > 0 && !stopCandidates.isEmpty()) {
	//			for(int stopIter = 0; stopIter < stopCandidates.size(); stopIter++) {
	//				/* Create a new Route object by adding one stop among the candidates to currentRoute */
	//				Route routeCandidate = currentRoute.extend(stopCandidates.get(stopIter));
	//				/* If it is valid, add it to the set of routes to add and call recursively */
	//				if(routeCandidate.isValid()) {
	//					routesToAdd.add(routeCandidate);
	//					/* If the stop currently added is not the last of the list, call recursively with the remaining candidates */
	//					if(stopIter < stopCandidates.size() - 1) {
	//						LinkedHashSet<Integer> newStopCandidates = new LinkedHashSet<Integer>(stopCandidates.subList(stopIter + 1, stopCandidates.size()));
	//						routesToAdd.addAll(computeAllRoutes(routeCandidate, newStopCandidates, nbRemainingStops - 1));
	//					}
	//				}
	//			}
	//		}
	//		return routesToAdd;
	//	}

	
	//TODO
	/**
	 * 
	 * @param setOfMaps
	 * @return
	 */
	private static HashSet<HashMap<Integer, LinkedHashSet<Route>>> getCombinations(HashMap<Integer, HashSet<LinkedHashSet<Route>>> setOfMaps) {
		HashSet<HashMap<Integer, LinkedHashSet<Route>>> rCombos = new HashSet<HashMap<Integer, LinkedHashSet<Route>>>();
		
		return rCombos;
	}
	
	
}
