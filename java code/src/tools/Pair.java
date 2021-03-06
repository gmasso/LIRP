package tools;

import org.json.JSONArray;

public class Pair<L, R> implements Comparable<Pair<L, R>>{
	    private L l;
	    private R r;

	    public Pair(L l, R r) {
	        this.l = l;
	        this.r = r;
	    }

	    public Pair(L l) {
	        this.l = l;
	    }
	
	    public String toString()
	    { 
	           return "<" + this.l.toString() + ", " + this.r.toString() + ">"; 
	    }

	    public L getL() {
	        return this.l;
	    }

	    public void setL(L l) {
	        this.l = l;
	    }

	    public R getR() {
	        return this.r;
	    }

	    public void setR(R r) {
	        this.r = r;
	    }
	    
	    public JSONArray getJSON() {
	    	JSONArray jsonPair = new JSONArray();
	    	jsonPair.put(this.l);
	    	jsonPair.put(this.r);
	    	
	    	return jsonPair;
	    }

		@Override
		public int compareTo(Pair<L, R> pair) {
			return - ((Comparable<R>) this.r).compareTo(pair.getR());
		}	
	 }
