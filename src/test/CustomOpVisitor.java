package test;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.OpVisitor;
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

public class CustomOpVisitor implements OpVisitor {
	
	private Stack<ExpandedGraph> graphStack = new Stack<ExpandedGraph>();
	private Stack<ExpandedGraph> unionStack = new Stack<ExpandedGraph>();
	private Stack<ExpandedGraph> joinStack = new Stack<ExpandedGraph>();
	private List<Var> projectionVars;
	private int bgpId = 0;
	private int unionId = 0;
	
	public CustomOpVisitor(){
		
	}
	
	public CustomOpVisitor(List<Var> pVars){
		this.projectionVars = pVars;
	}

	@Override
	public void visit(OpBGP arg0) {
		graphStack.add(new ExpandedGraph(arg0.getPattern().getList(), bgpId++));
	}

	@Override
	public void visit(OpQuadPattern arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpQuadBlock arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpTriple arg0) {
		System.out.println(arg0.getTriple());
		
	}

	@Override
	public void visit(OpQuad arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpPath arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpTable arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpNull arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpProcedure arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpPropFunc arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpFilter arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpGraph arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpService arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpDatasetNames arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpLabel arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpAssign arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpExtend arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpJoin arg0) {
		ExpandedGraph e1, e2;
		if (arg0.getLeft() instanceof OpBGP){
			e1 = new ExpandedGraph(((OpBGP)arg0.getLeft()).getPattern().getList(), bgpId++);
		}
		else if (arg0.getLeft() instanceof OpUnion){
			e1 = unionStack.pop();
		}
		else{
			e1 = joinStack.pop();
		}
		if (arg0.getRight() instanceof OpBGP){
			e2 = new ExpandedGraph(((OpBGP)arg0.getRight()).getPattern().getList(), bgpId++);
		}
		else if (arg0.getRight() instanceof OpUnion){
			e2 = unionStack.pop();
		}
		else{
			e2 = joinStack.pop();
		}
		e2.join(e1,bgpId++);
		joinStack.add(e2);
		graphStack.add(e2);		
	}

	@Override
	public void visit(OpLeftJoin arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpUnion arg0) {
		ExpandedGraph e1, e2;
		if (arg0.getLeft() instanceof OpBGP){
			e1 = new ExpandedGraph(((OpBGP)arg0.getLeft()).getPattern().getList(), bgpId++);
		}
		else if (arg0.getLeft() instanceof OpUnion){
			e1 = unionStack.pop();
		}
		else{
			e1 = joinStack.pop();
		}
		if (arg0.getRight() instanceof OpBGP){
			e2 = new ExpandedGraph(((OpBGP)arg0.getRight()).getPattern().getList(), bgpId++);
		}
		else if (arg0.getRight() instanceof OpUnion){
			e2 = unionStack.pop();
		}
		else{
			e2 = joinStack.pop();
		}
		e2.union(e1, unionId++);
		unionStack.add(e2);
		graphStack.add(e2);	
	}

	@Override
	public void visit(OpDiff arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpMinus arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpConditional arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpSequence arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpDisjunction arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpList arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpOrder arg0) {
		List<SortCondition> cond = arg0.getConditions();
		List<Var> vars = new ArrayList<Var>();
		List<Integer> dir = new ArrayList<Integer>();
		for (SortCondition c : cond){
			if (c.getExpression().isVariable()){
				vars.add(c.getExpression().asVar());
				dir.add(c.getDirection() == -1 ? -1 : 1);
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
			}
		}
		if (!graphStack.peek().containsProjection()){
			graphStack.peek().project(projectionVars);
		}
		graphStack.peek().orderBy(vars, dir);
	}

	@Override
	public void visit(OpProject arg0) {
		graphStack.peek().project(arg0.getVars());
	}

	@Override
	public void visit(OpReduced arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpDistinct arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpSlice arg0) {
		long offset = arg0.getStart() < 0 ? 0 : arg0.getStart();
		long limit = arg0.getLength();
		if (!graphStack.peek().containsProjection()){
			graphStack.peek().project(projectionVars);
		}
		graphStack.peek().slice((int)offset, (int)limit);
		
	}

	@Override
	public void visit(OpGroup arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpTopN arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}
	
	public ExpandedGraph getResult(){
		if (!graphStack.peek().containsProjection()){
			graphStack.peek().project(projectionVars);
		}
		return graphStack.peek();
	}

}
