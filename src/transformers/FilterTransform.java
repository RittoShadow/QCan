package transformers;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.expr.ExprList;

public class FilterTransform extends TransformCopy {
	
	/* (non-Javadoc)
	 * This only applies if the variables in the filter expression are contained in the safe variables in the left Op. Needs revision.
	 * @see org.apache.jena.sparql.algebra.TransformCopy#transform(org.apache.jena.sparql.algebra.op.OpJoin, org.apache.jena.sparql.algebra.Op, org.apache.jena.sparql.algebra.Op)
	 */
	public Op transform(OpJoin join, Op left, Op right) {
		if (left instanceof OpFilter && !(right instanceof OpFilter)) {
			Op ans = OpFilter.filter(((OpFilter) left).getExprs(), OpJoin.create(((OpFilter) left).getSubOp(), right));
			return ans;
		}
		else if (!(left instanceof OpFilter) && right instanceof OpFilter ) {
			return OpFilter.filter(((OpFilter) right).getExprs(), OpJoin.create(left, ((OpFilter) right).getSubOp()));
		}
		else if (left instanceof OpFilter && right instanceof OpFilter) {
			ExprList exp = ExprList.copy(((OpFilter) left).getExprs());
			exp.addAll(((OpFilter) right).getExprs());
			return OpFilter.filter(exp, OpJoin.create(((OpFilter) left).getSubOp(), ((OpFilter) right).getSubOp()));
		}
		else {
			return join;
		}		
	}
	
	public Op transform(OpUnion union, Op left, Op right) {
		if (left instanceof OpFilter && !(right instanceof OpFilter)) {
			return OpFilter.filter(((OpFilter) left).getExprs(), OpUnion.create(((OpFilter) left).getSubOp(), right));
		}
		else if (!(left instanceof OpFilter) && right instanceof OpFilter ) {
			return OpFilter.filter(((OpFilter) right).getExprs(), OpUnion.create(left, ((OpFilter) right).getSubOp()));
		}
		else if (left instanceof OpFilter && right instanceof OpFilter) {
			ExprList exp = ExprList.copy(((OpFilter) left).getExprs());
			exp.addAll(((OpFilter) right).getExprs());
			return OpFilter.filter(exp, OpUnion.create(((OpFilter) left).getSubOp(), ((OpFilter) right).getSubOp()));
		}
		else {
			return union;
		}		
	}
}
