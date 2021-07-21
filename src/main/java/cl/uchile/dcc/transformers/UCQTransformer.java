package cl.uchile.dcc.transformers;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import cl.uchile.dcc.op.OpEpsilon;

import java.util.ArrayList;
import java.util.List;

public class UCQTransformer extends TransformCopy{
	
	@Override
    public Op transform(OpJoin join, Op left, Op right) {
        if ((left instanceof OpUnion) && !(right instanceof OpUnion)){ // Case 1: (A_1 UNION A_2) JOIN A_3 = (A_1 JOIN A_3) UNION (A_2 JOIN A_3)
        	OpUnion leftU = (OpUnion) left;
        	List<Op> ops = opsInUnion(leftU);
        	if (ops == null) {
        		return join;
			}
			List<Op> newOps = new ArrayList<>();
			for (Op o : ops) {
				newOps.add(OpJoin.create(o,right));
			}
			Op ans = newOps.get(0);
			for (int i = 1; i < newOps.size(); i++) {
				ans = OpUnion.create(ans,newOps.get(i));
			}
        	return ans;
        }
        else if (!(left instanceof OpUnion) && (right instanceof OpUnion)){ // Case 2: A_1 JOIN (A_2 UNION A_3) = (A_1 JOIN A_2) UNION (A_1 JOIN A_3)
        	OpUnion rightU = (OpUnion) right;
			List<Op> ops = opsInUnion(rightU);
			if (ops == null) {
				return join;
			}
			List<Op> newOps = new ArrayList<>();
			for (Op o : ops) {
				newOps.add(OpJoin.create(left, o));
			}
        	Op ans = newOps.get(0);
			for (int i = 1; i < newOps.size(); i++) {
				ans = OpUnion.create(ans,newOps.get(i));
			}
        	return ans;
        }
        else if (left instanceof OpUnion && right instanceof OpUnion){ // Case 3: (A_1 UNION A_2) JOIN (A_3 UNION A_4) = (A_1 JOIN A_3) UNION (A_1 JOIN A_4) UNION (A_2 JOIN A_3) UNION (A_2 JOIN A_4)
			OpUnion leftU = (OpUnion) left;
			OpUnion rightU = (OpUnion) right;
			List<Op> leftOps = opsInUnion(leftU);
			List<Op> rightOps = opsInUnion(rightU);
			List<Op> newOps = new ArrayList<>();
			for (Op leftOp : leftOps) {
				for (Op rightOp : rightOps) {
					newOps.add(OpJoin.create(leftOp,rightOp));
				}
			}
			Op ans = newOps.get(0);
			for (int i = 1; i < newOps.size(); i++) {
				ans = OpUnion.create(ans,newOps.get(i));
			}
        	return ans;
        }
        else if ((left instanceof OpTriple) && (right instanceof OpTriple)) {
        	BasicPattern bp = new BasicPattern();
        	bp.add(((OpTriple) left).getTriple());
        	bp.add(((OpTriple) right).getTriple());
        	Op ans = new OpBGP(bp);
        	return ans;
        }
        else if ((left instanceof OpTriple) && (right instanceof OpBGP)) {
        	BasicPattern bp = new BasicPattern();
        	bp.addAll(((OpBGP) right).getPattern());
        	bp.add(((OpTriple) left).getTriple());
			return new OpBGP(bp);
        }
        else if ((right instanceof OpTriple) && (left instanceof OpBGP)) {
			BasicPattern bp = new BasicPattern();
        	bp.addAll((((OpBGP) left).getPattern()));
        	bp.add(((OpTriple) right).getTriple());
			return new OpBGP(bp);
        }
        else{
        	return OpJoin.create(left,right);
        }
    }
	
	public Op transform(OpLeftJoin leftJoin, Op left, Op right) {
		if ((left instanceof OpUnion)) { // Case: (A_1 UNION A_2) OPT A_3 = (A_1 OPT A_3) UNION (A_2 OPT A_3)
			List<Op> opsInUnion = opsInUnion(left);
			List<Op> newOps = new ArrayList<>();
			for (Op o : opsInUnion) {
				newOps.add(OpLeftJoin.create(o,right, leftJoin.getExprs()));
			}
			Op ans = newOps.get(0);
			for (int i = 1; i < newOps.size(); i++) {
				ans = OpUnion.create(ans,newOps.get(i));
			}
			return ans;
		}
		else {
			return OpLeftJoin.create(left,right,leftJoin.getExprs());
		}
	}

	public Op transform(OpMinus minus, Op left, Op right) {
		if (left instanceof OpUnion) {
			List<Op> opsInUnion = opsInUnion(left);
			List<Op> newOps = new ArrayList<>();
			for (Op o : opsInUnion) {
				newOps.add(OpMinus.create(o,right));
			}
			Op ans = newOps.get(0);
			for (int i = 1; i < newOps.size(); i++) {
				ans = OpUnion.create(ans,newOps.get(i));
			}
			return ans;
		}
		else {
			return OpMinus.create(left, right);
		}
	}

	public Op transform(OpUnion op, Op left, Op right) {
		if (left instanceof OpPath && right == null) {
			return left;
		}
		else if (right instanceof OpPath && left == null) {
			return right;
		}
		else {
			return OpUnion.create(left, right);
		}
	}
	
	public Op transform(OpSequence op, List<Op> elts) {
		Op ans = null;
		if (elts.isEmpty()) {
			return op;
		}
		for (Op e : elts) {
			List<Op> paths = new ArrayList<>();
			List<Op> basicPatterns = new ArrayList<>();
			if (ans == null) {
				ans = e;
			}
			else {
				if (e instanceof OpTriple && !(e instanceof OpEpsilon)) {
					basicPatterns.add(e);
					BasicPattern bp = new BasicPattern();
					bp.add(((OpTriple) e).getTriple());
					ans = OpJoin.create(ans, new OpBGP(bp));
				}
				else if (e instanceof OpPath) {
					paths.add(e);
				}
				else {
					ans = OpJoin.create(ans, e);
				}
			}
			if (paths.isEmpty()) {

			}
			else {
				if (!basicPatterns.isEmpty()) {
					BasicPattern bp = new BasicPattern();
					for (Op o : basicPatterns) {
						bp.add(((OpTriple) o).getTriple());
					}
				}
				for (Op o : paths) {
					ans = OpJoin.create(ans,o);
				}
			}
		}
		return ans;
	}
	
	public static void main(String[] args){
		String s = "SELECT DISTINCT ?r WHERE { { ?r <p> ?w } UNION { ?r <q> ?y } FILTER(?z < 15)}";
		Query query = QueryFactory.create(s);
		for (Var v : query.getProjectVars()){
			System.out.println(v);
		}
		BGPCollapser bgc = new BGPCollapser(query);
		Op op = Algebra.compile(query);
		op = Transformer.transform(bgc, op);
	}

	public List<Op> opsInUnion(Op op){
		List<Op> ans = new ArrayList<>();
		if (op instanceof OpUnion) {
			Op leftOp = ((OpUnion) op).getLeft();
			Op rightOp = ((OpUnion) op).getRight();
			if (leftOp instanceof OpUnion) {
				ans.addAll(opsInUnion(leftOp));
			}
			else {
				ans.add(leftOp);
			}
			if (rightOp instanceof OpUnion) {
				ans.addAll(opsInUnion(rightOp));
			}
			else {
				ans.add(rightOp);
			}
		}
		else {
			return ans;
		}
		return ans;
	}
}
