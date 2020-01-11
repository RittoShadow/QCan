package main;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpAssign;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpDatasetNames;
import org.apache.jena.sparql.algebra.op.OpDiff;
import org.apache.jena.sparql.algebra.op.OpDisjunction;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLabel;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpList;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpNull;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpProcedure;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpPropFunc;
import org.apache.jena.sparql.algebra.op.OpQuad;
import org.apache.jena.sparql.algebra.op.OpQuadBlock;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.algebra.op.OpReduced;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.op.OpTopN;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprList;

public class WellDesignedOptional implements OpVisitor {
	
	Set<Node> varsMentioned = new HashSet<Node>();
	
	public Op transform(OpLeftJoin leftjoin, Op left, Op right) {
		System.out.println(leftjoin.getExprs());
		System.out.println(left);
		System.out.println(right);
		System.out.println();
		return right;
	}
	
	public static void main(String[] args) {
		String s = "SELECT * WHERE { ?s <http://ex.org/p> ?a . ?s <http://ex.org/u> ?d . OPTIONAL { ?s <http://ex.org/q> ?b . ?s <http://ex.org/r> ?c . } OPTIONAL { ?s <http://ex.org/s> ?e . ?s <http://ex.org/t> ?f . } }";
		Op op = Algebra.compile(QueryFactory.create(s));
		System.out.println(op);
		OpWalker.walk(op, new WellDesignedOptional());
	}

	@Override
	public void visit(OpBGP arg0) {
		for (Triple t : arg0.getPattern().getList()) {
			if (t.getSubject().isVariable()) {
				varsMentioned.add(t.getSubject());
			}
			if (t.getPredicate().isVariable()) {
				varsMentioned.add(t.getPredicate());
			}
			if (t.getObject().isVariable()) {
				varsMentioned.add(t.getObject());
			}
		}
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpQuadPattern arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpQuadBlock arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpTriple arg0) {
		Triple t = arg0.getTriple();
		if (t.getSubject().isVariable()) {
			varsMentioned.add(t.getSubject());
		}
		if (t.getPredicate().isVariable()) {
			varsMentioned.add(t.getPredicate());
		}
		if (t.getObject().isVariable()) {
			varsMentioned.add(t.getObject());
		}
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpQuad arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpPath arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpTable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpNull arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpProcedure arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpPropFunc arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpFilter arg0) {
		ExprList exprs = arg0.getExprs();
		varsMentioned.addAll(exprs.getVarsMentioned());
		
	}

	@Override
	public void visit(OpGraph arg0) {
		if (arg0.getNode().isVariable()) {
			varsMentioned.add(arg0.getNode());
		}
	}

	@Override
	public void visit(OpService arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpDatasetNames arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpLabel arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpAssign arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpExtend arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpJoin arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpLeftJoin arg0) {
		System.out.println(arg0);
		System.out.println(varsMentioned);
		System.out.println();
	}

	@Override
	public void visit(OpUnion arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpDiff arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpMinus arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpConditional arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpSequence arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpDisjunction arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpList arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpOrder arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpProject arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpReduced arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpDistinct arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpSlice arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpGroup arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpTopN arg0) {
		// TODO Auto-generated method stub
		
	}
}
