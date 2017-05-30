package test;

import java.util.Iterator;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.Op1;
import org.apache.jena.sparql.algebra.op.Op2;
import org.apache.jena.sparql.algebra.op.OpN;
import org.apache.jena.sparql.algebra.op.OpUnion;

public class CustomOpWalker extends OpWalker{
	
	public static void walk(Op op, OpVisitor visitor){
		walk(new CustomOpVisitor(visitor), op);
	}
	
	public static void walk(CustomOpVisitor walkerVisitor, Op op)
    {
        op.visit(walkerVisitor) ;
    }

	public static class CustomOpVisitor extends OpWalker.WalkerVisitor {
	
		public CustomOpVisitor(OpVisitor visitor) {
			super(visitor);
		}
	
		@Override
		protected void visit1(Op1 op) {
			before(op);
			if ( visitor != null )
	            op.visit(visitor);
	        if ( op.getSubOp() != null )
	            op.getSubOp().visit(this);     
	        after(op);
			
		}
	
		@Override
	    protected void visit2(Op2 op) {
	        before(op);
	        if ( visitor != null ){
	        	if (op instanceof OpUnion){
	        		
	        	}
	            op.visit(visitor);
	        }
	        if ( op.getLeft() != null )
	            op.getLeft().visit(this);
	        if ( op.getRight() != null )
	            op.getRight().visit(this);
	        after(op) ;
	    }
	
		@Override
	    protected void visitN(OpN op) {
	        before(op);
	        if ( visitor != null )
	            op.visit(visitor);
	        for (Iterator<Op> iter = op.iterator(); iter.hasNext();) {
	            Op sub = iter.next();
	            sub.visit(this);
	        }
	        after(op);
	    }
	}
}
