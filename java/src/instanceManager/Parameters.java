/**
 * 
 */
package instanceManager;

import java.util.Random;

/**
 * @author gmas
 *
 */

/*
 * Attributes that are decided by the user
 */
//CITY_SIZES = [3, 6, 9, 12, 18]
//P_SIZES = [0.25, 0.3, 0.2, 0.18, 0.07]
//
//# Possible horizons
//# 100 : 3 months (in days) or a year (in weeks), 
//# 200 : 4 years in weeks,
//# 400 : a year in days
//HORIZONS = [100, 200, 400]
//D_PERIODS = [7, 30, 52]

public final class Parameters {
	// Random object to generate distributions
	public static Random rand = new Random(System.currentTimeMillis());
	
	// Grid size
	public static double grid_size = 100;
	
	// Granularity for the demand map (split into demand boxes of size nbSteps x nbSteps)
	public static int nbSteps = 1000;
	
	// Routes parameters
	public static double avg_speed = 50; // In km/h
	public static double stopping_time = 0.25; // In hours
	public static double max_time_route = 4; // In hours
	public static double fixed_cost_route = 100; // In euros
	public static double cost_km = 2; // Cost per km, in euros
	
	// Maximum number of locations to open depots on a map
	public static int max_nb_depots = 10;
	
	// The "bigM" if no capacity is given
	public static double bigM = 1000;
	
	//Solver parameters
	public static int max_nb_routes = 1000;  // Maximum number of routes in the mathematical model
	public static double epsilon = 0.000001; // Precision for the constraints
	public static final double TimeLimit = 1000;  // Time limit for the solver in seconds
	
}
