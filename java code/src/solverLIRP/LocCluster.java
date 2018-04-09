package solverLIRP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import instanceManager.ClientsMap;
import instanceManager.Depot;
import instanceManager.DepotsMap;
import instanceManager.Instance;
import instanceManager.Location;
import tools.Pair;
import tools.Parameters;
import tools.SortedLocSet;


public class LocCluster {

	private static int k = 3; 										//(parameter) number of closest depots considered
	private static double mu1 = 0.4 * Parameters.max_time_route;	//(parameter) max distance between client and depot for automatic allocation 
	private static double mu2 = 0.1 * Parameters.max_time_route;	//(parameter) max distance between clients in farther allocation
	private static double mu3 = Parameters.max_time_route;    		//(parameter) max route distance for farther allocation

	/*======================
	 *      ATTRIBUTES
	 =======================*/
	private Instance instLIRP;
	private HashMap<Location, HashSet<Location>> alloc;

	/*======================
	 *      CONSTRUCTOR 
	 =======================*/
	/**
	 * Constructor of the PreAllocation Matrix 
	 * @param number of client, number of depots
	 * @throws IOException
	 */
	public LocCluster(Instance instLIRP) throws IOException {
		this.instLIRP = instLIRP;
		this.alloc = new HashMap<Location, HashSet<Location>>();
	}

	/**
	 * Compute a pre-allocation of all locations of the instance to some locations of the upper level
	 */
	public void preAlloc() { 
		/* First allocation : to the k closest depots of the upper level */
		this.directAlloc();
		/*  Second allocation : allocate each location l to depots of the upper level that are already connected to another location of the same level, close to l */
		this.indirectAlloc();
	}

	/**
	 * Allocate locations to the level dc with direct links
	 */
	private void directAlloc() {
		/* For each location, calculate the distance to all depots of the upper level */
		for(int lvl = Parameters.nb_levels - 1; lvl > 0; lvl--) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			int nbLocUp = this.instLIRP.getNbLocations(lvl - 1);
			for (int loc  = 0; loc < nbLocLvl; loc++) {
				Location currentLoc = (lvl < Parameters.nb_levels - 1) ? (Location) this.instLIRP.getDepot(lvl, loc) : (Location) this.instLIRP.getClient(loc);
				/* The TreeSet stores each pair of location and distance to a given location according to their distance to the location of interest */
				SortedLocSet bestDistToLoc = new SortedLocSet(currentLoc);

				for (int locUp = 0; locUp < nbLocUp; locUp++) {
					bestDistToLoc.add(this.instLIRP.getDepot(lvl - 1, locUp));
				}

				/* Pre-allocation of the location to the k closest depots in the upper level */
				Iterator<Location> iterUp = bestDistToLoc.iterator();
				int closest = 0;
				while(closest < k && iterUp.hasNext()) {
					/* Get the next closest depot */
					Location dcUp = iterUp.next();
					/* If the current location is already assigned to at least one depot and 
					 * the distance between the current location and the upper depot is greater than mu1, stop here */
					if (closest > 0 && currentLoc.getDistance(dcUp) > mu1) {
						closest = k;
					}
					else {
						/* If the dc is not a key in the HashMap, add it */
						if(!this.alloc.containsKey(dcUp)) {
							this.alloc.put(dcUp, new HashSet<Location>());
						}
						/* Add the current location to the possible destination of the depot */
						this.alloc.get(dcUp).add(currentLoc);
						closest++;
					}
				}
			}
		}
	}

	/**
	 * Allocate 
	 */
	private void indirectAlloc() {
		/* For each location, calculate the distance to all depots of the upper level */
		for(int lvl = Parameters.nb_levels - 1; lvl > 0; lvl--) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			for (int loc  = 0; loc < nbLocLvl; loc++) {
				Location currentLoc = (lvl < Parameters.nb_levels - 1) ? (Location) this.instLIRP.getDepot(lvl, loc) : (Location) this.instLIRP.getClient(loc);
				/* The TreeSet stores each pair of location and distance to a given location according to their distance to the location of interest */
				SortedLocSet bestDistToLoc = new SortedLocSet(currentLoc);

				/* Sort locations on the same level according to their distance to the current location considered */
				for (int locInter = 0; locInter < nbLocLvl; locInter++) {
					if(locInter != loc) {
						Location otherLoc = (lvl < Parameters.nb_levels - 1) ? (Location) this.instLIRP.getDepot(lvl, locInter) : (Location) this.instLIRP.getClient(locInter);
						bestDistToLoc.add(otherLoc);
					}
				}

				/* Pre-allocation of the location to the k closest depots in the upper level */
				Iterator<Location> iterLvl = bestDistToLoc.iterator();
				boolean tooFar = false;
				while(!tooFar && iterLvl.hasNext())
				{
					Location interLoc = iterLvl.next();
					double distLvl = currentLoc.getDistance(interLoc);
					if(distLvl > mu2) {
						tooFar = true;
					}
					else {
						for(Location dcUp : this.alloc.keySet()) {
							if(!this.alloc.get(dcUp).contains(currentLoc) && this.alloc.get(dcUp).contains(interLoc)) {
								if(distLvl + currentLoc.getDistance(dcUp) + interLoc.getDistance(dcUp) < mu3) {
									this.alloc.get(dcUp).add(currentLoc);
								}
							}
						}
					}
				}
			}
		}
	}

	// Heuristic depot selection
	// Return a matrix with clients allocation (possibly to a dummy depot)
	private HashMap<Location, HashSet<Location>> depotSelection(int p)
	{
		this.preAlloc(); 
		int beta = 3; // parameter for roulette wheel selection

		double y; 	// random value used in roulette wheel selection
		HashMap<Location, HashSet<Location>> dSelect = new HashMap<Location, HashSet<Location>>(); 
		
		for(int lvl = Parameters.nb_levels - 1; lvl > 0; lvl--) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			int nbLocUp = this.instLIRP.getNbDepots(lvl);
			
			int score;
			int s = 0; // number of depots selected
			int nbAlloc = 0; // number of clients already allocated
			ArrayList<Pair<Location, Double>> depotScore = new ArrayList<Pair<Location, Double>>(); // depots and their scores

			// calculate score for each depot
			for (int d = 0; d < this.instLIRP.getNbDepots(lvl - 1); d++) {
				Depot dc = this.instLIRP.getDepot(lvl - 1, d);
				score = this.alloc.get(dc).size();
				Pair<Location, Double> ds = new Pair<Location, Double>(dc);
				ds.setScore(score);
				depotScore.add(ds);
			}

			//A[lvl] = new int[this.instLIRP.getNbLocations(lvl)][this.instLIRP.getNbLocations(lvl - 1)]; // nd depots + 1 dummy depot

			// Main loop
			while (s < p && nbAlloc < nbLocLvl){

				// rank depots in decreasing order of scores
				depotScore.sort(new Comparator<Pair<Location, Double>>() {
					@Override
					public int compare(Pair<Location, Double> o1, Pair<Location, Double> o2) 
					{
						if (o1.getScore() > o2.getScore()) {
							return -1;
						} 
						else if (o1.getScore() < o2.getScore()) {
							return 1; 
						} 
						else return 0;
					}
				});

				// biased roulette wheel depot selection 
				y = Parameters.rand.nextDouble();
				int position = 0; 
				while (position < Math.pow(y, beta) * nbLocUp) { position = position +1;} // generate biased random position
				Location dnew = depotScore.get(position).getL(); // select depot at the generated random position
				s = s + 1;

				// Update allocations and preAllocations
				for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
					Location currentLoc = (lvl < Parameters.nb_levels - 1) ? this.instLIRP.getDepot(lvl, loc) : this.instLIRP.getClient(loc);
					if (this.alloc.get(dnew).contains(loc)) {
						if(!dSelect.containsKey(loc)) {
							dSelect.put(currentLoc, new HashSet<Location>());
						}
						dSelect.get(loc).add(dnew);
						
						nbAlloc++;
						for (int d = 0; d < this.instLIRP.getNbDepots(lvl - 1); d++) {
							Depot dc = this.instLIRP.getDepot(lvl - 1, d);
							this.alloc.get(dc).remove(currentLoc)
							if (dc != dnew)
							{
								if (preAlloc[c][dep]==1) {
									preAlloc[c][dep]=0;
									depotScore.get(position).decrement();   // decrease scoreof depot dep
								}
							}
						}
					}
				}
			}


			// Complete allocation with a dummy depot
			if (nbclient < nc){
				for (int c=0;c<nc;c++) {
					for (int d=0;d<nd;d++) {
						if (preAlloc[c][d] ==1) {  		// if client c is still PreAllocated to some depot 
							A[c][nd]=1; // allocate it to the dummy depot (number nd)
							d=nd;
							c=c+1;
						}
					}
				}
			}
			return A;  // return the allocation matrix (the set of selected depots can be found from this)
		}
	}






