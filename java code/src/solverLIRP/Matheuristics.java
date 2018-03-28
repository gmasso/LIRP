package solverLIRP;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import ilog.concert.IloException;
import instanceManager.Instance;
import tools.Parameters;

public final class Matheuristics {

	private Matheuristics() {}

	public static Solution computeSolution(Instance inst, RouteManager rm, int[] rSplit, boolean locSampling, PrintStream printStreamSol)
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

		HashMap<Integer, ArrayList<Route>> mapRoutes = new HashMap<Integer, ArrayList<Route>>();
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			mapRoutes.put(lvl, new ArrayList<Route>(rm.getAllRoutesOfType(lvl, 1)));
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
		HashMap<Integer, ArrayList<Route>> setOfRoutes = new HashMap<Integer, ArrayList<Route>>();
		int[] subsetSizes = new int[Parameters.nb_levels];
		for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
			ArrayList<Route> lvlRoutes = new ArrayList<Route>();
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

		HashMap<Integer, ArrayList<Route>> routesCandidates = new HashMap<Integer, ArrayList<Route>>();

		while (bestSol.getObjVal() < 0 || currentSol.getObjVal() < bestSol.getObjVal()) {
			bestSol = currentSol;
			System.out.println("Best solution so far : " + bestSol.getObjVal());

			int nbSubsets = 1;
			HashMap<Integer, HashSet<ArrayList<Route>>> mapSample= new HashMap<Integer, HashSet<ArrayList<Route>>>();
			for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
				setOfRoutes.get(lvl).removeAll(routesCandidates.get(lvl));
				HashSet<ArrayList<Route>> rSample = sampleRoutes(setOfRoutes.get(lvl), subsetSizes[lvl]);
				nbSubsets *= rSample.size();
				mapSample.put(lvl, rSample);
			}
			
			System.out.println("========================================");
			System.out.println("== Solving with 	" + mapSample.values().size() + " subsets of routes ==");
			System.out.println("========================================");

			while (nbSubsets > 1) {
				HashMap<Integer, ArrayList<Route>> collectedRoutes = new HashMap<Integer, ArrayList<Route>>();
				for (HashMap<Integer, ArrayList<Route>> mapRoutes : getCombinations(mapSample)) {
					int computeIter = 0;
					while (computeIter < Parameters.recompute) {
						Solver solverLIRP = new Solver(inst, mapRoutes, null, false);
						Solution partialSol = solverLIRP.getSolution(printStreamSol);
						if (partialSol != null) {
							HashMap<Integer, ArrayList<Route>> usedRoutes = partialSol.collectUsedRoutes();
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
					HashSet<ArrayList<Route>> rSample = sampleRoutes(setOfRoutes.get(lvl), subsetSizes[lvl]);
					nbSubsets *= rSample.size();
					mapSample.put(lvl, rSample);
				}
			}
			
			for(int lvl = 0; lvl < Parameters.nb_levels; lvl++) {
				for(ArrayList<Route> solRoutes: mapSample.get(lvl))
					routesCandidates.get(lvl).addAll(solRoutes);
			}
			Solver solverLIRP = new Solver(inst, routesCandidates, null, true);
			System.out.println("done!");

			// Call the method from the solver
			currentSol = solverLIRP.getSolution(printStreamSol);
			currentSol.setObjValue();
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
	private static HashSet<ArrayList<Route>> sampleRoutes(ArrayList<Route> setOfRoutes, int subsetSizes) {
		int nbSubsets = (int) Math.ceil(((double) setOfRoutes.size()) / subsetSizes);
		HashSet<ArrayList<Route>> rSubsets = new HashSet<ArrayList<Route>>();

		/*
		 * If there is only one subset, return directly the set of indices taken as
		 * input
		 */
		if (nbSubsets < 2) {
			rSubsets.add(new ArrayList<Route>(setOfRoutes));
			return rSubsets;
		}

		/* Shuffle code */
		int[] permutDC = new int[nbSubsets * subsetSizes];
		/* Shuffle the array at random */
		Random rnd = ThreadLocalRandom.current();
		/* Fill an array with the indices of possible routes */
		for (int loopIndex = 0; loopIndex < setOfRoutes.size(); loopIndex++)
			permutDC[loopIndex] = loopIndex;
		for (int i = setOfRoutes.size(); i > 0; i--) {
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
		while (missingIndex > setOfRoutes.size()) {
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
			ArrayList<Route> currentSubset = new ArrayList<Route>();
			for (int idIter = 0; idIter < subsetSizes; idIter++) {
				currentSubset.add(setOfRoutes.get(permutDC[subsetIndex * subsetSizes + idIter]));
			}
			rSubsets.add(currentSubset);
		}

		return rSubsets;
	}
	
	private static HashSet<HashMap<Integer, ArrayList<Route>>> getCombinations(HashMap<Integer, HashSet<ArrayList<Route>>> setOfMaps) {
		HashSet<HashMap<Integer, ArrayList<Route>>> rCombos = new HashSet<HashMap<Integer, ArrayList<Route>>>();
		
		return rCombos;
	}
}



/* OLD CODE FROM RESOLUTIONMAIN */
/*
 * ======================================================= Apply the route
 * sampling algorithm: If the split parameter is big enough, the set of routes
 * will not be split =======================================================
 */
// ArrayList<ArrayList<Integer>> rSample = sampleRoutes(loopIndicesDC,
// subsetSizes);
// System.out.println("========================================");
// System.out.println("== Solving with " + rSample.size() + " subsets of routes
// ==" );
// System.out.println("========================================");
//
// while(rSample.size() > 1) {
// ArrayList<Integer> collectedRoutes = new ArrayList<Integer>();
// for(ArrayList<Integer> subsetRoutes : rSample) {
// Solver solverLIRP = new Solver(instLIRP, rm, new ArrayList<Integer> (),
// subsetRoutes);
// Solution partialSol = solverLIRP.getSolution(printStreamSol);
// if (partialSol != null){
// for(int collectedIndex : partialSol.collectRoutes())
// collectedRoutes.add(subsetRoutes.get(collectedIndex));
// }
// }
// rSample = sampleRoutes(collectedRoutes, subsetSizes);
// }
//
// loopIndicesDC = rSample.get(0);
// Solver solverLIRP = new Solver(instLIRP, rm, new ArrayList<Integer> (),
// loopIndicesDC);
// System.out.println("done!");
//
//// Call the method from the solver
// Solution sol = solverLIRP.getSolution(printStreamSol);
//
//
/// **
// *
// * @param loopIndices the indices of routes from which we form the subsets
// * @param subsetSizes the desired size for the subsets produced
// * @return a double-entry containing the indices of the routes contained in
// each subset
// */
// private static ArrayList<ArrayList<Integer>> sampleRoutes(ArrayList<Integer>
// loopIndices, int subsetSizes){
// int nbSubsets = (int) Math.ceil(((double) loopIndices.size())/subsetSizes);
// ArrayList<ArrayList<Integer>> rSubsets = new ArrayList<ArrayList<Integer>>();
//
// /* If there is only one subset, return directly the set of indices taken as
// input */
// if(nbSubsets < 2) {
// rSubsets.add(loopIndices);
// return rSubsets;
// }
//
// /* Shuffle code */
// int[] permutDC = new int[nbSubsets * subsetSizes];
// /* Shuffle the array at random */
// Random rnd = ThreadLocalRandom.current();
// /* Fill an array with the indices of possible routes */
// for(int loopIndex = 0; loopIndex < loopIndices.size(); loopIndex++)
// permutDC[loopIndex] = loopIndex;
// for (int i = loopIndices.size(); i > 0; i--) {
// int index = rnd.nextInt(i);
// int permutVal = permutDC[index];
// permutDC[index] = permutDC[i - 1];
// permutDC[i - 1] = permutVal;
// }
//
// /* Complete the remaining indices at the end */
// /* Define a range from which we draw the additional values */
// int rangeSelect = subsetSizes;
// /* Define the first index for which we do not have a route index */
// int missingIndex = nbSubsets * subsetSizes;
// while(missingIndex > loopIndices.size()) {
// /* Select a complete subset at random */
// int subsetRand = rnd.nextInt(nbSubsets - 1);
// /* Select the index of the element to copy from this subset at random */
// int swapIndex = rnd.nextInt(rangeSelect);
//
// /* Select an element to copy in the subset subsetRand at position swapIndex
// */
// int copyID = permutDC[subsetRand * subsetSizes + swapIndex];
//
// /* Swap the element to copy with the last one available in the selected
// subset
// * (this ensures it will not be selected in a subsequent iteration) */
// permutDC[subsetRand * subsetSizes + swapIndex] = permutDC[subsetRand *
// subsetSizes + rangeSelect - 1];
// permutDC[subsetRand * subsetSizes + rangeSelect - 1] = copyID;
//
// /* Add the item to copy t of the missing indices */
// permutDC[missingIndex - 1] = copyID;
//
// /* Increment the iterators */
// rangeSelect--;
// missingIndex--;
// }
//
// /* Copy the permutation to the result array */
// for(int subsetIndex = 0; subsetIndex < nbSubsets; subsetIndex++) {
// ArrayList<Integer> currentSubset = new ArrayList<Integer>();
// for(int idIter = 0; idIter < subsetSizes; idIter++) {
// currentSubset.add(permutDC[subsetIndex * subsetSizes + idIter]);
// }
// rSubsets.add(currentSubset);
// }
//
// return rSubsets;
