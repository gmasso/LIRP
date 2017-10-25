import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import ilog.concert.IloException;
public class Heuristic {
	private int _alpha = 300; // Max number of routes with more than one client
	// mindouble returns the minimum value among several (double) values
	public static double mindouble(double ... numbers) {
		double min = numbers[0];
		for (int i=1 ; i<numbers.length ; i++) {
			min = (min <= numbers[i]) ? min : numbers[i];
		}
		return min;
	}
	public Heuristic (Instance instance, Routes myRoutes, PrintStream printStreamsol) throws IOException, IloException{

		Routes finalRoutes = defineSetOfRoutes(instance, myRoutes, printStreamsol);

		// Fill the list with all the routes of length 1
		for(int routeIndex = 0; routeIndex < myRoutes.getNbRoutes(); routeIndex++){
			double[] currentRoute = myRoutes.getRoute(routeIndex);
			// If the route is direct, add it to the set of routes
			if(currentRoute[3] == -1)
				finalRoutes.addRoute(currentRoute);
		}

		// Solve the instance with the final set of routes
		System.out.println();
		System.out.println("----------SOLVING FINAL SOLUTION--------------");
		try {
		Solution finalSolution = Solver.solve(instance, finalRoutes, printStreamsol);
		finalSolution.print(finalRoutes, printStreamsol);
		}
		catch (IloException e) {
			System.out.println("Could not solve the problem after selecting promising routes");
			System.out.println("Error:" + e.toString());
		}

	}

	// Returns the final set of routes to be used to compute the solution of the problem
	private Routes defineSetOfRoutes(Instance instance, Routes setOfRoutes, PrintStream printStreamsol) throws IOException, IloException{

		// Create the list of indices of the direct routes length 1)
		ArrayList<Integer> directRoutes = new ArrayList<Integer>();
		// Create the list of indices of the routes of length more than 1
		ArrayList<Integer> multiRoutes = new ArrayList<Integer>();
		// Fill the lists with the corresponding routes in each case
		for(int route_index = 0; route_index < setOfRoutes.getNbRoutes(); route_index++){
			double[] current_route = setOfRoutes.getRoute(route_index);
			// Check that the route is at least of length two (check if there is a 2nd client)
			if(current_route[3] > -1)
				multiRoutes.add(route_index);
			// Otherwise add the current route to the list of direct routes
			else
				directRoutes.add(route_index);
		}
		// Shuffle both sets of routes
		Collections.shuffle(multiRoutes);

		// At some point, we should remove some of the direct routes to allow for more multiroutes
		if(setOfRoutes.getNbRoutes() <= _alpha + directRoutes.size())
			return setOfRoutes;

		Routes promisingRoutes = new Routes(instance, setOfRoutes.RMAX, setOfRoutes.CMAX, setOfRoutes.LMAX);
		// Add all the direct routes to the promising routes, making sure every client will be reachable on any subsequent call of the solver with promising routes
		Iterator<Integer> dIter = directRoutes.iterator();
		while(dIter.hasNext()) {
			promisingRoutes.addRoute(setOfRoutes.getRoute(dIter.next()));
		}

		int subsetCounter = 0;
		while(multiRoutes.size() > 0) {
			subsetCounter++;
			Routes currentSubset = new Routes(instance, setOfRoutes.RMAX, setOfRoutes.CMAX, setOfRoutes.LMAX);

			// Select the first _alpha elements of multiRoutes, add them to the current subset and remove them from multiRoutes
			int subsetIter = 0;
			while (subsetIter < this._alpha && !multiRoutes.isEmpty()) {
				currentSubset.addRoute(setOfRoutes.getRoute(multiRoutes.remove(0)));
				System.out.println(multiRoutes);
				subsetIter++;
				
			}
			System.out.println("Choosen SubsetofRoutes");
			// Add direct routes to the current subset so that all clients are reachable from every possible depot by at least one existing route
			Iterator<Integer> directIter = directRoutes.iterator();
			while(directIter.hasNext()) {
				int routeIndex = directIter.next();
				// Extract the corresponding directRoute and the index of the client it serves
				double[] directRoute = setOfRoutes.getRoute(routeIndex);
				int depotIndex = (int) directRoute[1];
				int clientIndex = (int) directRoute[2];
				System.out.println();
				System.out.println();
				System.out.print("Checking if there exists a route between depot " + depotIndex + " and client " + clientIndex + "...");
				boolean linkNotFound = true;
				subsetIter = 0;
				// While the client is not found in any of the routes in the subset, continue searching
				while (linkNotFound && subsetIter < currentSubset.getNbRoutes()) {
					// Select the next route in the subset
					double[] routeToCheck = currentSubset.getRoute(subsetIter);
					// If the depot is the right one, check if one stop on the route corresponds to the client
					if((int) routeToCheck[1] == depotIndex) {
						int stopIter = 2;
						while(linkNotFound && stopIter < routeToCheck.length) {
							linkNotFound = ((int) routeToCheck[stopIter] != clientIndex);
							if(!linkNotFound)
								System.out.println("Yes! Link found in the subset: " + currentSubset.printRoute(subsetIter) + ".");
								
							stopIter++;
						}
					}
					subsetIter++;	
				}					
				// If no route contains the link between this depot and the client has not been found, add the direct route to the subset
				if (linkNotFound) { 
					System.out.println("No! Adding route " + setOfRoutes.printRoute(routeIndex) + " to the subset.");
					currentSubset.addRoute(directRoute);
				}
			}

			// Solve the problem with the subset of routes that we collected
			System.out.println();
			System.out.println("----------SOLVING SUBSET "+subsetCounter+"--------------");
			long startChrono = System.currentTimeMillis();
			try {
				Solution currentSolution = Solver.solve(instance, currentSubset, printStreamsol);
				currentSolution.print(currentSubset, printStreamsol);

				// Collect the routes used in the solution and add them to the promisingRoutes
				
				for(int r = 0; r < currentSubset.getNbRoutes(); r++){
					int t = 0;
					boolean isPromising = promisingRoutes.containsRoute(currentSubset.getRoute(r));
					
					while(!isPromising && t < instance.getNbPeriods()) {
						
						if(currentSolution.getListOfRoutes(r,t) == 1){
							promisingRoutes.addRoute(currentSubset.getRoute(r));
							isPromising = true;	
							promisingRoutes.printRoute(r);
						}
						t++;		
						
					}	
				}
				long stopChrono = System.currentTimeMillis();
				long compute = (stopChrono-startChrono);
				System.out.println("Computational time :"+compute + " miliseconds");
			}
			catch (IloException e) {
				System.out.println("Could not solve the problem on subset " + subsetCounter);
				System.out.println("Error:" + e.toString());

				// Add all the routes of the subset regardless of their usefulness
				for(int r = 0; r < currentSubset.getNbRoutes(); r++){
					promisingRoutes.addRoute(currentSubset.getRoute(r));
				}
			}
		}

		return defineSetOfRoutes(instance, promisingRoutes, printStreamsol);
	}
}









