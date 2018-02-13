package solverLIRP;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import instanceManager.Instance;

public final class Matheuristics {

	private Matheuristics() {}
	
	public Solution computeSolution(Instance inst, RouteManager rm, int rSubSizes) {
		if(routeSampling) {
			Solver 
			return 
		}
	}
	
	/**
	 * Solves the LIRP problem on instance inst using the route sampling method, starting from 
	 * @param inst
	 * @param rm
	 * @param loopRoutes
	 * @return
	 */
	private Solution routeSampling(Instance inst, RouteManager rm, HashSet<Integer> routesSDId, HashSet<Integer> routesDCId, int subsetSizes) {
		Solution sol;
		/* =======================================================
		 *  Apply the route sampling algorithm: If the split 
		 *  parameter is big enough, the set of routes will not be 
		 *  split
		 * =======================================================*/
		ArrayList<HashSet<Integer>> rSample = sampleRoutes(routesDCId, subsetSizes);
		System.out.println("========================================");
		System.out.println("== Solving with 	" + rSample.size() + " subsets of routes ==" );
		System.out.println("========================================");

		while(rSample.size() > 1) {
			HashSet<Integer> collectedRoutes = new HashSet<Integer>();
			for(HashSet<Integer> subsetRoutes : rSample) {
				Solver solverLIRP = new Solver(inst, rm, new HashSet<Integer> (), subsetRoutes);
				Solution partialSol = solverLIRP.getSolution(printStreamSol);
				if (partialSol != null){	
						collectedRoutes.addAll(partialSol.collectLoopDCRoutes()); //collectedIndex));
				}
			}
			rSample = sampleRoutes(collectedRoutes, subsetSizes);
		}

		routesDCId = rSample.get(0);
		Solver solverLIRP = new Solver(inst, rm, new HashSet<Integer> (), routesDCId);
		System.out.println("done!");
		
		// Call the method from the solver
		Solution sol = solverLIRP.getSolution(printStreamSol);
		return sol;
	}
	
	
	/**
	 * 
	 * @param loopIndices	the indices of routes from which we form the subsets
	 * @param subsetSizes	the desired size for the subsets produced
	 * @return				a double-entry containing the indices of the routes contained in each subset
	 */
	private static ArrayList<HashSet<Integer>> sampleRoutes(HashSet<Integer> loopIndices, int subsetSizes){	
		int nbSubsets = (int) Math.ceil(((double) loopIndices.size())/subsetSizes);
		ArrayList<HashSet<Integer>> rSubsets = new ArrayList<HashSet<Integer>>();
		
		/* If there is only one subset, return directly the set of indices taken as input */
		if(nbSubsets < 2) {
			rSubsets.add(loopIndices);
			return rSubsets;
		}

		/* Shuffle code */
		int[] permutDC = new int[nbSubsets * subsetSizes];
		/* Shuffle the array at random */
		Random rnd = ThreadLocalRandom.current();
		/* Fill an array with the indices of possible routes */
		for(int loopIndex = 0; loopIndex < loopIndices.size(); loopIndex++)
			permutDC[loopIndex] = loopIndex;
		for (int i = loopIndices.size(); i > 0; i--) {
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
		while(missingIndex > loopIndices.size()) {
			/* Select a complete subset at random */
			int subsetRand = rnd.nextInt(nbSubsets - 1);
			/* Select the index of the element to copy from this subset at random */
			int swapIndex = rnd.nextInt(rangeSelect);
			
			/* Select an element to copy in the subset subsetRand at position swapIndex */
			int copyID = permutDC[subsetRand * subsetSizes + swapIndex];
			
			/* Swap the element to copy with the last one available in the selected subset 
			 * (this ensures it will not be selected in a subsequent iteration) */
			permutDC[subsetRand * subsetSizes + swapIndex] = permutDC[subsetRand * subsetSizes + rangeSelect - 1];
			permutDC[subsetRand * subsetSizes + rangeSelect - 1] = copyID;
			
			/* Add the item to copy t of the missing indices */
			permutDC[missingIndex - 1] = copyID;
			
			/* Increment the iterators */
			rangeSelect--;
			missingIndex--;
		}

		/* Copy the permutation to the result array */
		for(int subsetIndex = 0; subsetIndex < nbSubsets; subsetIndex++) {
			HashSet<Integer> currentSubset = new HashSet<Integer>();
			for(int idIter = 0; idIter < subsetSizes; idIter++) {
				currentSubset.add(permutDC[subsetIndex * subsetSizes + idIter]);
			}
			rSubsets.add(currentSubset);
		}

		return rSubsets;
	}
}
