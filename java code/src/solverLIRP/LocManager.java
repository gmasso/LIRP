package solverLIRP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import instanceManager.Depot;
import instanceManager.Instance;
import instanceManager.Location;
import tools.Parameters;
import tools.SortedLocSet;


public class LocManager {

	private static int k = 3; 										//(parameter) number of closest depots considered
	private static double mu1 = 0.4 * Parameters.max_time_route;	//(parameter) max distance between client and depot for automatic allocation 
	private static double mu2 = 0.1 * Parameters.max_time_route;	//(parameter) max distance between clients in farther allocation
	private static double mu3 = Parameters.max_time_route;    		//(parameter) max route distance for farther allocation
	private static int  beta = 3; 									// parameter for roulette wheel selection

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
	public LocManager(Instance instLIRP) throws IOException {
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
				while(!tooFar && iterLvl.hasNext()){
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

	/* Heuristic depot selection 
	 * Return a matrix with clients allocation (possibly to a dummy depot)
	 */
	public HashMap<Location, HashSet<Location>> depotSelection(int p) throws IOException {
		this.preAlloc(); 
		double y; 	/* random value used in roulette wheel selection */
		HashMap<Location, HashSet<Location>> dSelect = new HashMap<Location, HashSet<Location>>(this.alloc); 
		/* Add a dummy depot to the map */
		Location dummy = new Location(-1, -1);
		dSelect.put(dummy, new HashSet<Location>());

		for(int lvl = Parameters.nb_levels - 1; lvl > 0; lvl--) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			int nbLocUp = 0;
			for (int loc = 0; loc < this.instLIRP.getNbLocations(lvl); loc++) {
				Location currentLoc = (lvl < Parameters.nb_levels - 1) ? this.instLIRP.getDepot(lvl, loc) : this.instLIRP.getClient(loc);
				dSelect.get(dummy).add(currentLoc);
			}

			ArrayList<Depot> depotScore = new ArrayList<Depot>(); // depots and their scores

			/* Store each depot based on how many locations are allocated to it */
			for (int d = 0; d < this.instLIRP.getNbDepots(lvl - 1); d++) {
				Depot dc = this.instLIRP.getDepot(lvl - 1, d);
				if(!dc.isDummy() && dSelect.containsKey(dc)) {
					depotScore.add(dc);
					nbLocUp++;
				}
			}

			/* number of depots selected */
			int selectDC = 0;
			/* Set of locations already allocated at this level */
			HashSet<Location> allocated = new HashSet<Location>();

			/* Main loop */
			while (selectDC < p && allocated.size() < nbLocLvl){
				/* Rank depots in decreasing order of scores */
				depotScore.sort(new Comparator<Depot>() {
					@Override
					public int compare(Depot o1, Depot o2) 
					{
						if (dSelect.get(o1).size() > dSelect.get(o2).size()) {
							return -1;
						} 
						else if (dSelect.get(o1).size() < dSelect.get(o2).size()) {
							return 1; 
						} 
						else return 0;
					}
				});

				/* Biased roulette wheel depot selection */
				y = Parameters.rand.nextDouble();
				/* Generate biased random position */
				int position = 0; 
				/* The list of depots that can be selected is limited to: 
				 * Depots object that still have unassigned successors 
				 * (i.e. that haven't been selected yet)
				 */
				while (position < Math.pow(y, beta) * (nbLocUp - selectDC)) 
					position++; 
				/* Select Depot object at the position */
				Depot dnew = depotScore.get(position);
				selectDC++;

				allocated.addAll(dSelect.get(dnew));
				for(Location dc : dSelect.keySet()) {
					dSelect.get(dc).removeAll(allocated);
					if(dSelect.get(dc).isEmpty() && dc != dummy)
						dSelect.remove(dc);
				}
			}
		}
		return dSelect;
	}
}






