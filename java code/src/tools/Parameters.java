/**
 * 
 */
package tools;

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
	
	public static int nb_levels = 2;

	// Granularity for the demand map (split into demand boxes of size nbSteps x nbSteps)
	public static int nb_steps = 100;
	// Possible demand profiles(0: Light, 1: Heavy, 2: Mixture)
	public static double[][] demandProfiles = {{0.05, 0.15}, {0.2, 0.4}, {0.05, 0.4}};
	public static double spatial_threshold = 0.3;
	public static Boolean[][] active_profiles = {{true,true,true,true,true,true,true}, {true,true,true,true,true,true,false},
		{true,true,true,true,true,false,false}, {false,true,true,true,true,true,false}};
	public static double[] proba_profiles = {0.1, 0.4, 0.25, 0.25};

	// Routes parameters
	public static double avg_speed = 50; 			// In km/h
	public static double stopping_time = 0.25; 		// In hours
	public static double max_time_route = 4; 		// In hours
	public static double fixed_cost_route = 100; 	// In euros
	public static double cost_km = 2; 				// Cost per km, in euros

	// Maximum number of locations to open depots on a map
	public static int max_nb_depots = 10;
	public static double fixed_cost_dc = 1000;

	// The "bigM" if no capacity is given
	public static double bigM = 1000000;

	//Solver parameters
	public static int max_nb_routes = 1000;  		// Maximum number of routes in the mathematical model
	public static double epsilon = 0.000001; 		// Precision for the constraints
	public static final double mainTimeLimit = 3600; // Time limit for the solver in seconds
	
	
	public static final double auxTimeLimit = 360;  	// Time limit for the solver in seconds
	public static final int recompute = 2; 			// Number of recomputations using rejected routes in the route sampling algo


	public enum typeModel {
		direct_direct,
		direct_loop,
		loop_direct,
		loop_loop	
	}
}
