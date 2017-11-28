package instanceManager;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.geom.Point2D;

public class Client extends Location{
	private double[] demands; // The demand of the client for each period of the planning horizon
	
	/**
	 * Creates a Client object from its coordinates on the map and its demands sequence
	 * @param coordClient	the coordinates at which the client is located
	 * @param demands		the sequence of customers' demands faced by the client
	 * @throws IOException
	 */
	public Client(Point2D coordClient) throws IOException{
		super(coordClient);
	}
	
	/**
	 * Creates a Client object from all its attributes
	 * @param coordClient		the coordinates at which the client is located
	 * @param holdingCost		the holding cost at the client
	 * @param demands			the sequence of customers' demands faced by the client
	 * @param initialInventory	the initial on-hand inventory at the beginning of the planning horizon
	 * @param capacity			the inventory capacity
	 * @throws IOException
	 */
	public Client(Point2D coordClient, double holdingCost, double[] demands, double initialInventory, double capacity) throws IOException{
		super(coordClient, holdingCost, initialInventory, capacity);
		this.demands = demands;
	}	
	
	/**
	 * Creates a Client object from data contained in a JSON object and the length of the number of periods to consider
	 * @param jsonClient			the JSON object that contains the attributes of the client
	 * @param planningHorizon	the number of periods
	 * @throws IOException
	 */
	public Client(JSONObject jsonClient, int planningHorizon) throws IOException {
		super(jsonClient);
		// Check if the field "demands" is null
		if(jsonClient.isNull("demands"))
			demands = null;
		// If not, create the demands array for this client and fill it with the values of the demands
		else {
			JSONArray jsonDemands = jsonClient.getJSONArray("demands"); // The JSON table containing the demands
			if(jsonDemands.length() < planningHorizon)
				throw new IOException("Error in instance file : the number of demands is not enough to cover the planning horizon");
			demands = new double[planningHorizon]; 
			for(int t=0; t<jsonDemands.length(); t++) {
				this.demands[t] = jsonDemands.getDouble(t); // Demand for this client in period t
			}
		}
	}
	
	/*
	 * MUTATORS
	 */
	/**
	 * Set the value of the demand for a specific period
	 * @param period	the period to which the demand is associated
	 * @param value	the value of the demand for the period considered
	 */
	public void setDemand(int period, double value){
		this.demands[period] = value;
	}
	
	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @param period	the period at which the demand is collected
	 * @return		the value of the demand in period "period"	
	 */
	public double getDemand(int period) {
		return this.demands[period];
	}
	
	/**
	 * Compute the cumulative demand over a time interval
	 * @param start	the first period in the interval
	 * @param end	the first period out of the interval
	 * @return		the sum of the demands faced by the client in [start, end-1]
	 */
	public double getCumulDemands(int start, int end) {
		double cumDemand = 0;
		for(int t = start; t < end; t++) {
			cumDemand += this.demands[t];
		}
		return cumDemand;
	}

	/**
	 * Enrich the JSON object associated with the location with the demand sequence of the client
	 */
	@Override
	protected JSONObject getJSONLocSpec() throws IOException {
		// Create a JSON Object to describe the depots map
		JSONObject jsonClient = new JSONObject();
		
		jsonClient.put("demands", new JSONArray(this.demands));
		
		return jsonClient;
	}

	
}
