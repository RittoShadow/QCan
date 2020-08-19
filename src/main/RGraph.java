package main;

import cl.uchile.dcc.blabel.jena.JenaModelIterator;
import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.blabel.label.GraphLabelling;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingArgs;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingResult;
import cl.uchile.dcc.blabel.lean.DFSGraphLeaning;
import cl.uchile.dcc.blabel.lean.GraphLeaning.GraphLeaningResult;
import org.apache.jena.datatypes.xsd.XSDDatatype;
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
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.yars.nx.NodeComparator;
import paths.PGraph;
import tools.Tools;
import visitors.FilterVisitor;

import java.util.*;
import java.util.regex.Pattern;

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
	public boolean containsPaths = false;
	final String URI = "http://example.org/";
	private long labelTime = 0;
	private long minimisationTime = 0;
	private final Node typeNode = NodeFactory.createURI(this.URI+"type");
	private final Node tpNode = NodeFactory.createURI(this.URI+"TP");
	private final Node argNode = NodeFactory.createURI(this.URI+"arg");
	private final Node subNode = NodeFactory.createURI(this.URI+"subject");
	private final Node preNode = NodeFactory.createURI(this.URI+"predicate");
	private final Node objNode = NodeFactory.createURI(this.URI+"object");
	private final Node joinNode = NodeFactory.createURI(this.URI+"join");
	private final Node unionNode = NodeFactory.createURI(this.URI+"union");
	private final Node projectNode = NodeFactory.createURI(this.URI+"projection");
	private final Node askNode = NodeFactory.createURI(this.URI+"ask");
	private final Node constructNode = NodeFactory.createURI(this.URI+"construct");
	private final Node describeNode = NodeFactory.createURI(this.URI+"describe");
	private final Node opNode = NodeFactory.createURI(this.URI+"OP");
	private final Node limitNode = NodeFactory.createURI(this.URI+"limit");
	private final Node offsetNode = NodeFactory.createURI(this.URI+"offset");
	private final Node orderByNode = NodeFactory.createURI(this.URI+"orderBy");
	private final Node varNode = NodeFactory.createURI(this.URI+"var");
	private final Node orderNode = NodeFactory.createURI(this.URI+"order");
	private final Node valueNode = NodeFactory.createURI(this.URI+"value");
	private final Node dirNode = NodeFactory.createURI(this.URI+"direction");
	private final Node patternNode = NodeFactory.createURI(this.URI+"pattern");
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
	private final Node tempNode = NodeFactory.createURI(this.URI+"temp");
	private final Node bindNode = NodeFactory.createURI(this.URI+"bind");
	private final Node tableNode = NodeFactory.createURI(this.URI+"table");
	private final Node groupByNode = NodeFactory.createURI(this.URI+"group");
	private final Node aggregateNode = NodeFactory.createURI(this.URI+"aggregate");
	private final Node minusNode = NodeFactory.createURI(this.URI+"minus");
	private final Node extraNode = NodeFactory.createURI(this.URI+"extra");
	private final Node triplePathNode = NodeFactory.createURI(this.URI+"triplePath");
	private final Node serviceNode = NodeFactory.createURI(this.URI+"service");
	private final Node silentNode = NodeFactory.createURI(this.URI+"silent");
	@SuppressWarnings("unused")
	private final Node leafNode = NodeFactory.createURI(this.URI+"leaf");
	private final UpdateRequest duplicatesRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/normalisation/duplicates.ru"));
	private final UpdateRequest conjunctionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/normalisation/conjunction.ru"));
	private final UpdateRequest disjunctionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/normalisation/disjunction.ru"));
	private final UpdateRequest joinRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/join.ru"));
	private final UpdateRequest joinTripleRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/joinTriple.ru"));
	private final UpdateRequest redundancyRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/redundancy.ru"));
	private final UpdateRequest branchCleanUpRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/cleanUp.ru"));
	private final UpdateRequest branchCleanUpRule2 = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/branchCleanUp2.ru"));
	private final UpdateRequest branchRelabelRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/branchRelabel.ru"));
	private final UpdateRequest branchUnionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/branchUnion.ru"));
	private final UpdateRequest filterVarsRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/filterVars.ru"));
	private final UpdateRequest joinLabelRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/joinLabel.ru"));
	private final UpdateRequest tripleRelabelRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/tripleRelabel.ru"));
	private final UpdateRequest askRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/ask.ru"));
	
	

	/**
	 * @param triples List of RDF triples.
	 * @param vars List of the variables in the triples.
	 * @return A new r-graph based on the list of triples.
	 */
	public RGraph(List<Triple> triples, List<Var> vars){
		if (vars != null){
			this.vars.addAll(vars);
		}
		nTriples = triples.size();
		if (triples.size() > 1){
			this.root = NodeFactory.createBlankNode();
			//Adding typing now.
			graph.add(Triple.create(this.root, typeNode, joinNode));
		}
		for (Triple t : triples){
			Node n = NodeFactory.createBlankNode();
			if (triples.size() == 1){
				this.root = n;
			}
			else{
				graph.add(Triple.create(root, argNode, n));
			}
			graph.add(Triple.create(n, typeNode, tpNode));
			// We create a blank node if we have a variable. If it's a literal, we add a literal node, and so on.
			Triple s = Triple.create(n, subNode, getValidNode(t.getSubject()));
			Triple p = Triple.create(n, preNode, getValidNode(t.getPredicate()));
			Triple o = Triple.create(n, objNode, getValidNode(t.getObject()));
			graph.add(s);
			graph.add(p);
			graph.add(o);
		}	
	}
	
	
	/**
	 * @param root This node will be the new root of the r-graph.
	 * @param graph The graph on which the new r-graph is based.
	 * @param vars The list of variables in the r-graph.
	 * @return A new r-graph based on a given graph.
	 */
	public RGraph(Node root, Graph graph, Collection<Var> vars){
		this.root = root;
		this.graph = graph;
		if (vars != null) {
			this.vars.addAll(vars);
		}
	}
	
	/**
	 * @param triples List of RDF triples.
	 * @return A new r-graph based on the list of triples.
	 */
	public RGraph(List<Triple> triples){
		this(triples, null);
	}
	
	/**
	 * @param data A collection of triples of nodes.
	 * @return A new r-graph based on a collection of nodes.
	 */
	public RGraph(Collection<org.semanticweb.yars.nx.Node[]> data){
		Set<Node> rootCandidates = new HashSet<>();
		Set<Node> predicates = new HashSet<>();
		Set<Node> objects = new HashSet<>();
		for (org.semanticweb.yars.nx.Node[] node : data){
			Node subject;
			Node predicate;
			Node object;
			if (Pattern.matches("_:.+", node[0].toN3())){
				subject = NodeFactory.createBlankNode(node[0].toN3().substring(2));
			}
			else if (Pattern.matches("<.+>", node[0].toN3())){
				subject = NodeFactory.createURI(node[0].toN3().replaceAll("[<>]", ""));
			}
			else{
				subject = createLiteralWithType(node[0].toN3());
			}
			if (Pattern.matches("<.+>", node[1].toN3())){
				predicate = NodeFactory.createURI(node[1].toN3().replaceAll("[<>]", ""));
			}
			else if (Pattern.matches("_:.+", node[1].toN3())){
				predicate = NodeFactory.createBlankNode(node[1].toN3().substring(2));
			}
			else{
				predicate = createLiteralWithType(node[1].toN3());
			}
			if (Pattern.matches("_:.+", node[2].toN3())){
				object = NodeFactory.createBlankNode(node[2].toN3().substring(2));
			}
			else if (Pattern.matches("<.+>", node[2].toN3())){
				object = NodeFactory.createURI(node[2].toN3().replaceAll("[<>]", ""));
			}
			else{
				object = createLiteralWithType(node[2].toN3());
			}
			if (subject != null){
				graph.add(Triple.create(subject, predicate, object));
				rootCandidates.add(subject);
				predicates.add(predicate);
				objects.add(object);
			}
			else{
				System.err.println("Invalid blank node label.");
			}
		}
		for (Node p : predicates) {
			rootCandidates.remove(p);
		}
		for (Node o : objects) {
			rootCandidates.remove(o);
		}
		if (rootCandidates.size() == 1) {
			this.root = (Node) rootCandidates.toArray()[0];
		}
	}
	
	public RGraph(Node s, Node o, PGraph p) {
		Set<Var> vars = new HashSet<>();
		Graph graph = GraphFactory.createPlainGraph();
		Node tp = NodeFactory.createBlankNode();
		graph.add(Triple.create(tp, typeNode, triplePathNode));
		if (s.isVariable()) {
			vars.add(Var.alloc(s));
			graph.add(Triple.create(tp, subNode, NodeFactory.createBlankNode(s.getName())));
		}
		else if (s.isBlank()) {
			graph.add(Triple.create(tp, subNode, NodeFactory.createBlankNode(s.getBlankNodeLabel())));
		}
		else if (s.isURI()) {
			graph.add(Triple.create(tp, subNode, NodeFactory.createURI(s.getURI())));
		}
		else if (s.isLiteral()) {
			graph.add(Triple.create(tp, subNode, NodeFactory.createLiteralByValue(s.getLiteralValue(), s.getLiteralDatatype())));
		}
		if (o.isVariable()) {
			vars.add(Var.alloc(o));
			graph.add(Triple.create(tp, objNode, NodeFactory.createBlankNode(o.getName())));
		}
		else if (o.isBlank()) {
			graph.add(Triple.create(tp, objNode, NodeFactory.createBlankNode(o.getBlankNodeLabel())));
		}
		else if (o.isURI()) {
			graph.add(Triple.create(tp, objNode, NodeFactory.createURI(o.getURI())));
		}
		else if (o.isLiteral()) {
			graph.add(Triple.create(tp, objNode, NodeFactory.createLiteralByValue(o.getLiteralValue(), o.getLiteralDatatype())));
		}
		GraphUtil.addInto(graph, p.getMinimalDFA());
		Node n = NodeFactory.createBlankNode();
		Node pathNode = NodeFactory.createURI(this.URI + "path");
		graph.add(Triple.create(n, typeNode, pathNode));
		graph.add(Triple.create(n, argNode, p.getStartState()));
		graph.add(Triple.create(tp, preNode, n));
		this.graph = graph;
		this.vars = vars;
		this.root = tp;
	}
	
	public RGraph(Node s, Node o, Path p) {
		Set<Var> vars = new HashSet<>();
		Graph graph = GraphFactory.createPlainGraph();
		Node tp = NodeFactory.createBlankNode();
		graph.add(Triple.create(tp, typeNode, triplePathNode));
		if (s.isVariable()) {
			vars.add(Var.alloc(s));
		}
		graph.add(Triple.create(tp, subNode, getValidNode(s)));
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
	
	public static RGraph table(Table table) {
		Node tableRoot = NodeFactory.createBlankNode();
		Graph graph = GraphFactory.createDefaultGraph();
		RGraph ans = new RGraph(tableRoot, graph, null);
		ans.graph.add(Triple.create(tableRoot, ans.typeNode, ans.tableNode));
		Iterator<Binding> iter = table.rows();
		while (iter.hasNext()) {
			Node rowNode = NodeFactory.createBlankNode();
			ans.graph.add(Triple.create(tableRoot, ans.argNode, rowNode));
			Binding b = iter.next();
			Iterator<Var> vars = b.vars();
			while (vars.hasNext()) {
				Var var = vars.next();
				Node bindingNode = NodeFactory.createBlankNode();
				ans.graph.add(Triple.create(rowNode, ans.argNode, bindingNode));
				ans.graph.add(Triple.create(bindingNode, ans.typeNode, ans.bindNode));
				ans.graph.add(Triple.create(bindingNode, ans.varNode, NodeFactory.createBlankNode(var.getVarName())));
				Node value = b.get(var);
				ans.graph.add(Triple.create(bindingNode, ans.valueNode, value));
			}
		}
		return ans;
	}
	
	public Node getValidNode(Node n) {
		if (n.isVariable()) {
			return NodeFactory.createBlankNode(n.getName());
		}
		if (n.isLiteral()) {
			if (n.getLiteralLanguage().equals("")) {
				return NodeFactory.createLiteralByValue(n.getLiteralValue().toString(), n.getLiteralDatatype());
			}
			else {
				return NodeFactory.createLiteral(n.getLiteralValue().toString()+"@"+n.getLiteralLanguage());
			}
		}
		else {
			return n;
		}
	}
	
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
	public Node createLiteralWithType(String s){
		Node ans;
		s = s.replaceAll("\"", "");
		if (s.contains("^^")){
				ans = NodeFactory.createLiteralByValue(s.substring(0, s.indexOf("^^")), NodeFactory.getType(s.substring(1+s.lastIndexOf("^")).replaceAll("[<>]", "")));
		}
		else{
			ans = NodeFactory.createLiteralByValue(s, XSDDatatype.XSDstring);
		}
		return ans;
	}
	
	/**
	 * Adds all the variables in another r-graph to this one.
	 * @param e An r-graph.
	 */
	public void addVars(RGraph e){
		this.vars.addAll(e.vars);
	}
	
	/**
	 * Joins this r-graph with another r-graph. The result is a conjunction of both r-graphs. (Q_1 AND Q_2)
	 * @param arg1 An r-graph to join with this one.
	 */
	public void join(RGraph arg1){
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, joinNode));
		Node type0 = GraphUtil.listObjects(graph, this.root, typeNode).next();
		Node type1 = GraphUtil.listObjects(arg1.graph, arg1.root, typeNode).next();
		if (!this.root.equals(root)){
			graph.add(Triple.create(root, argNode, this.root));
		}
		if (!arg1.root.equals(root)){
			graph.add(Triple.create(root, argNode, arg1.root));
		}
		if (type0.equals(joinNode)) {
			boolean containsBind = false;
			Node pattern = GraphUtil.listObjects(this.graph, this.root, patternNode).hasNext() ? GraphUtil.listObjects(this.graph, this.root, patternNode).next() : null;
			if (pattern != null) {
				ExtendedIterator<Node> nodes = GraphUtil.listObjects(this.graph, pattern, argNode);
				while (nodes.hasNext()) {
					Node n = nodes.next();
					if (this.graph.contains(Triple.create(n, typeNode, bindNode))) {
						containsBind = true;
					}
				}
			}
			if (!containsBind) {
				ExtendedIterator<Node> args = GraphUtil.listObjects(graph, this.root, argNode);
				graph.delete(Triple.create(this.root, typeNode, joinNode));
				graph.delete(Triple.create(root, argNode, this.root));
				while (args.hasNext()) {
					Node n = args.next();
					graph.add(Triple.create(root, argNode, n));
					graph.delete(Triple.create(this.root, argNode, n));
				}
			}
			
		}
		if (type1.equals(joinNode)) {
			boolean containsBind = false;
			Node pattern = GraphUtil.listObjects(arg1.graph, arg1.root, patternNode).hasNext() ? GraphUtil.listObjects(arg1.graph, arg1.root, patternNode).next() : null;
			if (pattern != null) {
				ExtendedIterator<Node> nodes = GraphUtil.listObjects(arg1.graph, pattern, argNode);
				while (nodes.hasNext()) {
					Node n = nodes.next();
					if (this.graph.contains(Triple.create(n, typeNode, bindNode))) {
						containsBind = true;
					}
				}
			}
			if (!containsBind) {
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
			
		}
		//TODO Check if well designed
//		if (type0.equals(optionalNode) && type1.equals(optionalNode)){  //Joins between operands with optional must be reordered.
//
//		}
//		else if (type0.equals(optionalNode)){
//			Node left1 = GraphUtil.listObjects(this.graph, this.root, leftNode).next();
//			graph.delete(Triple.create(this.root, leftNode, left1));
//			graph.delete(Triple.create(root, argNode, this.root));
//			graph.add(Triple.create(this.root, leftNode, root));
//			graph.add(Triple.create(root, argNode, left1));
//			if (!root.equals(arg1.root)) {
//				graph.add(Triple.create(root, argNode, arg1.root));
//			}
//			root = this.root;
//		}
//		else if (type1.equals(optionalNode)){
//			Node left2 = GraphUtil.listObjects(arg1.graph, arg1.root, leftNode).next();
//			arg1.graph.delete(Triple.create(arg1.root, leftNode, left2));
//			graph.delete(Triple.create(root, argNode, arg1.root));
//			arg1.graph.add(Triple.create(arg1.root, leftNode, root));
//			arg1.graph.add(Triple.create(root, argNode, left2));
//			arg1.graph.add(Triple.create(root, argNode, this.root));
//			root = arg1.root;
//		}
		GraphUtil.addInto(this.graph, arg1.graph);
		addVars(arg1);
		this.root = root;
	}
	
	/**
	 * Joins this r-graph with another r-graph. The result is the union of both r-graphs. (Q_1 UNION Q_2)
	 * @param arg1 An r-graph to join with this one.
	 */
	public void union(RGraph arg1){
		Node root = NodeFactory.createBlankNode();
		boolean a = this.graph.contains(this.root, typeNode, unionNode);
		boolean b = arg1.graph.contains(arg1.root, typeNode, unionNode);
		if (a && b){
			ExtendedIterator<Node> nodes = GraphUtil.listObjects(arg1.graph, arg1.root, argNode);
			while(nodes.hasNext()){
				Node n = nodes.next();
				this.graph.add(Triple.create(this.root, argNode, n));
				GraphUtil.remove(arg1.graph, arg1.root, argNode, n);
			}
			GraphUtil.remove(arg1.graph, arg1.root, typeNode, unionNode);
		}
		else if (a){
			this.graph.add(Triple.create(this.root, argNode, arg1.root));
		}
		else if (b){
			this.graph.add(Triple.create(arg1.root, argNode, this.root));
			this.root = arg1.root;
		}
		else{
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
	 * @param arg1 An r-graph that represents an optional query pattern.
	 */
	public void optional(RGraph arg1){
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, optionalNode));
		graph.add(Triple.create(root, leftNode, this.root));
		graph.add(Triple.create(root, rightNode, arg1.root));
		this.root = root;
		addVars(arg1);
		GraphUtil.addInto(this.graph, arg1.graph);	
	}
	
	public void minus(RGraph arg1){
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, minusNode));
		graph.add(Triple.create(root, leftNode, this.root));
		graph.add(Triple.create(root, rightNode, arg1.root));
		this.root = root;
		addVars(arg1);
		GraphUtil.addInto(this.graph, arg1.graph);	
	}
	
	/**
	 * 
	 * @param arg1 An r-graph that represents a BIND expression (BIND var AS expr) and adds it to this r-graph.
	 */
	public void bind(RGraph arg1){
		if (!GraphUtil.listObjects(graph, root, patternNode).hasNext()) {
			Node n = NodeFactory.createBlankNode();
			graph.add(Triple.create(root, patternNode, n));
			graph.add(Triple.create(n, typeNode, extraNode));
		}
		Node n = GraphUtil.listObjects(graph, root, patternNode).next();
		graph.add(Triple.create(n, argNode, arg1.root));
		GraphUtil.addInto(this.graph, arg1.graph);
	}
	
	/**
	 * Creates an r-graph that represents each of the assignments in a BIND expression (BIND var AS expr) and adds it to this r-graph.
	 * @param expr An expression in an assignment.
	 * @param var The variable the expression is assigned to.
	 */
	public void bindNode(Node expr, Var var) {
		graph.add(Triple.create(this.root, varNode, NodeFactory.createBlankNode(var.getVarName())));
		graph.add(Triple.create(this.root, typeNode, bindNode));
		graph.add(Triple.create(this.root, argNode, expr));
	}
	
	/**
	 * Creates an r-graph that represents a FILTER expression (FILTER expr) and adds it to this r-graph. 
	 * @param n A node that represents an expression.
	 */
	public void filter(Node n){
		Node filter = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, modNode, filter));
		graph.add(Triple.create(filter, typeNode, filterNode));
		graph.add(Triple.create(filter, argNode, n));	
	}
	
	/**
	 * Creates an r-graph that represents a FILTER expression (FILTER expr) and adds it to this r-graph. 
	 * @param arg1 An r-graph that represents an expression.
	 */
	public void filter(RGraph arg1){
		if (!GraphUtil.listObjects(graph, root, patternNode).hasNext()) {
			Node n = NodeFactory.createBlankNode();
			graph.add(Triple.create(root, patternNode, n));
			graph.add(Triple.create(n, typeNode, extraNode));
		}
		Node n = GraphUtil.listObjects(graph, root, patternNode).next();
		Node filter = NodeFactory.createBlankNode();
		graph.add(Triple.create(n, argNode, filter));
		graph.add(Triple.create(filter, typeNode, filterNode));
		graph.add(Triple.create(filter, argNode, arg1.root));	
		GraphUtil.addInto(this.graph, arg1.graph);
	}
	
	/**
	 * Creates an r-graph that represents the conjunction of two expressions (FILTER expr1 && expr2) and adds it to this r-graph .
	 * @param arg1 An r-graph that represents an expression.
	 * @param arg2 An r-graph that represents an expression.
	 * @return Returns the node that represents the AND operator.
	 */
	public Node filterAnd(Node arg1, Node arg2){
		Node o = NodeFactory.createBlankNode();
		graph.add(Triple.create(o, typeNode, andNode));
		graph.add(Triple.create(o, argNode, arg1));
		graph.add(Triple.create(o, argNode, arg2));
		return o;
	}
	
	/**
	 * Creates an r-graph that represents the disjunction of two expressions (FILTER expr1 || expr2) and adds it to this r-graph. 
	 * @param arg1 An r-graph that represents an expression.
	 * @param arg2 An r-graph that represents an expression.
	 * @return Returns the node that represents the OR operator.
	 */
	public Node filterOr(Node arg1, Node arg2){
		Node o = NodeFactory.createBlankNode();
		graph.add(Triple.create(o, typeNode, orNode));
		graph.add(Triple.create(o, argNode, arg1));
		graph.add(Triple.create(o, argNode, arg2));
		return o;
	}
	
	/**
	 * Creates an r-graph that represents the negation of an expression (FILTER !expr) and adds it to this r-graph.
	 * @param arg1 An r-graph that represents an expression.
	 * @return Returns the node that represents the NOT operator.
	 */
	public Node filterNot(Node arg1){
		Node o = NodeFactory.createBlankNode();
		graph.add(Triple.create(o, typeNode, notNode));
		graph.add(Triple.create(o, argNode, arg1));
		return o;
	}

	/**
	 * Creates an r-graph that represents a function over an expression (FILTER (f expr)) and adds it to this r-graph.
	 * @param op A node that represents a SPARQL function.
	 * @param arg1 A node that represents an expression.
	 * @return Returns the node that represents the function.
	 */
	public Node filterFunction(Node op, Node arg1){
		Node n = NodeFactory.createBlankNode();
		Node a = NodeFactory.createBlankNode();
		if (!GraphUtil.listObjects(graph, arg1, functionNode).hasNext()){
			graph.add(Triple.create(a, valueNode, arg1));
		}
		else{
			a = arg1;
		}	
		graph.add(Triple.create(n, functionNode, op));
		graph.add(Triple.create(n, argNode, a));
		return n;
	}

	public Node filterFunction(String op, Node arg1){
		return filterFunction(NodeFactory.createLiteral(op), arg1);
	}

	/**
	 * Creates an r-graph that represents a binary SPARQL function (FILTER (f expr1 expr2)) and adds it to this r-graph.
	 * @param op A string that represents a binary SPARQL function.
	 * @param arg1 A node that represents an expression.
	 * @param arg2 A node that represents an expression.
	 * @return Returns a node that represents the function.
	 */
	public Node filterFunction(String op, Node arg1, Node arg2){
		Node n = NodeFactory.createBlankNode();
		Node o = NodeFactory.createLiteral(op);
		Node a = NodeFactory.createBlankNode();
		Node b = NodeFactory.createBlankNode();
		graph.add(Triple.create(n, functionNode, o));
		if (!GraphUtil.listObjects(graph, arg1, functionNode).hasNext()){
			graph.add(Triple.create(a, valueNode, arg1));
		}
		else{
			a = arg1;
		}
		if (!GraphUtil.listObjects(graph, arg2, functionNode).hasNext()){
			graph.add(Triple.create(b, valueNode, arg2));
		}
		else{
			b = arg2;
		}
		graph.add(Triple.create(n, argNode, a));
		graph.add(Triple.create(n, argNode, b));
		if (isOrderedFunction(op)){
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
			if (!GraphUtil.listObjects(graph, node, functionNode).hasNext()){
				graph.add(Triple.create(n, argNode, newNode));
				graph.add(Triple.create(newNode, valueNode, node));
				if (isOrderedFunction(op) || o.isURI()) {
					graph.add(Triple.create(newNode, orderNode, NodeFactory.createLiteralByValue(i++, XSDDatatype.XSDint)));
				}
				if (!isOrderedFunction(op) && o.isURI()) {

				}
			}
			else {
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
		while (!before.isIsomorphicWith(graph)){
			before = GraphFactory.createPlainGraph();
			GraphUtil.addInto(before, graph);
			UpdateAction.execute(conjunctionRule, graph);
			UpdateAction.execute(disjunctionRule, graph);
		}
	}
	
	public static RGraph group(OpGroup arg) {
		Node root = NodeFactory.createBlankNode();
		RGraph ans = new RGraph(root, GraphFactory.createDefaultGraph(), Set.of());
		ans.graph.add(Triple.create(root, ans.typeNode, ans.groupByNode));
		Node varNode = NodeFactory.createBlankNode();
		ans.graph.add(Triple.create(root, ans.argNode, varNode));
		VarExprList vExpr = arg.getGroupVars();
		for (Var v : vExpr.getVars()) {
			ans.graph.add(Triple.create(varNode, ans.valueNode, NodeFactory.createBlankNode(v.getVarName())));
		}
		Map<Var,Expr> varsExpr = vExpr.getExprs();
		for (Map.Entry<Var, Expr> m : varsExpr.entrySet()) {
			Var v = m.getKey();
			FilterVisitor fv = new FilterVisitor();
			ExprWalker.walk(fv, m.getValue());
			Node r = fv.getGraph().root;
			GraphUtil.addInto(ans.graph, fv.getGraph().graph);
			ans.graph.add(Triple.create(NodeFactory.createBlankNode(v.getVarName()), ans.valueNode, r));
		}
		return ans;
	}
	
	/**
	 * @param group
	 */
	public void groupBy(RGraph group) {
		if (!GraphUtil.listObjects(graph, root, patternNode).hasNext()) {
			Node n = NodeFactory.createBlankNode();
			graph.add(Triple.create(root, patternNode, n));
			graph.add(Triple.create(n, typeNode, extraNode));
		}
		Node n = GraphUtil.listObjects(graph, root, patternNode).next();
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
	 * @param args
	 */
	public void aggregation(RGraph r, List<RGraph> args) {
		if (!GraphUtil.listObjects(graph, root, patternNode).hasNext()) {
			Node n = NodeFactory.createBlankNode();
			graph.add(Triple.create(root, patternNode, n));
			graph.add(Triple.create(n, typeNode, extraNode));
		}
		Node n = GraphUtil.listObjects(graph, root, patternNode).next();
		Node extend = NodeFactory.createBlankNode();
		graph.add(Triple.create(r.root, patternNode, extend));
		graph.add(Triple.create(extend, typeNode, aggregateNode));
		graph.add(Triple.create(n, argNode, r.root));
		GraphUtil.addInto(this.graph, r.graph);
		for (RGraph arg : args) {
			graph.add(Triple.create(extend, argNode, arg.root));
			GraphUtil.addInto(this.graph, arg.graph);
		}
	}
	
	/**
	 * @param op
	 * @param var
	 * @param arg
	 * @return
	 */
	public Node aggregationFunction(String op, Var var, Node arg) {
		Node n = NodeFactory.createBlankNode();
		if (var != null) {
			n = NodeFactory.createBlankNode(var.getVarName());
		}			
		Node o = NodeFactory.createLiteral(op);
		graph.add(Triple.create(n, functionNode, o));
		if (!GraphUtil.listObjects(graph, arg, functionNode).hasNext()) {
			Node a = NodeFactory.createBlankNode();
			graph.add(Triple.create(n, argNode, a));
			graph.add(Triple.create(a, valueNode, arg));
			graph.add(Triple.create(arg, typeNode, varNode));
		}
		else {
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
	public Node aggregationCount(String op, Var var, Node arg) {
		Node n = NodeFactory.createBlankNode();
		if (var != null) {
			n = NodeFactory.createBlankNode(var.getVarName());
		}			
		Node o = NodeFactory.createLiteral(op);
		graph.add(Triple.create(n, functionNode, o));
		graph.add(Triple.create(n, argNode, arg));
		graph.add(Triple.create(arg, valueNode, NodeFactory.createLiteral("*")));
		return n;
	}
	
	/**
	 * @param s A string that represents a SPARQL function.
	 * @return Returns true if the function is ordered (i.e (f expr1 expr2) != (f expr2 expr1)).
	 */
	public boolean isOrderedFunction(String s){
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
	 * @param vars A collection of projected variables.
	 */
	public void project(Collection<Var> vars){
		// Used to check if there's a projection node
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, projectNode));
		graph.add(Triple.create(root, opNode, this.root));
		if (this.vars != null) {
			this.vars.addAll(vars);
		}		
		if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)){
			graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
		}
		for (Var v : vars){
			graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));		
		}
		this.root = root;
	}
	
	public void ask() {
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, askNode));
		graph.add(Triple.create(root, opNode, this.root));	
		if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)){
			graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
		}
		for (Var v : vars){
			graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));		
		}
		this.root = root;
	}
	
	public void construct() {
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, constructNode));
		graph.add(Triple.create(root, opNode, this.root));	
		if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)){
			graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
		}
		for (Var v : vars){
			graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));		
		}
		this.root = root;
	}
	
	public void describe() {
		Node root = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, typeNode, describeNode));
		graph.add(Triple.create(root, opNode, this.root));	
		if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)){
			graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
		}
		for (Var v : vars){
			graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));		
		}
		this.root = root;
	}
	
	/**
	 * Creates a node that indicates if the DISTINCT keyword has been used, and adds it to this r-graph.
	 * @param isDistinct A boolean that is true if the DISTINCT keyword has been used, and false otherwise.
	 */
	public void setDistinctNode(boolean isDistinct){
		graph.add(Triple.create(root, distinctNode, NodeFactory.createLiteralByValue(isDistinct, XSDDatatype.XSDboolean)));
	}
	
	/**
	 * @return Returns true if the DISTINCT keyword has been used, and false otherwise.
	 */
	public boolean isDistinct(){
		return this.graph.contains(root, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
	}
	
	/**
	 * Creates an r-graph that represents the RDF datasets that form the default graph, and adds it to this r-graph.
	 * @param g A collection of strings that identify RDF datasets.
	 */
	public void fromGraph(Collection<String> g){
		Node n = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, opNode, n));
		graph.add(Triple.create(n, typeNode, fromNode));
		for (String s : g){
			graph.add(Triple.create(n, argNode, NodeFactory.createURI(s)));
		}
	}
	
	/**
	 * Creates an r-graph that represents the RDF datasets that define the named graphs, and adds it to this r-graph.
	 * @param g A collection of strings that identify RDF datasets.
	 */
	public void fromNamedGraph(Collection<String> g){
		Node aux;
		ExtendedIterator<Node> e = listSubjects(graph, typeNode, fromNode);
		if (e.hasNext()){
			aux = e.next();
		}
		else{
			aux = NodeFactory.createBlankNode();
			graph.add(Triple.create(aux, typeNode, fromNode));
		}
		Node n = NodeFactory.createBlankNode();
		graph.add(Triple.create(aux, opNode, n));
		graph.add(Triple.create(n, typeNode, fromNamedNode));
		for (String s : g){
			graph.add(Triple.create(n, argNode, NodeFactory.createURI(s)));
		}
	}
	
	/**
	 * Creates an r-graph that represents an ORDER BY clause, and adds it to this r-graph.
	 * @param exprs The list of expressions according to which the results are ordered.
	 * @param dir A list of integers that indicates if results are ordered ascending or descending.
	 */
	
	public void orderBy(List<Expr> exprs, List<Integer> dir) {
		Node project = root;
		Node order = NodeFactory.createBlankNode();
		graph.add(Triple.create(project, modNode, order));
		graph.add(Triple.create(order, typeNode, orderByNode));
		for (int i = 0; i < exprs.size(); i++) {
			Node auxNode = NodeFactory.createBlankNode();
			graph.add(Triple.create(order, argNode, auxNode));
			if (exprs.get(i).isVariable()) {
				graph.add(Triple.create(auxNode, valueNode, NodeFactory.createBlankNode(exprs.get(i).getVarName())));
			}
			else {
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
	 * @param offset A number that causes the solutions to start at the specified number. An offset of 0 has no effect. 
	 * @param limit A number that specifies the number of solutions to return.
	 */
	public void slice(int offset, int limit){
		Node order = root;
		//Node order = GraphUtil.listSubjects(graph, typeNode, projectNode).next();
		Node lNode = NodeFactory.createBlankNode();
		graph.add(Triple.create(order, modNode, lNode));
		graph.add(Triple.create(lNode, typeNode, limitNode));
		graph.add(Triple.create(lNode, offsetNode, NodeFactory.createLiteralByValue(offset, XSDDatatype.XSDint)));
		graph.add(Triple.create(lNode, valueNode, NodeFactory.createLiteralByValue(limit, XSDDatatype.XSDint)));
	}
	
	public void graphOp(Node n){
		Node r = NodeFactory.createBlankNode();
		graph.add(Triple.create(r, typeNode, graphNode));
		if (n.isVariable()){
			n = NodeFactory.createBlankNode(n.getName());
		}
		graph.add(Triple.create(r, valueNode, n));
		graph.add(Triple.create(r, argNode, this.root));
		this.root = r;
	}
	
	public void turnDistinctOn(){
		this.distinct = true;
	}
	
	public void setLeaning(boolean b){
		this.leaning = b;
	}

	public boolean containsProjection(){
		return listSubjects(graph, typeNode, projectNode).hasNext();
	}
	
	public boolean containsUnion(){
		return listSubjects(graph, typeNode, unionNode).hasNext();
	}
	
	public boolean containsJoin(){
		return listSubjects(graph, typeNode, joinNode).hasNext();
	}
	
	public Model asModel(){
		return ModelFactory.createModelForGraph(graph);
	}

	public String toString(){
		String ans = "";
		ExtendedIterator<Triple> e = GraphUtil.findAll(this.graph);
		while (e.hasNext()){
			Triple t = e.next();
			ans += t.toString() + "\n";
		}
		return ans;
	}

	public void print(){
		System.out.println(this);
		System.out.println();
	}
	
	/**
	 * @return Returns a set containing all triples in this r-graph.
	 */
	public TreeSet<org.semanticweb.yars.nx.Node[]> getTriples(){
		Model model = this.asModel();
		JenaModelIterator jmi = new JenaModelIterator(model);
		TreeSet<org.semanticweb.yars.nx.Node[]> triples = new TreeSet<>(NodeComparator.NC);
		while(jmi.hasNext()){
			org.semanticweb.yars.nx.Node[] triple = jmi.next();
			triples.add(new org.semanticweb.yars.nx.Node[]{triple[0],triple[1],triple[2]});
		}
		return triples;
	}
	
	public GraphLeaningResult DFSLeaning(Collection<org.semanticweb.yars.nx.Node[]> triples) throws InterruptedException{
		DFSGraphLeaning dfsgl = new DFSGraphLeaning(triples, false, false);
		return dfsgl.call();
	}

	/**
	 * @param triples A collection of RDF triples.
	 * @return A canonical labeling of all blank nodes.
	 * @throws InterruptedException
	 * @throws HashCollisionException
	 */
	public GraphLabellingResult label(Collection<org.semanticweb.yars.nx.Node[]> triples) throws InterruptedException, HashCollisionException{
		GraphLabellingArgs gla = new GraphLabellingArgs();
		gla.setDistinguishIsoPartitions(false);
		GraphLabelling gl = new GraphLabelling(triples,gla);
		return gl.call();
	}
	
	/**
	 * Iteratively performs a SPARQL UPDATE query until no changes are made.
	 * @param request A SPARQL UPDATE query.
	 */
	public void iterativeUpdate(UpdateRequest request){
		Graph before = GraphFactory.createPlainGraph();
		while (!before.isIsomorphicWith(graph)){
			before = GraphFactory.createPlainGraph();
			GraphUtil.addInto(before, graph);
			UpdateAction.execute(request, graph);
		}
	}
	
	public void removeRedundantOperators(){
		iterativeUpdate(redundancyRule);
		iterativeUpdate(joinRule);
	}
	
	/**
	 * Re-labels all the variables in each conjunctive query.
	 */
	public void branchRelabelling(){
		iterativeUpdate(redundancyRule);
		iterativeUpdate(joinRule);
		UpdateAction.execute(joinLabelRule,this.graph);
	}
	
	/**
	 * Performs a leaning of the current r-graph.
	 * @return Returns the lean form of this graph.
	 * @throws InterruptedException
	 */
	public RGraph getLeanForm() throws InterruptedException{
		GraphLeaningResult glResult = this.DFSLeaning(getTriples());
		return new RGraph(glResult.getLeanData());
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
		removeRedundantOperators();
		if (leaning && distinct){
			long t  = System.nanoTime();
			if (verbose){
				System.out.println("Branch relabelling");
			}
			try{
				if (!containsPaths) {
					branchRelabelling();
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
			long time1 = System.nanoTime() - t;
			UpdateAction.execute(duplicatesRule,ans.graph);
			if (verbose){
				System.out.println("Beginning labelling");
			}
			t = System.nanoTime();
			GraphLabellingResult glr = this.label(ans.getTriples());
			long time2 = System.nanoTime() - t;			
			if (verbose){
				System.out.println("Labelling results: \n");
				System.out.println("Number of blank nodes: "+glr.getBnodeCount());
				System.out.println("Number of colouring iterations: "+glr.getColourIterationCount());
				System.out.println("Number of partitions found: "+glr.getPartitionCount());
			}
			ans = new RGraph(glr.getGraph());
			ans.setMinimisationTime(time1);
			ans.setLabelTime(time2);
			if (verbose){
				ans.print();
			}
			return ans;
		}
		else{
			GraphLabellingResult glr = this.label(this.getTriples());
			if (verbose){
				System.out.println("Labelling results: \n");
				System.out.println("Number of blank nodes: "+glr.getBnodeCount());
				System.out.println("Number of colouring iterations: "+glr.getColourIterationCount());
				System.out.println("Number of partitions found: "+glr.getPartitionCount());
			}
			return new RGraph(glr.getGraph());
		}	
	}

	public RGraph ucqMinimisation() throws InterruptedException {
		ArrayList<RGraph> result = new ArrayList<>();
		ArrayList<RGraph> redundant = new ArrayList<>();
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		ExtendedIterator<Node> ucqs = listSubjects(this.graph, typeNode, unionNode);
		if (!ucqs.hasNext()) {
			if (GraphUtil.listObjects(graph, root, opNode).hasNext()) {
				Node first = GraphUtil.listObjects(graph, root, opNode).next();
				if (GraphUtil.listObjects(graph, first, typeNode).next().equals(joinNode)) {
					if (isUCQ(first)) {
						Graph inner = ge.extract(first, graph);
						Graph outer = GraphFactory.createPlainGraph();
						GraphUtil.addInto(outer, graph);
						GraphUtil.deleteFrom(outer, inner);
						outer.delete(Triple.create(root, opNode, first));
						Graph filterGraph = GraphFactory.createPlainGraph();
						Graph orderByGraph = GraphFactory.createPlainGraph();
						ExtendedIterator<Node> filters = GraphUtil.listObjects(inner, first, patternNode);
						//FILTER, BIND, GROUP BY, etc.
						if (filters.hasNext()){
							filterGraph = ge.extract(filters.next(), inner);
						}
						GraphUtil.deleteFrom(inner,filterGraph);
						ExtendedIterator<Node> pattern = GraphUtil.listObjects(inner,first,patternNode);
						if (pattern.hasNext()) {
							inner.delete(Triple.create(first,patternNode,pattern.next()));
						}
						UpdateAction.execute(filterVarsRule,filterGraph);
						List<Node> filterVars = listSubjects(filterGraph, typeNode, varNode).toList();
						for (Node f : filterVars){
							filterGraph.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
							filterGraph.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
							inner.add(Triple.create(f, typeNode, varNode));
							inner.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
							inner.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
						}
						//PROJECTED
						ExtendedIterator<Node> pVars = GraphUtil.listObjects(graph, root, argNode);
						while (pVars.hasNext()){
							Node p = pVars.next();
							outer.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
							outer.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
							outer.add(Triple.create(p, typeNode, varNode));
							inner.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
							inner.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
						}
						//ORDER BY
						ExtendedIterator<Node> orderBy = listSubjects(graph, typeNode, orderByNode);
						if (orderBy.hasNext()) {
							orderByGraph = ge.extract(orderBy.next(), outer);
						}
						UpdateAction.execute(filterVarsRule, orderByGraph);
						while (orderBy.hasNext()){
							ExtendedIterator<Node> orderVars = listSubjects(orderByGraph, typeNode, varNode);
							while (orderVars.hasNext()){
								Node v = orderVars.next();
								Node var = GraphUtil.listObjects(graph, v, varNode).next();
								outer.add(Triple.create(var, valueNode, NodeFactory.createLiteral(var.getBlankNodeLabel())));
								outer.add(Triple.create(var, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								outer.add(Triple.create(var, typeNode, varNode));
								inner.add(Triple.create(var, valueNode, NodeFactory.createLiteral(var.getBlankNodeLabel())));
								inner.add(Triple.create(var, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
							}
						}
						//Create a new r-graph based on this conjunctive query.
						RGraph e = new RGraph(first,inner,this.vars);
						e.project(vars);
						ExtendedIterator<Node> cqFilterVars = listSubjects(e.graph, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
						while(cqFilterVars.hasNext()){
							Node p = cqFilterVars.next();
							e.graph.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
						}
						UpdateAction.execute(tripleRelabelRule,e.graph); // May not be necessary anymore.
						UpdateAction.execute(branchRelabelRule,e.graph);
//					UpdateAction.execute(branchCleanUpRule,e.graph);
						e = e.getLeanForm();
						RGraph eg = new RGraph(root,outer,this.vars);
						Node eRoot = listSubjects(e.graph, typeNode, joinNode).next();
						GraphUtil.addInto(eg.graph, ge.extract(eRoot, e.graph));
						eg.graph.add(Triple.create(root, opNode, eRoot));
						eg.root = root;
						if (!filterGraph.isEmpty()){
							Node fNode = listSubjects(filterGraph, typeNode, extraNode).next();
							eg.graph.add(Triple.create(eRoot, patternNode, fNode));
						}
						GraphUtil.addInto(eg.graph, filterGraph);
						UpdateAction.execute(joinTripleRule, eg.graph);
						eg.setDistinctNode(true);
						UpdateAction.execute(branchCleanUpRule,eg.graph);
						UpdateAction.execute(branchUnionRule,eg.graph);
						UpdateAction.execute(branchCleanUpRule2,eg.graph);
						UpdateAction.execute(joinRule, eg.graph);
						return eg;
					}
				}
			}
		}	
		while (ucqs.hasNext()){ //Make sure it's a union of conjunctive queries.
			Node union = ucqs.next();
			Node subUnion, preUnion;
			if (listSubjects(graph, opNode, union).hasNext()){
				subUnion = listSubjects(graph, opNode, union).next();
				preUnion = opNode;
			}
			else if (listSubjects(graph, leftNode, union).hasNext()){
				subUnion = listSubjects(graph, leftNode, union).next();
				preUnion = leftNode;
			}
			else if (listSubjects(graph, rightNode, union).hasNext()){
				subUnion = listSubjects(graph, rightNode, union).next();
				preUnion = rightNode;
			}
			else{
				subUnion = listSubjects(graph, argNode, union).next();
				preUnion = argNode;
			}
			Graph inner = ge.extract(union, graph);
			Graph outer = GraphFactory.createPlainGraph();
			Graph filterGraph = GraphFactory.createPlainGraph();
			Graph orderByGraph = GraphFactory.createPlainGraph();
			ExtendedIterator<Node> filters = GraphUtil.listObjects(inner, union, patternNode);
			ExtendedIterator<Node> cQueries = GraphUtil.listObjects(inner, union, argNode);

			boolean isUCQ = isUCQ(union);
			if (!isUCQ) {
				continue;
			}
			cQueries = GraphUtil.listObjects(inner, union, argNode);
			if (GraphUtil.listObjects(inner, union, argNode).toList().size() > 1){
				if (filters.hasNext()){
					filterGraph = ge.extract(filters.next(), inner);
				}
				GraphUtil.addInto(outer, graph);
				GraphUtil.deleteFrom(outer, inner);
				outer.remove(subUnion, preUnion, union);
				//Extract all projected variables from projection node.
				ExtendedIterator<Node> pVars = GraphUtil.listObjects(graph, root, argNode);
				while (pVars.hasNext()){
					Node p = pVars.next();
					outer.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
					outer.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
					outer.add(Triple.create(p, typeNode, varNode));
					inner.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
					inner.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
				}
				ExtendedIterator<Node> orderBy = listSubjects(graph, typeNode, orderByNode);
				if (orderBy.hasNext()){
					orderByGraph = ge.extract(orderBy.next(), outer);
				}
				//Extract all variables in ORDER BY clauses
				UpdateAction.execute(filterVarsRule, orderByGraph);
				while (orderBy.hasNext()){
					ExtendedIterator<Node> orderVars = listSubjects(orderByGraph, typeNode, varNode);
					while (orderVars.hasNext()){
						Node v = orderVars.next();
						Node var = GraphUtil.listObjects(graph, v, varNode).next();
						outer.add(Triple.create(var, valueNode, NodeFactory.createLiteral(var.getBlankNodeLabel())));
						outer.add(Triple.create(var, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
						outer.add(Triple.create(var, typeNode, varNode));
						inner.add(Triple.create(var, valueNode, NodeFactory.createLiteral(var.getBlankNodeLabel())));
						inner.add(Triple.create(var, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
					}
				}
				UpdateAction.execute(filterVarsRule,filterGraph);	
				//printGraph(filterGraph);
				List<Node> filterVars = listSubjects(filterGraph, typeNode, varNode).toList();
				for (Node f : filterVars){
					filterGraph.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
					filterGraph.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
				}
				//Iterate through all conjunctive queries.
				while (cQueries.hasNext()){
					Node cRoot = cQueries.next();
					Graph cGraph = ge.extract(cRoot, inner);
					//Check all variables in filter clauses.
					for (Node f : filterVars){
						cGraph.add(Triple.create(f, typeNode, varNode));
						cGraph.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
						cGraph.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
					}
					//Create a new r-graph based on this conjunctive query.
					RGraph e = new RGraph(cRoot,cGraph,this.vars);
					e.graph.add(Triple.create(union, argNode, e.root));
					e.root = union;
					e.graph.add(Triple.create(union, typeNode, unionNode));
					e.project(vars);
					ExtendedIterator<Node> projectedVars = GraphUtil.listObjects(e.graph, root, argNode);
					UpdateAction.execute(filterVarsRule,e.graph);
					//Projected variables are grounded.
					while(projectedVars.hasNext()){
						Node p = projectedVars.next();
						e.graph.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
						e.graph.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
					}
					//Variables in filter, group by, or bind clauses are grounded.
					for (Node f : filterVars){
						e.graph.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
						e.graph.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
					}
					ExtendedIterator<Node> cqFilterVars = listSubjects(e.graph, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
					while(cqFilterVars.hasNext()){
						Node p = cqFilterVars.next();
						e.graph.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
					}
					UpdateAction.execute(tripleRelabelRule,e.graph); // May not be necessary anymore.
					UpdateAction.execute(branchRelabelRule,e.graph);
//					UpdateAction.execute(branchCleanUpRule,e.graph);
					RGraph a = e.getLeanForm();
					result.add(a);
				}
				for (int i = 0; i < result.size(); i++){
					for (int j = i + 1; j < result.size(); j++){
						RGraph e = result.get(i);
						RGraph e1 = result.get(j);
						Graph g0 = GraphFactory.createDefaultGraph();
						Graph g1 = GraphFactory.createDefaultGraph();
						GraphUtil.addInto(g0, e.graph);
						GraphUtil.addInto(g1, e1.graph);
						UpdateAction.execute(askRule, g0);
						UpdateAction.execute(askRule, g1);
						ExtendedIterator<Triple> t0 = GraphUtil.findAll(g1);
						ExtendedIterator<Triple> t1 = GraphUtil.findAll(g0);
						BasicPattern b0 = new BasicPattern();
						BasicPattern b1 = new BasicPattern();
						while(t0.hasNext()){
							Triple t = t0.next();
							b0.add(getTripleWithVars(t));
						}
						while(t1.hasNext()){
							Triple t = t1.next();
							b1.add(getTripleWithVars(t));
						}
						Query q = OpAsQuery.asQuery(new OpBGP(b0));
						Query q1 = OpAsQuery.asQuery(new OpBGP(b1));
						q.setQueryAskType();
						q1.setQueryAskType();
						Model m0 = ModelFactory.createModelForGraph(g0);
						Model m1 = ModelFactory.createModelForGraph(g1);
						QueryExecution qexec = QueryExecutionFactory.create(q, m0);
						QueryExecution qexec1 = QueryExecutionFactory.create(q1, m1);
						boolean a = qexec.execAsk();
						boolean b = qexec1.execAsk();
						if (a && b){
							redundant.add(e);
						}
						else if (a){
							redundant.add(e);
						}
						else if (b){
							redundant.add(e1);
						}
					}
				}
				for (RGraph e : redundant){
					result.remove(e);
				}
				RGraph eg = new RGraph(root, outer, vars);
				Node uNode = NodeFactory.createBlankNode();
				//If there's only a single non-redundant CQ.
				if (result.size() == 1){
					for (RGraph e : result){
						Node eRoot = listSubjects(e.graph, typeNode, unionNode).next();
						eRoot = GraphUtil.listObjects(e.graph, eRoot, argNode).next();
						GraphUtil.addInto(eg.graph, ge.extract(eRoot, e.graph));				
						eg.graph.add(Triple.create(subUnion, preUnion, eRoot));
						if (!filterGraph.isEmpty()){
							Node fNode = listSubjects(filterGraph, typeNode, extraNode).next();
							eg.graph.add(Triple.create(eRoot, patternNode, fNode));
						}
						GraphUtil.addInto(eg.graph, filterGraph);
						UpdateAction.execute(joinTripleRule, eg.graph);
					}
				}
				else{
					eg.graph.add(Triple.create(subUnion, preUnion, uNode));
					eg.graph.add(Triple.create(uNode, typeNode, unionNode));
					for (RGraph e : result){
						UpdateAction.execute(joinTripleRule, e.graph);
						Node eRoot = listSubjects(e.graph, typeNode, unionNode).next();
						eRoot = GraphUtil.listObjects(e.graph, eRoot, argNode).next();
						GraphUtil.addInto(eg.graph, ge.extract(eRoot, e.graph));
						eg.graph.add(Triple.create(uNode, argNode, eRoot));
					}
					if (!filterGraph.isEmpty()){
						Node fNode = listSubjects(filterGraph, typeNode, extraNode).next();
						eg.graph.add(Triple.create(uNode, patternNode, fNode));
						GraphUtil.addInto(eg.graph, filterGraph);
					}
				}
				eg.setDistinctNode(true);
				UpdateAction.execute(branchCleanUpRule,eg.graph);
				UpdateAction.execute(branchUnionRule,eg.graph);
				UpdateAction.execute(branchCleanUpRule2,eg.graph);
				UpdateAction.execute(joinRule, eg.graph);
				this.graph = eg.graph;
//				return eg;
			}
		}	
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
					if (GraphUtil.listObjects(leftGraph, leftTriple, subNode).next().isBlank()) {
						leftVars.add(GraphUtil.listObjects(leftGraph, leftTriple, subNode).next());
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
					if (GraphUtil.listObjects(rightGraph, rightTriple, subNode).next().isBlank()) {
						rightVars.add(GraphUtil.listObjects(rightGraph, rightTriple, subNode).next());
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
					if (GraphUtil.listObjects(g, outerTriple, subNode).next().isBlank()) {
						outerVars.add(GraphUtil.listObjects(g, outerTriple, subNode).next());
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
