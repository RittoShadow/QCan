package cl.uchile.dcc.builder;

import cl.uchile.dcc.main.RGraph;
import cl.uchile.dcc.op.OpWDLeftJoin;
import cl.uchile.dcc.paths.PGraph;
import cl.uchile.dcc.paths.PathTransform;
import cl.uchile.dcc.tools.BGPSort;
import cl.uchile.dcc.tools.Tools;
import cl.uchile.dcc.transformers.*;
import cl.uchile.dcc.visitors.FilterVisitor;
import com.github.jsonldjava.shaded.com.google.common.collect.Sets;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryType;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.optimize.TransformExtendCombine;
import org.apache.jena.sparql.algebra.optimize.TransformMergeBGPs;
import org.apache.jena.sparql.algebra.optimize.TransformSimplify;
import org.apache.jena.sparql.algebra.walker.Walker;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.syntax.Template;

import java.util.*;
import java.util.Map.Entry;

/**
 * This class implements Jena's OpVisitor. It recursively builds an r-graph from a query.
 * @author Jaime
 *
 */
public class RGraphBuilder implements OpVisitor {
	
	private final Stack<RGraph> graphStack = new Stack<>();
	public List<Var> projectionVars;
	public Set<Var> totalVars = new HashSet<>();
	Template template = null;
	private List<String> graphURI = Collections.emptyList();
	private List<String> namedGraphURI = Collections.emptyList();
	private Map<Var,Node> exprMap = new HashMap<>();
	public int nTriples = 0;
	private boolean enableFilter = true;
	private boolean enableOptional = true;
	public boolean isDistinct = false;
	private boolean containsPaths = false;
	private boolean pathNormalisation = false;
	private boolean isWellDesigned = true;
	private int projectedQueries = 0;
	private QueryType queryType = QueryType.SELECT;
	public long rewriteTime = 0;
	public long graphTime = 0;
	public Op op;
	
	public RGraphBuilder(){
		
	}
	
	public RGraphBuilder(Op op) {
		this.op = op;
		OpWalker.walk(this.op, this);
	}
	
	public RGraphBuilder(Query query) {
		this(query,false);
	}

	public RGraphBuilder(Query query, boolean pathNormalisation) {
		this(query,true,pathNormalisation);
	}
	
	public RGraphBuilder(Query query, boolean rewrite, boolean pathNormalisation){
		this.queryType = query.queryType();
		if (query.isConstructType()) {
			template = query.getConstructTemplate();
		}
		this.projectionVars = query.getProjectVars();
		graphURI = query.getGraphURIs();
		namedGraphURI = query.getNamedGraphURIs();
		this.pathNormalisation = pathNormalisation;
		op = Algebra.compile(query);
		if (rewrite) {
			long normalTime = System.nanoTime();
			op = UCQTransformation(op);
			this.rewriteTime = System.nanoTime() - normalTime;
		}
		this.totalVars = varsContainedIn(op);
		this.setEnableFilter(true);
		this.setEnableOptional(true);
		long graphTime = System.nanoTime();
		OpWalker.walk(op, this);
		this.graphTime = System.nanoTime() - graphTime;
	}

	@Override
	public void visit(OpBGP arg0) {
		nTriples += arg0.getPattern().size();
		graphStack.add(new RGraph(arg0.getPattern().getList()));
		for (Triple t : arg0.getPattern().getList()){
			Node s = t.getSubject();
			Node p = t.getPredicate();
			Node o = t.getObject();
			if (s.isVariable()){
				totalVars.add(Var.alloc(s));
			}
			if (p.isVariable()){
				totalVars.add(Var.alloc(p));
			}
			if (o.isVariable()){
				totalVars.add(Var.alloc(o));
			}
		}
	}

	@Override
	public void visit(OpQuadPattern arg0) {
		
	}

	@Override
	public void visit(OpQuadBlock arg0) {
		
	}

	@Override
	public void visit(OpTriple arg0) {
		nTriples += 1;
		graphStack.add(new RGraph(Collections.singletonList(arg0.getTriple())));
		Triple t = arg0.getTriple();
		if (t.getSubject().isVariable()){
			totalVars.add((Var) t.getSubject());
		}
		if (t.getPredicate().isVariable()){
			totalVars.add((Var) t.getPredicate());
		}
		if (t.getObject().isVariable()){
			totalVars.add((Var) t.getObject());
		}
	}

	@Override
	public void visit(OpQuad arg0) {
		
	}


	@Override
	public void visit(OpPath arg0) {
		TriplePath tp = arg0.getTriplePath();
		PathTransform pt = new PathTransform();
		Path path = tp.getPath();
		path = pt.visit(path);
		Op o = pt.getResult(tp);
		if (o instanceof OpJoin) {
			OpPath left = (OpPath) ((OpJoin) o).getLeft();
			OpPath right = (OpPath) ((OpJoin) o).getRight();
			TriplePath leftTP = left.getTriplePath();
			TriplePath rightTP = right.getTriplePath();
			PGraph pLeft = new PGraph(leftTP);
			PGraph pRight = new PGraph(rightTP);
			RGraph leftG = new RGraph(leftTP.getSubject(), leftTP.getObject(), pLeft);
			RGraph rightG = new RGraph(rightTP.getSubject(), rightTP.getObject(), pRight);
//			RGraph leftG = new RGraph(leftTP.getSubject(), leftTP.getObject(), leftTP.getPath());
//			RGraph rightG = new RGraph(rightTP.getSubject(), rightTP.getObject(), rightTP.getPath());
			leftG.join(rightG);
			graphStack.add(leftG);
		}
		else {
			TriplePath tp0 = ((OpPath) o).getTriplePath();
			PGraph p = new PGraph(tp0);
			RGraph rg = new RGraph(tp0.getSubject(), tp0.getObject(), p);
//			RGraph rg = new RGraph(tp0.getSubject(), tp0.getObject(), tp0.getPath());
			graphStack.add(rg);		
		}
		this.containsPaths = true;
	}

	@Override
	public void visit(OpFind opFind) {

	}

	@Override
	public void visit(OpTable arg0) {
		Table t = arg0.getTable();
		RGraph table = RGraph.table(t);
		graphStack.add(table);		
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
		FilterVisitor fv = new FilterVisitor();
		List<Expr> exprs = new ArrayList<>(arg0.getExprs().getList());
		if (arg0.getSubOp() instanceof OpLeftJoin) {
			ExprList eList = ((OpLeftJoin) arg0.getSubOp()).getExprs();
			if (eList != null) {
				exprs.addAll(eList.getList());
			}
		}
		Expr expr = exprs.get(0);
//		if (expr instanceof E_NotExists) {
//			Op right = OpJoin.create(((E_NotExists) expr).getGraphPattern(), arg0.getSubOp());
//			RGraphBuilder rgb = new RGraphBuilder(right);
//			OpWalker.walk(right, rgb);
//			RGraph e = rgb.getResult();
//			RGraph e1 = graphStack.pop();
//			e1.minus(e);
//			graphStack.add(e1);
//			return;
//		}
		if (exprs.size() > 1) {
			for (Expr e : exprs.subList(1, exprs.size())){
				expr = new E_LogicalAnd(expr, e);
			}
		}
		totalVars.addAll(expr.getVarsMentioned());
        Walker.walk(expr,fv);
		if (enableFilter){
			graphStack.peek().filter(fv.getGraph());
		}
		else{
			throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		}
	}

	@Override
	public void visit(OpGraph arg0) {
		graphStack.peek().graphOp(arg0.getNode());
		if (arg0.getNode().isVariable()) {
			totalVars.add(Var.alloc(arg0.getNode()));
		}
	}

	@Override
	public void visit(OpService arg0) {
		graphStack.peek().service(arg0.getService(), arg0.getSilent());	
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
		VarExprList ve = arg0.getVarExprList();
		Map<Var,Expr> map = ve.getExprs();
		for (Map.Entry<Var, Expr> m : map.entrySet()) {
			totalVars.add(m.getKey());
			totalVars.addAll(m.getValue().getVarsMentioned());
			FilterVisitor bv = new FilterVisitor(m.getKey());
			Walker.walk(m.getValue(),bv);
			graphStack.peek().bind(bv.getGraph());
		}
	}

	@Override
	public void visit(OpJoin arg0) {
		RGraph e1, e2;
		e2 = graphStack.pop();
		e1 = graphStack.pop();
		e1.join(e2);
		graphStack.add(e1);		
	}

	public void visit(OpWDLeftJoin arg0) {
		RGraph e1;
		List<Op> graphs = arg0.getRightOps();
		for (Op op : graphs) {

		}
		System.out.println(arg0);
	}

	@Override
	public void visit(OpLeftJoin arg0) {
		RGraph e1, e2;
		if (enableOptional){
			e2 = graphStack.pop();
			if (arg0.getExprs() != null) {
				ExprList exprs = arg0.getExprs();
				Expr expr = exprs.get(0);
				if (exprs.size() > 1) {
					for (Expr e : exprs.subList(1, exprs.size())){
						expr = new E_LogicalAnd(expr, e);
					}
				}
				FilterVisitor fv = new FilterVisitor();
				totalVars.addAll(expr.getVarsMentioned());
				Walker.walk(expr,fv);
				e2.filter(fv.getGraph());
			}
			e1 = graphStack.pop();
			e1.optional(e2);
			graphStack.add(e1);
		}
		else{
			throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		}
		
	}

	@Override
	public void visit(OpUnion arg0) {
		RGraph e1, e2;
		e1 = graphStack.pop();
		e2 = graphStack.pop();
		e2.union(e1);
		graphStack.add(e2);	
	}

	@Override
	public void visit(OpDiff arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpMinus arg0) {
		RGraph e1, e2;
		Set<Var> leftVars = varsContainedIn(arg0.getLeft());
		Set<Var> rightVars = varsContainedIn(arg0.getRight());
		boolean shared = false;
		for (Var v : rightVars) {
			if (leftVars.contains(v)) {
				shared = true;
				break;
			}
		}
		e2 = graphStack.pop();
		e1 = graphStack.pop();
		if (shared) {
			e1.minus(e2);
		}
		graphStack.add(e1);
	}

	@Override
	public void visit(OpConditional arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpSequence arg0) {
		List<Op> ops = arg0.getElements();
		RGraph r = graphStack.pop();
		for (int i = 1; i < ops.size(); i++) {
			r.join(graphStack.pop());
		}
		graphStack.add(r);	
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
		List<Expr> exprs = new ArrayList<>();
		List<Integer> dir = new ArrayList<>();
		for (SortCondition c : cond){
			exprs.add(c.getExpression());
			dir.add(c.getDirection() == -1 ? -1 : 1);
		}
		graphStack.peek().orderBy(exprs, dir);
	}

	@Override
	public void visit(OpProject arg0) {
		graphStack.peek().project(arg0.getVars());
	}

	@Override
	public void visit(OpReduced arg0) {
		graphStack.peek().setReducedNode(true);
	}

	@Override
	public void visit(OpDistinct arg0) {
		isDistinct = true;
		graphStack.peek().setDistinctNode(true);
	}

	@Override
	public void visit(OpSlice arg0) {
		long offset = arg0.getStart() < 0 ? 0 : arg0.getStart();
		long limit = arg0.getLength();
		graphStack.peek().slice((int)offset, (int)limit);
	}

	@Override
	public void visit(OpGroup arg0) {
		RGraph r = RGraph.group(arg0);
		List<ExprAggregator> agg = arg0.getAggregators();
		List<RGraph> rGraphs = new ArrayList<>();
		for (ExprAggregator a : agg) {
			FilterVisitor fv = new FilterVisitor();
			Walker.walk(a,fv);
			rGraphs.add(fv.getGraph());
			exprMap.put(a.getVar(),fv.getGraph().root);
		}
		RGraph r0 = graphStack.peek();
		r0.aggregation(r,rGraphs);
		r0.groupBy(r);	
	}

	@Override
	public void visit(OpTopN arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}
	
	public void setEnableFilter(boolean b){
		this.enableFilter = b;
	}
	
	public void setEnableOptional(boolean b){
		this.enableOptional = b;
	}
	
	public RGraph getResult(){
		if (!graphStack.peek().isProjection()){
			if (this.queryType.equals(QueryType.ASK)){
				graphStack.peek().ask();
			}
			else if (this.queryType.equals(QueryType.CONSTRUCT)){
				graphStack.peek().construct(template);
			}
			else if (this.queryType.equals(QueryType.DESCRIBE)){
				graphStack.peek().describe();
			}
			if (this.queryType.equals(QueryType.SELECT)) {
				if (projectedQueries == 0) {
					projectionVars = new ArrayList<>();
					projectionVars.addAll(totalVars);
				}
				if (projectionVars != null) {
					graphStack.peek().project(projectionVars);
				}
			}
		}
		else {
			if (projectionVars != null) {
				graphStack.peek().project(projectionVars);
			}
		}
		if (!this.graphURI.isEmpty()){
			graphStack.peek().fromGraph(graphURI);
		}
		if (!this.namedGraphURI.isEmpty()){
			graphStack.peek().fromNamedGraph(namedGraphURI);
		}
		graphStack.peek().containsPaths = this.containsPaths;
		return graphStack.peek();
	}

//	public Op transitiveClosure(Op op) {
//		if (op instanceof OpBGP) {
//
//		}
//		else if (op instanceof OpSequence) {
//			List<Op> existingOps = new ArrayList<Op>();
//			List<Op> newOps = new ArrayList<Op>();
//			List<HashSet<Node>> partitions = new ArrayList<>();
//			Map<Node,Set<Path>> partitionsPaths = new HashMap<>();
//			for (Op o : ((OpSequence) op).getElements()) {
//				existingOps.add(o);
//				if (o instanceof OpPath) {
//					TriplePath tp = ((OpPath) o).getTriplePath();
//					Set<Path> paths = new HashSet<>();
//					Node sub = tp.getSubject();
//					Node obj = tp.getObject();
//					if (partitionsPaths.containsKey(sub)) {
//						paths.addAll(partitionsPaths.get(sub));
//						partitionsPaths.put(sub, paths);
//					}
//					else {
//						partitionsPaths.put(sub, paths);
//					}
//					if (PropertyPathFeatureCounter.minLength(tp.getPath()) == 0) {
//						if (sub.isVariable() && obj.isVariable()) {
//							paths.addAll(lengthZeroPaths(tp.getPath()));
//							partitionsPaths.put(sub, paths);
//							if (partitions.isEmpty()) {
//								HashSet<Node> newPart = new HashSet<>();
//								newPart.add(sub);
//								newPart.add(obj);
//								partitions.add(newPart);
//							}
//							else {
//								for (HashSet<Node> part : partitions) {
//									if (part.contains(sub)) {
//										part.add(obj);
//										break;
//									}
//									else if (part.contains(obj)) {
//										part.add(sub);
//										break;
//									}
//									else {
//										HashSet<Node> newPart = new HashSet<Node>();
//										newPart.add(sub);
//										newPart.add(obj);
//										partitions.add(newPart);
//									}
//								}
//							}
//						}
//					}
//				}
//				else if (o instanceof OpBGP) {
//
//				}
//				else if (o instanceof OpTriple) {
//
//				}
//			}
////			System.out.println(partitions);
//			for (HashSet<Node> partition : partitions) {
//				for (Node sub : partition) {
//					for (Node obj : partition) {
//						if (sub.equals(obj) || !partitionsPaths.containsKey(sub)) {
//							continue;
//						}
//						for (Path path : partitionsPaths.get(sub)) {
//							OpPath opPath1 = new OpPath(new TriplePath(sub, path, obj));
//							OpPath opPath2 = new OpPath(new TriplePath(obj, path, sub));
//							if (!existingOps.contains(opPath1)){
//								newOps.add(opPath1);
//							}
//							if (!existingOps.contains(opPath2)) {
//								newOps.add(opPath2);
//							}
//						}
//					}
//				}
//			}
////			System.out.println(newOps);
//		}
//		else if (op instanceof OpUnion) {
//
//		}
//		else {
//			if (op instanceof Op1) {
//				return transitiveClosure(((Op1) op).getSubOp());
//			}
//			return op;
//		}
//		return op;
//	}
	
//	public List<Path> lengthZeroPaths(Path path){
//		ArrayList<Path> ans = new ArrayList<>();
//		int length = PropertyPathFeatureCounter.minLength(path);
//		if (length == 0) {
//			if (path instanceof P_ZeroOrMore1 || path instanceof P_Seq) {
//				ans.add(path);
//			}
//			else if (path instanceof P_Alt) {
//				List<Path> left = lengthZeroPaths(((P_Alt) path).getLeft());
//				List<Path> right = lengthZeroPaths(((P_Alt) path).getRight());
//				ans.addAll(left);
//				ans.addAll(right);
//			}
//			return ans;
//		}
//		else {
//			return ans;
//		}
//	}
//
//	public Op uC2RPQCollapse(Op op) {
//		Op op1 = op;
//		Op op2 = op;
//		if (pathNormalisation) {
//			do {
//				op1 = op2;
//				PathVisitor tdv = new PathVisitor(op1, this.projectionVars);
//				op2 = tdv.getOp();
//			}
//			while (!op1.equals(op2));
//		}
//		return op1;
//	}
	
	public Op UCQNormalisation(Op op) {
		Op op1 = op;
		Op op2 = op;
		do {
			op1 = op2;
			op2 = Transformer.transform(new FilterTransform(), op2);
			op2 = Transformer.transform(new UCQTransformer(), op2);
			op2 = Transformer.transform(new TransformPath(), op2);
			op2 = Transformer.transform(new NotOneOfTransform(), op2);
		}
		while (!op1.equals(op2));
		return op2;
	}

	public Set<Var> varsContainedIn(Op op) {
		Set<Var> ans = new HashSet<>();
		if (op instanceof OpTriple) {
			Triple t = ((OpTriple) op).getTriple();
			if (t.getSubject().isVariable()) {
				if (!t.getSubject().getName().startsWith("?")) {
					ans.add(Var.alloc(t.getSubject().getName()));
				}
			}
			if (t.getPredicate().isVariable()) {
				if (!t.getPredicate().getName().startsWith("?")) {
					ans.add(Var.alloc(t.getPredicate().getName()));
				}
			}
			if (t.getObject().isVariable()) {
				if (!t.getObject().getName().startsWith("?")) {
					ans.add(Var.alloc(t.getObject().getName()));
				}
			}
		}
		else if (op instanceof OpBGP) {
			for (Triple t : ((OpBGP) op).getPattern().getList()) {
				if (t.getSubject().isVariable()) {
					if (!t.getSubject().getName().startsWith("?")) {
						ans.add(Var.alloc(t.getSubject().getName()));
					}
				}
				if (t.getPredicate().isVariable()) {
					if (!t.getPredicate().getName().startsWith("?")) {
						ans.add(Var.alloc(t.getPredicate().getName()));
					}
				}
				if (t.getObject().isVariable()) {
					if (!t.getObject().getName().startsWith("?")) {
						ans.add(Var.alloc(t.getObject().getName()));
					}
				}
			}
		}
		else if (op instanceof OpPath) {
			TriplePath tp = ((OpPath) op).getTriplePath();
			if (tp.getSubject().isVariable()) {
				ans.add(Var.alloc(tp.getSubject().getName()));
			}
			if (tp.getObject().isVariable()) {
				ans.add(Var.alloc(tp.getObject().getName()));
			}
		}
		else if (op instanceof OpGraph) {
			Node n = ((OpGraph) op).getNode();
			if (n.isVariable()) {
				ans.add(Var.alloc(n.getName()));
			}
			ans.addAll(varsContainedIn(((OpGraph) op).getSubOp()));
		}
		else if (op instanceof OpFilter) {
			ExprList eList = ((OpFilter) op).getExprs();
			ans.addAll(eList.getVarsMentioned());
			ans.addAll(varsContainedIn(((OpFilter) op).getSubOp()));
		}
		else if (op instanceof OpExtend) {
			Map<Var,Expr> map = ((OpExtend) op).getVarExprList().getExprs();
			for (Entry<Var, Expr> entry : map.entrySet()) {
				ans.add(entry.getKey());
				ans.addAll(entry.getValue().getVarsMentioned());
			}
			ans.addAll(varsContainedIn(((OpExtend) op).getSubOp()));
		}
		else if (op instanceof OpAssign) {
			Map<Var,Expr> map = ((OpAssign) op).getVarExprList().getExprs();
			for (Entry<Var, Expr> entry : map.entrySet()) {
				ans.add(entry.getKey());
				ans.addAll(entry.getValue().getVarsMentioned());
			}
			ans.addAll(varsContainedIn(((OpAssign) op).getSubOp()));
		}
		else if (op instanceof OpTable) {
			ans.addAll(((OpTable) op).getTable().getVars());
		}
		else if (op instanceof Op1) {
			Op subOp = ((Op1) op).getSubOp();
			ans.addAll(varsContainedIn(subOp));
		}
		else if (op instanceof Op2) {
			Op left = ((Op2) op).getLeft();
			Op right = ((Op2) op).getRight();
			ans.addAll(varsContainedIn(left));
			ans.addAll(varsContainedIn(right));
		}
		else if (op instanceof OpN) {
			for (Op o : ((OpN) op).getElements()) {
				ans.addAll(varsContainedIn(o));
			}
		}
		return ans;
	}

	public boolean isWellDesigned(Op op) {
		final boolean[] ans = {true};
		OpWalker.walk(op, new OpVisitor() {
			@Override
			public void visit(OpBGP opBGP) {

			}

			@Override
			public void visit(OpQuadPattern opQuadPattern) {

			}

			@Override
			public void visit(OpQuadBlock opQuadBlock) {

			}

			@Override
			public void visit(OpTriple opTriple) {

			}

			@Override
			public void visit(OpQuad opQuad) {

			}

			@Override
			public void visit(OpPath opPath) {

			}

			@Override
			public void visit(OpFind opFind) {

			}

			@Override
			public void visit(OpTable opTable) {

			}

			@Override
			public void visit(OpNull opNull) {

			}

			@Override
			public void visit(OpProcedure opProcedure) {

			}

			@Override
			public void visit(OpPropFunc opPropFunc) {

			}

			@Override
			public void visit(OpFilter opFilter) {
				Set<Var> subVars = Tools.varsContainedIn(opFilter.getSubOp());
				if (opFilter.getExprs() != null) {
					if (opFilter.getExprs().getVarsMentioned() != null) {
						Set<Var> filterVars = opFilter.getExprs().getVarsMentioned();
						for (Var var : filterVars) {
							if (!subVars.contains(var)) {
								ans[0] = false;
								break;
							}
						}
					}
				}
			}

			@Override
			public void visit(OpGraph opGraph) {

			}

			@Override
			public void visit(OpService opService) {

			}

			@Override
			public void visit(OpDatasetNames opDatasetNames) {

			}

			@Override
			public void visit(OpLabel opLabel) {

			}

			@Override
			public void visit(OpAssign opAssign) {

			}

			@Override
			public void visit(OpExtend opExtend) {

			}

			@Override
			public void visit(OpJoin opJoin) {

			}

			@Override
			public void visit(OpLeftJoin opLeftJoin) {
				Set<Var> outerVars = Tools.varsContainedInExcept(op,opLeftJoin);
				Set<Var> leftVars = Tools.varsContainedIn(opLeftJoin.getLeft());
				Set<Var> rightVars = Tools.varsContainedIn(opLeftJoin.getRight());
				Set<Var> intersection = Sets.intersection(outerVars,rightVars);
				for (Var var : intersection) {
					if (!leftVars.contains(var)) {
						ans[0] = false;
						break;
					}
				}
			}

			@Override
			public void visit(OpUnion opUnion) {
				ans[0] = false;
			}

			@Override
			public void visit(OpDiff opDiff) {

			}

			@Override
			public void visit(OpMinus opMinus) {

			}

			@Override
			public void visit(OpConditional opConditional) {

			}

			@Override
			public void visit(OpSequence opSequence) {

			}

			@Override
			public void visit(OpDisjunction opDisjunction) {

			}

			@Override
			public void visit(OpList opList) {

			}

			@Override
			public void visit(OpOrder opOrder) {

			}

			@Override
			public void visit(OpProject opProject) {

			}

			@Override
			public void visit(OpReduced opReduced) {

			}

			@Override
			public void visit(OpDistinct opDistinct) {

			}

			@Override
			public void visit(OpSlice opSlice) {

			}

			@Override
			public void visit(OpGroup opGroup) {

			}

			@Override
			public void visit(OpTopN opTopN) {

			}
		});
		return ans[0];
	}

	public Op UCQTransformation(Op op) {
//		op1 = transitiveClosure(op1);
		Op op2 = op;
		op2 = UCQNormalisation(op2);
//		op2 = uC2RPQCollapse(op2);
//		op2 = Transformer.transform(new BGPCollapser(op2, this.projectionVars, true), op2); // transform all sequences
//		op2 = Transformer.transform(new BGPCollapser(op2,this.projectionVars,false), op2); // transform BGPs
		BranchRenamer br = new BranchRenamer();
		op2 = br.visit(op2);
		op2 = Transformer.transform(new LocalVarRenamer(this.projectionVars), op2);
		op2 = Transformer.transform(new WellDesignedTransformer(op2),op2);
		op2 = Transformer.transform(new TransformSimplify(), op2);
		op2 = Transformer.transform(new TransformMergeBGPs(), op2);
		op2 = Transformer.transform(new TransformExtendCombine(), op2);
		op2 = Transformer.transform(new BGPSort(), op2);
		return op2;
	}

}
