package test;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.main.OpExecutor;
import org.apache.jena.sparql.engine.main.OpExecutorFactory;

public class OpExecutorBlank extends OpExecutor{
	
	  // Execute an operation with a different OpExecution Factory

	   // New context.
	   protected OpExecutorBlank(ExecutionContext execCxt) {
		super(execCxt);
		// TODO Auto-generated constructor stub
	}
	   
	   public static OpExecutorFactory factory = new OpExecutorFactory() {

	        @Override
	        public OpExecutor create(ExecutionContext execCxt) {
	            return new OpExecutorBlank(execCxt) ;
	        }} ;
	        
	   public QueryIterator execute(OpProject op, QueryIterator input){
		   QueryIterator aux = super.execute(op,input);
		   while (aux.hasNext()){
			   Binding q = aux.next();
		   }
		   return input;
		   
	   }

}
