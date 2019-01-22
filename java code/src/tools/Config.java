/**
 * 
 */
package tools;

import java.util.Random;

/**
 * @author gmas
 *
 */
public final class Config {
	// Random object to generate distributions
	public static Random RAND = new Random(System.currentTimeMillis());

	// Grid size
	public static double grid_size = 100;
	
	public static int NB_LEVELS = 2;

	/* Granularity for the demand map (split into demand boxes of size nbSteps x nbSteps) */
	public static int nb_steps = 100;
	/* Possible demand profiles(0: Light, 1: Heavy, 2: Mixture) */
	public static double[][] demand_profiles = {{0.05, 0.15}, {0.2, 0.4}, {0.05, 0.4}};
	public static String[] profile_names = {"L", "H", "M"};
	public static double spatial_threshold = 0.3;
	public static Boolean[][] active_profiles = {{true,true,true,true,true,true,true}, {true,true,true,true,true,true,false},
		{true,true,true,true,true,false,false}, {false,true,true,true,true,true,false}};
	public static double[][] proba_actives = {{0.1, 0.4, 0.25, 0.25}, {1, 0, 0, 0}, {0, 0, 1, 0}};

	/* Routes parameters */
	public static double AVG_SPEED = 50; 				// In km/h
	public static double STOPPING_TIME = 0.25; 			// In hours
	public static double MAX_TIME_ROUTE = 4; 			// In hours
	public static double FIXED_COST_ROUTE = 100; 		// In euros
	public static double COST_KM = 1; 					// Cost per km, in euros
	
	public static double FIXED_COST_DC = 2000;			// Cost incurred to open a DC on a location

	/* The "bigM" if no capacity is given */
	public static double BIGM = 1000000;

	/* Solver parameters */
	public static final double EPSILON = 0.000001; 		// Precision for the constraints
	public static final double MAIN_TILIM = 3600; 		// Time limit for the solver in seconds
	public static final int MAX_THREADS = 4;
	
	public static final double AUX_TILIM = 360;  		// Time limit for the solver in seconds
	public static final int RECOMPUTE = 2; 				// Number of recomputations using rejected routes in the route sampling algo

	public static final double NOSPLIT_TILIM = 7200;	// Time limit for the solver when no splitting is allowed

	public enum typeModel {
		direct_direct,
		direct_loop,
		loop_direct,
		loop_loop	
	}
}
