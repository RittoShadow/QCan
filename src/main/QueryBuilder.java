package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphExtract;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.TripleBoundary;
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
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.E_Add;
import org.apache.jena.sparql.expr.E_BNode;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.E_Coalesce;
import org.apache.jena.sparql.expr.E_Conditional;
import org.apache.jena.sparql.expr.E_Datatype;
import org.apache.jena.sparql.expr.E_DateTimeDay;
import org.apache.jena.sparql.expr.E_DateTimeHours;
import org.apache.jena.sparql.expr.E_DateTimeMinutes;
import org.apache.jena.sparql.expr.E_DateTimeMonth;
import org.apache.jena.sparql.expr.E_DateTimeSeconds;
import org.apache.jena.sparql.expr.E_DateTimeTZ;
import org.apache.jena.sparql.expr.E_DateTimeTimezone;
import org.apache.jena.sparql.expr.E_DateTimeYear;
import org.apache.jena.sparql.expr.E_Divide;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.E_Exists;
import org.apache.jena.sparql.expr.E_GreaterThan;
import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;
import org.apache.jena.sparql.expr.E_IRI;
import org.apache.jena.sparql.expr.E_IsBlank;
import org.apache.jena.sparql.expr.E_IsIRI;
import org.apache.jena.sparql.expr.E_IsLiteral;
import org.apache.jena.sparql.expr.E_IsNumeric;
import org.apache.jena.sparql.expr.E_IsURI;
import org.apache.jena.sparql.expr.E_Lang;
import org.apache.jena.sparql.expr.E_LangMatches;
import org.apache.jena.sparql.expr.E_LessThan;
import org.apache.jena.sparql.expr.E_LessThanOrEqual;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.E_LogicalNot;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_MD5;
import org.apache.jena.sparql.expr.E_Multiply;
import org.apache.jena.sparql.expr.E_NotEquals;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.E_NotOneOf;
import org.apache.jena.sparql.expr.E_Now;
import org.apache.jena.sparql.expr.E_NumAbs;
import org.apache.jena.sparql.expr.E_NumCeiling;
import org.apache.jena.sparql.expr.E_NumFloor;
import org.apache.jena.sparql.expr.E_NumRound;
import org.apache.jena.sparql.expr.E_OneOf;
import org.apache.jena.sparql.expr.E_Random;
import org.apache.jena.sparql.expr.E_Regex;
import org.apache.jena.sparql.expr.E_SHA1;
import org.apache.jena.sparql.expr.E_SHA224;
import org.apache.jena.sparql.expr.E_SHA256;
import org.apache.jena.sparql.expr.E_SHA384;
import org.apache.jena.sparql.expr.E_SHA512;
import org.apache.jena.sparql.expr.E_SameTerm;
import org.apache.jena.sparql.expr.E_Str;
import org.apache.jena.sparql.expr.E_StrAfter;
import org.apache.jena.sparql.expr.E_StrBefore;
import org.apache.jena.sparql.expr.E_StrConcat;
import org.apache.jena.sparql.expr.E_StrContains;
import org.apache.jena.sparql.expr.E_StrDatatype;
import org.apache.jena.sparql.expr.E_StrEncodeForURI;
import org.apache.jena.sparql.expr.E_StrEndsWith;
import org.apache.jena.sparql.expr.E_StrLang;
import org.apache.jena.sparql.expr.E_StrLength;
import org.apache.jena.sparql.expr.E_StrLowerCase;
import org.apache.jena.sparql.expr.E_StrReplace;
import org.apache.jena.sparql.expr.E_StrStartsWith;
import org.apache.jena.sparql.expr.E_StrSubstring;
import org.apache.jena.sparql.expr.E_StrUUID;
import org.apache.jena.sparql.expr.E_StrUpperCase;
import org.apache.jena.sparql.expr.E_Subtract;
import org.apache.jena.sparql.expr.E_URI;
import org.apache.jena.sparql.expr.E_UUID;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
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
	private final Node patternNode = NodeFactory.createURI(this.URI+"pattern");
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
	private final Node groupByNode = NodeFactory.createURI(this.URI+"group");
	private final Node minusNode = NodeFactory.createURI(this.URI+"minus");
	private final Node epsilon = NodeFactory.createURI(this.URI+"epsilon");
	private final Node finalNode = NodeFactory.createURI(this.URI+"final");
	private final Node pathNode = NodeFactory.createURI(this.URI+"path");
	private final Node triplePathNode = NodeFactory.createURI(this.URI+"triplePath");
	private Graph graph;
	private Node root;
	private Op op;
	private boolean isDistinct;
	
	public QueryBuilder(RGraph e){
		this.graph = e.graph;
		this.root = e.root;
		Node project = GraphUtil.listSubjects(this.graph, typeNode, projectNode).next();
		this.isDistinct = this.graph.contains(project, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
	}
	
	public QueryBuilder(Graph g, Node root) {
		this.graph = g;
		this.root = root;
		this.isDistinct = true;
		
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
				else if (f.equals("strdt")) {
					return new E_StrDatatype(exprs[0],exprs[1]);
				}
				else if (f.equals("strlang")) {
					return new E_StrLang(exprs[0],exprs[1]);
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
					return new E_StrStartsWith(exprs[0],exprs[1]);
				}
				else if (f.equals("strends")) {
					return new E_StrEndsWith(exprs[0],exprs[1]);
				}
				else if (f.equals("contains")) {
					return new E_StrContains(exprs[0],exprs[1]);
				}
				else if (f.equals("strbefore")) {
					return new E_StrBefore(exprs[0],exprs[1]);
				}
				else if (f.equals("strafter")) {
					return new E_StrAfter(exprs[0],exprs[1]);
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
				else if (f.equals("lang")){
					return new E_Lang(exprs[0]);
				}
				else if (f.equals("iri")) {
					return new E_IRI(exprs[0]);
				}
				else if (f.equals("uri")) {
					return new E_URI(exprs[0]);
				}
				else if (f.equals("langMatches")){
					return new E_LangMatches(exprs[0], exprs[1]);
				}
				else if (f.equals("isBlank")){
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
					return new E_Divide(exprs[0],exprs[1]);
				}
				else if (f.equals("sameTerm")) {
					return new E_SameTerm(exprs[0], exprs[1]);
				}
				else if (f.equals("MAX")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createMax(false, exprs[0]));
				}
				else if (f.equals("MIN")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createMin(false, exprs[0]));
				}
				else if (f.equals("AVG")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createAvg(false, exprs[0]));
				}
				else if (f.equals("MAX")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createMax(false, exprs[0]));
				}
				else if (f.equals("COUNT")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createCountExpr(false, exprs[0]));
				}
				else if (f.equals("SUM")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createSum(false, exprs[0]));
				}
				else if (f.equals("SAMPLE")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createSample(false, exprs[0]));
				}
				else if (f.equals("GROUP_CONCAT")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					return new ExprAggregator(Var.alloc(v.getBlankNodeLabel()), AggregatorFactory.createGroupConcat(false, exprs[0], f, null));
				}
				else if (f.equals("exists")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					Node t = GraphUtil.listObjects(graph, v, argNode).next();
					Node first = GraphUtil.listObjects(graph, t, valueNode).next();
					Op ans = null;
					if (graph.contains(Triple.create(first, typeNode, unionNode))){
						ans = unionToOp(first);
					}
					else if (graph.contains(Triple.create(first, typeNode, joinNode))){
						ans = joinToOp(first);
					}
					else if (graph.contains(Triple.create(first, typeNode, optionalNode))){
						ans = optionalToOp(first);
					}
					else if (graph.contains(Triple.create(first, typeNode, graphNode))){
						ans = graphToOp(first);
					}
					else if (graph.contains(Triple.create(first, typeNode, tpNode))){
						ans = tripleToOp(first);
					}
					return new E_Exists(ans);
				}
				else if (f.equals("notexists")) {
					Node v = GraphUtil.listSubjects(graph, functionNode, n).next();
					Node t = GraphUtil.listObjects(graph, v, argNode).next();
					Node first = GraphUtil.listObjects(graph, t, valueNode).next();
					Op ans = null;
					if (graph.contains(Triple.create(first, typeNode, unionNode))){
						ans = unionToOp(first);
					}
					else if (graph.contains(Triple.create(first, typeNode, joinNode))){
						ans = joinToOp(first);
					}
					else if (graph.contains(Triple.create(first, typeNode, optionalNode))){
						ans = optionalToOp(first);
					}
					else if (graph.contains(Triple.create(first, typeNode, graphNode))){
						ans = graphToOp(first);
					}
					else if (graph.contains(Triple.create(first, typeNode, tpNode))){
						ans = tripleToOp(first);
					}
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
					System.out.println(exprs.length);
					Expr arg0 = exprs[0];
					Expr arg1 = exprs[1];
					Expr arg2 = exprs[2];
					return new E_Conditional(arg0, arg1, arg2);
				}
				else {
					System.out.println(f);
					return ExprUtils.parse(f);
				}
				
			}
			else{
				return null;
			}
		}
	}
	
	public Expr filterOperatorToString(Node n, List<Expr> expr) {
		if (expr.size() == 1) {
			return filterOperatorToString(n,expr.get(0));
		}
		else if (expr.size() == 2) {
			return filterOperatorToString(n,expr.get(0),expr.get(1));
		}
		else if (expr.size() == 3) {
			return filterOperatorToString(n,expr.get(0),expr.get(1),expr.get(2));
		}
		if (n.isLiteral()) {
			String f = (String) n.getLiteralValue();
			System.out.println(f);
			if (f.equals("regex")) {
				Expr arg0 = expr.get(0);
				Expr arg1 = expr.size() > 1 ? expr.get(1) : null;
				Expr arg2 = expr.size() > 2 ? expr.get(2) : null;
				return new E_Regex(arg0, arg1, arg2);
			}
			else if (f.equals("coalesce")) {
				ExprList args = new ExprList();
				for (Expr e : expr) {
					args.add(e);
				}
				return new E_Coalesce(args);
			}
			else if (f.equals("concat")) {
				ExprList args = new ExprList();
				for (Expr e : expr) {
					args.add(e);
				}
				return new E_StrConcat(args);
			}
			else if (f.equals("replace")) {
				Expr arg0 = expr.get(0);
				Expr arg1 = expr.get(1);
				Expr arg2 = expr.get(2);
				Expr arg3 = expr.size() > 3 ? expr.get(3) : null;
				return new E_StrReplace(arg0, arg1, arg2, arg3);
			}
			else if (f.equals("in")) {
				Expr arg0 = expr.get(0);
				ExprList args = new ExprList();
				for (Expr e : expr.subList(1, expr.size())) {
					args.add(e);
				}
				return new E_OneOf(arg0, args);
			}
			else if (f.equals("notin")) {
				Expr arg0 = expr.get(0);
				ExprList args = new ExprList();
				for (Expr e : expr.subList(1, expr.size())) {
					args.add(e);
				}
				return new E_NotOneOf(arg0, args);
			}
		}
		return null;
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
		else if (function.equals(NodeFactory.createLiteral("regex"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("concat"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("if"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("in"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("notin"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("replace"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("strdt"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("strlang"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("strstarts"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("strends"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("contains"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("strbefore"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("strafter"))) {
			return true;
		}
		else if (function.equals(NodeFactory.createLiteral("substr"))) {
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
			else if (type.equals(triplePathNode)) {
				ans = OpJoin.create(ans, triplePathToOp(arg));
			}
			else if (type.equals(graphNode)){
				ans = OpJoin.create(ans, graphToOp(arg));
			}
			else if (type.equals(tableNode)) {
				ans = OpJoin.create(ans, tableToOp(arg));
			}
			else if (type.equals(minusNode)) {
				ans = OpJoin.create(ans, minusToOp(arg));
			}
		}
		Collections.sort(tripleList, new TripleComparator());
		for (Triple t : tripleList){
			bp.add(t);
		}
		if (!bp.isEmpty()){
			ans = OpJoin.create(ans, new OpBGP(bp));
		}
		ans = filterBindOrGroupToOp(ans, n);
		return ans;
	}
	
	public Op filterBindOrGroupToOp(Op in, Node n) {
		Op ans = in;
		Node eNode = GraphUtil.listObjects(graph, n, patternNode).hasNext() ? GraphUtil.listObjects(graph, n, patternNode).next() : null;
		if (eNode == null) {
			return ans;
		}
		ExtendedIterator<Node> filterOrBind = GraphUtil.listObjects(graph, eNode, argNode);
		VarExprList varExprList = new VarExprList();
		VarExprList groupByVars = new VarExprList();
		List<ExprAggregator> aggList = new ArrayList<ExprAggregator>();
		Expr filterExpr = null;
		while (filterOrBind.hasNext()) {
			Node f = filterOrBind.next();
			Node type = GraphUtil.listObjects(graph, f, typeNode).next();
			if (type.equals(filterNode)) {
				filterExpr = filterToOp(GraphUtil.listObjects(graph, f, argNode).next());
			}
			else if (type.equals(bindNode)) {
				Var var = Var.alloc(GraphUtil.listObjects(graph, f, varNode).next().getBlankNodeLabel());
				Expr expr = bindToOp(GraphUtil.listObjects(graph, f, argNode).next());
				varExprList.add(var, expr);
			}
			else if (type.equals(groupByNode)) {
				Node v = GraphUtil.listObjects(graph, f, argNode).next();
				ExtendedIterator<Node> groupVars = GraphUtil.listObjects(graph, v, valueNode);
				while (groupVars.hasNext()) {
					Node g = groupVars.next();
					Var var = Var.alloc(g.getBlankNodeLabel());
					Expr expr = null;
					if (GraphUtil.listObjects(graph, g, valueNode).hasNext()) {
						Node value = GraphUtil.listObjects(graph, g, valueNode).next();
						expr = filterToOp(value);
					}
					groupByVars.add(var, expr);
				}
				if (GraphUtil.listObjects(graph, f, patternNode).hasNext()) {
					Node p = GraphUtil.listObjects(graph, f, patternNode).next();
					ExtendedIterator<Node> aggregateList = GraphUtil.listObjects(graph, p, argNode);
					while (aggregateList.hasNext()) {
						Node g = aggregateList.next();
						aggList.add((ExprAggregator) aggregateToExpr(g));
					}
				}
			}
		}
		if (!groupByVars.isEmpty()) {
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
	
	public Op tripleToOp(Node n){
		Op ans = null;
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
		ans = new OpBGP(bp);
		ans = filterBindOrGroupToOp(ans,n);
		return ans;
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
		else if (firstType.equals(triplePathNode)) {
			ans = triplePathToOp(firstArg);
		}
		else if (firstType.equals(graphNode)){
			ans = graphToOp(firstArg);
		}
		else if (firstType.equals(tableNode)) {
			ans = tableToOp(firstArg);
		}
		else if (firstType.equals(minusNode)) {
			ans = minusToOp(firstArg);
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
			else if (type.equals(triplePathNode)) {
				ans = new OpUnion(ans, triplePathToOp(arg));
			}
			else if (type.equals(graphNode)){
				ans = new OpUnion(ans, graphToOp(arg));
			}
			else if (type.equals(tableNode)) {
				ans = new OpUnion(ans, tableToOp(arg));
			}
			else if (type.equals(minusNode)) {
				ans = new OpUnion(ans, minusToOp(arg));
			}
		}
		ans = filterBindOrGroupToOp(ans, n);
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
		else if (graph.contains(Triple.create(left, typeNode, triplePathNode))) {
			leftOp = triplePathToOp(left);
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
		else if (graph.contains(Triple.create(left, typeNode, minusNode))) {
			leftOp = minusToOp(left);
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
		else if (graph.contains(Triple.create(right, typeNode, triplePathNode))) {
			rightOp = triplePathToOp(right);
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
		else if (graph.contains(Triple.create(right, typeNode, minusNode))) {
			rightOp = minusToOp(right);
		}
		leftOp = filterBindOrGroupToOp(leftOp, n);
		return OpLeftJoin.createLeftJoin(leftOp, rightOp, null);
	}
	
	public Op minusToOp(Node n){
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
		else if (graph.contains(Triple.create(left, typeNode, triplePathNode))) {
			leftOp = triplePathToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, optionalNode))){
			leftOp = optionalToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, graphNode))){
			leftOp = graphToOp(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, minusNode))){
			leftOp = minusToOp(left);
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
		else if (graph.contains(Triple.create(right, typeNode, triplePathNode))) {
			rightOp = triplePathToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, optionalNode))){
			rightOp = optionalToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, graphNode))){
			rightOp = graphToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, minusNode))){
			rightOp = minusToOp(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, tableNode))) {
			rightOp = tableToOp(right);
		}
		leftOp = filterBindOrGroupToOp(leftOp, n);
		return OpMinus.create(leftOp, rightOp);
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
			else if (graph.contains(Triple.create(next, typeNode, triplePathNode))) {
				ans = new OpGraph(value, triplePathToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, optionalNode))){
				ans = new OpGraph(value, optionalToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, graphNode))){
				ans = new OpGraph(value, graphToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, minusNode))){
				ans = new OpGraph(value, minusToOp(next));
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
			else if (graph.contains(Triple.create(next, typeNode, triplePathNode))) {
				ans = new OpGraph(val, triplePathToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, optionalNode))){
				ans = new OpGraph(val, optionalToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, graphNode))){
				ans = new OpGraph(val, graphToOp(next));
			}
			else if (graph.contains(Triple.create(next, typeNode, minusNode))){
				ans = new OpGraph(val, minusToOp(next));
			}
		}
		ExtendedIterator<Node> filterOrBind = GraphUtil.listObjects(graph, n, patternNode);
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
			if (nParams == 0) {
				return filterOperatorToString(function);
			}
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
						int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
						params.set(order, argString);
					}
					else{
						params.set(i, argString);
					}
				}
				if (GraphUtil.listObjects(graph, arg, functionNode).hasNext()){
					if (isOrderedFunction(function)){
						int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
						params.set(order, bindToOp(arg));
					}
					else{
						params.set(i, bindToOp(arg));
					}
				}
				i++;
			}
			return filterOperatorToString(function, params);
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
	
	public Expr aggregateToExpr(Node n) {
		Expr e = null;
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
						Node s = GraphUtil.listObjects(graph, arg, orderNode).next();
						String c = getCleanLiteral(s);
						int order = Integer.parseInt(c);
						params.set(order, argString);
					}
					else{
						params.set(i, argString);
					}
				}
				if (GraphUtil.listObjects(graph, arg, functionNode).hasNext()){
					if (isOrderedFunction(function)){
						int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
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
		return e;
	}
	
	public Op triplePathToOp(Node n) {
		Op ans = null;
		Path path = null;
		Node subjects = GraphUtil.listObjects(graph, n, subNode).next();
		if (subjects.isBlank()){
			subjects = NodeFactory.createVariable(subjects.getBlankNodeLabel());
		}
		Node predicates = GraphUtil.listObjects(graph, n, preNode).next();
		if (predicates.isBlank()){
			path = propertyPathToOp(GraphUtil.listObjects(graph, predicates, argNode).next());	
		}
		Node objects = GraphUtil.listObjects(graph, n, objNode).next();
		if (objects.isBlank()){
			objects = NodeFactory.createVariable(objects.getBlankNodeLabel());
		}
		ans = new OpPath(new TriplePath(subjects,path,objects));
		ans = filterBindOrGroupToOp(ans,n);
		return ans;
	}
	
	public void printGraph(Graph g){
		ExtendedIterator<Triple> e = GraphUtil.findAll(g);
		while (e.hasNext()){
			System.out.println(e.next());
		}
	}
	
	public Path propertyPathToOp(Node n) {
		Path ans = null;
		List<Node> predicates = GraphUtil.listObjects(graph, n, preNode).toList();
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		Map<List<Node>,Path> transitionTable = new HashMap<List<Node>,Path>();
		Graph dfa = ge.extract(n, graph);
		boolean needStartState = false;
		Node startState = n;
		Node newFinalState = null;
		for (Node p : predicates) {
			ExtendedIterator<Node> aux = GraphUtil.listSubjects(dfa, p, n);
			if (aux.hasNext()) {
				needStartState = true;
			}
		}
		for (Node p : predicates) {
			dfa.remove(n, preNode, p);
		}
		if (needStartState) {
			startState = NodeFactory.createBlankNode();
			dfa.add(Triple.create(startState, epsilon, n));
		}
		List<Node> finalStates = GraphUtil.listSubjects(dfa, typeNode, finalNode).toList();
		if (finalStates.size() > 1) {
			newFinalState = NodeFactory.createBlankNode();
			for (Node finalState : finalStates) {
				dfa.add(Triple.create(finalState, epsilon, newFinalState));
				dfa.remove(finalState, typeNode, finalNode);
			}
			dfa.add(Triple.create(newFinalState, typeNode, finalNode));
		}	
		else {
			newFinalState = finalStates.get(0);
		}
		Set<Node> states = findStates(dfa);
		ExtendedIterator<Triple> transitions = GraphUtil.findAll(dfa);
		while (transitions.hasNext()) {
			Triple t = transitions.next();
			if (!t.getPredicate().equals(typeNode)) {
				List<Node> pair = new ArrayList<Node>();
				pair.add(t.getSubject());
				pair.add(t.getObject());
				if (t.getPredicate().toString().startsWith("\"^")) {
					String u = t.getPredicate().toString();
					u = u.substring(2,u.length()-1);
					transitionTable.put(pair, PathFactory.pathInverse(PathFactory.pathLink(NodeFactory.createURI(u))));
				}
				else {
					transitionTable.put(pair, PathFactory.pathLink(t.getPredicate()));
				}
				
			}
			if (t.getPredicate().equals(epsilon)) {
				List<Node> pair = new ArrayList<Node>();
				pair.add(t.getSubject());
				pair.add(t.getObject());
				transitionTable.put(pair, null);
			}
		}
		Iterator<Node> tempStates = states.iterator();
		while (states.size() > 2) {
			Node state = tempStates.next();
			if (state.equals(startState) || state.equals(newFinalState)) {
				continue;
			}
			else {
				path(state,transitionTable,states);
				states.remove(state);
			}	
		}
		List<Node> finalPair = new ArrayList<Node>();
		finalPair.add(startState);
		finalPair.add(newFinalState);
		ans = finalState(startState,newFinalState,transitionTable);
		return ans;
	}
	
	public Set<Node> findStates(Graph g){
		Set<Node> ans = new HashSet<Node>();
		ExtendedIterator<Triple> triples = GraphUtil.findAll(g);
		while (triples.hasNext()) {
			Triple triple = triples.next();
			Node subject = triple.getSubject();
			Node object = triple.getObject();
			if (subject.isBlank()) {
				ans.add(subject);
			}
			if (object.isBlank()) {
				ans.add(object);
			}
		}
		return ans;
	}
	
	public Path path(Node n, Map<List<Node>,Path> transitionTable, Set<Node> states) {
		Path ans = null;
		Set<List<Node>> toDelete = new HashSet<List<Node>>();
		for (Node n0 : states) {
			for (Node n1 : states) {
				if (n0.equals(n) || n1.equals(n)) {
					continue;
				}
				Path regex0 = null;
				Path regex1 = null;
				Path regex2 = null;
				Path regex3 = null;
				List<Node> pair0 = new ArrayList<Node>();
				List<Node> pair1 = new ArrayList<Node>();
				List<Node> pair2 = new ArrayList<Node>();
				List<Node> pair3 = new ArrayList<Node>();
				pair0.add(n0);
				pair0.add(n);
				pair1.add(n);
				pair1.add(n1);
				pair2.add(n0);
				pair2.add(n1);
				pair3.add(n);
				pair3.add(n);
				if (transitionTable.containsKey(pair0) && transitionTable.containsKey(pair1)) {
					regex0 = transitionTable.get(pair0);
					regex1 = transitionTable.get(pair1);
					if (transitionTable.containsKey(pair2)) {
						regex2 = transitionTable.get(pair2);
					}
					if (transitionTable.containsKey(pair3)) {
						regex3 = transitionTable.get(pair3);
					}
					if (regex0 != null) {
						ans = regex0;
					}
					if (regex3 != null) {
						if (ans == null) {
							ans = PathFactory.pathZeroOrMore1(regex3);
						}
						else {
							ans = PathFactory.pathSeq(ans, PathFactory.pathZeroOrMore1(regex3));
						}
					}
					if (regex1 != null) {
						if (ans == null) {
							ans = regex1;
						}
						else {
							ans = PathFactory.pathSeq(ans, regex1);
						}
					}
					if (regex2 != null) {
						if (ans == null) {
							ans = regex2;
						}
						else {
							ans = PathFactory.pathAlt(ans, regex2);
						}			
					}
					transitionTable.put(pair2, ans);
				}	
				toDelete.add(pair0);
				toDelete.add(pair1);
			}	
		}
		for (List<Node> pair : toDelete) {
			transitionTable.remove(pair);
		}		
		return ans;
	}
	
	public Path finalState(Node startState, Node endState, Map<List<Node>,Path> transitionTable) {
		List<Node> pair = new ArrayList<Node>();
		pair.add(startState);
		pair.add(endState);
		if (transitionTable.get(pair) == null) {
			pair.remove(startState);
			pair.add(endState);
			return PathFactory.pathZeroOrMore1(transitionTable.get(pair));
		}
		else {
			Path p0 = transitionTable.get(pair);
			pair.remove(startState);
			pair.add(endState);
			Path p1 = transitionTable.get(pair);
			if (p1 != null) {
				return PathFactory.pathSeq(p0, PathFactory.pathZeroOrMore1(p1));
			}
			else {
				return p0;
			}
		}
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
				if (nParams == 0) {
					return filterOperatorToString(function);
				}
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
							if (GraphUtil.listObjects(graph, value, functionNode).hasNext()) {
								argString = aggregateToExpr(value);
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
							Node s = GraphUtil.listObjects(graph, arg, orderNode).next();
							String c = getCleanLiteral(s);
							int order = Integer.parseInt(c);
							params.set(order, argString);
						}
						else{
							params.set(i, argString);
						}
					}
					if (GraphUtil.listObjects(graph, arg, functionNode).hasNext()){
						if (isOrderedFunction(function)){
							int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
							params.set(order, filterToOp(arg));
						}
						else{
							params.set(i, filterToOp(arg));
						}
					}
					i++;
				}
				return filterOperatorToString(function, params);
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
		ArrayList<Var> pVariables = new ArrayList<Var>();
		ExtendedIterator<Node> f = GraphUtil.listSubjects(graph, typeNode, fromNode);
		ExtendedIterator<Node> fn = GraphUtil.listSubjects(graph, typeNode, fromNamedNode);
		Node first;
		if (graph.contains(this.root, typeNode, projectNode)) {
			ExtendedIterator<Node> m = GraphUtil.listObjects(graph, root, opNode);
			first = m.next();
			if (graph.contains(Triple.create(first, typeNode, fromNode))){
				first = m.next();
			}
		}
		else {
			first = root;
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
		else if (graph.contains(Triple.create(first, typeNode, minusNode))){
			op = minusToOp(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, triplePathNode))){
			op = triplePathToOp(first);
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
				int order = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, a, orderNode).next()));
				Node varName = GraphUtil.listObjects(graph, a, varNode).next();
				int dir = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, a, dirNode).next()));
				SortCondition sc = new SortCondition(Var.alloc(varName.getBlankNodeLabel()), dir);
				params.set(order, sc);
			}
			op = new OpOrder(op, params);
		}
		if (GraphUtil.listSubjects(graph, typeNode, limitNode).hasNext()){
			Node limit = GraphUtil.listSubjects(graph, typeNode, limitNode).next();
			int start = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, limit, offsetNode).next()));
			int finish = Integer.parseInt(getCleanLiteral(GraphUtil.listObjects(graph, limit, valueNode).next()));
			op = new OpSlice(op, start, finish);
		}
		if (!pVariables.isEmpty()){
			Collections.sort(pVariables, new NodeComparator());
			op = new OpProject(op, pVariables);
		}
		if (this.graph.contains(root, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean))){
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
