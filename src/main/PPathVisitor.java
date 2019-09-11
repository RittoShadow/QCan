package main;

import java.util.Stack;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.algebra.optimize.TransformExtendCombine;
import org.apache.jena.sparql.algebra.optimize.TransformMergeBGPs;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Distinct;
import org.apache.jena.sparql.path.P_FixedLength;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Mod;
import org.apache.jena.sparql.path.P_Multi;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_OneOrMoreN;
import org.apache.jena.sparql.path.P_Path0;
import org.apache.jena.sparql.path.P_Path1;
import org.apache.jena.sparql.path.P_ReverseLink;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_Shortest;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMoreN;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathVisitor;

public class PPathVisitor implements PathVisitor {
	
	private Stack<Node> nodeStack = new Stack<Node>();
	private Stack<RGraph> rootStack = new Stack<RGraph>();
	private Stack<Op> opStack = new Stack<Op>();
	
	public PPathVisitor(Node subject, Node object) {
		nodeStack.push(object);
		nodeStack.push(subject);
	}

	@Override
	public void visit(P_Link pathNode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(P_ReverseLink pathNode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(P_NegPropSet pathNotOneOf) {
		pathNotOneOf.getNodes();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(P_Inverse inversePath) {
		Node a = nodeStack.pop();
		Node b = nodeStack.pop();
		nodeStack.push(a);
		nodeStack.push(b);
		inversePath.getSubPath().visit(this);
	}

	@Override
	public void visit(P_Mod pathMod) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(P_FixedLength pFixedLength) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(P_Distinct pathDistinct) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(P_Multi pathMulti) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(P_Shortest pathShortest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(P_ZeroOrOne path) {
		opStack.push(new OpPath(new TriplePath(nodeStack.pop(),path,nodeStack.pop())));
	}

	@Override
	public void visit(P_ZeroOrMore1 path) {
		opStack.push(new OpPath(new TriplePath(nodeStack.pop(),path,nodeStack.pop())));
	}

	@Override
	public void visit(P_ZeroOrMoreN path) {
		opStack.push(new OpPath(new TriplePath(nodeStack.pop(),path,nodeStack.pop())));
	}

	@Override
	public void visit(P_OneOrMore1 path) {
		System.out.println(nodeStack);
		System.out.println(opStack);
		opStack.push(new OpPath(new TriplePath(nodeStack.pop(),path,nodeStack.pop())));
	}

	@Override
	public void visit(P_OneOrMoreN path) {

	}

	@Override
	public void visit(P_Alt pathAlt) {
		Path left = pathAlt.getLeft();
		Path right = pathAlt.getRight();
		Node a = nodeStack.pop();
		Node b = nodeStack.pop();
		nodeStack.push(b);
		nodeStack.push(a);
		if (left instanceof P_Path0) {
			System.out.println(pathAlt);
			System.out.println(left);
			System.out.println(nodeStack);
			Triple t = Triple.create(a, ((P_Path0) left).getNode(), b);
			BasicPattern bp = new BasicPattern();
			bp.add(t);
			opStack.add(new OpBGP(bp));
		}
		else if (left instanceof P_Path1) {
			System.out.println(pathAlt);
			System.out.println(left);
			System.out.println(nodeStack);
			if (left instanceof P_Inverse) {
				opStack.push(new OpPath(new TriplePath(b,left,a)));
			}
			else {
				opStack.push(new OpPath(new TriplePath(a,left,b)));
			}
		}
		else {
			left.visit(this);
		}
		
		if (right instanceof P_Path0) {
			System.out.println(pathAlt);
			System.out.println(right);
			System.out.println(nodeStack);
			Triple t = Triple.create(a, ((P_Path0) right).getNode(), b);
			nodeStack.push(b);
			BasicPattern bp = new BasicPattern();
			bp.add(t);
			opStack.add(new OpBGP(bp));
		}
		else if (right instanceof P_Path1) {
			System.out.println(pathAlt);
			System.out.println(right);
			System.out.println(nodeStack);
			if (right instanceof P_Inverse) {
				opStack.push(new OpPath(new TriplePath(b,right,a)));
			}
			else {
				opStack.push(new OpPath(new TriplePath(a,right,b)));
			}
			nodeStack.push(b);
		}
		else {
			right.visit(this);
		}
		opStack.push(OpUnion.create(opStack.pop(), opStack.pop()));
	}

	@Override
	public void visit(P_Seq pathSeq) {
		Path left = pathSeq.getLeft();
		Path right = pathSeq.getRight();
		if (left instanceof P_Path0) {
			System.out.println(pathSeq);
			System.out.println(left);
			System.out.println(nodeStack);
			Node a = nodeStack.pop();
			Node n = NodeFactory.createBlankNode();
			Triple t = Triple.create(a, ((P_Path0) left).getNode(), n);
			nodeStack.push(n);
			BasicPattern bp = new BasicPattern();
			bp.add(t);
			opStack.add(new OpBGP(bp));
			
		}
		else if (left instanceof P_Path1) {
			System.out.println(pathSeq);
			System.out.println(left);
			System.out.println(nodeStack);
			Node a = nodeStack.pop();
			Node n = NodeFactory.createBlankNode();
			if (left instanceof P_Inverse) {
				opStack.add(new OpPath(new TriplePath(n,left,a)));
			}
			else {
				opStack.add(new OpPath(new TriplePath(a,left,n)));
			}
			nodeStack.push(n);
		}
		else {
			left.visit(this);
		}
		if (right instanceof P_Path0) {
			System.out.println(pathSeq);
			System.out.println(right);
			System.out.println(nodeStack);
			Triple t = Triple.create(nodeStack.pop(), ((P_Path0) right).getNode(), nodeStack.pop());
			BasicPattern bp = new BasicPattern();
			bp.add(t);
			opStack.add(new OpBGP(bp));
		}
		else if (right instanceof P_Path1) {
			System.out.println(pathSeq);
			System.out.println(right);
			System.out.println(nodeStack);
			Node a = nodeStack.pop();
			Node b = nodeStack.pop();
			if (right instanceof P_Inverse) {
				opStack.add(new OpPath(new TriplePath(b,right,a)));
			}
			else {
				opStack.add(new OpPath(new TriplePath(a,right,b)));
			}		
		}
		else {
			right.visit(this);
		}
		opStack.push(OpJoin.create(opStack.pop(), opStack.pop()));
	}
	
	public RGraph getGraph() {
		return rootStack.pop();
	}
	
	public Op getOp() {
		Op op = opStack.peek();
		Op op2 = Transformer.transform(new UCQVisitor(), op);
		while (!op.equals(op2)){
			op = op2;
			op2 = Transformer.transform(new UCQVisitor(), op2);
		}
		op2 = Transformer.transform(new FilterTransform(), op2);
		op2 = Transformer.transform(new TransformMergeBGPs(), op2);
		op2 = Transformer.transform(new TransformExtendCombine(), op2);
		op2 = Transformer.transform(new BGPSort(), op2);
		return op2;
	}

}
