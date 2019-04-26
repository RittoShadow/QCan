package main;

import java.util.Collections;
import java.util.Stack;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprFunction0;
import org.apache.jena.sparql.expr.ExprFunction1;
import org.apache.jena.sparql.expr.ExprFunction2;
import org.apache.jena.sparql.expr.ExprFunction3;
import org.apache.jena.sparql.expr.ExprFunctionN;
import org.apache.jena.sparql.expr.ExprFunctionOp;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.ExprVisitor;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * A class based on Jena's ExprVisitor that creates an r-graph that represents a FILTER expression.
 * @author Jaime
 *
 */
public class FilterVisitor implements ExprVisitor {
	
	private Stack<Node> nodeStack = new Stack<Node>();
	private RGraph filterGraph;

	public FilterVisitor(){
		filterGraph = new RGraph(NodeFactory.createBlankNode(), GraphFactory.createPlainGraph(), Collections.<Var> emptyList());
	}
	
	@Override
	public void visit(ExprFunction0 func) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(ExprFunction1 func) {
		Node arg = nodeStack.pop();
		if (func.getOpName().equals("!")){
			nodeStack.add(filterGraph.filterNot(arg));
		}
		else{
			nodeStack.add(filterGraph.filterFunction(func.getOpName(), arg));
		}
	}

	@Override
	public void visit(ExprFunction2 func) {
		Node arg2 = nodeStack.pop();
		Node arg1 = nodeStack.pop();
		if (func.getOpName().equals("&&")){
			nodeStack.add(filterGraph.filterAnd(arg1, arg2));
		}
		else if (func.getOpName().equals("||")){
			nodeStack.add(filterGraph.filterOr(arg1, arg2));
		}
		else{
			nodeStack.add(filterGraph.filterFunction(func.getOpName(), arg1, arg2));
		}
	}

	@Override
	public void visit(ExprFunction3 func) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(ExprFunctionN func) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(ExprFunctionOp funcOp) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(NodeValue nv) {
		nodeStack.add(nv.asNode());
	}

	@Override
	public void visit(ExprVar nv) {
		nodeStack.add(NodeFactory.createBlankNode(nv.getVarName()));	
	}

	@Override
	public void visit(ExprAggregator eAgg) {
		// TODO Auto-generated method stub
		
	}
	
	public RGraph getGraph(){
		filterGraph.root = nodeStack.peek();
		return this.filterGraph;
	}

}
