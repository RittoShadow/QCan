package cl.uchile.dcc.qcan.builder;

import cl.uchile.dcc.qcan.main.RGraph;
import cl.uchile.dcc.qcan.tools.CommonNodes;
import cl.uchile.dcc.qcan.tools.OpUtils;
import cl.uchile.dcc.qcan.tools.PathUtils;
import cl.uchile.dcc.qcan.visitors.OpRenamer;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ext.com.google.common.collect.BiMap;
import org.apache.jena.ext.com.google.common.collect.HashBiMap;
import org.apache.jena.graph.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.TableFactory;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.graph.NodeTransformLib;
import org.apache.jena.sparql.path.*;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.*;
import java.util.regex.Pattern;

/**
 * This class builds a query out of an r-graph.
 * @author Jaime
 *
 */
public class QueryBuilder {
	public BiMap<Node,Node> varMap = HashBiMap.create();
	public Map<String, String> finalVarMap = new HashMap<>();
	public Map<Node,Node> typeMap = new HashMap<>();
	private final Graph graph;
	private final Node root;
	private Op op;
	private final Node queryType;
	private final Set<Var> vars = new HashSet<>();

	public QueryBuilder(RGraph e) {
		this.graph = e.graph;
		this.root = e.root;
		this.varMap = e.varMap;
		this.typeMap = e.typeMap;
		Node first;
		this.queryType = GraphUtil.listObjects(graph, root, CommonNodes.typeNode).hasNext() ? GraphUtil.listObjects(graph, root, CommonNodes.typeNode).next() : null;
		if (queryType == null){
			first = root;
		}
		else if (queryType.equals(CommonNodes.projectNode)) {
			first = root;
		}
		else if (queryType.equals(CommonNodes.askNode) || queryType.equals(CommonNodes.describeNode)) {
			ExtendedIterator<Node> m = GraphUtil.listObjects(graph, root, CommonNodes.opNode);
			first = m.next();
			if (graph.contains(Triple.create(first, CommonNodes.typeNode, CommonNodes.fromNode))){
				first = m.next();
			}
		}
		else if (queryType.equals(CommonNodes.constructNode)) {
			ExtendedIterator<Node> m = GraphUtil.listObjects(graph, root, CommonNodes.opNode);
			first = m.next();
			if (graph.contains(Triple.create(first, CommonNodes.typeNode, CommonNodes.fromNode))){
				first = m.next();
			}
		}
		else {
			first = root;
		}
		op = nextOpByType(first);
		if (!OpUtils.isNull(op)) {
			op = newLabels(op);
		}
		else {
			BasicPattern bp = new BasicPattern();
			bp.add(Triple.create(NodeFactory.createLiteral("subject"),NodeFactory.createURI("predicate"),NodeFactory.createLiteral("object")));
			op = new OpBGP(bp);
		}
	}

	public Op nextOpByType(Node n) {
		Op ans = null;
		Node type = typeMap.get(n);
		if (type.equals(CommonNodes.unionNode)) {
			ans = unionToOp(n);
		}
		else if (type.equals(CommonNodes.joinNode)) {
			ans = joinToOp(n);
		}
		else if (type.equals(CommonNodes.tpNode)) {
			ans = tripleToOp(n);
		}
		else if (type.equals(CommonNodes.triplePathNode)) {
			ans = triplePathToOp(n);
		}
		else if (type.equals(CommonNodes.optionalNode)) {
			ans = optionalToOp(n);
		}
		else if (type.equals(CommonNodes.graphNode)) {
			ans = graphToOp(n);
		}
		else if (type.equals(CommonNodes.tableNode)) {
			ans = tableToOp(n);
		}
		else if (type.equals(CommonNodes.minusNode)) {
			ans = minusToOp(n);
		}
		else if (type.equals(CommonNodes.serviceNode)) {
			ans = serviceToOp(n);
		}
		else if (type.equals(CommonNodes.extraNode)) {
			ans = filterBindOrGroupToOp(n);
		}
		else if (type.equals(CommonNodes.projectNode)) {
			ans = projectToOp(n);
		}
		else if (type.equals(CommonNodes.nullNode)) {
			ans = OpNull.create();
		}
		return ans;
	}

	public Expr filterOperatorToExpr(Node n, Expr... exprs) {
		if (n.equals(NodeFactory.createLiteral("="))) {
			return new E_Equals(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createLiteral("!="))) {
			return new E_NotEquals(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createLiteral("<"))) {
			return new E_LessThan(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createLiteral(">"))) {
			return new E_GreaterThan(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createLiteral("<="))) {
			return new E_LessThanOrEqual(exprs[0], exprs[1]);
		}
		else if (n.equals(NodeFactory.createLiteral(">="))) {
			return new E_GreaterThanOrEqual(exprs[0], exprs[1]);
		}
		else if (n.isURI()) {
			List<Expr> exprs1 = Arrays.asList(exprs);
			return new E_Function(n.getURI(), new ExprList(exprs1));
		}
		else {
			if (n.isLiteral()) {
				String f = (String) n.getLiteralValue();
				if (f.equals("bound")) {
					return new E_Bound(exprs[0]);
				}
				else if (f.equals("rand")) {
					return new E_Random();
				}
				else if (f.equals("bnode")) {
					if (exprs.length == 0) {
						return new E_BNode();
					}
					else {
						return new E_BNode(exprs[0]);
					}
				}
				else if (f.equals("now")) {
					return new E_Now();
				}
				else if (f.equals("round")) {
					return new E_NumRound(exprs[0]);
				}
				else if (f.equals("ceil")) {
					return new E_NumCeiling(exprs[0]);
				}
				else if (f.equals("floor")) {
					return new E_NumFloor(exprs[0]);
				}
				else if (f.equals("tz")) {
					return new E_DateTimeTZ(exprs[0]);
				}
				else if (f.equals("timezone")) {
					return new E_DateTimeTimezone(exprs[0]);
				}
				else if (f.equals("year")) {
					return new E_DateTimeYear(exprs[0]);
				}
				else if (f.equals("month")) {
					return new E_DateTimeMonth(exprs[0]);
				}
				else if (f.equals("day")) {
					return new E_DateTimeDay(exprs[0]);
				}
				else if (f.equals("hours")) {
					return new E_DateTimeHours(exprs[0]);
				}
				else if (f.equals("minutes")) {
					return new E_DateTimeMinutes(exprs[0]);
				}
				else if (f.equals("seconds")) {
					return new E_DateTimeSeconds(exprs[0]);
				}
				else if (f.equals("MD5")) {
					return new E_MD5(exprs[0]);
				}
				else if (f.equals("SHA1")) {
					return new E_SHA1(exprs[0]);
				}
				else if (f.equals("SHA224")) {
					return new E_SHA224(exprs[0]);
				}
				else if (f.equals("SHA256")) {
					return new E_SHA256(exprs[0]);
				}
				else if (f.equals("SHA384")) {
					return new E_SHA384(exprs[0]);
				}
				else if (f.equals("SHA512")) {
					return new E_SHA512(exprs[0]);
				}
				else if (f.equals("encode_for_uri")) {
					return new E_StrEncodeForURI(exprs[0]);
				}
				else if (f.equals("str")) {
					return new E_Str(exprs[0]);
				}
				else if (f.equals("concat")) {
					ExprList eList = new ExprList();
					for (Expr expr : exprs) {
						eList.add(expr);
					}
					return new E_StrConcat(eList);
				}
				else if (f.equals("strdt")) {
					return new E_StrDatatype(exprs[0], exprs[1]);
				}
				else if (f.equals("strlang")) {
					return new E_StrLang(exprs[0], exprs[1]);
				}
				else if (f.equals("uuid")) {
					return new E_UUID();
				}
				else if (f.equals("struuid")) {
					return new E_StrUUID();
				}
				else if (f.equals("strlen")) {
					return new E_StrLength(exprs[0]);
				}
				else if (f.equals("strstarts")) {
					return new E_StrStartsWith(exprs[0], exprs[1]);
				}
				else if (f.equals("strends")) {
					return new E_StrEndsWith(exprs[0], exprs[1]);
				}
				else if (f.equals("contains")) {
					return new E_StrContains(exprs[0], exprs[1]);
				}
				else if (f.equals("strbefore")) {
					return new E_StrBefore(exprs[0], exprs[1]);
				}
				else if (f.equals("strafter")) {
					return new E_StrAfter(exprs[0], exprs[1]);
				}
				else if (f.equals("ucase")) {
					return new E_StrUpperCase(exprs[0]);
				}
				else if (f.equals("lcase")) {
					return new E_StrLowerCase(exprs[0]);
				}
				else if (f.equals("substr")) {
					if (exprs.length == 2) {
						return new E_StrSubstring(exprs[0], exprs[1], null);
					}
					else {
						return new E_StrSubstring(exprs[0], exprs[1], exprs[2]);
					}
				}
				else if (f.equals("datatype")) {
					return new E_Datatype(exprs[0]);
				}
				else if (f.equals("lang")) {
					return new E_Lang(exprs[0]);
				}
				else if (f.equals("iri")) {
					return new E_IRI(exprs[0]);
				}
				else if (f.equals("uri")) {
					return new E_URI(exprs[0]);
				}
				else if (f.equals("langMatches")) {
					return new E_LangMatches(exprs[0], exprs[1]);
				}
				else if (f.equals("isBlank")) {
					return new E_IsBlank(exprs[0]);
				}
				else if (f.equals("isLiteral")) {
					return new E_IsLiteral(exprs[0]);
				}
				else if (f.equals("isNumeric")) {
					return new E_IsNumeric(exprs[0]);
				}
				else if (f.equals("isIRI")) {
					return new E_IsIRI(exprs[0]);
				}
				else if (f.equals("isURI")) {
					return new E_IsURI(exprs[0]);
				}
				else if (f.equals("abs")) {
					return new E_NumAbs(exprs[0]);
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
				else if (f.equals("/")) {
					return new E_Divide(exprs[0], exprs[1]);
				}
				else if (f.equals("sameTerm")) {
					return new E_SameTerm(exprs[0], exprs[1]);
				}
				else if (f.equals("MAX")) {
					Node v = GraphUtil.listSubjects(graph, CommonNodes.functionNode, n).next();
					Var var = Var.alloc(v.getBlankNodeLabel());
					vars.add(var);
					return new ExprAggregator(var, AggregatorFactory.createMax(false, exprs[0]));
				}
				else if (f.equals("MIN")) {
					Node v = GraphUtil.listSubjects(graph, CommonNodes.functionNode, n).next();
					Var var = Var.alloc(v.getBlankNodeLabel());
					vars.add(var);
					return new ExprAggregator(var, AggregatorFactory.createMin(false, exprs[0]));
				}
				else if (f.equals("AVG")) {
					Node v = GraphUtil.listSubjects(graph, CommonNodes.functionNode, n).next();
					Var var = Var.alloc(v.getBlankNodeLabel());
					vars.add(var);
					return new ExprAggregator(var, AggregatorFactory.createAvg(false, exprs[0]));
				}
				else if (f.equals("COUNT")) {
					Node v = GraphUtil.listSubjects(graph, CommonNodes.functionNode, n).next();
					Var var = Var.alloc(v.getBlankNodeLabel());
					vars.add(var);
					if (exprs[0].equals(NodeValue.makeString("*"))) {
						return new ExprAggregator(var, AggregatorFactory.createCount(false));
					}
					else{
						return new ExprAggregator(var, AggregatorFactory.createCountExpr(false, exprs[0]));
					}
				}
				else if (f.equals("SUM")) {
					Node v = GraphUtil.listSubjects(graph, CommonNodes.functionNode, n).next();
					Var var = Var.alloc(v.getBlankNodeLabel());
					vars.add(var);
					return new ExprAggregator(var, AggregatorFactory.createSum(false, exprs[0]));
				}
				else if (f.equals("SAMPLE")) {
					Node v = GraphUtil.listSubjects(graph, CommonNodes.functionNode, n).next();
					Var var = Var.alloc(v.getBlankNodeLabel());
					vars.add(var);
					return new ExprAggregator(var, AggregatorFactory.createSample(false, exprs[0]));
				}
				else if (f.equals("GROUP_CONCAT")) {
					Node v = GraphUtil.listSubjects(graph, CommonNodes.functionNode, n).next();
					Var var = Var.alloc(v.getBlankNodeLabel());
					vars.add(var);
					return new ExprAggregator(var, AggregatorFactory.createGroupConcat(false, exprs[0], f, null));
				}
				else if (f.equals("exists")) {
					Node v = GraphUtil.listSubjects(graph, CommonNodes.functionNode, n).next();
					Node t = GraphUtil.listObjects(graph, v, CommonNodes.argNode).next();
					Node first = GraphUtil.listObjects(graph, t, CommonNodes.valueNode).next();
					Op ans = nextOpByType(first);
					return new E_Exists(ans);
				}
				else if (f.equals("notexists")) {
					Node v = GraphUtil.listSubjects(graph, CommonNodes.functionNode, n).next();
					Node t = GraphUtil.listObjects(graph, v, CommonNodes.argNode).next();
					Node first = GraphUtil.listObjects(graph, t, CommonNodes.valueNode).next();
					Op ans = null;
					ans = nextOpByType(first);
					return new E_NotExists(ans);
				}
				else if (f.equals("regex")) {
					Expr arg0 = exprs[0];
					Expr arg1 = exprs.length > 1 ? exprs[1] : null;
					Expr arg2 = exprs.length > 2 ? exprs[2] : null;
					return new E_Regex(arg0, arg1, arg2);
				}
				else if (f.equals("replace")) {
					Expr arg0 = exprs[0];
					Expr arg1 = exprs[1];
					Expr arg2 = exprs[2];
					return new E_StrReplace(arg0, arg1, arg2, null);
				}
				else if (f.equals("if")) {
					Expr arg0 = exprs[0];
					Expr arg1 = exprs[1];
					Expr arg2 = exprs[2];
					return new E_Conditional(arg0, arg1, arg2);
				}
				else if (f.equals("notin")) {
					ExprList eList = new ExprList();
					for (int i = 1; i < exprs.length; i++) {
						eList.add(exprs[i]);
					}
					return new E_NotOneOf(exprs[0], eList);
				}
				else if (f.equals("in")) {
					ExprList eList = new ExprList();
					for (int i = 1; i < exprs.length; i++) {
						eList.add(exprs[i]);
					}
					return new E_OneOf(exprs[0], eList);
				}
				else if (f.equals("coalesce")) {
					ExprList eList = new ExprList();
					for (Expr expr : exprs) {
						eList.add(expr);
					}
					return new E_Coalesce(eList);
				}
				else if (f.equals("function")) {
					ExprList eList = new ExprList();
					for (Expr expr : exprs) {
						eList.add(expr);
					}
					return new E_Function(f, eList);
				}
				else if (Pattern.matches(".+://.+", f)) {
					ExprList eList = new ExprList();
					for (Expr expr : exprs) {
						eList.add(expr);
					}
					return new E_Function(f, eList);
				}
				else {
					return ExprUtils.parse(f);
				}

			} else {
				return new E_BNode();
			}
		}
	}

	public Expr aggregatorOperatorToExpr(Node v, boolean distinct, List<Expr> exprs) {
		Node function = GraphUtil.listObjects(graph,v, CommonNodes.functionNode).next();
		if (function.isURI()) {
			return new E_Function(function.getURI(), new ExprList(exprs));
		}
		String f = (String) function.getLiteralValue();
		if (f.equals("MAX")) {
			Var var = Var.alloc(v.getBlankNodeLabel());
			vars.add(var);
			return new ExprAggregator(var, AggregatorFactory.createMax(distinct, exprs.get(0)));
		} else if (f.equals("MIN")) {
			Var var = Var.alloc(v.getBlankNodeLabel());
			vars.add(var);
			return new ExprAggregator(var, AggregatorFactory.createMin(distinct, exprs.get(0)));
		} else if (f.equals("AVG")) {
			Var var = Var.alloc(v.getBlankNodeLabel());
			vars.add(var);
			return new ExprAggregator(var, AggregatorFactory.createAvg(distinct, exprs.get(0)));
		} else if (f.equals("COUNT")) {
			Var var = Var.alloc(v.getBlankNodeLabel());
			vars.add(var);
			if (exprs.size() == 0 || exprs.get(0) == null) {
				return new ExprAggregator(var, AggregatorFactory.createCount(distinct));
			} else {
				return new ExprAggregator(var, AggregatorFactory.createCountExpr(distinct, exprs.get(0)));
			}
		} else if (f.equals("SUM")) {
			Var var = Var.alloc(v.getBlankNodeLabel());
			vars.add(var);
			return new ExprAggregator(var, AggregatorFactory.createSum(distinct, exprs.get(0)));
		} else if (f.equals("SAMPLE")) {
			Var var = Var.alloc(v.getBlankNodeLabel());
			vars.add(var);
			return new ExprAggregator(var, AggregatorFactory.createSample(distinct, exprs.get(0)));
		} else if (f.equals("GROUP_CONCAT")) {
			Var var = Var.alloc(v.getBlankNodeLabel());
			vars.add(var);
			return new ExprAggregator(var, AggregatorFactory.createGroupConcat(distinct, exprs.get(0), f, null));
		}
		else {
			return filterOperatorToExpr(function,exprs);
		}
	}

	public Expr filterOperatorToExpr(Node n, List<Expr> expr) {
		if (!isOrderedFunction(n)) {
			expr.sort(new ExprComparator());
		}
		if (expr.size() == 0 ) {
			return filterOperatorToExpr(n);
		}
		if (expr.size() == 1) {
			return filterOperatorToExpr(n, expr.get(0));
		} else if (expr.size() == 2) {
			return filterOperatorToExpr(n, expr.get(0), expr.get(1));
		} else if (expr.size() == 3) {
			return filterOperatorToExpr(n, expr.get(0), expr.get(1), expr.get(2));
		}
		if (n.isLiteral()) {
			String f = (String) n.getLiteralValue();
			if (f.equals("regex")) {
				Expr arg0 = expr.get(0);
				Expr arg1 = expr.size() > 1 ? expr.get(1) : null;
				Expr arg2 = expr.size() > 2 ? expr.get(2) : null;
				return new E_Regex(arg0, arg1, arg2);
			} else if (f.equals("coalesce")) {
				ExprList args = new ExprList();
				for (Expr e : expr) {
					args.add(e);
				}
				return new E_Coalesce(args);
			} else if (f.equals("concat")) {
				ExprList args = new ExprList();
				for (Expr e : expr) {
					args.add(e);
				}
				return new E_StrConcat(args);
			} else if (f.equals("replace")) {
				Expr arg0 = expr.get(0);
				Expr arg1 = expr.get(1);
				Expr arg2 = expr.get(2);
				Expr arg3 = expr.size() > 3 ? expr.get(3) : null;
				return new E_StrReplace(arg0, arg1, arg2, arg3);
			} else if (f.equals("in")) {
				Expr arg0 = expr.get(0);
				ExprList args = new ExprList();
				for (Expr e : expr.subList(1, expr.size())) {
					args.add(e);
				}
				return new E_OneOf(arg0, args);
			} else if (f.equals("notin")) {
				Expr arg0 = expr.get(0);
				ExprList args = new ExprList();
				for (Expr e : expr.subList(1, expr.size())) {
					args.add(e);
				}
				return new E_NotOneOf(arg0, args);
			}
		} else if (n.isURI()) {
			return new E_Function(n.getURI(), new ExprList(expr));
		}
		return null;
	}

	public String getCleanLiteral(Node n) {
		if (n.isLiteral()) {
			String o = n.getLiteralValue().toString();
			if (o.contains("^^")) {
				return o.substring(0, o.indexOf("^")).replace("\"", "");
			} else {
				return o;
			}
		} else {
			return "";
		}
	}

	public boolean isOrderedFunction(Node function) {
		if (function.equals(NodeFactory.createLiteral("<"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral(">"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("<="))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral(">="))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("-"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("/"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("regex"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("concat"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("if"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("in"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("notin"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("replace"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("strdt"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("strlang"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("strstarts"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("strends"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("contains"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("strbefore"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("strafter"))) {
			return true;
		} else if (function.equals(NodeFactory.createLiteral("substr"))) {
			return true;
		} else return function.isURI();
	}

	public Op joinToOp(Node n) {
		Op ans = null;
		ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, CommonNodes.argNode);
		List<Triple> tripleList = new ArrayList<>();
		List<Op> opPaths = new ArrayList<>();
		BasicPattern bp = new BasicPattern();
		while (args.hasNext()) {
			Node arg = args.next();
			Node type = typeMap.get(arg);
			//Node type = GraphUtil.listObjects(graph, arg, typeNode).next();
			if (type.equals(CommonNodes.tpNode)) {
				tripleList.add(nodeToTriple(arg));
			} else if (type.equals(CommonNodes.triplePathNode)) {
				opPaths.add(nextOpByType(arg));
			} else {
				ans = OpJoin.create(ans, nextOpByType(arg));
			}
		}
		tripleList.sort(new TripleComparator());
		for (Triple t : tripleList) {
			bp.add(t);
		}
		if (!opPaths.isEmpty()) {
			if (ans != null) {
				opPaths.add(ans);
			}
			if (!bp.isEmpty()) {
				opPaths.add(new OpBGP(bp));
			}
			OpSequence opS = OpSequence.create();
			for (Op o : opPaths) {
				opS.add(o);
			}
			return opS;
		} else {
			if (!bp.isEmpty()) {
				ans = OpJoin.create(ans, new OpBGP(bp));
			}
		}
		return ans;
	}

	public Op filterBindOrGroupToOp(Node n) {
		Node subOp = GraphUtil.listObjects(graph,n, CommonNodes.subNode).next();
		Op ans = nextOpByType(subOp);
		VarExprList varExprList = new VarExprList();
		VarExprList groupByVars = new VarExprList();
		List<ExprAggregator> aggList = new ArrayList<>();
		Node filter = GraphUtil.listObjects(graph,n, CommonNodes.filterNode).hasNext() ? GraphUtil.listObjects(graph,n, CommonNodes.filterNode).next() : null;
		Node group = GraphUtil.listObjects(graph,n, CommonNodes.argNode).hasNext() ? GraphUtil.listObjects(graph,n, CommonNodes.argNode).next() : null;
		ExtendedIterator<Node> bindings = GraphUtil.listObjects(graph,n, CommonNodes.extendNode);
		Expr filterExpr = null;
		if (filter != null) {
			filterExpr = nodeToExpr(GraphUtil.listObjects(graph, filter, CommonNodes.argNode).next());
		}
		while (bindings.hasNext()) {
			Node f = bindings.next();
			Var var = Var.alloc(GraphUtil.listObjects(graph, f, CommonNodes.varNode).next().getBlankNodeLabel());
			vars.add(var);
			Expr expr = bindToOp(GraphUtil.listObjects(graph, f, CommonNodes.argNode).next());
			if (expr instanceof ExprAggregator) {
				Node anon = GraphUtil.listObjects(graph,f, CommonNodes.argNode).next();
				expr = new ExprVar(Var.alloc(anon.getBlankNodeLabel()));
			}
			varExprList.add(var, expr);
		}
		if (group != null) {
			Node v = GraphUtil.listObjects(graph, group, CommonNodes.argNode).next();
			ExtendedIterator<Node> groupVars = GraphUtil.listObjects(graph, v, CommonNodes.valueNode);
			while (groupVars.hasNext()) {
				Node g = groupVars.next();
				Var var = Var.alloc(g.getBlankNodeLabel());
				this.vars.add(var);
				Expr expr = null;
				if (GraphUtil.listObjects(graph, g, CommonNodes.valueNode).hasNext()) {
					Node value = GraphUtil.listObjects(graph, g, CommonNodes.valueNode).next();
					expr = nodeToExpr(value);
				}
				groupByVars.add(var, expr);
			}
			if (GraphUtil.listObjects(graph, group, CommonNodes.patternNode).hasNext()) {
				Node p = GraphUtil.listObjects(graph, group, CommonNodes.patternNode).next();
				ExtendedIterator<Node> aggregateList = GraphUtil.listObjects(graph, p, CommonNodes.argNode);
				while (aggregateList.hasNext()) {
					Node g = aggregateList.next();
					aggList.add((ExprAggregator) aggregateToExpr(g));
				}
			}
		}
		if (!(groupByVars.isEmpty() && aggList.isEmpty())) {
			ans = new OpGroup(ans, groupByVars, aggList);
		}
		if (!varExprList.isEmpty()) {
			ans = OpExtend.create(ans, varExprList);
		}
		if (filterExpr != null) {
			ans = OpFilter.filter(filterExpr, ans);
		}
		return ans;
	}

	public Op tripleToOp(Node n) {
		Op ans = null;
		BasicPattern bp = new BasicPattern();
		Node subjects = GraphUtil.listObjects(graph, n, CommonNodes.subjectNode).next();
		if (subjects.isBlank()) {
			vars.add(Var.alloc(subjects.getBlankNodeLabel()));
			subjects = NodeFactory.createVariable(subjects.getBlankNodeLabel());
		}
		Node predicates = GraphUtil.listObjects(graph, n, CommonNodes.preNode).next();
		if (predicates.isBlank()) {
			vars.add(Var.alloc(predicates.getBlankNodeLabel()));
			predicates = NodeFactory.createVariable(predicates.getBlankNodeLabel());
		}
		Node objects = GraphUtil.listObjects(graph, n, CommonNodes.objNode).next();
		if (objects.isBlank()) {
			vars.add(Var.alloc(objects.getBlankNodeLabel()));
			objects = NodeFactory.createVariable(objects.getBlankNodeLabel());
		} else if (objects.isLiteral()) {
			String s = objects.toString();
			s = s.replaceAll("\"", "");
			String[] split = s.split("@");
			if (split.length > 1) {
				String lang = split[split.length - 1];
				if (s.contains("^^")) {
					lang = lang.substring(0, lang.indexOf("^^"));
				}
				objects = NodeFactory.createLiteral(s.substring(0, s.lastIndexOf("@")), lang);
			}
		}
		bp.add(Triple.create(subjects, predicates, objects));
		ans = new OpBGP(bp);
		return ans;
	}

	public Triple nodeToTriple(Node n) {
		Node subjects = GraphUtil.listObjects(graph, n, CommonNodes.subjectNode).next();
		if (subjects.isBlank()) {
			subjects = NodeFactory.createVariable(subjects.getBlankNodeLabel());
			vars.add(Var.alloc(subjects));
		}
		Node predicates = GraphUtil.listObjects(graph, n, CommonNodes.preNode).next();
		if (predicates.isBlank()) {
			predicates = NodeFactory.createVariable(predicates.getBlankNodeLabel());
			vars.add(Var.alloc(predicates));
		}
		Node objects = GraphUtil.listObjects(graph, n, CommonNodes.objNode).next();
		if (objects.isBlank()) {
			objects = NodeFactory.createVariable(objects.getBlankNodeLabel());
			vars.add(Var.alloc(objects));
		}
		return Triple.create(subjects, predicates, objects);
	}

	public Op tableToOp(Node n) {
		Op ans = null;
		Table t = TableFactory.create();
		ExtendedIterator<Node> rows = GraphUtil.listObjects(graph, n, CommonNodes.argNode);
		while (rows.hasNext()) {
			BindingMap b = BindingFactory.create();
			Node row = rows.next();
			ExtendedIterator<Node> bindings = GraphUtil.listObjects(graph, row, CommonNodes.argNode);
			while (bindings.hasNext()) {
				Node binding = bindings.next();
				Node var = GraphUtil.listObjects(graph, binding, CommonNodes.varNode).next();
				Node value = GraphUtil.listObjects(graph, binding, CommonNodes.valueNode).hasNext() ? GraphUtil.listObjects(graph, binding, CommonNodes.valueNode).next() : null;
				b.add(Var.alloc(var.getBlankNodeLabel()), value);
				vars.add(Var.alloc(var.getBlankNodeLabel()));
			}
			if (!b.isEmpty()) {
				t.addBinding(b);
			}
		}
		ans = OpTable.create(t);
		if (t.isEmpty()) {
			ans = OpTable.unit();
		}
		return ans;
	}

	public Op unionToOp(Node n) {
		Op ans = null;
		ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, CommonNodes.argNode);
		List<Node> argList = args.toList();
		argList.sort(new NodeComparator());
		Node firstArg = argList.get(0);
		ans = nextOpByType(firstArg);
		for (int i = 1; i < argList.size(); i++) {
			Node arg = argList.get(i);
			ans = new OpUnion(ans, nextOpByType(arg));
		}
		return ans;
	}

	public Op serviceToOp(Node n) {
		Op ans = null;
		Node next = GraphUtil.listObjects(graph, n, CommonNodes.argNode).next();
		Node value = GraphUtil.listObjects(graph, n, CommonNodes.valueNode).next();
		Node silent = GraphUtil.listObjects(graph, n, CommonNodes.silentNode).next();
		ans = nextOpByType(next);
		ans = new OpService(value, ans, (boolean) silent.getLiteralValue());
		return ans;
	}

	public Op optionalToOp(Node n) {
		Op leftOp = null;
		Op rightOp = null;
		Node left = GraphUtil.listObjects(graph, n, CommonNodes.leftNode).next();
		leftOp = nextOpByType(left);
		List<Node> rightNodes = GraphUtil.listObjects(graph,n,CommonNodes.rightNode).toList();
		for (Node right : rightNodes) {
			rightOp = nextOpByType(right);
		}
		Node right = GraphUtil.listObjects(graph, n, CommonNodes.rightNode).next();
		rightOp = nextOpByType(right);
		return OpLeftJoin.createLeftJoin(leftOp, rightOp, null);
	}

	public Op minusToOp(Node n) {
		Op leftOp = null;
		Op rightOp = null;
		Node left = GraphUtil.listObjects(graph, n, CommonNodes.leftNode).next();
		leftOp = nextOpByType(left);
		Node right = GraphUtil.listObjects(graph, n, CommonNodes.rightNode).next();
		rightOp = nextOpByType(right);
		return OpMinus.create(leftOp, rightOp);
	}

	public Op graphToOp(Node n) {
		Op ans = null;
		Node val = GraphUtil.listObjects(graph, n, CommonNodes.valueNode).next();
		Node next = GraphUtil.listObjects(graph, n, CommonNodes.argNode).next();
		if (val.isBlank()) {
			val = NodeFactory.createVariable(val.getBlankNodeLabel());
			Var value = Var.alloc(val);
			vars.add(value);
			ans = new OpGraph(value, nextOpByType(next));
		} else {
			if (val.isURI()) {
				val = NodeFactory.createURI(val.getURI());
			}
			ans = new OpGraph(val, nextOpByType(next));
		}
		return ans;
	}

	public Expr bindToOp(Node n) {
		if (GraphUtil.listObjects(graph, n, CommonNodes.functionNode).hasNext()) {
			Node function = GraphUtil.listObjects(graph, n, CommonNodes.functionNode).next();
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, CommonNodes.argNode);
			List<Node> argList = args.toList();
			int nParams = argList.size();
			int i = 0;
			if (nParams == 0) {
				return filterOperatorToExpr(function);
			}
			List<Expr> params = new ArrayList<>();
			for (int k = 0; k < nParams; k++) {
				params.add(null);
			}
			for (Node arg : argList) {
				if (GraphUtil.listObjects(graph, arg, CommonNodes.valueNode).hasNext()) {
					Node value = GraphUtil.listObjects(graph, arg, CommonNodes.valueNode).next();
					Expr argString = argToExpr(value);
					if (value.isBlank()) {
						if (GraphUtil.listObjects(graph, value, CommonNodes.functionNode).hasNext()) {
							argString = bindToOp(value);
						} else {
							vars.add(Var.alloc(value.getBlankNodeLabel()));
							argString = NodeValue.makeNode(Var.alloc(value.getBlankNodeLabel()));
						}
					}

					if (isOrderedFunction(function)) {
						int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, arg, CommonNodes.orderNode).next()));
						params.set(order, argString);
					} else {
						params.set(i, argString);
					}
				}
				if (GraphUtil.listObjects(graph, arg, CommonNodes.functionNode).hasNext()) {
					if (isOrderedFunction(function)) {
						int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, arg, CommonNodes.orderNode).next()));
						params.set(order, bindToOp(arg));
					} else {
						params.set(i, bindToOp(arg));
					}
				}
				i++;
			}
			if (!isOrderedFunction(function)) {
				params.sort(new ExprComparator());
			}
			if (nParams == 1) {
				//return filterOperatorToString(function, bindToOp(argList.get(0)));
				return filterOperatorToExpr(function, params.get(0));
			}
			return filterOperatorToExpr(function, params);
		}
		if (GraphUtil.listObjects(graph, n, CommonNodes.valueNode).hasNext()) {
			Node v = GraphUtil.listObjects(graph, n, CommonNodes.valueNode).next();
			return argToExpr(v);
		}
		else {
			return argToExpr(n);
		}
	}

	public Expr argToExpr(Node value) {
		Expr argString = null;
		if (value.isBlank()) {
			vars.add(Var.alloc(value.getBlankNodeLabel()));
			argString = NodeValue.makeNode(Var.alloc(value.getBlankNodeLabel()));
		} else if (value.isURI()) {
			argString = NodeValue.makeNode(value);
		} else {
			if (value.isLiteral()) {
				if (value.getLiteralLexicalForm().replace("\"","").equals("*")) {
					return NodeValue.makeString("*");
				}
				else {
					String s = value.toString();
					s = s.replaceAll("\"", "");
					String[] split = s.split("@");
					if (split.length > 1) {
						String lang = split[split.length - 1];
						if (s.contains("^^")) {
							lang = lang.substring(0, lang.indexOf("^^"));
						}
						value = NodeFactory.createLiteral(s.substring(0, s.lastIndexOf("@")), lang);
					}
				}
			}
			argString = NodeValue.makeNode(value);
		}
		return argString;
	}

	public Expr aggregateToExpr(Node n) {
		if (GraphUtil.listObjects(graph, n, CommonNodes.functionNode).hasNext()) {
			Node function = GraphUtil.listObjects(graph, n, CommonNodes.functionNode).next();
			boolean distinct = graph.contains(Triple.create(n, CommonNodes.distinctNode,NodeFactory.createLiteralByValue(true,XSDDatatype.XSDboolean)));
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, CommonNodes.argNode);
			List<Node> argList = args.toList();
			int nParams = argList.size();
			int i = 0;
			List<Expr> params = new ArrayList<>();
			for (int k = 0; k < nParams; k++) {
				params.add(null);
			}
			for (Node arg : argList) {
				if (GraphUtil.listObjects(graph, arg, CommonNodes.valueNode).hasNext()) {
					Node value = GraphUtil.listObjects(graph, arg, CommonNodes.valueNode).next();
					Expr argString = argToExpr(value);
					if (isOrderedFunction(function)) {
						Node s = GraphUtil.listObjects(graph, arg, CommonNodes.orderNode).next();
						String c = getCleanLiteral(s);
						int order = Integer.parseInt(c);
						params.set(order, argString);
					} else {
						params.set(i, argString);
					}
				}
				if (GraphUtil.listObjects(graph, arg, CommonNodes.functionNode).hasNext()) {
					if (isOrderedFunction(function)) {
						int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, arg, CommonNodes.orderNode).next()));
						params.set(order, nodeToExpr(arg));
					} else {
						params.set(i, nodeToExpr(arg));
					}
				}
				i++;
			}
			if (!isOrderedFunction(function)) {
				params.sort(new ExprComparator());
			}
			if (nParams == 0) {
				return aggregatorOperatorToExpr(n, distinct, Collections.emptyList());
			}
			else if (nParams == 1) {
				return aggregatorOperatorToExpr(n, distinct, Collections.singletonList(nodeToExpr(argList.get(0))));
			}
			else {
				return aggregatorOperatorToExpr(n, distinct,params);
			}
		}
		if (GraphUtil.listObjects(graph, n, CommonNodes.valueNode).hasNext()) {
			Node v = GraphUtil.listObjects(graph, n, CommonNodes.valueNode).next();
			return argToExpr(v);
		}
		return null;
	}

	public Op triplePathToOp(Node n) {
		Op ans = null;
		Path path = null;
		Node subjects = GraphUtil.listObjects(graph, n, CommonNodes.subjectNode).next();
		if (subjects.isBlank()) {
			vars.add(Var.alloc(subjects.getBlankNodeLabel()));
			subjects = NodeFactory.createVariable(subjects.getBlankNodeLabel());
		}
		Node predicates = GraphUtil.listObjects(graph, n, CommonNodes.preNode).next();
		if (predicates.isBlank()) {
			vars.add(Var.alloc(predicates.getBlankNodeLabel()));
			path = propertyPathToOp(GraphUtil.listObjects(graph, predicates, CommonNodes.argNode).next());
		}
		Node objects = GraphUtil.listObjects(graph, n, CommonNodes.objNode).next();
		if (objects.isBlank()) {
			vars.add(Var.alloc(objects.getBlankNodeLabel()));
			objects = NodeFactory.createVariable(objects.getBlankNodeLabel());
		}
		ans = new OpPath(new TriplePath(subjects, path, objects));
		return ans;
	}

	public Path propertyPathToOp(Node n) {
		if (graph.contains(Triple.create(n, CommonNodes.typeNode, NodeFactory.createURI(CommonNodes.URI + "notOneOf")))) {
			List<Node> predicates = GraphUtil.listObjects(graph, n, CommonNodes.argNode).toList();
			predicates.sort(new NodeComparator());
			P_NegPropSet path = new P_NegPropSet();
			for (Node p : predicates) {
				if (p.isURI()) {
					path.add((P_Path0) PathFactory.pathLink(p));
				} else {
					String uri = p.toString().substring(p.toString().indexOf("^") + 1).replaceAll("\"", "");
					path.add(new P_ReverseLink(NodeFactory.createURI(uri)));
				}
			}
			return path;
		}
		List<Node> predicates = GraphUtil.listObjects(graph, n, CommonNodes.preNode).toList();
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		Graph dfa = ge.extract(n, graph);
		return PathUtils.dfaToPath(dfa,n,predicates);
	}

	public Expr nodeToExpr(Node n) {
		Expr e = null;
		Node type = typeMap.get(n);
		if (type != null) {
			if (type.equals(CommonNodes.andNode)) {
				ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, CommonNodes.argNode);
				while (args.hasNext()) {
					Node a = args.next();
					if (e == null) {
						e = nodeToExpr(a);
					} else {
						e = new E_LogicalAnd(e, nodeToExpr(a));
					}
				}
			}
			else if (type.equals(CommonNodes.orNode)) {
				ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, CommonNodes.argNode);
				while (args.hasNext()) {
					Node a = args.next();
					if (e == null) {
						e = nodeToExpr(a);
					} else {
						e = new E_LogicalOr(e, nodeToExpr(a));
					}
				}
			}
			else if (type.equals(CommonNodes.notNode)) {
				Node args = GraphUtil.listObjects(graph, n, CommonNodes.argNode).next();
				e = new E_LogicalNot(nodeToExpr(args));
			}
		}
		else {
			if (GraphUtil.listObjects(graph, n, CommonNodes.functionNode).hasNext()) { //If it's a function.
				Node function = GraphUtil.listObjects(graph, n, CommonNodes.functionNode).next();
				ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, CommonNodes.argNode);
				List<Node> argList = args.toList();
				int nParams = argList.size();
				int i = 0;
				if (nParams == 0) {
					return filterOperatorToExpr(function);
				}
				if (nParams == 1) {
					return filterOperatorToExpr(function, nodeToExpr(argList.get(0)));
				}
				List<Expr> params = new ArrayList<>();
				for (int k = 0; k < nParams; k++) {
					params.add(null);
				}
				for (Node arg : argList) {
					if (GraphUtil.listObjects(graph, arg, CommonNodes.valueNode).hasNext()) {
						Node value = GraphUtil.listObjects(graph, arg, CommonNodes.valueNode).next();
						Expr argString = argToExpr(value);
						if (value.isBlank()) {
							if (GraphUtil.listObjects(graph, value, CommonNodes.functionNode).hasNext()) {
								argString = aggregateToExpr(value);
							} else {
								vars.add(Var.alloc(value.getBlankNodeLabel()));
								argString = NodeValue.makeNode(Var.alloc(value.getBlankNodeLabel()));
							}
						}
						if (isOrderedFunction(function)) {
							Node s = GraphUtil.listObjects(graph, arg, CommonNodes.orderNode).next();
							String c = getCleanLiteral(s);
							int order = Integer.parseInt(c);
							params.set(order, argString);
						} else {
							params.set(i, argString);
						}
					}
					if (GraphUtil.listObjects(graph, arg, CommonNodes.functionNode).hasNext()) {
						if (isOrderedFunction(function)) {
							int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, arg, CommonNodes.orderNode).next()));
							params.set(order, nodeToExpr(arg));
						} else {
							params.set(i, nodeToExpr(arg));
						}
					}
					i++;
				}
				if (!isOrderedFunction(function)) {
					params.sort(new ExprComparator());
				}
				return filterOperatorToExpr(function, params);
			}
			if (GraphUtil.listObjects(graph, n, CommonNodes.valueNode).hasNext()) { //If it's a variable representing a function.
				Node v = GraphUtil.listObjects(graph, n, CommonNodes.valueNode).next();
				if (v.isBlank()) {
					if (GraphUtil.listObjects(graph, v, CommonNodes.functionNode).hasNext()) {
						return aggregateToExpr(v);
					}
					else {
						vars.add(Var.alloc(v.getBlankNodeLabel()));
						return NodeValue.makeNode(Var.alloc(v.getBlankNodeLabel()));
					}
				}
				else {
					return argToExpr(v);
				}
			}
			else {
				return argToExpr(n);
			}
		}
		return e;
	}

	public Op projectToOp(Node n) {
		ArrayList<Var> pVariables = new ArrayList<>();
		ExtendedIterator<Node> pNodes = GraphUtil.listObjects(graph,n, CommonNodes.argNode);
		while (pNodes.hasNext()) {
			Node pNode = pNodes.next();
			pVariables.add(Var.alloc(pNode.getBlankNodeLabel()));
		}
		Node first = GraphUtil.listObjects(graph,n, CommonNodes.opNode).next();
		op = nextOpByType(first);
		Node limit = null;
		Node orderBy = null;
		ExtendedIterator<Node> mods = GraphUtil.listObjects(graph,n, CommonNodes.modNode);
		while (mods.hasNext()) {
			Node current = mods.next();
			if (GraphUtil.listObjects(graph,current, CommonNodes.typeNode).next().equals(CommonNodes.limitNode)) {
				limit = current;
			}
			if (GraphUtil.listObjects(graph,current, CommonNodes.typeNode).next().equals(CommonNodes.orderByNode)) {
				orderBy = current;
			}
		}
		if (orderBy != null) {
			List<Node> args = GraphUtil.listObjects(graph, orderBy, CommonNodes.argNode).toList();
			List<SortCondition> params = new ArrayList<>();
			for (int i = 0; i < args.size(); i++){
				params.add(null);
			}
			for (Node a : args){
				int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, a, CommonNodes.orderNode).next()));
				Expr expr = nodeToExpr(a);
				int dir = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, a, CommonNodes.dirNode).next()));
				SortCondition sc = new SortCondition(expr, dir);
				params.set(order, sc);
			}
			op = new OpOrder(op, params);
		}
		if (!pVariables.isEmpty()) {
			op = new OpProject(op,pVariables);
		}
		if (graph.contains(Triple.create(n, CommonNodes.distinctNode,NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)))) {
			op = new OpDistinct(op);
		}
		else if (graph.contains(Triple.create(n, CommonNodes.reducedNode,NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)))){
			op = OpReduced.create(op);
		}
		if (limit != null) {
			int start = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, limit, CommonNodes.offsetNode).next()));
			int finish = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, limit, CommonNodes.valueNode).next()));
			op = new OpSlice(op,start,finish);
		}
		return op;
	}
	
	public String getQuery(){
		ExtendedIterator<Node> f = GraphUtil.listSubjects(graph, CommonNodes.typeNode, CommonNodes.fromNode);
		ExtendedIterator<Node> fn = GraphUtil.listSubjects(graph, CommonNodes.typeNode, CommonNodes.fromNamedNode);
		Query query = OpAsQuery.asQuery(op);
		if (f.hasNext()){
			ExtendedIterator<Node> URIs = GraphUtil.listObjects(graph, f.next(), CommonNodes.argNode);
			if (URIs.hasNext()){
				while (URIs.hasNext()){
					query.addGraphURI(URIs.next().getURI());
				}
			}
		}
		if (fn.hasNext()){
			ExtendedIterator<Node> URIs = GraphUtil.listObjects(graph, fn.next(), CommonNodes.argNode);
			if (URIs.hasNext()){
				while (URIs.hasNext()){
					query.addNamedGraphURI(URIs.next().getURI());
				}
			}
		}
		String ans = query.toString();
		if (queryType == null){
			return ans;
		}
		else if (queryType.equals(CommonNodes.askNode)) {
			ans = ans.substring(ans.indexOf("WHERE"));
			ans = "ASK " + ans;
		}
		else if (queryType.equals(CommonNodes.constructNode)) {
			ExtendedIterator<Node> template = GraphUtil.listObjects(graph,root, CommonNodes.argNode);
			if (template.hasNext()) {
				Node t = template.next();
				Op templateOp = nextOpByType(t);
				templateOp = newLabels(templateOp);
				if (templateOp instanceof OpBGP) {
					query.setConstructTemplate(new Template(((OpBGP) templateOp).getPattern()));
				}
			}
			query.setQueryConstructType();
			return query.toString();
/*			ans = ans.substring(ans.indexOf("WHERE"));
			ans = "CONSTRUCT " + ans;*/
		}
		else if (queryType.equals(CommonNodes.describeNode)) {
			ans = ans.substring(ans.indexOf("WHERE"));
			ans = "DESCRIBE " + ans;
		}
		return ans;
	}
	
	public Op newLabels(Op op){
		Set<Var> vars = OpUtils.varsContainedIn(op);
		List<String> varLabels = new ArrayList<>();
		for (Var v : vars) {
			varLabels.add(v.getVarName());
		}
		Collections.sort(varLabels);
		Map<Var,Var> newLabels = new HashMap<>();
		for (int i = 0; i < varLabels.size(); i++) {
			newLabels.put(Var.alloc(varLabels.get(i)),Var.alloc("v"+i));
		}
		op = NodeTransformLib.transform(new OpRenamer(newLabels),op);
		for (Map.Entry<Var,Var> entry : newLabels.entrySet()) {
			String skLabel = entry.getKey().getVarName();
			String newLabel = entry.getValue().getVarName();
			if (varMap.inverse().containsKey(NodeFactory.createBlankNode(skLabel))) {
				String originalLabel = varMap.inverse().get(NodeFactory.createBlankNode(skLabel)).getName();
				finalVarMap.put(originalLabel,newLabel);
			}
		}
		return op;
	}

	public Op getOp() {
		return this.op;
	}

	@Override
	public String toString() {
		return getQuery();
	}
	
	public Set<Var> getVars(){
		return this.vars;
	}
	
	public static class TripleComparator implements Comparator<Triple>{

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
	
	public static class NodeComparator implements Comparator<Node>{

		@Override
		public int compare(Node o1, Node o2) {
			return o1.toString().compareTo(o2.toString());
		}
		
	}
	
	public static class ExprComparator implements Comparator<Expr>{

		@Override
		public int compare(Expr o1, Expr o2) {
			if (o1.isFunction()) {
				if (o2.isFunction()) {
					if (o1.getFunction().equals(o2.getFunction())) {
						List<Expr> exprs1 = new ArrayList<>(o1.getFunction().getArgs());
						List<Expr> exprs2 = new ArrayList<>(o2.getFunction().getArgs());
						exprs1.sort(new ExprComparator());
						exprs2.sort(new ExprComparator());
						int k = Math.min(exprs1.size(), exprs2.size());
						for (int i = 0; i < k; i++){
							int ans = compare(exprs1.get(i), exprs2.get(i));
							if (ans != 0){
								return ans;
							}
						}
						return -1;
					}
				}
				else if (o2.isConstant()) {
					return -1;
				}
				else if (o2.isVariable()) {
					return -1;
				}
			}
			else if (o1.isConstant()) {
				NodeValue n1 = o1.getConstant();
				if (o2.isFunction()) {
					return 1;
				}
				else if (o2.isConstant()) {
					NodeValue n2 = o2.getConstant();
					if (n1.hasNode() && n2.hasNode()) {
						Node node1 = n1.asNode();
						Node node2 = n2.asNode();
						NodeComparator nc = new NodeComparator();
						return -nc.compare(node1, node2);
					}
					return NodeValue.compare(o1.getConstant(), o2.getConstant());
				}
				else if (o2.isVariable()) {
					return 1;
				}
			}
			else if (o1.isVariable()) {
				if (o2.isFunction()) {
					return 1;
				}
				else if (o2.isConstant()) {
					return -1;
				}
				else if (o2.isVariable()) {
					return o1.getVarName().compareTo(o2.getVarName());
				}
			}
			return 0;
		}
		
		
	}
}
