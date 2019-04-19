package tools;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;

public class Util {

	public static <T> ImmutableBiMap<T,IloIntVar> makeBinaryVariables(IloCplex cplex, Iterable<T> set) throws IloException{
	    Builder<T,IloIntVar> ans = ImmutableBiMap.builder();
	    for(T t: set){
	        ans.put(t, cplex.boolVar());
	    }
	    return ans.build();
	}
}
