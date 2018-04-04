package instanceManager;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Mask {

	/**
	 * 
	 */
	private int[] activeSites;
	private int[] inactiveSites;
	private Layer map;

	/**
	 * 
	 * @param map
	 * @param nbActive
	 */
	public Mask(Layer map, int nbActive) {
		this.map = map;
		this.activeSites = new int[Math.min(nbActive, this.map.getNbSites())];
		this.inactiveSites = new int[Math.max(0, this.map.getNbSites() - nbActive)];
		this.setActiveSites();
	}
	
	/**
	 * 
	 * @param map
	 * @param nbActive
	 */
	public Mask(Layer map) {
		this.map = map;
		this.activeSites = new int[this.map.getNbSites()];
		for(int i = 0; i < this.activeSites.length; i++) {
			this.activeSites[i] = i;
		}
	}

	/**
	 * 
	 * @param map
	 * @param activeSites
	 */
	public Mask(Layer map, int[] activeSites) {
		this.map = map;
		this.activeSites = activeSites;
		this.inactiveSites = new int[this.map.getNbSites() - this.activeSites.length];
		int iterSites = 0;
		int inactiveIndex = 0;
		while(iterSites < this.map.getNbSites() && inactiveIndex < this.inactiveSites.length) {
			boolean isInactive = true;
			int activeIter = 0;
			while(isInactive && activeIter < this.activeSites.length) {
				if(this.activeSites[activeIter] == iterSites)
					isInactive = false;
				else
					activeIter++;
			}
			if(isInactive) {
				this.inactiveSites[inactiveIndex] = iterSites;
				inactiveIndex++;
			}
			iterSites++;
		}
	}

	/**
	 * Set the active sites randomly
	 */
	public void setActiveSites() {
		int[] indices = new int[this.map.getNbSites()];
		for(int i = 0; i < indices.length; i++) {
			indices[i] = i;
		}
		
		/* Shuffle as many elements in the array indices as there are active sites */
		Random rnd = ThreadLocalRandom.current();
		/* Fill an array with the indices of possible routes */
		for (int nbElts = 0; nbElts < this.activeSites.length; nbElts++) {
			int swapIndex = rnd.nextInt(this.activeSites.length - nbElts);
			/* Save the selected index to the active sites list */
			this.activeSites[nbElts] = indices[swapIndex];
			/* Replace the last element at the position of the last selected index */
			indices[swapIndex] = indices[this.activeSites.length - nbElts - 1];
		}
		for(int inactive = 0; inactive < this.inactiveSites.length; inactive++) {
			this.inactiveSites[inactive] = indices[inactive];
		}
	}
	
	/**
	 * 
	 * @param siteIndex	The index of the site of interest
	 * @return			The site of interest
	 */
	public Location getSite(int siteIndex) {
		return this.map.getSite(this.activeSites[siteIndex]);
	}
	
	/**
	 * 
	 * @return	The layer on which the mask applies
	 */
	public Layer getLayer() {
		return this.map;
	}
	
	/**
	 * 
	 * @return
	 */
	public int[] getInactiveSites() {
		return this.inactiveSites;
	}
	
	public int getNbActiveSites() {
		return this.activeSites.length;
	}
}
