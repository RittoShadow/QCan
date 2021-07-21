package cl.uchile.dcc.visitors;

import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import cl.uchile.dcc.transformers.BGPCollapser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class PathVisitor extends TopDownVisitor {
	Op op;
	List<Var> projectedVars;
	List<Var> namedVars = new ArrayList<>();

	public PathVisitor(Query q) {
		super();
		this.op = Algebra.compile(q);
		this.projectedVars = q.getProjectVars();
		this.namedVars.addAll(q.getProjectVars());
		this.op = visit(this.op);
	}

	public PathVisitor(Op op, List<Var> project) {
		super();
		this.op = op;
		this.projectedVars = project;
		this.namedVars.addAll(project);
		this.op = visit(this.op);
	}
	
	public Op getOp() {
		return this.op;
	}

	public Op visit(OpProject op) {
		Op subOp = op.getSubOp();
		List<Var> copy = new ArrayList<>();
		copy.addAll(namedVars);
		namedVars.addAll(op.getVars());
		Op ans = new OpProject(visit(subOp), op.getVars());
		this.namedVars = copy;
		return ans;
	}
	
	public Op visit(OpFilter op) {
		Op subOp = op.getSubOp();
		Op ans;
		List<Var> copy = new ArrayList<>();
		Set<Var> filterVars = op.getExprs().getVarsMentioned();
		namedVars.addAll(filterVars);
		ans = OpFilter.filter(op.getExprs(), visit(subOp));
		this.namedVars = copy;
		return ans;
	}
	
	public Op visit(OpGraph op) {
		Op subOp = op.getSubOp();
		List<Var> copy = new ArrayList<>();
		copy.addAll(namedVars);
		if (op.getNode().isVariable()) {
			namedVars.add((Var) op.getNode());
		}
		Op ans = new OpGraph(op.getNode(), visit(subOp));
		this.namedVars = copy;
		return ans;
	}
	
	public Op visit(OpGroup op) {
		Op subOp = op.getSubOp();
		Op ans;
		List<Var> copy = new ArrayList<Var>();
		copy.addAll(namedVars);
		List<Var> groupVars = op.getGroupVars().getVars();
		namedVars.addAll(groupVars);
		ans = new OpGroup(visit(subOp), op.getGroupVars(), op.getAggregators());
		this.namedVars = copy;
		return ans;
	}
	
	public Op visit(OpSlice op) {
		Op subOp = op.getSubOp();
		Op ans;
		ans = new OpSlice(visit(subOp), op.getStart(), op.getLength());
		return ans;
	}
	
	public Op visit(OpOrder op) {
		Op subOp = op.getSubOp();
		Op ans;
		List<Var> copy = new ArrayList<Var>();
		copy.addAll(namedVars);
		for (SortCondition lc : op.getConditions()) {
			namedVars.addAll(lc.getExpression().getVarsMentioned());
		}
		ans = new OpOrder(visit(subOp), op.getConditions());
		this.namedVars = copy;
		return ans;
	}
	
	public Op visit(OpTopN op) {
		Op subOp = op.getSubOp();
		List<Var> copy = new ArrayList<Var>();
		copy.addAll(namedVars);
		for (SortCondition lc : op.getConditions()) {
			namedVars.addAll(lc.getExpression().getVarsMentioned());
		}
		Op ans = new OpTopN(visit(subOp), op.getLimit(), op.getConditions());
		return ans;
	}
	
	public Op visit(OpDistinct op) {
		Op subOp = op.getSubOp();
		Op ans = new OpDistinct(visit(subOp));
		return ans;
	}
	
	public Op visit(OpReduced op) {
		Op subOp = op.getSubOp();
		Op ans = OpReduced.create(visit(subOp));
		return ans;
	}
	
	public Op visit(OpList op) {
		Op subOp = op.getSubOp();
		Op ans = new OpList(visit(subOp));
		return ans;
	}
	
	public Op visit(OpLabel op) {
		Op subOp = op.getSubOp();
		Op ans = OpLabel.create(op.getObject(), visit(subOp));
		return ans;
	}
	
	public Op visit(OpPropFunc op) {
		Op subOp = op.getSubOp();
		Op ans = new OpPropFunc(op.getProperty(), op.getSubjectArgs(), op.getObjectArgs(), visit(subOp));
		return ans;
	}
	
	public Op visit(OpProcedure op) {
		return op;
	}
	
	public Op visit(OpService op) {
		Op subOp = op.getSubOp();
		Op ans = new OpService(op.getService(), visit(subOp), op.getServiceElement(), op.getSilent());
		return ans;
	}
	
	public Op visit(OpExtendAssign op) {
		Op subOp = op.getSubOp();
		Op ans;
		List<Var> copy = new ArrayList<Var>();
		copy.addAll(namedVars);
		VarExprList vExprList = op.getVarExprList();
		for (Entry<Var, Expr> entry : vExprList.getExprs().entrySet()) {
			namedVars.add(entry.getKey());
			namedVars.addAll(entry.getValue().getVarsMentioned());
		}
		if (op instanceof OpExtend) {
			ans = OpExtend.create(visit(subOp), vExprList);
		}
		else{
			ans = OpAssign.create(visit(subOp), vExprList);
		}
		this.namedVars = copy;
		return ans;
	}

	public Op visit(OpUnion op) {
		Op ans = op;
		BGPCollapser bc = new BGPCollapser(op, projectedVars, true);
		ans = bc.transform(op, op.getLeft(), op.getRight());
		return ans;
	}
	
	public Op visit(OpJoin op) {
		Op ans = op;
		if (isCQ(op)) {
			BGPCollapser bc = new BGPCollapser(op,projectedVars,true);
			ans = bc.transform(op, op.getLeft(), op.getRight());
		}
		return ans;
	}
	
	public boolean isCQ(OpJoin op) {
		Op leftOp = op.getLeft();
		Op rightOp = op.getRight();
		boolean left = true;
		boolean right = true;
		if (leftOp instanceof OpJoin) {
			left = isCQ((OpJoin) leftOp);
		}
		else if (leftOp instanceof OpTriple || leftOp instanceof OpBGP || leftOp instanceof OpPath) {
			left = true;
		}
		else {
			left = false;
		}
		if (rightOp instanceof OpJoin) {
			right = isCQ((OpJoin) rightOp);
		}
		else if (rightOp instanceof OpTriple || rightOp instanceof OpBGP || rightOp instanceof OpPath) {
			right = true;
		}
		else {
			right = false;
		}
		return left && right;
	}
}
