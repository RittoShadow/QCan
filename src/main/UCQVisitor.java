package main;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.Var;

public class UCQVisitor extends TransformCopy{
	
	@Override
    public Op transform(OpJoin join, Op left, Op right) {
        if ((left instanceof OpUnion) && !(right instanceof OpUnion)){
        	OpUnion leftU = (OpUnion) left;
        	OpUnion ans = (OpUnion) OpUnion.create(OpJoin.create(leftU.getLeft(), right), OpJoin.create(leftU.getRight(), right));
        	return ans;
        }
        else if (!(left instanceof OpUnion) && (right instanceof OpUnion)){
        	OpUnion rightU = (OpUnion) right;
        	OpUnion ans = (OpUnion) OpUnion.create(OpJoin.create(rightU.getLeft(), left), OpJoin.create(rightU.getRight(), left));
        	return ans;
        }
        else if ((left instanceof OpUnion) && (right instanceof OpUnion)){
        	OpUnion leftU = (OpUnion) left;
        	OpUnion rightU = (OpUnion) right;
        	OpUnion leftAns = new OpUnion(OpJoin.create(leftU.getLeft(), rightU.getLeft()), OpJoin.create(leftU.getLeft(), rightU.getRight()));
        	OpUnion rightAns = new OpUnion(OpJoin.create(leftU.getRight(), rightU.getLeft()), OpJoin.create(leftU.getRight(), rightU.getRight()));
        	return (OpUnion) OpUnion.create(leftAns, rightAns);
        }
        else{
        	return join;
        }
    }
	
	public static void main(String[] args){
		String s = "SELECT DISTINCT * WHERE {	{ ?x <http://ex.org/a> ?y ; ?p  ?o . }	UNION 	{ ?x <http://ex.org/b> ?y }	?y <http://ex.org/c> ?z .        ?x <http://ex.org/a> ?y .        FILTER(?y > 0) }";
		Query query = QueryFactory.create(s);
		for (Var v : query.getProjectVars()){
			System.out.println(v);
		}
		
	}
}
