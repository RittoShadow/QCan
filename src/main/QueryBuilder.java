package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.TableFactory;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.E_Add;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.E_GreaterThan;
import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;
import org.apache.jena.sparql.expr.E_IsBlank;
import org.apache.jena.sparql.expr.E_Lang;
import org.apache.jena.sparql.expr.E_LangMatches;
import org.apache.jena.sparql.expr.E_LessThan;
import org.apache.jena.sparql.expr.E_LessThanOrEqual;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.E_LogicalNot;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_Multiply;
import org.apache.jena.sparql.expr.E_NotEquals;
import org.apache.jena.sparql.expr.E_Subtract;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * This class builds a query out of an r-graph.
 * @author Jaime
 *
 */
public class QueryBuilder {
	final String URI = "http://example.org/";
	private final Node typeNode = NodeFactory.createURI(this.URI+"type");
	private final Node tpNode = NodeFactory.createURI(this.URI+"TP");
	private final Node argNode = NodeFactory.createURI(this.URI+"arg");
	private final Node subNode = NodeFactory.createURI(this.URI+"subject");
	private final Node preNode = NodeFactory.createURI(this.URI+"predicate");
	private final Node objNode = NodeFactory.createURI(this.URI+"object");
	private final Node joinNode = NodeFactory.createURI(this.URI+"join");
	private final Node unionNode = NodeFactory.createURI(this.URI+"union");
	private final Node projectNode = NodeFactory.createURI(this.URI+"projection");
	private final Node opNode = NodeFactory.createURI(this.URI+"OP");
	private final Node limitNode = NodeFactory.createURI(this.URI+"limit");
	private final Node offsetNode = NodeFactory.createURI(this.URI+"offset");
	private final Node orderByNode = NodeFactory.createURI(this.URI+"orderBy");
	private final Node varNode = NodeFactory.createURI(this.URI+"var");
	private final Node orderNode = NodeFactory.createURI(this.URI+"order");
	private final Node valueNode = NodeFactory.createURI(this.URI+"value");
	private final Node dirNode = NodeFactory.createURI(this.URI+"direction");
	private final Node modNode = NodeFactory.createURI(this.URI+"modifier");
	private final Node filterNode = NodeFactory.createURI(this.URI+"filter");
	private final Node functionNode = NodeFactory.createURI(this.URI+"function");
	private final Node andNode = NodeFactory.createURI(this.URI+"and");
	private final Node orNode = NodeFactory.createURI(this.URI+"or");
	private final Node notNode = NodeFactory.createURI(this.URI+"not");
	private final Node optionalNode = NodeFactory.createURI(this.URI+"optional");
	private final Node leftNode = NodeFactory.createURI(this.URI+"left");
	private final Node rightNode = NodeFactory.createURI(this.URI+"right");
	private final Node fromNode = NodeFactory.createURI(this.URI+"from");
	private final Node fromNamedNode = NodeFactory.createURI(this.URI+"fromNamed");
	private final Node graphNode = NodeFactory.createURI(this.URI+"graph");
	private final Node distinctNode = NodeFactory.createURI(this.URI+"distinct");
	private final Node bindNode = NodeFactory.createURI(this.URI+"bind");
	private final Node tableNode = NodeFactory.createURI(this.URI+"table");
	private Graph graph;
	private Op op;
	private boolean isDistinct;
	
	public QueryBuilder(RGraph e){
		this.graph = e.graph;
		Node project = GraphUtil.listSubjects(this.graph, typeNode, projectNode).next();
		this.isDistinct = this.graph.contains(project, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
	}
	
	public Expr filterOperatorToString(Node n, Expr... exprs){
		if (n.equals(NodeFactory.createURI(this.URI+"eq"))){
			return new E_Equals(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"neq"))){
			return new E_NotEquals(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"lt"))){
			return new E_LessThan(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"gt"))){
			return new E_GreaterThan(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"lteq"))){
			return new E_LessThanOrEqual(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"gteq"))){
			return new E_GreaterThanOrEqual(exprs[0], exprs[1]);
		}
		else{
			if (n.isLiteral()){
				String f = (String) n.getLiteralValue();
				if (f.equals("bound")){
					return new E_Bound(exprs[0]);
				}
				else if (f.equals("lang")){
					return new E_Lang(exprs[0]);
				}
				else if (f.equals("langMatches")){
					return new E_LangMatches(exprs[0], exprs[1]);
				}
				else if (f.equals("isBlank")){
					return new E_IsBlank(exprs[0]);
				}
				else if (f.equals("+")) {
					return new E_Add(exprs[0], exprs[1]);
				}
				else if (f.equals("*")) {
					return new E_Multiply(exprs[0], exprs[1]);
				}
				else if (f.equals("-")) {
					return new E_Subtract(exprs[0], exprs[1]);
				}
				else if (f.equals("MAX")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createMax(isDistinct, exprs[0]));
				}
				else if (f.equals("MIN")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createMin(isDistinct, exprs[0]));
				}
				else if (f.equals("AVG")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createAvg(isDistinct, exprs[0]));
				}
				else if (f.equals("MAX")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createMax(isDistinct, exprs[0]));
				}
				else if (f.equals("COUNT")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createCountExpr(isDistinct, exprs[0]));
				}
				else if (f.equals("SUM")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createSum(isDistinct, exprs[0]));
				}
				else if (f.equals("SAMPLE")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createSample(isDistinct, exprs[0]));
				}
				else if (f.equals("GROUP_CONCAT")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createGroupConcat(isDistinct, exprs[0], f, null));
				}
				else {
					return ExprUtils.parse(f);
				}
				
			}
			//TODO
			else{
				return null;
			}
		}
	}
	
	public String getCleanLiteral(Node n){
		if (n.isLiteral()){
			String o = n.getLiteralValue().toString();
			if (o.contains("^^")){
				return o.substring(0, o.indexOf("^")).replace("\"", "");
			}
			else{
				return o;
			}
		}
		else{
			return "";
		}
	}
	
	public boolean isOrderedFunction(Node function){
		if (function.equals(NodeFactory.createURI(this.URI+"lt"))){
			return true;
		}
		else if (function.equals(NodeFactory.createURI(this.URI+"gt"))){
			return true;
		}
		else if (function.equals(NodeFactory.createURI(this.URI+"lteq"))){
			return true;
		}
		else if (function.equals(NodeFactory.createURI(this.URI+"gteq"))){
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("-"))){
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("/"))){
			return true;
		}
		return false;
	}
	
	public boolean isOperator(Node s){
		if (isOrderedFunction(s)){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"eq"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"neq"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"times"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"plus"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"subtract"))){
			return true;
		}
		else if (s.equals(NodeFactory.createURI(this.URI+"divide"))){
			return true;
		}
		else{
			return false;
		}
	}
	
	public Op joinToOp(Node n){
		Op ans = null;
		ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
		List<Triple> tripleList = new ArrayList<Triple>();
		BasicPattern bp = new BasicPattern();
		while (args.hasNext()){
			Node arg = args.next();
			Node type = GraphUtil.listObjects(graph, arg, typeNode).next();
			if (type.equals(unionNode)){
				ans = OpJoin.create(ans, unionToOp(arg));
			}
			else if (type.equals(joinNode)){
				ans = OpJoin.create(ans, joinToOp(arg));
			}
			else if (type.equals(optionalNode)){
				ans = OpJoin.create(ans, optionalToOp(arg));
			}
			else if (type.equals(tpNode)){
				tripleList.add(nodeToTriple(arg));
			}
			else if (type.equals(graphNode)){
				ans = OpJoin.create(ans, graphToOp(arg));
			}
			else if (type.equals(tableNode)) {
				ans = OpJoin.create(ans, tableToOp(arg));
			}
		}
		Collections.sort(tripleList, new TripleComparator());
		for (Triple t : tripleList){
			bp.add(t);
		}
		if (!bp.isEmpty()){
			ans = OpJoin.create(ans, new OpBGP(bp));
		}
		ans = filterOrBindToOp(ans, n);
		return ans;
	}
	
	public Op filterOrBindToOp(Op in, Node n) {
		Op ans = in;
		ExtendedIterator<Node> filterOrBind = GraphUtil.listObjects(graph, n, modNode);
		VarExprList varExprList = new VarExprList();
		Expr filterExpr = null;
		while (filterOrBind.hasNext()) {
			Node f = filterOrBind.next();
			Node type = GraphUtil.listObjects(graph, f, typeNode).next();
			if (type.equals(filterNode)) {
				filterExpr = filterToOp(GraphUtil.listObjects(graph, f, argNode).next());
			}
			else if (type.equals(bindNode)) {
				Var var = Var.alloc(GraphUtil.listObjects(graph, f, varNode).next().getBlankNodeLabel());
				varExprList.add(var, bindToOp(GraphUtil.listObjects(graph, f, argNode).next()));
			}
		}
		if (filterExpr != null) {
			ans = OpFilter.filter(filterExpr, ans);
		}
		if (!varExprList.isEmpty()) {
			ans = OpExtend.create(ans, varExprList);
		}
		return ans;
	}
	
	public Op tripleToOp(Node n){
		BasicPattern bp = new BasicPattern();
		Node subjects = GraphUtil.listObjects(graph, n, subNode).next();
		if (subjects.isBlank()){
			subjects = NodeFactory.createVariable(subjects.getBlankNodeLabel());
		}
		Node predicates = GraphUtil.listObjects(graph, n, preNode).next();
		if (predicates.isBlank()){
			predicates = NodeFactory.createVariable(predicates.getBlankNodeLabel());
		}
		Node objects = GraphUtil.listObjects(graph, n, objNode).next();
		if (objects.isBlank()){
			objects = NodeFactory.createVariable(objects.getBlankNodeLabel());
		}
		bp.add(Triple.create(subjects, predicates, objects));
		return new OpBGP(bp);
	}
	
	public Triple nodeToTriple(Node n){
		Node subjects = GraphUtil.listObjects(graph, n, subNode).next();
		if (subjects.isBlank()){
			subjects = NodeFactory.createVariable(subjects.getBlankNodeLabel());
		}
		Node predicates = GraphUtil.listObjects(graph, n, preNode).next();
		if (predicates.isBlank()){
			predicates = NodeFactory.createVariable(predicates.getBlankNodeLabel());
		}
		Node objects = GraphUtil.listObjects(graph, n, objNode).next();
		if (objects.isBlank()){
			objects = NodeFactory.createVariable(objects.getBlankNodeLabel());
		}
		return Triple.create(subjects, predicates, objects);
	}
	
	public String tripleToOp(Triple n){
		String ans = "";
		Node subjects = n.getSubject();
		Node predicates = n.getPredicate();
		Node objects = n.getObject();
		String sub = subjects.toString();
		String pre = predicates.toString();
		String obj = objects.toString();
		if (obj.contains("^^")){
			obj = obj.substring(0, obj.indexOf("^^"))+"^^<"+obj.substring(obj.lastIndexOf("^")+1)+">";
		}
		if (subjects.isBlank()){
			sub = "?"+subjects.getBlankNodeLabel();
			sub = sub.replace("-", "").replace(":", "");
		}
		else if (subjects.isURI()){
			sub = "<"+sub+">";
		}
		if (predicates.isBlank()){
			pre = "?"+predicates.getBlankNodeLabel();
			pre = pre.replace("-", "").replace(":", "");
		}
		else if (predicates.isURI()){
			pre = "<"+pre+">";
		}
		if (objects.isBlank()){
			obj = "?"+objects.getBlankNodeLabel();
			obj = obj.replace("-", "").replace(":", "");
		}
		else if (objects.isURI()){
			obj = "<"+obj+">";
		}
		ans = sub +" "+ pre +" "+ obj + " . ";
		return ans;
	}
	
	public Op tableToOp(Node n) {
		Op ans = null;
		Table t = TableFactory.create();
		ExtendedIterator<Node> rows = GraphUtil.listObjects(graph, n, argNode);
		while (rows.hasNext()) {
			BindingMap b = BindingFactory.create();
			Node row = rows.next();
			ExtendedIterator<Node> bindings = GraphUtil.listObjects(graph, row, argNode);
			while (bindings.hasNext()) {
				Node binding = bindings.next();
				Node var = GraphUtil.listObjects(graph, binding, varNode).next();
				Node value = GraphUtil.listObjects(graph, binding, valueNode).hasNext() ? GraphUtil.listObjects(graph, binding, valueNode).next() : null;
				b.add(Var.alloc(var.getBlankNodeLabel()), value);
			}
			t.addBinding(b);
		}
		ans = OpTable.create(t);
		return ans;
	}
	
	public Op unionToOp(Node n){
		Op ans = null;
		ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
		List<Node> argList = args.toList();
		Collections.sort(argList, new NodeComparator());
		Node firstArg = argList.get(0);
		Node firstType = GraphUtil.listObjects(graph, firstArg, typeNode).next();
		if (firstType.equals(unionNode)){
			ans = unionToOp(firstArg);
		}
		else if (firstType.equals(joinNode)){
			ans = joinToOp(firstArg);
		}
		else if (firstType.equals(optionalNode)){
			ans = optionalToOp(firstArg);
		}
		else if (firstType.equals(tpNode)){
			ans = tripleToOp(firstArg);
		}
		else if (firstType.equals(graphNode)){
			ans = graphToOp(firstArg);
		}
		else if (firstType.equals(tableNode)) {
			ans = tableToOp(firstArg);
		}
		for (int i = 1; i < argList.size(); i++){
			Node arg = argList.get(i);
			Node type = GraphUtil.listObjects(graph, arg, typeNode).next();
			if (type.equals(unionNode)){
				ans = new OpUnion(ans, unionToOp(arg));
			}
			else if (type.equals(joinNode)){
				ans = new OpUnion(ans, joinToOp(arg));
			}
			else if (type.equals(optionalNode)){
				ans = new OpUnion(ans, optionalToOp(arg));
			}
			else if (type.equals(tpNode)){
				ans = new OpUnion(ans, tripleToOp(arg));
			}
			else if (type.equals(graphNode)){
				ans = new OpUnion(ans, graphToOp(arg));
			}
			else if (type.equals(tableNode)) {
				ans = new OpUnion(ans, tableToOp(arg));
			}
		}
		ans = filterOrBindToOp(ans, n);
		return ans;
	}
	
	public Op optionalToOp(Node n){
		Op leftOp = null;
		Op rightOp = null;
		Node left = GraphUtil.listObjects(graph, n, leftNode).next();
		if (graph.contains(Triple.create(left, typeNode, unionNode))){
			leftOp = unionToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, joinNode))){
			leftOp = joinToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, tpNode))){
			leftOp = tripleToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, optionalNode))){
			leftOp = optionalToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, graphNode))){
			leftOp = graphToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, tableNode))) {
			leftOp = tableToOp(left);
		}
		Node right = GraphUtil.listObjects(graph, n, rightNode).next();
		if (graph.contains(Triple.create(right, typeNode, unionNode))){
			rightOp = unionToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, joinNode))){
			rightOp = joinToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, tpNode))){
			rightOp = tripleToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, optionalNode))){
			rightOp = optionalToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, graphNode))){
			rightOp = graphToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, tableNode))) {
			rightOp = tableToOp(right);
		}
		leftOp = filterOrBindToOp(leftOp, n);
		return OpLeftJoin.createLeftJoin(leftOp, rightOp, null);
	}
	
	public Op graphToOp(Node n){
		Op ans = null;
		Node val = GraphUtil.listObjects(graph, n, valueNode).next();	
		Node next = GraphUtil.listObjects(graph, n, argNode).next();
		if (val.isBlank()){
			val = NodeFactory.createVariable(val.getBlankNodeLabel());
			Var value = Var.alloc(val);
			if (graph.contains(Triple.create(next, typeNode, unionNode))){
				ans = new OpGraph(value, unionToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, joinNode))){
				ans = new OpGraph(value, joinToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, tpNode))){
				ans = new OpGraph(value, tripleToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, optionalNode))){
				ans = new OpGraph(value, optionalToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, graphNode))){
				ans = new OpGraph(value, graphToOp(next));
			}
		}
		else{
			if (val.isURI()){
				val = NodeFactory.createURI(val.getURI());
			}
			if (graph.contains(Triple.create(next, typeNode, unionNode))){
			ans = new OpGraph(val, unionToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, joinNode))){
				ans = new OpGraph(val, joinToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, tpNode))){
				ans = new OpGraph(val, tripleToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, optionalNode))){
				ans = new OpGraph(val, optionalToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, graphNode))){
				ans = new OpGraph(val, graphToOp(next));
			}
		}
		ExtendedIterator<Node> filterOrBind = GraphUtil.listObjects(graph, n, modNode);
		while (filterOrBind.hasNext()){
			Node f = filterOrBind.next();
			Node type = GraphUtil.listObjects(graph, f, typeNode).next();
			if (type.equals(filterNode)) {
				ans = OpFilter.filter(filterToOp(GraphUtil.listObjects(graph, f, argNode).next()), ans);
			}
			else if (type.equals(bindNode)) {
				Var var = Var.alloc(GraphUtil.listObjects(graph, f, varNode).next().getBlankNodeLabel());
				ans = OpExtend.create(ans, var, bindToOp(GraphUtil.listObjects(graph, f, argNode).next()));
			}
		}
		return ans;
	}
	
	public Expr bindToOp(Node n){
		Expr e = null;
		if (GraphUtil.listObjects(graph, n, functionNode).hasNext()){
			Node function = GraphUtil.listObjects(graph, n, functionNode).next();
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
			List<Node> argList = args.toList();
			int nParams = argList.size();
			int i = 0;
			if (nParams == 1){
				return filterOperatorToString(function, bindToOp(argList.get(0)));
			}
			List<Expr> params = new ArrayList<Expr>();
			for (int k = 0; k < nParams; k++){
				params.add(null);
			}
			for (Node arg: argList){
				if (GraphUtil.listObjects(graph, arg, valueNode).hasNext()){
					Node value = GraphUtil.listObjects(graph, arg, valueNode).next();
					Expr argString = null;
					if (value.isBlank()){
						if (GraphUtil.listObjects(graph, value, functionNode).hasNext()) {
							argString = bindToOp(value);
						}
						else {
							argString = NodeValue.makeNode(Var.alloc(value.getBlankNodeLabel()));
						}
					}
					else if (value.isURI()){
						argString = NodeValue.makeNode(value);
					}
					else{
						argString = NodeValue.makeNode(value);
					}
					
					if (isOrderedFunction(function)){
						int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
						params.set(order, argString);
					}
					else{
						params.set(i, argString);
					}
				}
				if (GraphUtil.listObjects(graph, arg, functionNode).hasNext()){
					if (isOrderedFunction(function)){
						int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
						params.set(order, bindToOp(arg));
					}
					else{
						params.set(i, bindToOp(arg));
					}
				}
				i++;
			}
			return filterOperatorToString(function, params.get(0), params.get(1));
		}
		if (GraphUtil.listObjects(graph, n, valueNode).hasNext()){
			Node v = GraphUtil.listObjects(graph, n, valueNode).next();
			if (v.isBlank()){
				return NodeValue.makeNode(Var.alloc(v.getBlankNodeLabel()));
			}
			else{
				return NodeValue.makeNode(v);
			}
		}
		if (graph.contains(n,typeNode,varNode)){
			if (n.isBlank()){
				return NodeValue.makeNode(Var.alloc(n.getBlankNodeLabel()));
			}
			else{
				return NodeValue.makeNode(n);
			}
		}
		return e;
	}
	
	public Expr filterToOp(Node n){
		Expr e = null;
		if (graph.contains(n, typeNode, andNode)){
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
			while (args.hasNext()){
				Node a = args.next();
				if (args.hasNext()){
					e = new E_LogicalAnd(filterToOp(args.next()), filterToOp(a));
				}
				else{
					e = new E_LogicalAnd(e, filterToOp(a));
				}	
			}
		}
		else if (graph.contains(n, typeNode, orNode)){
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
			while (args.hasNext()){
				Node a = args.next();
				if (args.hasNext()){
					e = new E_LogicalOr(filterToOp(args.next()), filterToOp(a));
				}
				else{
					e = new E_LogicalOr(e, filterToOp(a));
				}	
			}
		}
		else if (graph.contains(n, typeNode, notNode)){
			Node args = GraphUtil.listObjects(graph, n, argNode).next();
			e = new E_LogicalNot(filterToOp(args));
		}
		else{
			if (GraphUtil.listObjects(graph, n, functionNode).hasNext()){
				Node function = GraphUtil.listObjects(graph, n, functionNode).next();
				ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
				List<Node> argList = args.toList();
				int nParams = argList.size();
				int i = 0;
				if (nParams == 1){
					return filterOperatorToString(function, filterToOp(argList.get(0)));
				}
				List<Expr> params = new ArrayList<Expr>();
				for (int k = 0; k < nParams; k++){
					params.add(null);
				}
				for (Node arg: argList){
					if (GraphUtil.listObjects(graph, arg, valueNode).hasNext()){
						Node value = GraphUtil.listObjects(graph, arg, valueNode).next();
						Expr argString = null;
						if (value.isBlank()){
							argString = NodeValue.makeNode(Var.alloc(value.getBlankNodeLabel()));
						}
						else if (value.isURI()){
							argString = NodeValue.makeNode(value);
						}
						else{
							argString = NodeValue.makeNode(value);
						}
						
						if (isOrderedFunction(function)){
							int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
							params.set(order, argString);
						}
						else{
							params.set(i, argString);
						}
					}
					if (GraphUtil.listObjects(graph, arg, functionNode).hasNext()){
						if (isOrderedFunction(function)){
							int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
							params.set(order, filterToOp(arg));
						}
						else{
							params.set(i, filterToOp(arg));
						}
					}
					i++;
				}
				return filterOperatorToString(function, params.get(0), params.get(1));
			}
			if (GraphUtil.listObjects(graph, n, valueNode).hasNext()){
				Node v = GraphUtil.listObjects(graph, n, valueNode).next();
				if (v.isBlank()){
					return NodeValue.makeNode(Var.alloc(v.getBlankNodeLabel()));
				}
				else{
					return NodeValue.makeNode(v);
				}
			}
		}
		return e;
	}
	
	public String getQuery(){
		Node project = GraphUtil.listSubjects(this.graph, typeNode, projectNode).next();
		ExtendedIterator<Node> projectVars = GraphUtil.listObjects(this.graph, project, argNode);
		ArrayList<Var> pVariables = new ArrayList<Var>();
		String s = "SELECT ";
		if (projectVars.hasNext()){
			while (projectVars.hasNext()){
				pVariables.add(Var.alloc(projectVars.next().getBlankNodeLabel()));
			}
		}
		else{
			s += "* ";
		}
		s += "\n";
		ExtendedIterator<Node> f = GraphUtil.listSubjects(graph, typeNode, fromNode);
		ExtendedIterator<Node> fn = GraphUtil.listSubjects(graph, typeNode, fromNamedNode);
		
		s = s + "\nWHERE {\n";
		ExtendedIterator<Node> m = GraphUtil.listObjects(graph, project, opNode);
		Node first = m.next();
		if (graph.contains(Triple.create(first, typeNode, fromNode))){
			first = m.next();
		}
		if (graph.contains(Triple.create(first, typeNode, unionNode))){
			op = unionToOp(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, joinNode))){
			op = joinToOp(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, optionalNode))){
			op = optionalToOp(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, graphNode))){
			op = graphToOp(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, tpNode))){
			op = tripleToOp(first);
		}
		if (GraphUtil.listSubjects(graph, typeNode, orderByNode).hasNext()){
			Node orderBy = GraphUtil.listSubjects(graph, typeNode, orderByNode).next();
			List<Node> args = GraphUtil.listObjects(graph, orderBy, argNode).toList();
			List<SortCondition> params = new ArrayList<SortCondition>();
			for (int i = 0; i < args.size(); i++){
				params.add(null);
			}
			for (Node a : args){
				int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, a, orderNode).next()));
				Node varName = GraphUtil.listObjects(graph, a, varNode).next();
				int dir = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, a, dirNode).next()));
				SortCondition sc = new SortCondition(Var.alloc(varName.getBlankNodeLabel()), dir);
				params.set(order, sc);
			}
			op = new OpOrder(op, params);
		}
		if (GraphUtil.listSubjects(graph, typeNode, limitNode).hasNext()){
			Node limit = GraphUtil.listSubjects(graph, typeNode, limitNode).next();
			int start = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, limit, offsetNode).next()));
			int finish = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, limit, valueNode).next()));
			op = new OpSlice(op, start, finish);
		}
		if (!pVariables.isEmpty()){
			op = new OpProject(op, pVariables);
		}
		if (this.graph.contains(project, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean))){
			op = new OpDistinct(op);
		}
		Query q = OpAsQuery.asQuery(op);
		if (f.hasNext()){
			ExtendedIterator<Node> URIs = GraphUtil.listObjects(graph, f.next(), argNode);
			if (URIs.hasNext()){
				while (URIs.hasNext()){
					q.addGraphURI(URIs.next().getURI());
				}
			}
		}
		if (fn.hasNext()){
			ExtendedIterator<Node> URIs = GraphUtil.listObjects(graph, fn.next(), argNode);
			if (URIs.hasNext()){
				while (URIs.hasNext()){
					q.addNamedGraphURI(URIs.next().getURI());
				}
			}
		}
		Query query = OpAsQuery.asQuery(op);
		System.out.println(op);
		String ans = query.toString();
		ans = newLabels(ans);
		return ans;
	}
	
	public String newLabels(String q){
		String ans = q;
		HashSet<String> vars = new HashSet<String>();
		Pattern pattern = Pattern.compile("\\?SK\\w+");
		Matcher matcher = pattern.matcher(q);
		while(matcher.find()){
			vars.add(matcher.group(0));
		}
		List<String> newVars = new ArrayList<String>();
		for (String var : vars){
			newVars.add(var);
		}
		Collections.sort(newVars);
		int i = 0;
		for (String var : newVars){
			ans = ans.replace(var, "?v"+i);
			i++;
		}
		return ans;
	}
	
	public class TripleComparator implements Comparator<Triple>{

		@Override
		public int compare(Triple o1, Triple o2) {
			if (compareSubject(o1,o2) == 0){
				if (comparePredicate(o1,o2) == 0){
					return compareObject(o1,o2);
				}
				else{
					return comparePredicate(o1,o2);
				}
			}
			else{
				return compareSubject(o1,o2);
			}
		}
		
		public int compareSubject(Triple o1, Triple o2){
			if (o1.getSubject().isBlank()){
				if (o2.getSubject().isBlank()){
					return o1.getSubject().getBlankNodeLabel().compareTo(o2.getSubject().getBlankNodeLabel());
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getSubject().isBlank()){
					return -1;
				}
				else{
					return o1.getSubject().toString().compareTo(o2.getSubject().toString());
				}
			}
		}
		public int comparePredicate(Triple o1, Triple o2){
			if (o1.getPredicate().isBlank()){
				if (o2.getPredicate().isBlank()){
					return o1.getPredicate().getBlankNodeLabel().compareTo(o2.getPredicate().getBlankNodeLabel());
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getPredicate().isBlank()){
					return -1;
				}
				else{
					return o1.getPredicate().toString().compareTo(o2.getPredicate().toString());
				}
			}
		}
		public int compareObject(Triple o1, Triple o2){
			if (o1.getObject().isBlank()){
				if (o2.getObject().isBlank()){
					return o1.getObject().getBlankNodeLabel().compareTo(o2.getObject().getBlankNodeLabel());
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getObject().isBlank()){
					return -1;
				}
				else{
					return o1.getObject().toString().compareTo(o2.getObject().toString());
				}
			}
		}
	}
	
	public class NodeComparator implements Comparator<Node>{

		@Override
		public int compare(Node o1, Node o2) {
			return o1.toString().compareTo(o2.toString());
		}
		
	}
}
