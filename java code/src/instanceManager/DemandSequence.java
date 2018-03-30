package instanceManager;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import tools.Parameters;

public class DemandSequence extends Location {
	private double[] demands; // The demand of the client for each period of the planning horizon

	/**
	 * Creates a DemandSequence object from its coordinates on the map
	 * @param coordBox		the coordinates at which the demand box is located
	 * @throws IOException
	 */
	public DemandSequence(Point2D coordBox) throws IOException{
		super(coordBox);
	}

	/**
	 * Creates a Client object from data contained in a JSON object and the length of the number of periods to consider
	 * @param jsonClient			the JSON object that contains the attributes of the client
	 * @param planningHorizon	the number of periods
	 * @throws IOException
	 */
	public DemandSequence(JSONObject jsonDemandSeq) throws IOException {
		super(jsonDemandSeq);
		// Check if the field "demands" is null
		if(jsonDemandSeq.isNull("demands"))
			demands = null;
		// If not, create the demands array for this client and fill it with the values of the demands
		else {
			JSONArray jsonDemands = jsonDemandSeq.getJSONArray("demands"); // The JSON table containing the demands
			this.demands = new double[jsonDemands.length()]; 
			for(int t = 0; t < jsonDemands.length(); t++) {
				this.demands[t] = jsonDemands.getDouble(t); // Demand for this client in period t
			}
		}
	}

	/*
	 * MUTATORS
	 */
	public void fillValues(int planningHorizon, double intensity, boolean isUniform, int period) {
		this.demands = new double[planningHorizon];

		// If the demand is uniform the sequence of demands is filled with uniform r.v.
		if (isUniform) {
			for (int t = 0; t < planningHorizon; t++)
				this.demands[t] = intensity * Parameters.rand.nextDouble();
		}
		// Otherwise the demand is drawn according to a truncated normal distribution,
		// around its average value (sinus if periodic, constant otherwise)
		else {
			double avgDemand = 0.5;
			for (int t = 0; t < planningHorizon; t++) {
				double currentMean = avgDemand;
				double currentDemand = -1;
				// In the periodic case, the average demand follows sinusoidal
				if (period > 0)
					currentMean += 0.5 * Math.sin(2 * Math.PI * t / period);
				while (currentDemand < 0 || currentDemand > currentMean + 0.5) {
					currentDemand = intensity * (currentMean + Parameters.rand.nextGaussian() / 6);	
				}
				this.demands[t] = currentDemand;
			}
		}
	}
	/*
	 * ACCESSORS
	 */
	/**
	 * 
	 * @param period	the period at which the demand is collected
	 * @return		the value of the demand in period "period"	
	 */
	public double getValue(int period) {
		return this.demands[period];
	}

	/**
	 * 
	 * @return	The sequence of demands over the entire planning horizon
	 */
	public double[] getSequence() {
		return this.demands;
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
		JSONObject jsonSequence = new JSONObject();
		if(this.demands != null)
			jsonSequence.put("demands",new JSONArray(this.demands));

		return jsonSequence;
	}


}
