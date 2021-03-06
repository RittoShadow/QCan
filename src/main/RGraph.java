package main;

import cl.uchile.dcc.blabel.jena.JenaModelIterator;
import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.blabel.label.GraphLabelling;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingArgs;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingResult;
import cl.uchile.dcc.blabel.lean.DFSGraphLeaning;
import cl.uchile.dcc.blabel.lean.GraphLeaning.GraphLeaningResult;
import com.google.common.hash.HashCode;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ext.com.google.common.collect.BiMap;
import org.apache.jena.ext.com.google.common.collect.HashBiMap;
import org.apache.jena.graph.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprWalker;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.sse.writers.WriterPath;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.yars.nx.BNode;
import paths.PGraph;
import static tools.CommonNodes.*;
import tools.CustomTripleBoundary;
import visitors.FilterVisitor;

import java.util.*;
import java.util.regex.Pattern;

import static org.apache.jena.graph.GraphUtil.listObjects;
import static org.apache.jena.graph.GraphUtil.listSubjects;

/**
 * @author Jaime
 */
public class RGraph {

	public Graph graph = GraphFactory.createDefaultGraph();
	public Set<Var> vars = new HashSet<>();
	public int nTriples;
	public int id = 0;
	public Node root;
	public boolean distinct = false;
	public boolean leaning = true;
	public boolean canonical = true;
	public boolean containsPaths = false;
	private long labelTime = 0;
	private long minimisationTime = 0;
	public BiMap<Node, Node> varMap = HashBiMap.create();
	public Map<Var, Node> exprMap = new HashMap<>();
	private final UpdateRequest duplicatesRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/normalisation/duplicates.ru"));
	private final UpdateRequest conjunctionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/normalisation/conjunction.ru"));
	private final UpdateRequest disjunctionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/normalisation/disjunction.ru"));
	private final UpdateRequest joinRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/join.ru"));
	private final UpdateRequest branchCleanUpRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/cleanUp.ru"));
	private final UpdateRequest branchUnionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/branchUnion.ru"));
	private final UpdateRequest filterVarsRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/filterVars.ru"));
	private final UpdateRequest subgraphRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/subLabel.ru"));


	/**
	 * @param triples List of RDF triples.
	 * @param vars    List of the variables in the triples.
	 */
	public RGraph(List<Triple> triples, List<Var> vars) {
		if (vars != null) {
			this.vars.addAll(vars);
		}
		nTriples = triples.size();
		if (triples.size() > 1) {
			this.root = NodeFactory.createBlankNode();
			//Adding typing now.
			graph.add(Triple.create(this.root, typeNode, joinNode));
		}
		for (Triple t : triples) {
			Node n = NodeFactory.createBlankNode();
			if (triples.size() == 1) {
				this.root = n;
			} else {
				graph.add(Triple.create(root, argNode, n));
			}
			graph.add(Triple.create(n, typeNode, tpNode));
			// We create a blank node if we have a variable. If it's a literal, we add a literal node, and so on.
			Triple s = Triple.create(n, subjectNode, getValidNode(t.getSubject()));
			Triple p = Triple.create(n, preNode, getValidNode(t.getPredicate()));
			Triple o = Triple.create(n, objNode, getValidNode(t.getObject()));
			graph.add(s);
			graph.add(p);
			graph.add(o);
		}
	}


	/**
	 * @param root  This node will be the new root of the r-graph.
	 * @param graph The graph on which the new r-graph is based.
	 * @param vars  The list of variables in the r-graph.
	 */
	public RGraph(Node root, Graph graph, Collection<Var> vars) {
		this.root = root;
		this.graph = graph;
		if (vars != null) {
			this.vars.addAll(vars);
		}
	}

	/**
	 * @param triples List of RDF triples.
	 */
	public RGraph(List<Triple> triples) {
		this(triples, new ArrayList<>());
	}

	/**
	 * @param data A collection of triples of nodes.
	 */
	public RGraph(Collection<org.semanticweb.yars.nx.Node[]> data) {
		Set<Node> rootCandidates = new HashSet<>();
		Set<Node> predicates = new HashSet<>();
		Set<Node> objects = new HashSet<>();
		for (org.semanticweb.yars.nx.Node[] node : data) {
			Node subject;
			Node predicate;
			Node object;
			subject = nxToJenaNode(node[0]);
			predicate = nxToJenaNode(node[1]);
			object = nxToJenaNode(node[2]);
			if (subject != null) {
				graph.add(Triple.create(subject, predicate, object));
				rootCandidates.add(subject);
				predicates.add(predicate);
				objects.add(object);
			} else {
				System.err.println("Invalid blank node label.");
			}
		}
		for (Node p : predicates) {
			rootCandidates.remove(p);
		}
		for (Node o : objects) {
			rootCandidates.remove(o);
		}
		ExtendedIterator<Node> tempNodes = GraphUtil.listSubjects(graph, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
		while (tempNodes.hasNext()) {
			Node tempNode = tempNodes.next();
			rootCandidates.remove(tempNode);
		}
		if (rootCandidates.size() == 1) {
			this.root = (Node) rootCandidates.toArray()[0];
		} else {
			for (Node n : rootCandidates) {
				if (graph.contains(Triple.create(n, typeNode, projectNode))) {
					this.root = n;
				}
			}
		}
		if (this.root == null) {
			System.err.println("Something went wrong.");
			System.err.println(this);
			System.err.println("Root candidates: ");
			for (Node n : rootCandidates) {
				System.err.println(n);
			}
		}
	}

	/**
	 * @param s The subject of this path pattern.
	 * @param o The object of this path pattern.
	 * @param p The graph representation of this property path.
	 */
	public RGraph(Node s, Node o, PGraph p) {
		Set<Var> vars = new HashSet<>();
		Graph graph = GraphFactory.createPlainGraph();
		Node tp = NodeFactory.createBlankNode();
		graph.add(Triple.create(tp, typeNode, triplePathNode));
		if (s.isVariable()) {
			vars.add(Var.alloc(s));
			graph.add(Triple.create(tp, subjectNode, NodeFactory.createBlankNode(s.getName())));
		} else if (s.isBlank()) {
			graph.add(Triple.create(tp, subjectNode, NodeFactory.createBlankNode(s.getBlankNodeLabel())));
		} else if (s.isURI()) {
			graph.add(Triple.create(tp, subjectNode, NodeFactory.createURI(s.getURI())));
		} else if (s.isLiteral()) {
			graph.add(Triple.create(tp, subjectNode, NodeFactory.createLiteralByValue(s.getLiteralValue(), s.getLiteralDatatype())));
		}
		if (o.isVariable()) {
			vars.add(Var.alloc(o));
			graph.add(Triple.create(tp, objNode, NodeFactory.createBlankNode(o.getName())));
		} else if (o.isBlank()) {
			graph.add(Triple.create(tp, objNode, NodeFactory.createBlankNode(o.getBlankNodeLabel())));
		} else if (o.isURI()) {
			graph.add(Triple.create(tp, objNode, NodeFactory.createURI(o.getURI())));
		} else if (o.isLiteral()) {
			graph.add(Triple.create(tp, objNode, NodeFactory.createLiteralByValue(o.getLiteralValue(), o.getLiteralDatatype())));
		}
		GraphUtil.addInto(graph, p.getMinimalDFA());
		Node n = NodeFactory.createBlankNode();
		graph.add(Triple.create(n, typeNode, pathNode));
		graph.add(Triple.create(n, argNode, p.getStartState()));
		graph.add(Triple.create(tp, preNode, n));
		this.graph = graph;
		this.vars = vars;
		this.root = tp;
	}

	/**
	 * @param s The subject of this path pattern.
	 * @param o The object of this path pattern.
	 * @param p The property path of this path pattern.
	 */
	public RGraph(Node s, Node o, Path p) {
		Set<Var> vars = new HashSet<>();
		Graph graph = GraphFactory.createPlainGraph();
		Node tp = NodeFactory.createBlankNode();
		graph.add(Triple.create(tp, typeNode, triplePathNode));
		if (s.isVariable()) {
			vars.add(Var.alloc(s));
		}
		graph.add(Triple.create(tp, subjectNode, getValidNode(s)));
		if (o.isVariable()) {
			vars.add(Var.alloc(o));
		}
		graph.add(Triple.create(tp, objNode, getValidNode(o)));
		Node predicate = NodeFactory.createLiteral(WriterPath.asString(p));
		graph.add(Triple.create(tp, preNode, predicate));
		this.graph = graph;
		this.vars = vars;
		this.root = tp;
	}

	/**
	 * @param table A table containing explicit values.
	 */
	public static RGraph table(Table table) {
		Node tableRoot = NodeFactory.createBlankNode();
		Graph graph = GraphFactory.createDefaultGraph();
		RGraph ans = new RGraph(tableRoot, graph, null);
		ans.graph.add(Triple.create(tableRoot, typeNode, tableNode));
		Iterator<Binding> iter = table.rows();
		while (iter.hasNext()) {
			Node rowNode = NodeFactory.createBlankNode();
			ans.graph.add(Triple.create(tableRoot, argNode, rowNode));
			Binding b = iter.next();
			Iterator<Var> vars = b.vars();
			while (vars.hasNext()) {
				Var var = vars.next();
				Node bindingNode = NodeFactory.createBlankNode();
				ans.graph.add(Triple.create(rowNode, argNode, bindingNode));
				ans.graph.add(Triple.create(bindingNode, typeNode, bindNode));
				ans.graph.add(Triple.create(bindingNode, varNode, NodeFactory.createBlankNode(var.getVarName())));
				Node value = b.get(var);
				ans.graph.add(Triple.create(bindingNode, valueNode, value));
			}
		}
		return ans;
	}

	/**
	 * @param n A node that may be a literal or a variable.
	 * @return A blank node if the input is a variable or a literal with the correct language or datatype otherwise.
	 */
	public Node getValidNode(Node n) {
		if (n.isVariable()) {
			Node ans = NodeFactory.createBlankNode(n.getName());
			graph.add(Triple.create(ans, typeNode, varNode));
			varMap.put(n,ans);
			return ans;
		}
		if (n.isLiteral()) {
			if (n.getLiteralLanguage().equals("")) {
				return NodeFactory.createLiteralByValue(n.getLiteralLexicalForm(), n.getLiteralDatatype());
			} else {
				return NodeFactory.createLiteral(n.getLiteralLexicalForm() + "@" + n.getLiteralLanguage());
			}
		} else {
			return n;
		}
	}

	/**
	 * @param t A triple pattern.
	 * @return A triple where all variable nodes have been replaced with blank nodes.
	 */
	public Triple getTripleWithVars(Triple t) {
		Node s = t.getSubject();
		if (s.isBlank()) {
			s = Var.alloc(s.getBlankNodeLabel());
		}
		Node p = t.getPredicate();
		if (p.isBlank()) {
			p = Var.alloc(p.getBlankNodeLabel());
		}
		Node o = t.getObject();
		if (o.isBlank()) {
			o = Var.alloc(o.getBlankNodeLabel());
		}
		return Triple.create(s, p, o);
	}


	/**
	 * @param s An RDF literal with a datatype.
	 * @return A node that represents a literal with a datatype. If no datatype is specified, it is assumed to be a string.
	 */
	public Node createLiteralWithType(String s) {
		Node ans;
		s = s.replaceAll("\"", "");
		if (s.contains("^^")) {
			ans = NodeFactory.createLiteralByValue(s.substring(0, s.indexOf("^^")), NodeFactory.getType(s.substring(1 + s.lastIndexOf("^")).replaceAll("[<>]", "")));
		} else {
			ans = NodeFactory.createLiteralByValue(s, XSDDatatype.XSDstring);
		}
		return ans;
	}

	/**
	 * Adds all the variables in another r-graph to this one.
	 *
	 * @param e An r-graph.
	 */
	public void addVars(RGraph e) {
		this.vars.addAll(e.vars);
	}

	/**
	 * Joins this r-graph with another r-graph. The result is a conjunction of both r-graphs. (Q_1 AND Q_2)
	 *
	 * @param arg1 An r-graph to join with this one.
	 */
	public void join(RGraph arg1) {
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, joinNode));
		Node type0 = GraphUtil.listObjects(graph, this.root, typeNode).next();
		Node type1 = GraphUtil.listObjects(arg1.graph, arg1.root, typeNode).next();
		if (!this.root.equals(root)) {
			graph.add(Triple.create(root, argNode, this.root));
		}
		if (!arg1.root.equals(root)) {
			graph.add(Triple.create(root, argNode, arg1.root));
		}
		if (type0.equals(joinNode)) {
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, this.root, argNode);
			graph.delete(Triple.create(this.root, typeNode, joinNode));
			graph.delete(Triple.create(root, argNode, this.root));
			while (args.hasNext()) {
				Node n = args.next();
				graph.add(Triple.create(root, argNode, n));
				graph.delete(Triple.create(this.root, argNode, n));
			}

		}
		if (type1.equals(joinNode)) {
			ExtendedIterator<Node> args = GraphUtil.listObjects(arg1.graph, arg1.root, argNode);
			arg1.graph.delete(Triple.create(arg1.root, typeNode, joinNode));
			graph.delete(Triple.create(root, argNode, arg1.root));
			while (args.hasNext()) {
				Node n = args.next();
				arg1.graph.add(Triple.create(root, argNode, n));
				arg1.graph.delete(Triple.create(arg1.root, argNode, n));
			}
			arg1.root = root;

		}
		GraphUtil.addInto(this.graph, arg1.graph);
		addVars(arg1);
		this.root = root;
	}

	/**
	 * Joins this r-graph with another r-graph. The result is the union of both r-graphs. (Q_1 UNION Q_2)
	 *
	 * @param arg1 An r-graph to join with this one.
	 */
	public void union(RGraph arg1) {
		Node root = NodeFactory.createBlankNode();
		boolean a = this.graph.contains(this.root, typeNode, unionNode);
		boolean b = arg1.graph.contains(arg1.root, typeNode, unionNode);
		if (a && b) {
			ExtendedIterator<Node> nodes = GraphUtil.listObjects(arg1.graph, arg1.root, argNode);
			while (nodes.hasNext()) {
				Node n = nodes.next();
				this.graph.add(Triple.create(this.root, argNode, n));
				GraphUtil.remove(arg1.graph, arg1.root, argNode, n);
			}
			GraphUtil.remove(arg1.graph, arg1.root, typeNode, unionNode);
		} else if (a) {
			this.graph.add(Triple.create(this.root, argNode, arg1.root));
		} else if (b) {
			this.graph.add(Triple.create(arg1.root, argNode, this.root));
			this.root = arg1.root;
		} else {
			graph.add(Triple.create(root, typeNode, unionNode));
			graph.add(Triple.create(root, argNode, this.root));
			graph.add(Triple.create(root, argNode, arg1.root));
			this.root = root;
		}
		addVars(arg1);
		GraphUtil.addInto(this.graph, arg1.graph);
	}

	/**
	 * Joins this r-graph with another r-graph. This includes the second r-graph as an optional query pattern. (Q_1 OPT Q_2)
	 *
	 * @param arg1 An r-graph that represents an optional query pattern.
	 */
	public void optional(RGraph arg1) {
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, optionalNode));
		graph.add(Triple.create(root, leftNode, this.root));
		graph.add(Triple.create(root, rightNode, arg1.root));
		this.root = root;
		addVars(arg1);
		GraphUtil.addInto(this.graph, arg1.graph);
	}

	public void minus(RGraph arg1) {
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, minusNode));
		graph.add(Triple.create(root, leftNode, this.root));
		graph.add(Triple.create(root, rightNode, arg1.root));
		this.root = root;
		addVars(arg1);
		GraphUtil.addInto(this.graph, arg1.graph);
	}

	/**
	 * @param arg1 An r-graph that represents a BIND expression (BIND var AS expr) and adds it to this r-graph.
	 */
	public void bind(RGraph arg1) {
		checkForAnonVars(arg1);
		Node n = getPatternNode();
		graph.add(Triple.create(n, extendNode, arg1.root));
		GraphUtil.addInto(this.graph, arg1.graph);
	}

	public void checkForAnonVars(RGraph arg1) {
		for (Var v : exprMap.keySet()) {
			Node vNode = NodeFactory.createBlankNode(v.getVarName());
			ExtendedIterator<Node> ops = GraphUtil.listSubjects(arg1.graph, argNode, vNode);
			while (ops.hasNext()) {
				Node op = ops.next();
				arg1.graph.delete(Triple.create(op, argNode, vNode));
				arg1.graph.add(Triple.create(op, argNode, exprMap.get(v)));
			}
			ExtendedIterator<Node> blanks = GraphUtil.listSubjects(arg1.graph, valueNode, vNode);
			while (blanks.hasNext()) {
				Node blank = blanks.next();
				arg1.graph.delete(Triple.create(blank, valueNode, vNode));
				arg1.graph.add(Triple.create(blank, valueNode, exprMap.get(v)));
			}
		}
	}

	/**
	 * Creates an r-graph that represents each of the assignments in a BIND expression (BIND var AS expr) and adds it to this r-graph.
	 *
	 * @param expr An expression in an assignment.
	 * @param var  The variable the expression is assigned to.
	 */
	public void bindNode(Node expr, Var var) {
		graph.add(Triple.create(this.root, varNode, NodeFactory.createBlankNode(var.getVarName())));
		graph.add(Triple.create(this.root, typeNode, bindNode));
		graph.add(Triple.create(this.root, argNode, expr));
	}

	/**
	 * Creates an r-graph that represents a FILTER expression (FILTER expr) and adds it to this r-graph.
	 *
	 * @param n A node that represents an expression.
	 */
	public void filter(Node n) {
		Node filter = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, modNode, filter));
		graph.add(Triple.create(filter, typeNode, filterNode));
		graph.add(Triple.create(filter, argNode, n));
	}

	/**
	 * Creates an r-graph that represents a FILTER expression (FILTER expr) and adds it to this r-graph.
	 *
	 * @param arg1 An r-graph that represents an expression.
	 */
	public void filter(RGraph arg1) {
		checkForAnonVars(arg1);
		Node n = getPatternNode();
		Node filter = NodeFactory.createBlankNode();
		graph.add(Triple.create(n, filterNode, filter));
		graph.add(Triple.create(filter, argNode, arg1.root));
		GraphUtil.addInto(this.graph, arg1.graph);
	}

	/**
	 * Creates an r-graph that represents the conjunction of two expressions (FILTER expr1 && expr2) and adds it to this r-graph .
	 *
	 * @param arg1 An r-graph that represents an expression.
	 * @param arg2 An r-graph that represents an expression.
	 * @return Returns the node that represents the AND operator.
	 */
	public Node filterAnd(Node arg1, Node arg2) {
		Node o = NodeFactory.createBlankNode();
		graph.add(Triple.create(o, typeNode, andNode));
		graph.add(Triple.create(o, argNode, arg1));
		graph.add(Triple.create(o, argNode, arg2));
		return o;
	}

	/**
	 * Creates an r-graph that represents the disjunction of two expressions (FILTER expr1 || expr2) and adds it to this r-graph.
	 *
	 * @param arg1 An r-graph that represents an expression.
	 * @param arg2 An r-graph that represents an expression.
	 * @return Returns the node that represents the OR operator.
	 */
	public Node filterOr(Node arg1, Node arg2) {
		Node o = NodeFactory.createBlankNode();
		graph.add(Triple.create(o, typeNode, orNode));
		graph.add(Triple.create(o, argNode, arg1));
		graph.add(Triple.create(o, argNode, arg2));
		return o;
	}

	/**
	 * Creates an r-graph that represents the negation of an expression (FILTER !expr) and adds it to this r-graph.
	 *
	 * @param arg1 An r-graph that represents an expression.
	 * @return Returns the node that represents the NOT operator.
	 */
	public Node filterNot(Node arg1) {
		Node o = NodeFactory.createBlankNode();
		graph.add(Triple.create(o, typeNode, notNode));
		graph.add(Triple.create(o, argNode, arg1));
		return o;
	}

	/**
	 * Creates an r-graph that represents a function over an expression (FILTER (f expr)) and adds it to this r-graph.
	 *
	 * @param op   A node that represents a SPARQL function.
	 * @param arg1 A node that represents an expression.
	 * @return Returns the node that represents the function.
	 */
	public Node filterFunction(Node op, Node arg1) {
		Node n = NodeFactory.createBlankNode();
		Node a = NodeFactory.createBlankNode();
		if (!GraphUtil.listObjects(graph, arg1, functionNode).hasNext()) {
			graph.add(Triple.create(a, valueNode, arg1));
		} else {
			a = arg1;
		}
		graph.add(Triple.create(n, functionNode, op));
		graph.add(Triple.create(n, argNode, a));
		return n;
	}

	public Node filterFunction(String op, Node arg1) {
		return filterFunction(NodeFactory.createLiteral(op), arg1);
	}

	/**
	 * Creates an r-graph that represents a binary SPARQL function (FILTER (f expr1 expr2)) and adds it to this r-graph.
	 *
	 * @param op   A string that represents a binary SPARQL function.
	 * @param arg1 A node that represents an expression.
	 * @param arg2 A node that represents an expression.
	 * @return Returns a node that represents the function.
	 */
	public Node filterFunction(String op, Node arg1, Node arg2) {
		Node n = NodeFactory.createBlankNode();
		Node o = NodeFactory.createLiteral(op);
		Node a = NodeFactory.createBlankNode();
		Node b = NodeFactory.createBlankNode();
		graph.add(Triple.create(n, functionNode, o));
		if (!GraphUtil.listObjects(graph, arg1, functionNode).hasNext()) {
			graph.add(Triple.create(a, valueNode, arg1));
		} else {
			a = arg1;
		}
		if (!GraphUtil.listObjects(graph, arg2, functionNode).hasNext()) {
			graph.add(Triple.create(b, valueNode, arg2));
		} else {
			b = arg2;
		}
		graph.add(Triple.create(n, argNode, a));
		graph.add(Triple.create(n, argNode, b));
		if (isOrderedFunction(op)) {
			graph.add(Triple.create(a, orderNode, NodeFactory.createLiteralByValue(0, XSDDatatype.XSDint)));
			graph.add(Triple.create(b, orderNode, NodeFactory.createLiteralByValue(1, XSDDatatype.XSDint)));
		}
		return n;
	}

	public Node filterFunction(Node o, List<Node> nodes) {
		int i = 0;
		Node n = NodeFactory.createBlankNode();
		String op = o.isURI() ? o.getURI() : o.getLiteralLexicalForm();
		graph.add(Triple.create(n, functionNode, o));
		for (Node node : nodes) {
			Node newNode = NodeFactory.createBlankNode();
			if (!GraphUtil.listObjects(graph, node, functionNode).hasNext()) {
				graph.add(Triple.create(n, argNode, newNode));
				graph.add(Triple.create(newNode, valueNode, node));
				if (isOrderedFunction(op) || o.isURI()) {
					graph.add(Triple.create(newNode, orderNode, NodeFactory.createLiteralByValue(i++, XSDDatatype.XSDint)));
				}
			} else {
				graph.add(Triple.create(n, argNode, node));
				if (isOrderedFunction(op) || o.isURI()) {
					graph.add(Triple.create(node, orderNode, NodeFactory.createLiteralByValue(i++, XSDDatatype.XSDint)));
				}
			}
		}
		return n;
	}

	public void filterNormalisation() {
		Graph before = GraphFactory.createPlainGraph();
		while (!before.isIsomorphicWith(graph)) {
			before = GraphFactory.createPlainGraph();
			GraphUtil.addInto(before, graph);
			UpdateAction.execute(conjunctionRule, graph);
			UpdateAction.execute(disjunctionRule, graph);
		}
	}

	public static RGraph group(OpGroup arg) {
		Node root = NodeFactory.createBlankNode();
		RGraph ans = new RGraph(root, GraphFactory.createDefaultGraph(), new HashSet<>());
		ans.graph.add(Triple.create(root, typeNode, groupByNode));
		Node varNode = NodeFactory.createBlankNode();
		ans.graph.add(Triple.create(root, argNode, varNode));
		VarExprList vExpr = arg.getGroupVars();
		for (Var v : vExpr.getVars()) {
			ans.graph.add(Triple.create(varNode, valueNode, NodeFactory.createBlankNode(v.getVarName())));
		}
		Map<Var, Expr> varsExpr = vExpr.getExprs();
		for (Map.Entry<Var, Expr> m : varsExpr.entrySet()) {
			Var v = m.getKey();
			FilterVisitor fv = new FilterVisitor();
			Node vNode = NodeFactory.createBlankNode();
			ans.exprMap.put(v, vNode);
			ExprWalker.walk(fv, m.getValue());
			Node r = fv.getGraph().root;
			GraphUtil.addInto(ans.graph, fv.getGraph().graph);
			ans.graph.add(Triple.create(vNode, valueNode, r));
		}
		return ans;
	}

	public Node getPatternNode() {
		if (!graph.contains(Triple.create(root, typeNode, extraNode))) {
			Node n = NodeFactory.createBlankNode();
			graph.add(Triple.create(n, subNode, root));
			graph.add(Triple.create(n, typeNode, extraNode));
			root = n;
		}
		return root;
	}

	/**
	 * @param group An r-graph representing a GROUP BY expression.
	 */
	public void groupBy(RGraph group) {
		Node n = getPatternNode();
		graph.add(Triple.create(n, argNode, group.root));
		GraphUtil.addInto(graph, group.graph);
		ExtendedIterator<Node> vars = listSubjects(group.graph, typeNode, varNode);
		while (vars.hasNext()) {
			Node v = vars.next();
			if (GraphUtil.listObjects(group.graph, v, functionNode).hasNext()) {
				graph.delete(Triple.create(v, typeNode, varNode));
			}
		}
	}

	/**
	 * @param args A list of r-graphs that represent aggregation expressions.
	 */
	public void aggregation(RGraph r, List<RGraph> args) {
		Node n = getPatternNode();
		Node extend = NodeFactory.createBlankNode();
		graph.add(Triple.create(r.root, patternNode, extend));
		graph.add(Triple.create(extend, typeNode, aggregateNode));
		graph.add(Triple.create(n, argNode, r.root));
		GraphUtil.addInto(this.graph, r.graph);
		for (RGraph arg : args) {
			graph.add(Triple.create(extend, argNode, arg.root));
			exprMap.putAll(arg.exprMap);
			GraphUtil.addInto(this.graph, arg.graph);
		}
	}

	/**
	 * @param op       A name of the aggregate function.
	 * @param distinct True if distinct.
	 * @param var      The variable to which the function is assigned.
	 * @param arg      A node pointing to a variable or expression.
	 * @return A node pointing to this aggregate function.
	 */
	public Node aggregationFunction(String op, boolean distinct, Var var, Node arg) {
		Node n = getAggregateNode(op, distinct, var);
		if (!GraphUtil.listObjects(graph, arg, functionNode).hasNext()) {
			Node a = NodeFactory.createBlankNode();
			graph.add(Triple.create(n, argNode, a));
			graph.add(Triple.create(a, valueNode, arg));
			graph.add(Triple.create(arg, typeNode, varNode));
		} else {
			graph.add(Triple.create(n, argNode, arg));
		}
		return n;
	}

	/**
	 * @param op
	 * @param var
	 * @param arg
	 * @return
	 */
	public Node aggregationCount(String op, boolean distinct, Var var, Node arg) {
		Node n = getAggregateNode(op, distinct, var);
		graph.add(Triple.create(n, argNode, arg));
		graph.add(Triple.create(arg, valueNode, NodeFactory.createLiteral("*")));
		return n;
	}

	public Node getAggregateNode(String op, boolean distinct, Var var) {
		Node n = NodeFactory.createBlankNode();
		if (var != null) {
			exprMap.put(var, n);
		}
		Node o = NodeFactory.createLiteral(op);
		graph.add(Triple.create(n, functionNode, o));
		if (distinct) {
			graph.add(Triple.create(n, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
		}
		return n;
	}

	/**
	 * @param s A string that represents a SPARQL function.
	 * @return Returns true if the function is ordered (i.e (f expr1 expr2) != (f expr2 expr1)).
	 */
	public boolean isOrderedFunction(String s) {
		switch (s) {
			case "<":
			case "concat":
			case ">":
			case "<=":
			case ">=":
			case "-":
			case "/":
			case "regex":
			case "if":
			case "in":
			case "notin":
			case "replace":
			case "strdt":
			case "strlang":
			case "strstarts":
			case "strends":
			case "contains":
			case "strbefore":
			case "strafter":
			case "substr":
				return true;
			default:
				return false;
		}
	}

	/**
	 * Creates an r-graph that represents the projection of variables (SELECT vars) and adds it to this r-graph.
	 *
	 * @param vars A collection of projected variables.
	 */
	public void project(Collection<Var> vars) {
		// Used to check if there's a projection node
		if (!isProjection()) {
			Node root = NodeFactory.createBlankNode();
			graph.add(Triple.create(root, typeNode, projectNode));
			graph.add(Triple.create(root, opNode, this.root));
			this.root = root;
		} else {
			List<Var> varList = new ArrayList<>();
			ExtendedIterator<Node> varNodes = GraphUtil.listObjects(graph, root, argNode);
			while (varNodes.hasNext()) {
				varList.add(Var.alloc(varNodes.next().getBlankNodeLabel()));
			}
			if (!varList.isEmpty()) {
				if (!(varList.containsAll(vars) && vars.containsAll(varList))) {
					Node root = NodeFactory.createBlankNode();
					graph.add(Triple.create(root, typeNode, projectNode));
					graph.add(Triple.create(root, opNode, this.root));
					this.root = root;
				}
			}
		}
		if (this.vars != null) {
			this.vars.addAll(vars);
		}
		for (Var v : vars) {
			graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));
		}
	}

	public void project(Collection<Var> vars, int n) {
		this.project(vars);
		graph.add(Triple.create(root, idNode, NodeFactory.createLiteralByValue(n, XSDDatatype.XSDinteger)));
	}

	public void ask() {
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, askNode));
		graph.add(Triple.create(root, opNode, this.root));
		if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)) {
			graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
		}
		for (Var v : vars) {
			graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));
		}
		this.root = root;
	}

	public void construct(Template template) {
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, constructNode));
		graph.add(Triple.create(root, opNode, this.root));
		if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)) {
			graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
		}
		if (template != null) {
			BasicPattern bp = template.getBGP();
			RGraph rg = new RGraph(bp.getList());
			graph.add(Triple.create(root, argNode, rg.root));
			GraphUtil.addInto(graph, rg.graph);
		}
		this.root = root;
	}

	public void describe() {
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, describeNode));
		graph.add(Triple.create(root, opNode, this.root));
		if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)) {
			graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
		}
		for (Var v : vars) {
			graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));
		}
		this.root = root;
	}

	/**
	 * Creates a node that indicates if the DISTINCT keyword has been used, and adds it to this r-graph.
	 *
	 * @param isDistinct A boolean that is true if the DISTINCT keyword has been used, and false otherwise.
	 */
	public void setDistinctNode(boolean isDistinct) {
		if (isProjection()) {
			graph.add(Triple.create(root, distinctNode, NodeFactory.createLiteralByValue(isDistinct, XSDDatatype.XSDboolean)));
		} else if (isConstruction()) {
			graph.add(Triple.create(root, distinctNode, NodeFactory.createLiteralByValue(isDistinct, XSDDatatype.XSDboolean)));
		} else {
			Node p = NodeFactory.createBlankNode();
			graph.add(Triple.create(p, typeNode, projectNode));
			graph.add(Triple.create(p, opNode, root));
			graph.add(Triple.create(p, distinctNode, NodeFactory.createLiteralByValue(isDistinct, XSDDatatype.XSDboolean)));
			this.root = p;
		}
	}

	public void setReducedNode(boolean isReduced) {
		if (isProjection()) {
			graph.add(Triple.create(root, reducedNode, NodeFactory.createLiteralByValue(isReduced, XSDDatatype.XSDboolean)));
		} else if (isConstruction()) {
			graph.add(Triple.create(root, reducedNode, NodeFactory.createLiteralByValue(isReduced, XSDDatatype.XSDboolean)));
		} else {
			Node p = NodeFactory.createBlankNode();
			graph.add(Triple.create(p, typeNode, projectNode));
			graph.add(Triple.create(p, opNode, root));
			graph.add(Triple.create(p, reducedNode, NodeFactory.createLiteralByValue(isReduced, XSDDatatype.XSDboolean)));
			this.root = p;
		}
	}

	/**
	 * @return Returns true if the DISTINCT keyword has been used, and false otherwise.
	 */
	public boolean isDistinct() {
		return this.graph.contains(root, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
	}

	/**
	 * Creates an r-graph that represents the RDF datasets that form the default graph, and adds it to this r-graph.
	 *
	 * @param g A collection of strings that identify RDF datasets.
	 */
	public void fromGraph(Collection<String> g) {
		Node n = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, modNode, n));
		graph.add(Triple.create(n, typeNode, fromNode));
		for (String s : g) {
			graph.add(Triple.create(n, argNode, NodeFactory.createURI(s)));
		}
	}

	/**
	 * Creates an r-graph that represents the RDF datasets that define the named graphs, and adds it to this r-graph.
	 *
	 * @param g A collection of strings that identify RDF datasets.
	 */
	public void fromNamedGraph(Collection<String> g) {
		Node aux;
		ExtendedIterator<Node> e = listSubjects(graph, typeNode, fromNode);
		if (e.hasNext()) {
			aux = e.next();
		} else {
			aux = NodeFactory.createBlankNode();
			graph.add(Triple.create(aux, typeNode, fromNode));
		}
		Node n = NodeFactory.createBlankNode();
		graph.add(Triple.create(aux, modNode, n));
		graph.add(Triple.create(n, typeNode, fromNamedNode));
		for (String s : g) {
			graph.add(Triple.create(n, argNode, NodeFactory.createURI(s)));
		}
	}

	/**
	 * Creates an r-graph that represents an ORDER BY clause, and adds it to this r-graph.
	 *
	 * @param exprs The list of expressions according to which the results are ordered.
	 * @param dir   A list of integers that indicates if results are ordered ascending or descending.
	 */

	public void orderBy(List<Expr> exprs, List<Integer> dir) {
		if (!isProjection()) {
			Node project = NodeFactory.createBlankNode();
			graph.add(Triple.create(project, typeNode, projectNode));
			graph.add(Triple.create(project, opNode, root));
			this.root = project;
		}
		Node order = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, modNode, order));
		graph.add(Triple.create(order, typeNode, orderByNode));
		for (int i = 0; i < exprs.size(); i++) {
			Node auxNode = NodeFactory.createBlankNode();
			graph.add(Triple.create(order, argNode, auxNode));
			if (exprs.get(i).isVariable()) {
				graph.add(Triple.create(auxNode, valueNode, NodeFactory.createBlankNode(exprs.get(i).getVarName())));
			} else {
				FilterVisitor fv = new FilterVisitor();
				ExprWalker.walk(fv, exprs.get(i));
				RGraph rg = fv.getGraph();
				graph.add(Triple.create(auxNode, valueNode, rg.root));
				GraphUtil.addInto(this.graph, rg.graph);
			}
			graph.add(Triple.create(auxNode, orderNode, NodeFactory.createLiteralByValue(i, XSDDatatype.XSDint)));
			graph.add(Triple.create(auxNode, dirNode, NodeFactory.createLiteralByValue(dir.get(i), XSDDatatype.XSDint)));
		}
	}

	/**
	 * Creates an r-graph that represents a combination of LIMIT and OFFSET, and adds it to this r-graph.
	 *
	 * @param offset A number that causes the solutions to start at the specified number. An offset of 0 has no effect.
	 * @param limit  A number that specifies the number of solutions to return.
	 */
	public void slice(int offset, int limit) {
		//Node order = GraphUtil.listSubjects(graph, typeNode, projectNode).next();
		if (!isProjection()) {
			Node root = NodeFactory.createBlankNode();
			graph.add(Triple.create(root, typeNode, projectNode));
			graph.add(Triple.create(root, opNode, this.root));
			this.root = root;
		}
		Node lNode = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, modNode, lNode));
		graph.add(Triple.create(lNode, typeNode, limitNode));
		graph.add(Triple.create(lNode, offsetNode, NodeFactory.createLiteralByValue(offset, XSDDatatype.XSDint)));
		graph.add(Triple.create(lNode, valueNode, NodeFactory.createLiteralByValue(limit, XSDDatatype.XSDint)));
	}

	public void graphOp(Node n) {
		Node r = NodeFactory.createBlankNode();
		graph.add(Triple.create(r, typeNode, graphNode));
		if (n.isVariable()) {
			n = NodeFactory.createBlankNode(n.getName());
		}
		graph.add(Triple.create(r, valueNode, n));
		graph.add(Triple.create(r, argNode, this.root));
		this.root = r;
	}

	public void turnDistinctOn() {
		this.distinct = true;
	}

	public void setLeaning(boolean b) {
		this.leaning = b;
	}

	public boolean isProjection() {
		ExtendedIterator<Node> nodes = listObjects(graph, root, typeNode);
		if (nodes.hasNext()) {
			Node type = listObjects(graph, root, typeNode).next();
			return type.equals(projectNode);
		} else {
			return false;
		}
	}

	public boolean isConstruction() {
		ExtendedIterator<Node> nodes = listObjects(graph, root, typeNode);
		if (nodes.hasNext()) {
			Node type = listObjects(graph, root, typeNode).next();
			return type.equals(constructNode);
		} else {
			return false;
		}
	}

	public boolean containsProjection() {
		return listSubjects(graph, typeNode, projectNode).hasNext();
	}

	public boolean containsUnion() {
		return listSubjects(graph, typeNode, unionNode).hasNext();
	}

	public boolean containsJoin() {
		return listSubjects(graph, typeNode, joinNode).hasNext();
	}

	public Model asModel() {
		return ModelFactory.createModelForGraph(graph);
	}

	public String toString() {
		StringBuilder ans = new StringBuilder();
		ExtendedIterator<Triple> e = GraphUtil.findAll(this.graph);
		while (e.hasNext()) {
			Triple t = e.next();
			ans.append(t.toString());
			ans.append("\n");
		}
		return ans.toString();
	}

	public void print() {
		System.out.println(this);
		System.out.println();
	}

	/**
	 * @return Returns a set containing all triples in this r-graph.
	 */
	public TreeSet<org.semanticweb.yars.nx.Node[]> getTriples() {
		Model model = this.asModel();
		JenaModelIterator jmi = new JenaModelIterator(model);
		TreeSet<org.semanticweb.yars.nx.Node[]> triples = new TreeSet<>(org.semanticweb.yars.nx.NodeComparator.NC);
		while (jmi.hasNext()) {
			org.semanticweb.yars.nx.Node[] triple = jmi.next();
			triples.add(new org.semanticweb.yars.nx.Node[]{triple[0], triple[1], triple[2]});
		}
		return triples;
	}

	public GraphLeaningResult DFSLeaning(Collection<org.semanticweb.yars.nx.Node[]> triples) throws InterruptedException {
		return this.DFSLeaning(triples,false,false);
	}

	public GraphLeaningResult DFSLeaning(Collection<org.semanticweb.yars.nx.Node[]> triples, boolean randomiseBindings, boolean prune) throws InterruptedException {
		DFSGraphLeaning dfsgl = new DFSGraphLeaning(triples,randomiseBindings,prune);
		return dfsgl.call();
	}

	/**
	 * @param triples A collection of RDF triples.
	 * @return A canonical labeling of all blank nodes.
	 * @throws InterruptedException
	 * @throws HashCollisionException
	 */
	public GraphLabellingResult label(Collection<org.semanticweb.yars.nx.Node[]> triples) throws InterruptedException, HashCollisionException {
		GraphLabellingArgs gla = new GraphLabellingArgs();
		gla.setDistinguishIsoPartitions(false);
		GraphLabelling gl = new GraphLabelling(triples, gla);
		return gl.call();
	}

	/**
	 * Iteratively performs a SPARQL UPDATE query until no changes are made.
	 *
	 * @param request A SPARQL UPDATE query.
	 */
	public void iterativeUpdate(UpdateRequest request) {
		Graph before = GraphFactory.createPlainGraph();
		while (!before.isIsomorphicWith(graph)) {
			before = GraphFactory.createPlainGraph();
			GraphUtil.addInto(before, graph);
			UpdateAction.execute(request, graph);
		}
	}

	public void subQueryRelabelling() {
		ExtendedIterator<Node> projections = GraphUtil.listSubjects(graph,typeNode,projectNode);
		if (projections.hasNext()) {
			List<Node> projectionList = projections.toList();
			int n = 0;
			if (projectionList.size() > 1) {
				Map<Node,Graph> map = new HashMap<>();
				for (Node p : projectionList) {
					graph.add(Triple.create(p,idNode,NodeFactory.createLiteralByValue(n++,XSDDatatype.XSDinteger)));
					GraphExtract ge = new GraphExtract(new CustomTripleBoundary(projectionList,p));
					Graph g = ge.extract(p,graph);
					map.put(p,g);
				}
				List<Graph> subGraphs = new ArrayList<>();
				for (Map.Entry<Node, Graph> entry : map.entrySet()) {
					Graph currentGraph = entry.getValue();
					Node currentNode = entry.getKey();
					List<Node> currentProjectionNodes = GraphUtil.listObjects(currentGraph,currentNode,argNode).toList();
					Node id = GraphUtil.listObjects(currentGraph,currentNode,idNode).hasNext() ? GraphUtil.listObjects(currentGraph,currentNode,idNode).next() : NodeFactory.createLiteralByValue(0,XSDDatatype.XSDinteger);
					ExtendedIterator<Node> nodes = GraphUtil.listSubjects(currentGraph,typeNode,varNode);
					while (nodes.hasNext()) {
						Node node = nodes.next();
						if (node.isBlank()) {
							if (!currentProjectionNodes.contains(node)) {
								currentGraph.add(Triple.create(node,idNode,id));
							}
						}
					}
					UpdateAction.execute(subgraphRule,currentGraph);
					subGraphs.add(currentGraph);
					currentGraph.delete(Triple.create(currentNode,idNode,id));
				}
				Graph ans = GraphFactory.createDefaultGraph();
				for (Graph g : subGraphs) {
					GraphUtil.addInto(ans,g);
				}
				this.graph = ans;
			}
			else {
				Node p = projectionList.get(0);
				ExtendedIterator<Node> ids = GraphUtil.listObjects(graph,p,idNode);
				if (ids.hasNext()) {
					Node id = ids.next();
					graph.delete(Triple.create(p,idNode,id));
				}
			}
		}
	}
	
	/**
	 * Performs a leaning of the current r-graph.
	 * @return Returns the lean form of this graph.
	 * @throws InterruptedException
	 */
	public RGraph getLeanForm() throws InterruptedException {
		GraphLeaningResult glResult = this.DFSLeaning(getTriples());
		Collection<org.semanticweb.yars.nx.Node[]> nodes = glResult.getLeanData();
		return new RGraph(nodes);
	}

	public RGraph canonicalLabel(boolean verbose) throws HashCollisionException, InterruptedException {
		GraphLabellingResult glr = this.label(this.getTriples());
		if (verbose){
			System.out.println("Labelling results: \n");
			System.out.println("Number of blank nodes: "+glr.getBnodeCount());
			System.out.println("Number of colouring iterations: "+glr.getColourIterationCount());
			System.out.println("Number of partitions found: "+glr.getPartitionCount());
		}
		RGraph ans = new RGraph(glr.getGraph());
		ans.varMap = this.varMap;
		HashMap<org.semanticweb.yars.nx.Node, HashCode> map = glr.getHashGraph().getBlankNodeHashes();
		for (Map.Entry<org.semanticweb.yars.nx.Node,HashCode> entry : map.entrySet()) {
			Node n = nxToJenaNode(entry.getKey());
			if (varMap.inverse().containsKey(n)) {
				Node in = varMap.inverse().get(n);
				varMap.put(in,NodeFactory.createBlankNode("SK00"+entry.getValue().toString()));
			}
		}
		return ans;
	}
	
	/**
	 * @param verbose A boolean that allows messages to appear during canonicalisation.
	 * @return Returns the canonical form of the r-graph.
	 * @throws InterruptedException
	 * @throws HashCollisionException
	 */
	public RGraph getCanonicalForm(boolean verbose) throws InterruptedException, HashCollisionException{
		if (verbose){
			print();
			System.out.println("CQ Normalisation");
		}
		boolean distinct = this.graph.contains(this.root, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
		if (leaning && distinct){
			long t  = System.nanoTime();
			if (verbose){
				System.out.println("Branch relabelling");
			}
			try{
				if (!containsPaths) {
					subQueryRelabelling();
				}
			}
			catch (Exception e){
				e.printStackTrace();
			}
			if (verbose){
				print();
				System.out.println("UCQ minimisation");
			}
			RGraph ans;
			if (containsPaths) {
				ans = this;
//				ans = uc2rpqMinimisation();
			}
			else {
				ans = ucqMinimisation();
			}
			if (verbose){
				ans.print();
				System.out.println("Beginning leaning.");
			}
			ExtendedIterator<Node> vars = GraphUtil.listSubjects(ans.graph,typeNode,varNode);
			while (vars.hasNext()) {
				Node var = vars.next();
				ans.graph.delete(Triple.create(var,typeNode,varNode));
			}
			long time1 = System.nanoTime() - t;
			UpdateAction.execute(duplicatesRule,ans.graph);
			if (verbose){
				System.out.println("Beginning labelling");
			}
			t = System.nanoTime();
			if (verbose){
				ans.print();
			}
			if (canonical) {
				long time = System.nanoTime();
				ans = ans.canonicalLabel(verbose);
				time = System.nanoTime() - time;
				ans.setLabelTime(time);
			}
			ans.setMinimisationTime(time1);
			return ans;
		}
		else{
			try{
				if (!containsPaths) {
					subQueryRelabelling();
				}
			}
			catch (Exception e){
				e.printStackTrace();
			}
			ExtendedIterator<Node> vars = GraphUtil.listSubjects(graph,typeNode,varNode);
			while (vars.hasNext()) {
				Node var = vars.next();
				graph.delete(Triple.create(var,typeNode,varNode));
			}
			if (canonical) {
				long time = System.nanoTime();
				RGraph ans = canonicalLabel(verbose);
				time = System.nanoTime() - time;
				ans.setLabelTime(time);
				return ans;
			}
			else {
				return this;
			}
		}
	}

	public Set<Node> listMonotoneParts() {
		Set<Node> ans = new HashSet<>();
		ExtendedIterator<Node> cqs = listSubjects(graph,typeNode,joinNode);
		ExtendedIterator<Node> ucqs = listSubjects(graph,typeNode,unionNode);
		while (cqs.hasNext()) {
			Node cq = cqs.next();
			if (isUCQ(cq)) {
				ans.add(cq);
			}
		}
		while (ucqs.hasNext()) {
			Node ucq = ucqs.next();
			if (isUCQ(ucq)) {
				ans.add(ucq);
				ExtendedIterator<Node> cqsInUnion = GraphUtil.listObjects(graph,ucq,argNode);
				while (cqsInUnion.hasNext()) {
					Node cq = cqsInUnion.next();
					ans.remove(cq);
				}
			}
		}
		return ans;
	}

	public RGraph ucqMinimisation() throws InterruptedException {
		List<RGraph> result = new ArrayList<>();
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		Set<Node> projectedVars = new HashSet<>();
		Set<Node> ucqs = listMonotoneParts();
		for (Node ucq : ucqs) {
			projectedVars = new HashSet<>();
			ExtendedIterator<Node> projectionNodes = GraphUtil.listSubjects(graph,typeNode,projectNode);
			while (projectionNodes.hasNext()) {
				Node currentProjection = projectionNodes.next();
				ExtendedIterator<Node> currentArgs = GraphUtil.listObjects(graph,currentProjection,argNode);
				while (currentArgs.hasNext()) {
					Node currentArg = currentArgs.next();
					projectedVars.add(currentArg);
				}
			}
			if (graph.contains(Triple.create(ucq, typeNode, unionNode))) { //Make sure it's a union of conjunctive queries.
				Node union = ucq;
				Node subjectUnion, predicateUnion;
				if (listSubjects(graph, opNode, union).hasNext()) {
					subjectUnion = listSubjects(graph, opNode, union).next();
					predicateUnion = opNode;
				} else if (listSubjects(graph, leftNode, union).hasNext()) {
					subjectUnion = listSubjects(graph, leftNode, union).next();
					predicateUnion = leftNode;
				} else if (listSubjects(graph, rightNode, union).hasNext()) {
					subjectUnion = listSubjects(graph, rightNode, union).next();
					predicateUnion = rightNode;
				} else if (listSubjects(graph,subNode,union).hasNext()) {
					subjectUnion = listSubjects(graph,subNode,union).next();
					predicateUnion = subNode;
				}
				else if (listSubjects(graph,valueNode,union).hasNext()) {
					subjectUnion = listSubjects(graph,valueNode,union).next();
					predicateUnion = valueNode;
				}
				else {
					subjectUnion = listSubjects(graph, argNode, union).next();
					predicateUnion = argNode;
				}
				ExtendedIterator<Node> cQueries = GraphUtil.listObjects(graph, union, argNode);
				cQueries = GraphUtil.listObjects(graph, union, argNode);
				if (GraphUtil.listObjects(graph, union, argNode).toList().size() > 1) { //More than one BGP in union
					Graph inner = ge.extract(union, graph);
					Graph outer = GraphFactory.createPlainGraph();
					CustomTripleBoundary ctb = new CustomTripleBoundary(Collections.singletonList(union),null);
					GraphExtract extract = new GraphExtract(ctb);
					outer = extract.extract(root, graph);
					List<Node> innerVars = GraphUtil.listSubjects(inner,typeNode,varNode).toList();
					List<Node> outerVars = GraphUtil.listSubjects(outer,typeNode,varNode).toList();
					outer.remove(subjectUnion, predicateUnion, union);
					for (Node p : innerVars) {
						inner.delete(Triple.create(p,typeNode,varNode));
					}
					for (Node p : outerVars) {
						outer.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
						outer.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
						outer.add(Triple.create(p, typeNode, varNode));
						inner.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
						inner.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
						inner.add(Triple.create(p,typeNode,varNode));
					}
					//Iterate through all conjunctive queries.
					while (cQueries.hasNext()) {
						Node cRoot = cQueries.next();
						Graph cGraph = ge.extract(cRoot, inner);
						//Check all variables in filter clauses.
						for (Node f : outerVars) {
							cGraph.add(Triple.create(f, typeNode, varNode));
							cGraph.add(Triple.create(f,valueNode,NodeFactory.createLiteral(f.getBlankNodeLabel())));
							cGraph.add(Triple.create(f,tempNode,NodeFactory.createLiteralByValue(true,XSDDatatype.XSDboolean)));
						}
						result.add(cqMinimisation(cRoot,cGraph));
					}
					result = containmentChecks(result);
					RGraph eg = new RGraph(root, outer, vars);
					Node uNode = NodeFactory.createBlankNode();
					//If there's only a single non-redundant CQ.
					if (result.size() == 1) {
						RGraph e = result.get(0);
						Node eRoot = e.root;
						GraphUtil.addInto(eg.graph, e.graph);
						eg.graph.add(Triple.create(subjectUnion, predicateUnion, eRoot));
						eg.graph.delete(Triple.create(subjectUnion,predicateUnion,union));
						eg.eliminateRedundantJoins();
					} else {
						eg.graph.add(Triple.create(subjectUnion, predicateUnion, uNode));
						eg.graph.add(Triple.create(uNode, typeNode, unionNode));
						for (RGraph e : result) {
							Node eRoot = e.root;
							GraphUtil.addInto(eg.graph, e.graph);
							eg.graph.add(Triple.create(uNode, argNode, eRoot));
							eg.eliminateRedundantJoins();
						}
						eg.graph.delete(Triple.create(subjectUnion,predicateUnion,union));
					}
					eg.setDistinctNode(true);
					UpdateAction.execute(branchCleanUpRule, eg.graph);
					eg.branchUnion(varMap);
					this.graph = eg.graph;
				}
			}
			else { // BGP instead
				Node first = ucq;
				if (GraphUtil.listObjects(graph, first, typeNode).next().equals(joinNode)) {
					if (isUCQ(first)) {
						Node subjectBGP, predicateBGP;
						if (listSubjects(graph, opNode, first).hasNext()) {
							subjectBGP = listSubjects(graph, opNode, first).next();
							predicateBGP = opNode;
						} else if (listSubjects(graph, leftNode, first).hasNext()) {
							subjectBGP = listSubjects(graph, leftNode, first).next();
							predicateBGP = leftNode;
						} else if (listSubjects(graph, rightNode, first).hasNext()) {
							subjectBGP = listSubjects(graph, rightNode, first).next();
							predicateBGP = rightNode;
						} else if (listSubjects(graph,subNode,first).hasNext()) {
							subjectBGP = listSubjects(graph,subNode,first).next();
							predicateBGP = subNode;
						}
						else if (listSubjects(graph,valueNode,first).hasNext()) {
							subjectBGP = listSubjects(graph,valueNode,first).next();
							predicateBGP = valueNode;
						}
						else {
							subjectBGP = listSubjects(graph, argNode, first).next();
							predicateBGP = argNode;
						}
						Graph inner = ge.extract(first, graph);
						Graph outer = GraphFactory.createPlainGraph();
						CustomTripleBoundary ctb = new CustomTripleBoundary(Collections.singletonList(first),null);
						GraphExtract extract = new GraphExtract(ctb);
						outer = extract.extract(root, graph);
						List<Node> innerVars = GraphUtil.listSubjects(inner,typeNode,varNode).toList();
						List<Node> outerVars = GraphUtil.listSubjects(outer,typeNode,varNode).toList();
						outer.remove(subjectBGP, predicateBGP, first);
						for (Node p : innerVars) {
							inner.delete(Triple.create(p,typeNode,varNode));
						}
						for (Node p : outerVars) {
							outer.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
							outer.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
							outer.add(Triple.create(p, typeNode, varNode));
							inner.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
							inner.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
							inner.add(Triple.create(p,typeNode,varNode));
						}
						//Create a new r-graph based on this conjunctive query.
						RGraph e = cqMinimisation(first,inner);
						RGraph eg = new RGraph(root, outer, this.vars);
						Node eRoot = e.root;
						if (e.graph.contains(Triple.create(e.root,typeNode,joinNode))) {
							List<Node> bgps = GraphUtil.listObjects(e.graph,e.root,argNode).toList();
							if (bgps.size() == 1) {
								eRoot = bgps.get(0);
							}
						}
						GraphUtil.addInto(eg.graph, e.graph);
						eg.graph.delete(Triple.create(subjectBGP,predicateBGP,first));
						eg.graph.add(Triple.create(subjectBGP, predicateBGP, eRoot));
						eg.root = root;
						eg.eliminateRedundantJoins();
						eg.setDistinctNode(true);
						eg.branchUnion(varMap);
						this.graph = eg.graph;
					}
				}
			}
			result = new ArrayList<>();
		}
		UpdateAction.execute(branchCleanUpRule,graph);
		UpdateAction.execute(branchUnionRule,graph);
		//UpdateAction.execute(branchCleanUpRule2,graph);
		removeTemporaryVars();
		UpdateAction.execute(joinRule, graph);
		return this;
	}
	public boolean isUCQ(Node n) {
		Node type = GraphUtil.listObjects(graph,n,typeNode).next();
		boolean ans = true;
		if (type.equals(unionNode) || type.equals(joinNode)) {
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph,n,argNode);
			while (args.hasNext()) {
				Node arg = args.next();
				ans = ans & isUCQ(arg);
			}
			return ans;
		}
		else if (type.equals(tpNode)) {
			return true;
		}
		else {
			return false;
		}
	}

	public void removeTemporaryVars() {
		ExtendedIterator<Node> tempNodes = GraphUtil.listSubjects(graph,tempNode,NodeFactory.createLiteralByValue(true,XSDDatatype.XSDboolean));
		while (tempNodes.hasNext()) {
			Node temp = tempNodes.next();
			Node value = GraphUtil.listObjects(graph,temp,valueNode).hasNext() ? GraphUtil.listObjects(graph,temp,valueNode).next() : null;
			graph.delete(Triple.create(temp,tempNode,NodeFactory.createLiteralByValue(true,XSDDatatype.XSDboolean)));
			if (value != null) {
				graph.delete(Triple.create(temp,valueNode,value));
			}
			if (graph.contains(Triple.create(temp,typeNode,varNode))) {
				graph.delete(Triple.create(temp,typeNode,varNode));
			}
		}
	}

	public void branchUnion(Map<Node, Node> varMap) {
		UpdateAction.execute(branchUnionRule, graph);
		ExtendedIterator<Node> vars = GraphUtil.listSubjects(graph,typeNode,varNode);
		while (vars.hasNext()) {
			Node var = vars.next();
			Node value = GraphUtil.listObjects(graph,var,valueNode).hasNext() ? GraphUtil.listObjects(graph,var,valueNode).next() : null;
			if (value != null) {
				String string = value.toString().replace("\"","");
				varMap.put(Var.alloc(string),var);
				graph.delete(Triple.create(var,valueNode,value));
			}
		}
	}

	public void eliminateRedundantJoins() {
		ExtendedIterator<Node> joins = GraphUtil.listSubjects(graph,typeNode,joinNode);
		while (joins.hasNext()) {
			Node join = joins.next();
			List<Node> args = GraphUtil.listObjects(graph,join,argNode).toList();
			if (args.size() == 1) {
				Node arg = args.get(0);
				if (graph.contains(Triple.create(arg,typeNode,tpNode))) {
					Node subject = GraphUtil.listObjects(graph,arg,subjectNode).next();
					Node predicate = GraphUtil.listObjects(graph,arg,preNode).next();
					Node object = GraphUtil.listObjects(graph,arg,objNode).next();
					graph.delete(Triple.create(arg,subjectNode,subject));
					graph.delete(Triple.create(arg,preNode,predicate));
					graph.delete(Triple.create(arg,objNode,object));
					graph.delete(Triple.create(arg,typeNode,tpNode));
					graph.delete(Triple.create(join,typeNode,joinNode));
					graph.add(Triple.create(join,typeNode,tpNode));
					graph.add(Triple.create(join,subjectNode,subject));
					graph.add(Triple.create(join,preNode,predicate));
					graph.add(Triple.create(join,objNode,object));
				}
			}
		}
	}

	public RGraph cqMinimisation(Node cRoot, Graph cGraph) throws InterruptedException {
		RGraph e = new RGraph(cRoot, cGraph, this.vars);
		UpdateAction.execute(filterVarsRule, e.graph);
		ExtendedIterator<Node> cqFilterVars = listSubjects(e.graph, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
		while (cqFilterVars.hasNext()) {
			Node p = cqFilterVars.next();
			e.graph.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
		}
		RGraph a = null;
		a = e.getLeanForm();
		if (a.graph.contains(a.root,typeNode,joinNode)) {
			List<Node> bgps = GraphUtil.listObjects(a.graph,a.root,argNode).toList();
			if (bgps.size() == 1) {
				Node bgp = bgps.get(0);
				a.graph.delete(Triple.create(a.root,argNode,bgp));
				a.graph.delete(Triple.create(a.root,typeNode,joinNode));
				a.root = bgp;
			}
		}
		return a;
	}

	public List<RGraph> containmentChecks(List<RGraph> result) {
		List<RGraph> redundant = new ArrayList<>();
		long time = System.nanoTime();
		for (int i = 0; i < result.size(); i++) {
			for (int j = i + 1; j < result.size(); j++) {
				RGraph e = result.get(i);
				RGraph e1 = result.get(j);
				ExtendedIterator<Triple> t0 = GraphUtil.findAll(e1.graph);
				ExtendedIterator<Triple> t1 = GraphUtil.findAll(e.graph);
				BasicPattern b0 = new BasicPattern();
				BasicPattern b1 = new BasicPattern();
				while (t0.hasNext()) {
					Triple t = t0.next();
					b0.add(getTripleWithVars(t));
				}
				while (t1.hasNext()) {
					Triple t = t1.next();
					b1.add(getTripleWithVars(t));
				}
				Query q = OpAsQuery.asQuery(new OpBGP(b0));
				Query q1 = OpAsQuery.asQuery(new OpBGP(b1));
				q.setQueryAskType();
				q1.setQueryAskType();
				Model m0 = ModelFactory.createModelForGraph(e.graph);
				Model m1 = ModelFactory.createModelForGraph(e1.graph);
				QueryExecution qexec = QueryExecutionFactory.create(q, m0);
				QueryExecution qexec1 = QueryExecutionFactory.create(q1, m1);
				boolean a = qexec.execAsk();
				boolean b = qexec1.execAsk();
				if (a && b) {
					redundant.add(e);
				} else if (a) {
					redundant.add(e);
				} else if (b) {
					redundant.add(e1);
				}
			}
		}
		for (RGraph e : redundant) {
			result.remove(e);
		}
		time = System.nanoTime() - time;
		System.out.println("Containment checks: " + time + " ns");
		return result;
	}

	public int getNumberOfNodes(){
		return this.graph.size();
	}
	
	public int getNumberOfTriples(){
		ExtendedIterator<Node> triples = listSubjects(graph, typeNode, tpNode);
		return triples.toList().size();
	}
	
	public int getNumberOfVars(){
		return listSubjects(graph, typeNode, varNode).toList().size();
	}

	public boolean isWellDesigned() {
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		ExtendedIterator<Node> optionalPatterns = listSubjects(graph, typeNode, optionalNode);
		if (optionalPatterns.hasNext()) {
			while (optionalPatterns.hasNext()) {
				Set<Node> leftVars = new HashSet<>();
				Set<Node> rightVars = new HashSet<>();
				Set<Node> outerVars = new HashSet<>();
				Graph g = GraphFactory.createPlainGraph();
				GraphUtil.addInto(g, graph);
				Node opt = optionalPatterns.next();
				Graph inner = ge.extract(opt, g);
				Node left = GraphUtil.listObjects(inner, opt, leftNode).next();
				Node right = GraphUtil.listObjects(inner, opt, rightNode).next();
				Graph leftGraph = ge.extract(left, inner);
				Graph rightGraph = ge.extract(right, inner);
				GraphUtil.deleteFrom(g, inner);
				ExtendedIterator<Node> outerTriples = listSubjects(g, typeNode, tpNode);
				ExtendedIterator<Node> leftTriples = listSubjects(leftGraph, typeNode, tpNode);
				ExtendedIterator<Node> rightTriples = listSubjects(rightGraph, typeNode, tpNode);
				while (leftTriples.hasNext()) {
					Node leftTriple = leftTriples.next();
					if (GraphUtil.listObjects(leftGraph, leftTriple, subjectNode).next().isBlank()) {
						leftVars.add(GraphUtil.listObjects(leftGraph, leftTriple, subjectNode).next());
					}
					if (GraphUtil.listObjects(leftGraph, leftTriple, preNode).next().isBlank()) {
						leftVars.add(GraphUtil.listObjects(leftGraph, leftTriple, preNode).next());
					}
					if (GraphUtil.listObjects(leftGraph, leftTriple, objNode).next().isBlank()) {
						leftVars.add(GraphUtil.listObjects(leftGraph, leftTriple, objNode).next());
					}
				}
				while (rightTriples.hasNext()) {
					Node rightTriple = rightTriples.next();
					if (GraphUtil.listObjects(rightGraph, rightTriple, subjectNode).next().isBlank()) {
						rightVars.add(GraphUtil.listObjects(rightGraph, rightTriple, subjectNode).next());
					}
					if (GraphUtil.listObjects(rightGraph, rightTriple, preNode).next().isBlank()) {
						rightVars.add(GraphUtil.listObjects(rightGraph, rightTriple, preNode).next());
					}
					if (GraphUtil.listObjects(rightGraph, rightTriple, objNode).next().isBlank()) {
						rightVars.add(GraphUtil.listObjects(rightGraph, rightTriple, objNode).next());
					}
				}
				while (outerTriples.hasNext()) {
					Node outerTriple = outerTriples.next();
					if (GraphUtil.listObjects(g, outerTriple, subjectNode).next().isBlank()) {
						outerVars.add(GraphUtil.listObjects(g, outerTriple, subjectNode).next());
					}
					if (GraphUtil.listObjects(g, outerTriple, preNode).next().isBlank()) {
						rightVars.add(GraphUtil.listObjects(g, outerTriple, preNode).next());
					}
					if (GraphUtil.listObjects(g, outerTriple, objNode).next().isBlank()) {
						rightVars.add(GraphUtil.listObjects(g, outerTriple, objNode).next());
					}
				}
				for (Node v : rightVars) {
					if (outerVars.contains(v)) {
						if (!leftVars.contains(v)) {
							return false;
						}
					}
				}
			}
			return true;
		}
		else {
			return true;
		}
	}

	public Node nxToJenaNode(org.semanticweb.yars.nx.Node node) {
		Node ans;
		if (Pattern.matches("_:.+", node.toN3())) {
			String s = BNode.unescapeForBNode(node.toN3());
			s = s.substring(s.indexOf(":")+1);
			if (s.startsWith("j")) {
				s = s.substring(1);
			}
			ans = NodeFactory.createBlankNode(s);
		} else if (Pattern.matches("<.+>", node.toN3())) {
			ans = NodeFactory.createURI(node.toN3().replaceAll("[<>]", ""));
		} else {
			ans = createLiteralWithType(node.toN3());
		}
		return ans;
	}


	public void service(Node service, boolean silent) {
		Node n = NodeFactory.createBlankNode();
		graph.add(Triple.create(n, typeNode, serviceNode));
		graph.add(Triple.create(n, valueNode, service));
		graph.add(Triple.create(n, silentNode, NodeFactory.createLiteralByValue(silent, XSDDatatype.XSDboolean)));
		graph.add(Triple.create(n, argNode, root));
		this.root = n;	
	}


	public long getLabelTime() {
		return labelTime;
	}


	public void setLabelTime(long labelTime) {
		this.labelTime = labelTime;
	}


	public long getMinimisationTime() {
		return minimisationTime;
	}


	public void setMinimisationTime(long minimisationTime) {
		this.minimisationTime = minimisationTime;
	}
}
