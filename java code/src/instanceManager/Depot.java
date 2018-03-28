package instanceManager;
import java.awt.geom.Point2D;
import java.io.IOException;

import org.json.JSONObject;

public class Depot extends Location{
	/*
	 * ATTRIBUTES
	 */
	private double fixedCost; // The cost to open the depot
	private double orderingCost; // The fixed ordering cost of the depot
	
	/*
	 * CONSTRUCTORS
	 */
	/**
	 * Creates a Depot object located at specific coordinates
	 * @param coordDepot	the coordinates of the depot
	 * @throws IOException
	 */
	public Depot(Point2D coordDepot, Location supplier) throws IOException{
		super(coordDepot);
		this.fixedCost = 0;
		this.orderingCost = 0;
		this.holdingCost = 1;
	}
	
	/**
	 * Creates a Depot object at a specified location, with specific attributes
	 * @param coordDepot			the coordinates of the depot
	 * @param fixedCost			the fixed cost incurred when this depot is open to operate
	 * @param holdingCost		the holding cost of the depot
	 * @param orderingCost		the ordering cost of the depot
	 * @param initialInventory	the initial inventory at the beginning of the planning horizon
	 * @param capacity			the capacity bound on the inventory level at the depot
	 * @throws IOException
	 */
	public Depot(Point2D coordDepot, double fixedCost, double holdingCost, double orderingCost, double initialInventory, double capacity, Location supplier) throws IOException{
		super(coordDepot, holdingCost, initialInventory, capacity);
		this.fixedCost = fixedCost;
		this.orderingCost = (orderingCost>0) ? orderingCost : 0;
	}	
	
	/**
	 * Creates a Depot object from a JSON object
	 * @param jsonDepot	the JSON object containing data on the depot
	 * @throws IOException
	 */
	public Depot(JSONObject jsonDepot) throws IOException {
		super(jsonDepot);
		this.fixedCost = jsonDepot.isNull("fc") ? 0 : jsonDepot.getDouble("fc"); // Fixed cost for opening this depot
		this.orderingCost = jsonDepot.isNull("oc") ? 0 : jsonDepot.getDouble("oc"); // Fixed ordering cost of the depot
	}
	
	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @return	the ordering cost of the depot
	 */
	public double getOrderingCost() {
		return this.orderingCost;
	}
	
	/**
	 * 
	 * @return	the fixed cost incurred for opening the depot
	 */
	public double getFixedCost() {
		return this.fixedCost;
	}
	
	/*
	 * MUTATORS
	 */
	/**
	 * Reset the fixed cost for this depot
	 * @param fc		the new fixed cost incurred for opening the depot
	 */
	public void setFixedCost(double fc) {
		this.fixedCost = fc;
	}
	
	/**
	 * Reset the ordering cost for this depot
	 * @param oc		the new ordering cost of the depot
	 */
	public void setOrderingCost(double oc) {
		this.orderingCost = oc;
	}
	
	/**
	 * Enrich a JSON object with specific attributes of the depot
	 */
	@Override
	protected JSONObject getJSONLocSpec() throws IOException {
		// Create a JSON Object to describe the depot
		JSONObject jsonDepot = new JSONObject();
		
		jsonDepot.put("fc", this.fixedCost > 0 ? this.fixedCost : 0);
		jsonDepot.put("oc", this.orderingCost > 0 ? this.orderingCost : 0);

	
		return jsonDepot;
	}

}
