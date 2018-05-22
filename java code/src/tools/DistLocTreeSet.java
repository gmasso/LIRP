package tools;

import java.util.Comparator;
import java.util.TreeSet;

import instanceManager.Location;

public class DistLocTreeSet extends TreeSet<Location> implements Comparator<Location> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Location ref;

	public DistLocTreeSet(Location ref) {
		this.ref = ref;
	}

	@Override
	public int compare(Location loc1, Location loc2) {
		if (this.ref.getDistance(loc1) < this.ref.getDistance(loc2)) {
			return -1;
		} 
		else if (this.ref.getDistance(loc1) > this.ref.getDistance(loc2)) {
			return 1; 
		} 
		else return 0;
	}

}
