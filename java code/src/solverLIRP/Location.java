package solverLIRP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import instanceManager.ClientsMap;
import instanceManager.DepotsMap;
import instanceManager.Instance;
import tools.Pair;
import tools.Parameters;


public class Location {

	/*======================
	 *      ATTRIBUTES
	 =======================*/
	private Instance instLIRP;

	/*======================
	 *      CONSTRUCTOR 
	 =======================*/
	/**
	 * Constructor of the PreAllocation Matrix 
	 * @param number of client, number of depots
	 * @throws IOException
	 */
	public Location(Instance instLIRP, int nbClients, int nbDepots) throws IOException {
		this.instLIRP = instLIRP;
		//	this.nbClients = nbClients;
		//this.nbDepots = nbDepots;
	}

	/* ----------------------------------------------
	 *  Pre-allocation of all clients to a subset of depots
	 *  First allocaiton : to the k closest depots
	 *  Second allocation : allocate c to depot d already connected to another client j, close to c
	 */

	private int[][] preAllocation(Instance inst) {
		int k = 3; 			//(parameter) number of closest depots considered
		double mu1 = Parameters.max_time_route;	//(parameter) max distance between client and depot for automatic allocation 
		double mu2 = 0.1 * Parameters.max_time_route;		//(parameter) max distance between clients in farther allocation
		double mu3 = Parameters.max_time_route;    //(parameter) max route distance for farther allocation 
		double dist;
		int[][] preAlloc = new int[this.instLIRP.getNbClients()][this.instLIRP.getNbDepots(00)];
		ArrayList<Pair<Integer, Double>> distanceTable = new ArrayList<Pair<Integer, Double>>(); // distances between a client and all other clients

		for (int c = 0; c < preAlloc.length; c++) {
			// initialization of the pre-allocation matrix to zero
			for (int d = 0; d < preAlloc[c].length;d++) {
				preAlloc[c][d] = 0;
			}

			// for each client, calculate the distance to all depots
			for (c  = 0; c < this.instLIRP.getNbClients();c++) {
				for (int d = 0; d < this.instLIRP.getNbDepots(0); d++) {
					dist = this.instLIRP.getClient(c).getDistance(this.instLIRP.getDepot(0, d));
					Pair<Integer, Double> nextClient = new Pair<Integer, Double>(d,dist);
					distanceTable.add(nextClient);
				}
			}

			// rank the list of depots in non-decreasing order of distance
			distanceTable.sort(new Comparator<Pair<Integer, Double>>() {
				@Override
				public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) 
				{
					if (o1.getR() > o2.getR()) {
						return -1;
					} 
					else if (o1.getR() < o2.getR()) {
						return 1; 
					} 
					else return 0;
				}
			});

			// Pre-allocation to the closest depot
			Pair<Integer, Double> p = distanceTable.get(0); // get closest depot
			int dep = p.getL(); 
			preAlloc[c][dep]=1;					// pre-allocate client c to closest depot 

			// Pre-allocation to the k closest depots
			for (int d = 1; d < k; d++) {
				p = distanceTable.get(d); // get the dth closest depot
				dep = p.getL(); 

				dist = this.instLIRP.getClient(c).getDistance(this.instLIRP.getDepot(0, dep));
				if (dist < mu1) {
					preAlloc[c][dep]=1;					// pre-allocate client c to depot k
				}
			}


			// Second allocation (to further depot)
			for (c = 0; c < this.instLIRP.getNbClients(); c++) {
				ArrayList<Pair<Integer, Double>> distanceClients = new ArrayList<Pair<Integer, Double>>(); // distances to other clients
				// calculate the distance to any other client
				for (int j = 0; j < this.instLIRP.getNbClients(); j++) {
					dist = this.instLIRP.getClient(c).getDistance(inst.getClient(j));
					Pair<Integer, Double> nextClient = new Pair<Integer, Double>(j,dist);
					distanceClients.add(nextClient);
				}

				// sort table of distances
				distanceClients.sort(new Comparator<Pair<Integer, Double>>() {
					@Override
					public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
						if (o1.getR() > o2.getR()) {
							return -1;
						} 
						else if (o1.getR() < o2.getR()) {
							return 1; 
						} 
						else return 0;
					}
				});

				for (int j = 0; j < this.instLIRP.getNbClients(); j++)
				{
					p = distanceClients.get(j); 
					int cprime = p.getL(); // get the jth closest client
					double distcj = this.instLIRP.getClient(c).getDistance(this.instLIRP.getClient(cprime));
					if (distcj < mu2)
					{
						for (int d=0; d < this.instLIRP.getNbDepots(0); d++)
						{
							if (preAlloc[j][d] ==1 && preAlloc[c][d]==0)	
							{
								double distdj = this.instLIRP.getClient(j).getDistance(this.instLIRP.getDepot(0, d));
								double distcd = this.instLIRP.getClient(c).getDistance(this.instLIRP.getDepot(0, d));
								if(distdj + distcj + distcd < mu3)
								{
									preAlloc[c][d] = 1;
								}
							}
						}
					}
					else 
						j = this.instLIRP.getNbClients(); // pour sortir de la boucle
				}
			}
		}
		return preAlloc;
	}


	// Heuristic depot selection
	// Return a matrix with clients allocation (possibly to a dummy depot)

	private int[][] depotSelection(Instance inst, int p)
	{
		int[][] preAlloc = preAllocation(inst); 
		int nc = inst.getNbClients();
		int nd = inst.getNbDepots(0);
		int beta = 3; // parameter for roulette wheel selection

		double y; 	// random value used in roulette wheel selection
		int[][] A = new int[nc][nd+1]; // nd depots + 1 dummy depot
		for (int c=0; c<nc;c++) {
			for (int d=0;d<nd+1;d++) {
				A[c][d] =0;
			}
		}

		int score;
		int s=0; // number of depots selected
		int nbclient = 0; // number of clients already allocated
		ArrayList<Pair<Integer, Double>> depotScore = new ArrayList<Pair<Integer, Double>>(); // depots and their scores


		// calculate score for each depot
		for (int d=0; d<nd;d++) {
			score =0;
			for (int c=0; c<nc;c++){
				score = score + preAlloc[c][d];
			}
			Pair<Integer, Double> ds = new Pair<Integer, Double>(d);
			ds.setScore(score);
			depotScore.add(ds);
		}
		
		// Main loop
		while (s<p && nbclient < nc){
			
			// rank depots in decreasing order of scores
			depotScore.sort(new Comparator<Pair<Integer, Double>>() {
				@Override
				public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) 
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

			//biased roulette wheel depot selection 
			y = Math.random();
			int position = 0; 
			while (position < Math.pow(y,beta) * nd) { position = position +1;} // generate biased random position
			int dnew = depotScore.get(position).getL(); // select depot at the generated random position
			s=s+1;

			// Update allocations and preAllocations
			for (int c=0;c<nc;c++) {
				if (preAlloc[c][dnew]==1) {
					A[c][dnew] =1;
					nbclient = nbclient +1;
					for (int dep=0; dep<nd;dep++) {
						if (dep != dnew)
						{
							if (preAlloc[c][dep]==1) {
								preAlloc[c][dep]=0;
								depotScore.get(dep).decrement();   // decrease scoreof depot dep
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






