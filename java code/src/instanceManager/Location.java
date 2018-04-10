package instanceManager;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.geom.Point2D;

public class Location {
	protected Point2D locationCoords; // Coordinates of the location
	protected double holdingCost = -1; // The per-unit per-period holding cost for this location
	protected double initialInventory = -1; // The inventory on hand at the beginning of the planning horizon
	protected double capacity = -1; // The storage capacity of the location
	
	/*
	 * CONSTRUCTOR
	 */

	/**
	 * 
	 * @param x	 			the x-axis coordinate of the location
	 * @param y				the y-axis coordinate of the location
	 * @throws IOException
	 */
	public Location(double x, double y) throws IOException {
		this.locationCoords = new Point2D.Double(x,y);
	}
	
	/**
	 * Create a Location object from a set of coordinates
	 * @param locationCoord
	 * @throws IOException
	 */
	public Location(Point2D locationCoord) throws IOException {
		this.locationCoords = locationCoord;
	}
	
	/**
	 * Create a Location object from its coordinates and its holding cost
	 * @param locationCoord, holdingCost
	 * @throws IOException
	 */
	public Location(Point2D locationCoord, double holdingCost) throws IOException {
		this.locationCoords = locationCoord;
		this.holdingCost = holdingCost;
	}
	
	/**
	 * Create a Location object from its coordinates and other attributes
	 * @param locationCoord, holdingCost, initialInventory, capacity
	 * @throws IOException
	 */
	public Location(Point2D locationCoord, double holdingCost, double initialInventory, double capacity) throws IOException {
		this.locationCoords = locationCoord;
		this.holdingCost = holdingCost;
		this.initialInventory = initialInventory;
		this.capacity = capacity;
	}
	
	/**
	 * Create a Location object from a JSON object containing data
	 * @param jsonLoc		the JSON object that describes the location
	 * @throws IOException
	 * @throws NullPointerException
	 */
	public Location(JSONObject jsonLoc) throws IOException, NullPointerException {
		JSONArray jsonCoords = (JSONArray) jsonLoc.get("coordinates");

		/* Remark: Coordinates can't be null but other fields may not have values, in this case the corresponding attribute of the location is set to zero. */
		double coord_x = jsonCoords.isNull(0) ? -1 : jsonCoords.getDouble(0); // Coordinate of the client on the x-axis
		double coord_y = jsonCoords.isNull(1) ? -1 : jsonCoords.getDouble(1); // Coordinate of the client on the y-axis
		// If one the coordinates does not appear, throw an exception
		if(coord_x == -1 || coord_y == -1)
			throw new NullPointerException();	
		this.locationCoords = new Point2D.Double(coord_x, coord_y);
		this.holdingCost = jsonLoc.isNull("hc") ? -1 : jsonLoc.getDouble("hc"); // Holding cost for this client
		this.initialInventory = jsonLoc.isNull("is") ? -1 : jsonLoc.getDouble("is"); // Initial inventory at the client at the beginning of the planning horizon
		this.capacity = jsonLoc.isNull("cap") ? -1 : jsonLoc.getDouble("cap"); // The capacity of the client
		
		/* TO BE MODIFIED */
		if(this.capacity <= 0)
			this.capacity = 1000000;
	}
	
	/*
	 * ACCESSORS 
	 */
	/**
	 * 
	 * @return	the coordinates of the location
	 */
	public Point2D getCoordinates() {
		return this.locationCoords;
	}
	
	/**
	 * 
	 * @return	the holding cost of the location
	 */
	public double getHoldingCost() {
		return this.holdingCost;
	}
	
	/**
	 * 
	 * @return	the capacity of the location
	 */
	public double getCapacity() {
		return this.capacity;
	}
	
	/**
	 * 
	 * @return	the initial inventory of the location
	 */
	public double getInitialInventory() {
		return this.initialInventory;
	}
	
	/**
	 * 
	 * @return	true if the location is dummy (i.e. doest not correspond to any physical facility of the network)
	 */
	public boolean isDummy() {
		return (this.locationCoords.getX() < 0) || (this.locationCoords.getY() < 0);
	}
	
	/*
	 * MUTATORS
	 */
	/**
	 * Relocate the Location object on a new point
	 * @param coords	the coordinates at which the location is relocated
	 */
	public void setCoordinates(Point2D coords) {
		this.locationCoords = coords;
	}
	
	/**
	 * Reset the holding cost of the Location
	 * @param hc	the new holding cost value
	 */
	public void setHC(double hc) {
		this.holdingCost = hc;
	}
	
	/**
	 * Reset the capacity constraint of the inventory of this location
	 * @param capa	the new capacity
	 */
	public void setCapacity(double capa) {
		this.capacity = (capa > 0) ? capa : 0;
	}
	
	/**
	 * Set the initial inventory at the beginning of the planning horizon
	 * @param initInv	the new starting stock level
	 */
	public void setInitInv(double initInv) {
		this.initialInventory = (initInv > 0) ? initInv : 0;
	}
	
	/*
	 * PUBLIC METHODS
	 */
	/**
	 * Compute the distance between this Location object and an other one
	 * @param otherLocation	the other Location of interest
	 * @return				the euclidian distance between this Location and the other one					
	 */
	public double getDistance(Location otherLocation) {
		return this.locationCoords.distance(otherLocation.getCoordinates());
	}
	
	/**
	 * Compute the distance between this Location object and a point
	 * @param otherLocation	the point of interest
	 * @return				the euclidian distance between this Location and the point of interest
	 */
	public double getDistance(Point2D otherLocation) {
		return this.locationCoords.distance(otherLocation);
	}
	
	/**
	 * Creates and fill a new JSON object containing data about this Location object
	 * @return	a JSON object filled with this Location object characteristics
	 * @throws IOException
	 */
	protected JSONObject getJSONLoc() throws IOException {
		JSONObject jsonLoc = this.getJSONLocSpec();
		jsonLoc.put("coordinates", new JSONArray(new double[] {this.locationCoords.getX(), this.locationCoords.getY()}));
		if(this.holdingCost > -1) {
			jsonLoc.put("hc", this.holdingCost > 0 ? this.holdingCost : 0);
		}
		if(this.initialInventory > -1) {
			jsonLoc.put("is", this.initialInventory > 0 ? this.initialInventory : 0);
		}
		if(this.capacity > -1) {
			jsonLoc.put("capacity", this.capacity);
		}
		
		return jsonLoc;
	}
	
	/**
	 * Creates a JSON object to store info about this Location object (overridden in subclasses)
	 * @return	a new JSON object
	 * @throws IOException
	 */
	protected JSONObject getJSONLocSpec() throws IOException {
		return new JSONObject();
	}
	
}
