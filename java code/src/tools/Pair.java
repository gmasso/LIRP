package tools;

import org.json.JSONArray;

public class Pair<L, R> {
	    private L l;
	    private R r;
	    private double score;

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
	    
	    public double getScore() {
	        return this.score;
	    }
	    

	    public void setR(R r) {
	        this.r = r;
	    }
	    
	    public void setScore(double score) {
	        this.score = score;
	    }
	    
	    public void decrement() {
	        this.score = score-1;
	    }
	    
	    public JSONArray getJSON() {
	    	JSONArray jsonPair = new JSONArray();
	    	jsonPair.put(this.l);
	    	jsonPair.put(this.r);
	    	
	    	return jsonPair;
	    }
	 }
