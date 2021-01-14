package transformers;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.ExprTransformer;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FilterTransform extends TransformCopy {
	
	/* (non-Javadoc)
	 * This only applies if the variables in the filter expression are contained in the safe variables in the left Op. Needs revision.
	 * @see org.apache.jena.sparql.algebra.TransformCopy#transform(org.apache.jena.sparql.algebra.op.OpJoin, org.apache.jena.sparql.algebra.Op, org.apache.jena.sparql.algebra.Op)
	 */

	@Override
	public Op transform (OpFilter op, Op subOp) {
		Op ans = op;
		List<Expr> exprs = op.getExprs().getList();
		Expr expr = exprs.get(0);
		if (exprs.size() > 1) {
			for (Expr e : exprs.subList(1, exprs.size())){
				expr = new E_LogicalAnd(expr, e);
			}
		}
		expr = ExprTransformer.transform(new transformers.ExprTransformer(),expr);
		List<Expr> exprList = getConjunctions(expr);
		ans = OpFilter.filter(exprList.get(0),subOp);
		if (exprList.size() > 1) {
			for (Expr e : exprList.subList(1,exprList.size())) {
				ans = OpUnion.create(ans,OpFilter.filter(e,subOp));
			}
		}
		return ans;
	}

	public List<Expr> getConjunctions(Expr expr) {
		List<Expr> ans = new ArrayList<>();
		if (expr instanceof E_LogicalOr) {
			ans.addAll(getConjunctions(((E_LogicalOr) expr).getArg1()));
			ans.addAll(getConjunctions(((E_LogicalOr) expr).getArg2()));
		}
		else {
			ans.add(expr);
		}
		return ans;
	}

	public Op transform(OpJoin join, Op left, Op right) {
		if (left instanceof OpFilter && !(right instanceof OpFilter)) {
			Set<Var> eVars = ((OpFilter) left).getExprs().getVarsMentioned();
			Set<Var> safeVars = Tools.safeVars(left);
			for (Var v : eVars) {
				if (!safeVars.contains(v)) {
					return OpJoin.create(left,right);
				}
			}
			Op ans = OpFilter.filter(((OpFilter) left).getExprs(), OpJoin.create(((OpFilter) left).getSubOp(), right));
			return ans;
		}
		else if (!(left instanceof OpFilter) && right instanceof OpFilter ) {
			Set<Var> eVars = ((OpFilter) right).getExprs().getVarsMentioned();
			Set<Var> safeVars = Tools.safeVars(right);
			for (Var v : eVars) {
				if (!safeVars.contains(v)) {
					return OpJoin.create(left,right);
				}
			}
			return OpFilter.filter(((OpFilter) right).getExprs(), OpJoin.create(left, ((OpFilter) right).getSubOp()));
		}
		else if (left instanceof OpFilter && right instanceof OpFilter) {
			Set<Var> leftEVars = ((OpFilter) left).getExprs().getVarsMentioned();
			Set<Var> leftSafeVars = Tools.safeVars(left);
			for (Var v : leftEVars) {
				if (!leftSafeVars.contains(v)) {
					return OpJoin.create(left,right);
				}
			}
			Set<Var> rightEVars = ((OpFilter) right).getExprs().getVarsMentioned();
			Set<Var> rightSafeVars = Tools.safeVars(right);
			for (Var v : rightEVars) {
				if (!rightSafeVars.contains(v)) {
					return OpJoin.create(left,right);
				}
			}
			ExprList exp = ExprList.copy(((OpFilter) left).getExprs());
			exp.addAll(((OpFilter) right).getExprs());
			return OpFilter.filter(exp, OpJoin.create(((OpFilter) left).getSubOp(), ((OpFilter) right).getSubOp()));
		}
		else {
			return OpJoin.create(left,right);
		}		
	}
	
	public Op transform(OpUnion union, Op left, Op right) {
		if (left instanceof OpFilter && right instanceof OpFilter) {
			ExprList exp = ExprList.copy(((OpFilter) left).getExprs());
			if (exp.equals(((OpFilter) right).getExprs())) {
				return OpFilter.filter(exp, OpUnion.create(((OpFilter) left).getSubOp(), ((OpFilter) right).getSubOp()));
			}
			else {
				return OpUnion.create(left,right);
			}
		}
		else {
			return OpUnion.create(left,right);
		}		
	}

	public Op transform(OpLeftJoin op, Op left, Op right) {
		if (left instanceof OpFilter) {
			Set<Var> eVars = ((OpFilter) left).getExprs().getVarsMentioned();
			Set<Var> safeVars = Tools.safeVars(left);
			for (Var v : eVars) {
				if (!safeVars.contains(v)) {
					return op;
				}
			}
			ExprList exprs = ((OpFilter) left).getExprs();
			if (op.getExprs() != null) {
				exprs.addAll(op.getExprs());
			}
			Op ans = OpFilter.filter(exprs,OpLeftJoin.create(((OpFilter) left).getSubOp(),right,op.getExprs()));
			return ans;
		}
		else {
			return op;
		}
	}
}
