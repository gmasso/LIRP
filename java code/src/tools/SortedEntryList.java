package tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import instanceManager.Location;

public class SortedEntryList extends ArrayList<Pair<Location, Integer>>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SortedEntryList(HashMap<Location, HashSet<Location>> map){
		for(HashMap.Entry<Location, HashSet<Location>> mapEntry : map.entrySet()) {
			this.add(new Pair<Location, Integer>(mapEntry.getKey(), mapEntry.getValue().size()));
		}
	}
}

