package solverLIRP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import instanceManager.Instance;
import instanceManager.Location;
import tools.Config;
import tools.Pair;


public class LocManager {

	private static int k = 3; 									//(parameter) number of closest depots considered
	private static double mu1 = 0.4 * Config.MAX_TIME_ROUTE;	//(parameter) max distance between client and depot for automatic allocation 
	private static double mu2 = 0.1 * Config.MAX_TIME_ROUTE;	//(parameter) max distance between clients in farther allocation
	private static double mu3 = Config.MAX_TIME_ROUTE;    		//(parameter) max route distance for farther allocation
	private static int  beta = 3; 								//(parameter) for roulette wheel selection

	/*======================
	 *      ATTRIBUTES
	 =======================*/
	private Instance instLIRP;
	private HashMap<Location, HashSet<Location>> alloc;

	/*======================
	 *      CONSTRUCTOR 
	 =======================*/
	/**
	 * Constructor of a LocManager Object from an instance, with an initial empty allocation
	 * @param instLIRP		The instance considered
	 * @throws IOException
	 */
	public LocManager(Instance instLIRP) throws IOException {
		this.instLIRP = instLIRP;
		this.alloc = new HashMap<Location, HashSet<Location>>();
	}

	/**
	 * Compute a pre-allocation of all locations of the instance to some locations of the upper level
	 */
	public void init() { 
		/* First allocation : to the k closest depots of the upper level */
		this.directAlloc();
		/*  Second allocation : allocate each location l to depots of the upper level that are already connected to another location of the same level, close to l */
		this.indirectAlloc();
	}

	/**
	 * 
	 * @return	The instance used for this LocManager object
	 */
	public Instance getInstance() {
		return this.instLIRP;
	}

	/**
	 * Allocate locations to the level dc with direct links
	 */
	private void directAlloc() {
		/* For each location, calculate the distance to all depots of the upper level */
		for(int lvl = this.instLIRP.getNbLevels() - 1; lvl > 0; lvl--) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			int nbLocUp = this.instLIRP.getNbLocations(lvl - 1);
			for (int loc  = 0; loc < nbLocLvl; loc++) {
				Location currentLoc = (lvl < this.instLIRP.getNbLevels() - 1) ? (Location) this.instLIRP.getDepot(lvl, loc) : (Location) this.instLIRP.getClient(loc);
				/* The TreeSet stores each pair of location and distance to a given location according to their distance to the location of interest */
				TreeSet<Location> bestDistToLoc = initTreeSetLoc(currentLoc);

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
					 * the distance between the current location and the upper depot is greater than mu1, 
					 * stop here (because all other depots will be further */
					if (closest > 0 && currentLoc.getDistance(dcUp) / Config.AVG_SPEED > mu1) {
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
	 * Allocate locations to the upper level dcs by considering routes going through other locations of the same level
	 */
	private void indirectAlloc() {
		/* For each location, calculate the distance to all locations of the same level */
		for(int lvl = this.instLIRP.getNbLevels() - 1; lvl > 0; lvl--) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);
			for (int loc  = 0; loc < nbLocLvl; loc++) {
				Location currentLoc = (lvl < this.instLIRP.getNbLevels() - 1) ? (Location) this.instLIRP.getDepot(lvl, loc) : (Location) this.instLIRP.getClient(loc);
				/* The TreeSet stores each pair of location and distance to a given location according to their distance to the location of interest */
				TreeSet<Location> bestDistToLoc = initTreeSetLoc(currentLoc);

				/* Sort locations on the same level according to their distance to the current location considered */
				for (int locInter = 0; locInter < nbLocLvl; locInter++) {
					if(locInter != loc) {
						Location otherLoc = (lvl < this.instLIRP.getNbLevels() - 1) ? (Location) this.instLIRP.getDepot(lvl, locInter) : (Location) this.instLIRP.getClient(locInter);
						bestDistToLoc.add(otherLoc);
					}
				}

				/* If another location of the same level is not too far, allocate it to the dc of currentLoc (unless it is already allocated to this dc) */
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

	/**
	 * 
	 * @param p	Number of depots to select
	 * @return	A HashMap linking a subset of the depots to close locations of the next layer that are affected to them
	 * @throws IOException
	 */
	public HashMap<Location, HashSet<Location>> assignLocations(int p) throws IOException {
		if(this.alloc.isEmpty()) {
			this.init();
		}

		/* A HashMap allocating locations of the subsequent level to depots, initialized with the first allocation */
		HashMap<Location, HashSet<Location>> locAlloc = new HashMap<Location, HashSet<Location>>(); 
		/* Add the supplier as a dummy depot for unaffected clients at the end */
		locAlloc.put(this.instLIRP.getSupplier(), new HashSet<Location>());

		for(int lvl = 1; lvl < this.instLIRP.getNbLevels(); lvl++) {
			int nbLocLvl = this.instLIRP.getNbLocations(lvl);

			/* HashMap that allows to dynamically keep track of the unallocated clients wrt to the remaining unselected depots */ 
			HashMap<Location, HashSet<Location>> notAlloc = new HashMap<Location, HashSet<Location>>(this.alloc);
			ArrayList<Pair<Location, Integer>> sortedDC = new ArrayList<Pair<Location, Integer>>();
			for(HashMap.Entry<Location, HashSet<Location>> dynEntry : notAlloc.entrySet()) {
				sortedDC.add(new Pair<Location, Integer>(dynEntry.getKey(), dynEntry.getValue().size()));
			}
			/* Sort the DCs according to how many locations on the next level are assigned to each of them */
			Collections.sort(sortedDC);

			/* Number of depots selected */
			int selectDC = 0;
			/* Set of locations already allocated at this level */
			HashSet<Location> allocated = new HashSet<Location>();

			/* Main loop */
			while (selectDC < p && allocated.size() < nbLocLvl && !sortedDC.isEmpty()){
				/* Generate biased random position */
				int position = 0; 
				/* The list of depots that can be selected is limited to: 
				 * Depots object that still have unassigned successors 
				 * (i.e. that haven't been selected yet)
				 */
				double proportion = Math.pow(Config.RAND.nextDouble(), beta) * sortedDC.size();
				while (position <  Math.floor(proportion)) 
					position++; 

				/* Remove the selected DC from the list of candidate DCs */
				Location selectedDC = sortedDC.remove(position).getL();
				/* Add all the locations associated with the selected DCs to the result HashMap */
				locAlloc.put(selectedDC, new HashSet<Location>(this.alloc.get(selectedDC)));
				/* Increase the list of allocated locations with the elements associated with the selected DC (only the remaining ones) */
				allocated.addAll(notAlloc.get(selectedDC));
				/* Remove the selected DC from the list of candidate DCs and add its allocated list to the */
				notAlloc.remove(selectedDC);
				/* Clear the list to update its remaining elements */
				sortedDC.clear();
				/* Updated the remaining locations not yet allocated to a DC */
				for(Location dcKey : notAlloc.keySet()) {
					notAlloc.get(dcKey).removeAll(allocated);
					if(!notAlloc.get(dcKey).isEmpty())
						sortedDC.add(new Pair<Location, Integer>(dcKey, notAlloc.get(dcKey).size()));					
				}
				/* Sort the DC list according to the number of locations they cover */
				Collections.sort(sortedDC);
				selectDC++;
			}
		}
		return locAlloc;
	}

	/**
	 * 
	 * @param p	maximum number of depots to select from the instance
	 * @return	A set of depots selected randomly according to how many locations their can serve
	 */
	public HashSet<Location> depotSelect(int p) {
		try {
			return new HashSet<Location>(this.assignLocations(p).keySet());
		}
		catch (IOException ioe) {
			System.out.println("Exception met while assigning depots");
		}
		return new HashSet<Location>();
	}

	/**
	 * Create a TreeSet of locations that are ordered according to their respective distance to a reference location
	 * @param ref	The reference location that is used to sort the elements of the set
	 * @return		An set of Location sorted in increasing order wrt their distance to ref
	 */
	private TreeSet<Location> initTreeSetLoc(Location ref) {
		TreeSet<Location> tSet = new TreeSet<Location>(new Comparator<Location>() {
			@Override
			public int compare(Location loc1, Location loc2) {
				if (ref.getDistance(loc1) < ref.getDistance(loc2)) {
					return -1;
				} 
				else if (ref.getDistance(loc1) > ref.getDistance(loc2)) {
					return 1; 
				} 
				else return 0;
			}
		});
		return tSet;
	}
}




