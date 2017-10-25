import java.io.PrintStream;

//import javax.print.attribute.standard.PrinterLocation;

public class Checker {
	public boolean check(Solution sol, Instance instance, Routes myRoutes, PrintStream printStreamSol) {
		int I = instance.getNbClients();  // number of clients
		int J = instance.getNbDepots();  // number of depots
		int T = instance.getNbPeriods(); // number of periods
		int R = myRoutes.getNbRoutes();  // number of routes
		int[][] Alpha = new int[I][R]; // =1 if client i is in route r
		int[][] Beta = new int[J][R]; // = 1 if depot j is in route r
		int verbose=0;
		int CMAX = 4;
		
		// loading the starting point of each route
		int[] startRoute = new int[R];
		for (int r = 0; r < R; r++) {
			startRoute[r] = (int) myRoutes.getListRoutes(r, 1)-I;
		}
		
		for (int r = 0; r < R; r++) {
			for (int c = 0; c < CMAX ; c++) {
				int cli; 
				cli = (int) myRoutes.getListRoutes(r, c+2);
					if (cli ==-1)
					{
						break;
					}
				Alpha[cli][r]= 1;
			}
		}
		

		for (int r = 0; r < R; r++) {
			int depot = startRoute[r];
			if (depot == -1) {
				break;
			}
			Beta[depot][r] = 1;
		}
		
		boolean isFeasible = true;
	
		
		/*CHECK CONSTRAINT 2 */
		for(int i = 0; i < I; i++ )
		{
			for(int t = 0; t < T; t++)
			{
				double leftTerm = 0;
				for (int r = 0; r < R; r++)
				{
						leftTerm += sol.getListOfRoutes(r, t)*Alpha[i][r];
				}
				if(leftTerm > 1)
				{
					System.out.println("ERROR, Constraint 2, client"+i+",  period "+t);
					isFeasible = false;
				}
				else
				{
					if (verbose < 0)
					{
					System.out.println("BINDING, Constraint 2, client"+i+",  period "+t+"         "+leftTerm );
					}
				}
			}
			
		}
		
		/*CHECK CONSTRAINT 3 */
		for(int r = 0; r < R; r++ ) 
		{
			for(int t = 0; t < T; t++)
			{
				double leftTerm = 0;
				for (int j = 0; j <J; j++)
				{
					leftTerm += sol.getQuantityDeliveredToDepot(j, t)-(instance.getCapacityVehicle()*sol.getDeliveryDepot(j, t));
				}
				if(leftTerm > 0)
				{
					System.out.println("ERROR, Constraint 3, route "+r+", period "+t);
					isFeasible = false;
				}
				else 
				{
					if (verbose < 0)
					{
					System.out.println("BINDING, Constraint 3, route"+r+",  period "+t);
					}
				}		
			}
		}

		/*CHECK CONSTRAINT 4 */
		for(int j = 0; j < J; j++ )
		{
			double leftTerm = 0;
			for(int t = 0;t < T; t++)
			{					leftTerm +=sol.getDeliveryDepot(j, t)-sol.getOpenDepots(j); 
				
				if(leftTerm > 0)
				{
					System.out.println("ERROR, Constraint 4, depot "+j+", period "+t);
					isFeasible = false;
				}
				else 
				{
					if (verbose>1)
					{
					System.out.println("BINDING, Constraint 4, depot"+j+", period "+t);
					}
				}
			}
		}
		
		/*CHECK CONSTRAINT 5 */
		for(int i = 0; i < I; i++ )
		{
			for(int t = 0; t < T; t++)
			{
				double leftTerm = 0;
				for (int r = 0; r < R; r++)
				{
					leftTerm += sol.getQuantityDeliveredToClient(i, r, t)-(instance.getCapacityVehicle()*sol.getListOfRoutes(r, t));
				}
				if(leftTerm > 0)
				{
				System.out.println("ERROR, Constraint 5, client "+i+", period "+t);
				isFeasible = false;
				}
				else 
				{
					if (verbose>1)
					{
					System.out.println("BINDING, Constraint 5, client"+i+", period "+t);
					}
				}		
			}
		}
	
		/*CHECK CONSTRAINT 6 */
		for(int t = 0; t < T; t++)
		{
			double leftTerm = 0;
			for (int r = 0; r < R; r++)
			{
				leftTerm += sol.getListOfRoutes(r, t)-instance.getNbVehicles();
			
			if(leftTerm > 0)
			{
				System.out.println("ERROR, Constraint 6, period "+t+", route "+r);
				isFeasible = false;
			}
			else 
			{
				if (verbose>1)
				{
				System.out.println("BINDING, Constraint 6, period "+t+", route "+r);
				}
			}
			}
		}
	
	/*CHECK CONSTRAINT 7 */
		for(int r = 0; r < R; r++ )
		{
			for(int t = 0; t < T; t++)
			{
				double leftTerm = sol.getListOfRoutes(r, t);
				double rightTerm = 0;	
				for (int j = 0; j < J; j++)
			{
				rightTerm += Beta[j][r]*sol.getOpenDepots(j);	
			}
				System.out.println();
				if(leftTerm > rightTerm)
				{
					System.out.println("ERROR, Constraint 7, route "+r+", period "+t);
					isFeasible= false;
				}
				else if (verbose>1)
				{
					System.out.println("BINDING, Constraint 7, route "+r+", period "+t);
						
				}	
			}
		}
		
		
		/*CHECK CONSTRAINT 8bis : for period zero */
		for(int j = 0; j < J; j++ )
		{
			double leftTerm = 0;
			double rightTerm = 0;
				for(int r = 0; r < R; r++)
				{
					for (int i = 0; i < I; i++)
					{
						rightTerm += Alpha[i][r] * Beta[j][r] * sol.getQuantityDeliveredToClient(i, r, 0);
					}
				}	
				rightTerm += sol.getStockDepot(j, 0) ;
				
				leftTerm += sol.getQuantityDeliveredToDepot(j, 0) + instance.getInventoryInitialDepot(j) ;
				
			if(Math.abs(leftTerm-rightTerm)>ResolutionMain.epsilon)
			{
				System.out.println("ERROR, Constraint 8b, depot "+j+" period 0");
			}		
		}
	
	/*CHECK CONSTRAINT 8*/
	for(int t = 1; t < T; t++ )
	{
		
		for(int j = 0; j < J; j++)
		{
			double leftTerm = 0;
			leftTerm +=   sol.getStockDepot(j, t-1) + sol.getQuantityDeliveredToDepot(j, t);
			double rightTerm = 0;
			for (int r = 0; r < R; r++)	{
				for (int i = 0; i < I; i++) 
					rightTerm += (Alpha[i][r] * Beta[j][r] * sol.getQuantityDeliveredToClient(i, r, t));
			}
			rightTerm+=	sol.getStockDepot(j, t);
			if(Math.abs(leftTerm-rightTerm)>ResolutionMain.epsilon)
			{
				System.out.println("ERROR, Constraint 8, period "+t+", depot "+j);
			}		
		}
	}
		


	/*CHECK CONSTRAINT 9bis : period zero */
	for(int i = 0; i < I; i++ )
	{
			double leftTerm = sol.getStockClient(i, 0);
			double rightTerm = 0;
			for (int r = 0; r < R; r++)
			{
			rightTerm += (Alpha[i][r]*sol.getQuantityDeliveredToClient(i, r, 0));
			}
			
			rightTerm += instance.getInventoryInitialClient(i)-instance.getDemand(i, 0);
			if(Math.abs(leftTerm-rightTerm)>ResolutionMain.epsilon)
			{ 
				System.out.println("ERROR, Constraint 9b, client "+i+", period 0");
			}
	}
			
	/*CHECK CONSTRAINT 9*/
		for(int i = 0; i < I; i++ )
		{
			for (int t = 1; t < T; t++) {
				double leftTerm = sol.getStockClient(i, t);
				double rightTerm =sol.getStockClient(i, t-1) - instance.getDemand(i, t);
				for (int r = 0; r < R; r++) {
					rightTerm += (Alpha[i][r]*sol.getQuantityDeliveredToClient(i, r, t));
				}
				if(Math.abs(leftTerm-rightTerm)>ResolutionMain.epsilon)
				{
					System.out.println("ERROR, Constraint 9, client "+i+", period "+t);
					//System.out.println(leftTerm +" > " + rightTerm);
				}
			}
		}
	/*CHECK CONSTRAINT 10*/		
		for (int i = 0; i < I; i++) {
			for (int t = 0; t < T; t++) {
				double leftTerm = sol.getStockClient(i, t); ;
				double rightTerm = 0;
					for (int t2 = t+1; t2 < T; t2++) {
						rightTerm += instance.getDemand(i, t2);
					}
					if(leftTerm > rightTerm)
					{
					System.out.println("ERROR, Constraint 10, client "+i+", period "+t);
					isFeasible = false;
					}
					else 
					{
						if (verbose>0)
						{
						System.out.println("BINDING, Constraint 10, client "+i+", period "+t);
						}
					}
			}
	}	
	
	/*CHECK CONSTRAINT 10b*/	
		for (int t = 0; t < T; t++) {
			double leftTerm = 0;
			for (int i = 0; i < I; i++) {
				leftTerm += sol.getStockClient(i, t)-instance.getCapacityClient();
			
			if(leftTerm > 0)
			{
			System.out.println("ERROR, Constraint 10b, period "+t+", client "+i);
			isFeasible = false;
			}
			else 
			{
				if (verbose>0)
				{
				System.out.println("BINDING, Constraint 10b, period "+t+", client "+i);
				}
			}
			}
		}
		
	/*CHECK CONSTRAINT 11bis*/	
		for (int j = 0; j < J; j++) {
			double rightTerm = instance.getInventoryInitialDepot(j);
			for (int r = 0; r < R; r++) {
				double leftTerm = 0; 
					for (int i = 0; i < I; i++) {
						leftTerm += Beta[j][r]*sol.getQuantityDeliveredToClient(i, r, 0)-sol.getQuantityDeliveredToDepot(j, 0);	
					}		
			if(leftTerm > rightTerm)	
			{
				System.out.println("ERROR, Constraint 11b, depot "+j+" period 0");
				isFeasible = false;
			}
			else 
			{
				if (verbose > 0)
				{
				System.out.println("BINDING, Constraint 11b, depot "+j+" period 0");
				}
			}
			}
		}		
			
	/*CHECK CONSTRAINT 11*/	
		for (int t = 1; t < T; t++) { 
			double rightTerm = 0;// for all t
			for (int j = 0; j < J; j++)// for all j	
			{
				rightTerm += sol.getStockDepot(j, t-1)+ sol.getQuantityDeliveredToDepot(j, t);	
				for (int r = 0; r < R; r++) {
					double leftTerm = 0;
					for (int i = 0; i < I; i++) 
					{
						leftTerm += Beta[j][r]*sol.getQuantityDeliveredToClient(i, r, t);	
					}
					if(leftTerm > rightTerm)	
					{
						System.out.println("ERROR, Constraint 11, depot "+j+", period "+t);
						isFeasible = false;
					}	
					else 
					{
						if (verbose>0)
						{
							System.out.println("BINDING, Constraint 11b, depot "+j+", period "+t);
						}
					}
				}
			}
		}
	
	/*CHECK CONSTRAINT 12*/
		 for (int i = 0; i < I; i++)
		 {
			 for (int t = 1; t < T; t++) 
			 {
				 
			 	 for (int r = 0; r < R; r++) {
			 		 double leftTerm = sol.getQuantityDeliveredToClient(i, r, t);
			 		 double rightTerm = instance.getCapacityVehicle()*Alpha[i][r]; 
			 		 if(leftTerm > rightTerm)
			 		 {
						System.out.println("ERROR, Constraint 12, client "+i+" period "+t);
						System.out.println(+leftTerm+ "and" +rightTerm);
						isFeasible = false;
			 		 }
						else 
						{
							if (verbose > 0) 
							{
							System.out.println("BINDING, Constraint 12, client "+i+" period "+t);
							}
						}
			 	}
			 }
		 }
		 
		return isFeasible;
	}
}
