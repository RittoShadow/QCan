package main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.graph.GraphFactory;
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
import org.apache.jena.sparql.path.P_ReverseLink;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_Shortest;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMoreN;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.PathVisitor;
import org.apache.jena.util.iterator.ExtendedIterator;

public class PathWalker implements PathVisitor {
	
	Graph graph = GraphFactory.createPlainGraph();
	Node epsilon = NodeFactory.createURI("http://example.org/epsilon");
	Stack<Node> nodeStack = new Stack<Node>();
	Stack<Triple> tripleStack = new Stack<Triple>();
	Set<Node> predicates = new HashSet<Node>();

	@Override
	public void visit(P_Link arg0) {
		Node n = NodeFactory.createBlankNode();
		Node b = NodeFactory.createBlankNode();
		Triple t = Triple.create(n, arg0.getNode(), b);
		tripleStack.add(t);
		nodeStack.add(b);
		graph.add(t);
		predicates.add(arg0.getNode());
	}

	@Override
	public void visit(P_ReverseLink arg0) {
		Node n = NodeFactory.createBlankNode();
		Node b = NodeFactory.createBlankNode();
		Node p = NodeFactory.createLiteral("^"+arg0.getNode().toString());
		Triple t = Triple.create(n, p, b);
		tripleStack.add(t);
		nodeStack.add(b);
		graph.add(t);
		predicates.add(p);
	}

	@Override
	public void visit(P_NegPropSet arg0) {
		List<P_Path0> list = arg0.getNodes();
		for (P_Path0 p : list ) {
			p.visit(this);
		}
	}

	@Override
	public void visit(P_Inverse arg0) {
		arg0.getSubPath().visit(this);	
	}

	@Override
	public void visit(P_Mod arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_FixedLength arg0) {
		arg0.getSubPath().visit(this);
		
	}

	@Override
	public void visit(P_Distinct arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_Multi arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_Shortest arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_ZeroOrOne arg0) {
		arg0.getSubPath().visit(this);
		Node n = NodeFactory.createBlankNode();
		Node f = NodeFactory.createBlankNode();
		Triple t = Triple.create(n, epsilon, f);
		graph.add(Triple.create(n, epsilon, tripleStack.pop().getSubject()));
		graph.add(Triple.create(nodeStack.pop(), epsilon, f));
		graph.add(t);
		tripleStack.add(t);
		nodeStack.add(f);
	}

	@Override
	public void visit(P_ZeroOrMore1 arg0) {
		arg0.getSubPath().visit(this);
		Node n = NodeFactory.createBlankNode();
		Node f = NodeFactory.createBlankNode();
		Triple t = Triple.create(n, epsilon, f);
		Node t1 = tripleStack.pop().getSubject();
		Node finalState = nodeStack.pop();
		graph.add(Triple.create(n, epsilon, t1));
		graph.add(Triple.create(finalState, epsilon, f));
		graph.add(Triple.create(finalState, epsilon, t1));
		graph.add(t);
		tripleStack.add(t);
		nodeStack.add(f);
	}

	@Override
	public void visit(P_ZeroOrMoreN arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_OneOrMore1 arg0) {
		arg0.getSubPath().visit(this);
		Node n = NodeFactory.createBlankNode();
		Node f = NodeFactory.createBlankNode();
		Node t1 = tripleStack.pop().getSubject();
		Triple t = Triple.create(n, epsilon, t1);
		Node finalState = nodeStack.pop();
		graph.add(Triple.create(finalState, epsilon, f));
		graph.add(Triple.create(finalState, epsilon, t1));
		graph.add(t);
		tripleStack.add(t);
		nodeStack.add(f);
	}

	@Override
	public void visit(P_OneOrMoreN arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_Alt arg0) {
		Node n = NodeFactory.createBlankNode();
		Node f = NodeFactory.createBlankNode();
		arg0.getLeft().visit(this);
		Triple t1 = Triple.create(n, epsilon, tripleStack.pop().getSubject());
		Node finalState1 = nodeStack.pop();
		graph.add(t1);
		graph.add(Triple.create(finalState1, epsilon, f));
		arg0.getRight().visit(this);
		Triple t2 = Triple.create(n, epsilon, tripleStack.pop().getSubject());
		Node finalState2 = nodeStack.pop();
		graph.add(t2);
		graph.add(Triple.create(finalState2, epsilon, f));
		tripleStack.add(t1);
		nodeStack.add(f);
	}

	@Override
	public void visit(P_Seq arg0) {
		arg0.getLeft().visit(this);
		Node finalState = nodeStack.pop();
		Triple t1 = tripleStack.pop();
		arg0.getRight().visit(this);
		Triple t2 = tripleStack.pop();
		graph.add(Triple.create(finalState, epsilon, t2.getSubject()));
		tripleStack.add(t1);
	}
	
	public void print() {
		ExtendedIterator<Triple> et = GraphUtil.findAll(graph);
		while (et.hasNext()) {
			System.out.println(et.next());
		}
		System.out.println("Start: "+tripleStack.peek().getSubject());
		System.out.println("End: "+nodeStack.peek());
	}
	
	public Node getStartState() {
		if (!tripleStack.empty()) {
			return tripleStack.peek().getSubject();
		}
		else {
			return null;
		}
	}
	
	public Node getEndState() {
		if (!nodeStack.empty()) {
			return nodeStack.peek();
		}
		else {
			return null;
		}
	}

}
