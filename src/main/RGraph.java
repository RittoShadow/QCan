package main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprWalker;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.sse.writers.WriterPath;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.yars.nx.NodeComparator;

import cl.uchile.dcc.blabel.jena.JenaModelIterator;
import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.blabel.label.GraphLabelling;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingArgs;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingResult;
import cl.uchile.dcc.blabel.lean.BFSGraphLeaning;
import cl.uchile.dcc.blabel.lean.DFSGraphLeaning;
import cl.uchile.dcc.blabel.lean.GraphLeaning.GraphLeaningResult;

/**
 * @author Jaime
 */
public class RGraph {
	
	public Graph graph = GraphFactory.createDefaultGraph();
	public Set<Var> vars = new HashSet<Var>();
	public int nTriples;
	public int id = 0;
	public Node root;
	public boolean distinct = false;
	public boolean leaning = true;
	public boolean containsPaths = false;
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
	private final Node pathNode = NodeFactory.createURI(this.URI+"path");
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

			Triple s, p, o;
			// We create a blank node if we have a variable. If it's a literal, we add a literal node.
			if (t.getSubject().isVariable()){
				Node temp = NodeFactory.createBlankNode(t.getSubject().getName());
				s = Triple.create(n, subNode, temp);
//				graph.add(Triple.create(temp, typeNode, varNode));
			}
			else if (t.getSubject().isBlank()) {
				Node temp = NodeFactory.createBlankNode(t.getSubject().getBlankNodeLabel());
				s = Triple.create(n, subNode, temp);
			}
			else if (t.getSubject().isURI()){
				s = Triple.create(n, subNode, NodeFactory.createURI(t.getSubject().toString()));
			}
			else{
				s = Triple.create(n, subNode, NodeFactory.createLiteralByValue(t.getSubject().getLiteralValue().toString(), t.getSubject().getLiteralDatatype()));
			}
			if (t.getPredicate().isVariable()){
				Node temp = NodeFactory.createBlankNode(t.getPredicate().getName());
				p = Triple.create(n, preNode, temp);
//				graph.add(Triple.create(temp, typeNode, varNode));
			}
			else if (t.getPredicate().isBlank()) {
				Node temp = NodeFactory.createBlankNode(t.getPredicate().getBlankNodeLabel());
				p = Triple.create(n, preNode, temp);
			}
			else if (t.getPredicate().isURI()){
				p = Triple.create(n, preNode, NodeFactory.createURI(t.getPredicate().toString()));
			}
			else{
				p = Triple.create(n, preNode, NodeFactory.createLiteralByValue(t.getPredicate().getLiteralValue().toString(), t.getPredicate().getLiteralDatatype()));
			}
			if (t.getObject().isVariable()){
				Node temp = NodeFactory.createBlankNode(t.getObject().getName());
				o = Triple.create(n, objNode, temp);
//				graph.add(Triple.create(temp, typeNode, varNode));
			}
			else if (t.getObject().isBlank()) {
				Node temp = NodeFactory.createBlankNode(t.getObject().getBlankNodeLabel());
				o = Triple.create(n, objNode, temp);
			}
			else if (t.getObject().isURI()){
				o = Triple.create(n, objNode, NodeFactory.createURI(t.getObject().toString()));
			}
			else{
				o = Triple.create(n, objNode, NodeFactory.createLiteralByValue(t.getObject().getLiteralValue().toString(), t.getObject().getLiteralDatatype()));
			}
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
		Set<Node> rootCandidates = new HashSet<Node>();
		Set<Node> predicates = new HashSet<Node>();
		Set<Node> objects = new HashSet<Node>();
		for (org.semanticweb.yars.nx.Node[] node : data){
			Node subject = null, predicate = null, object = null;
			if (Pattern.matches("_:.+", node[0].toN3())){
				subject = NodeFactory.createBlankNode(node[0].toN3().substring(2));
			}
			else if (Pattern.matches("<.+>", node[0].toN3())){
				subject = NodeFactory.createURI(node[0].toN3().replaceAll("<|>", ""));
			}
			else{
				subject = createLiteralWithType(node[0].toN3());
			}
			if (Pattern.matches("<.+>", node[1].toN3())){
				predicate = NodeFactory.createURI(node[1].toN3().replaceAll("<|>", ""));
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
				object = NodeFactory.createURI(node[2].toN3().replaceAll("<|>", ""));
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
		Set<Var> vars = new HashSet<Var>();
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
		graph.add(Triple.create(n, typeNode, pathNode));
		graph.add(Triple.create(n, argNode, p.getStartState()));
		graph.add(Triple.create(tp, preNode, n));
		this.graph = graph;
		this.vars = vars;
		this.root = tp;
	}
	
	public RGraph(Node s, Node o, Path p) {
		Set<Var> vars = new HashSet<Var>();
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

	
	/**
	 * @param s An RDF literal with a datatype.
	 * @return A node that represents a literal with a datatype. If no datatype is specified, it is assumed to be a string.
	 */
	public Node createLiteralWithType(String s){
		Node ans;
		s = s.replaceAll("\"", "");
		if (s.contains("^^")){
				NodeFactory.getType(s);
				ans = NodeFactory.createLiteralByValue(s.substring(0, s.indexOf("^^")), NodeFactory.getType(s.substring(1+s.lastIndexOf("^")).replaceAll("<|>", "")));
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
			}
			
		}
		if (type0.equals(optionalNode) && type1.equals(optionalNode)){  //Joins between operands with optional must be reordered.
			
		}
		else if (type0.equals(optionalNode)){
			Node left1 = GraphUtil.listObjects(this.graph, this.root, leftNode).next();
			graph.delete(Triple.create(this.root, leftNode, left1));
			graph.delete(Triple.create(root, argNode, this.root));
			graph.add(Triple.create(this.root, leftNode, root));
			graph.add(Triple.create(root, argNode, left1));
			graph.add(Triple.create(root, argNode, arg1.root));
			root = this.root;
		}
		else if (type1.equals(optionalNode)){
			Node left2 = GraphUtil.listObjects(arg1.graph, arg1.root, leftNode).next();
			arg1.graph.delete(Triple.create(arg1.root, leftNode, left2));
			graph.delete(Triple.create(root, argNode, arg1.root));
			arg1.graph.add(Triple.create(arg1.root, leftNode, root));
			arg1.graph.add(Triple.create(root, argNode, left2));
			arg1.graph.add(Triple.create(root, argNode, this.root));
			root = arg1.root;
		}
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
	 * @param n An r-graph that represents an expression.
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
	 * @param op A string that represents a SPARQL function.
	 * @param arg1 A string that represents an expression.
	 * @return Returns the node that represents the function.
	 */
	public Node filterFunction(String op, String arg1){
		Node n = NodeFactory.createBlankNode();
		Node o = filterOperator(op);
		Node a = NodeFactory.createBlankNode();
		if (arg1.startsWith("?")){
			graph.add(Triple.create(a, valueNode, NodeFactory.createBlankNode(arg1.substring(1))));
		}
		if (Pattern.matches("<.+://.+>", arg1.toString())){
			Node aux = NodeFactory.createURI(arg1);
			graph.add(Triple.create(a, valueNode, aux));
		}
		else{
			Node aux = NodeFactory.createLiteral(arg1);
			graph.add(Triple.create(a, valueNode, aux));
		}
		graph.add(Triple.create(n, functionNode, o));
		graph.add(Triple.create(n, argNode, a));
		graph.add(Triple.create(n, valueNode, NodeFactory.createLiteral(arg1.toString())));
		return n;
	}
	
	/**
	 * Creates an r-graph that represents a function over an expression (FILTER (f expr)) and adds it to this r-graph.
	 * @param op A string that represents a SPARQL function.
	 * @param arg1 A node that represents an expression.
	 * @return Returns the node that represents the function.
	 */
	public Node filterFunction(String op, Node arg1){
		Node n = NodeFactory.createBlankNode();
		Node o = filterOperator(op);
		Node a = NodeFactory.createBlankNode();
		if (!GraphUtil.listObjects(graph, arg1, functionNode).hasNext()){
			graph.add(Triple.create(a, valueNode, arg1));
		}
		else{
			a = arg1;
		}	
		graph.add(Triple.create(n, functionNode, o));
		graph.add(Triple.create(n, argNode, a));
		return n;
	}
	
	/**
	 * Creates an r-graph that represents a binary SPARQL function (FILTER (f expr1 expr2)) and adds it to this r-graph.
	 * @param op A string that represents a binary SPARQL function.
	 * @param arg1 A string that represents an expression.
	 * @param arg2 A string that represents an expression.
	 * @return Returns a node that represents the function.
	 */
	public Node filterFunction(String op, String arg1, String arg2){
		Node n = NodeFactory.createBlankNode();
		Node o = filterOperator(op);
		Node a = NodeFactory.createBlankNode();
		Node b = NodeFactory.createBlankNode();
		arg1 = arg1.replace("\"", "");
		arg2 = arg2.replace("\"", "");
		if (arg1.startsWith("?")){
			graph.add(Triple.create(a, valueNode, NodeFactory.createBlankNode(arg1.substring(1))));
		}
		else{
			Node aux = NodeFactory.createLiteral(arg1);
			graph.add(Triple.create(a, valueNode, aux));
		}
		if (arg2.startsWith("?")){
			graph.add(Triple.create(b, valueNode, NodeFactory.createBlankNode(arg2.substring(1))));
		}
		else{
			Node aux = NodeFactory.createLiteral(arg2);
			graph.add(Triple.create(b, valueNode, aux));
		}
		graph.add(Triple.create(n, functionNode, o));
		graph.add(Triple.create(n, argNode, a));
		graph.add(Triple.create(n, argNode, b));
		if (isOrderedFunction(op)){
			graph.add(Triple.create(a, orderNode, NodeFactory.createLiteralByValue(0, XSDDatatype.XSDint)));
			graph.add(Triple.create(b, orderNode, NodeFactory.createLiteralByValue(1, XSDDatatype.XSDint)));
		}
		return n;
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
		Node o = filterOperator(op);
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
	
	public Node filterFunction(String op, List<Node> nodes) {
		int i = 0;
		Node n = NodeFactory.createBlankNode();
		Node o = filterOperator(op);
		graph.add(Triple.create(n, functionNode, o));
		for (Node node : nodes) {
			Node newNode = NodeFactory.createBlankNode();
			if (!GraphUtil.listObjects(graph, node, functionNode).hasNext()){
				graph.add(Triple.create(n, argNode, newNode));
				graph.add(Triple.create(newNode, valueNode, node));
				if (isOrderedFunction(op)) {
					graph.add(Triple.create(newNode, orderNode, NodeFactory.createLiteralByValue(i++, XSDDatatype.XSDint)));
				}
			}
			else {
				graph.add(Triple.create(n, argNode, node));
				if (isOrderedFunction(op)) {
					graph.add(Triple.create(node, orderNode, NodeFactory.createLiteralByValue(i++, XSDDatatype.XSDint)));
				}
			}
		}
		return n;
	}
	
	public void filterNormalisation() {
		iterativeUpdate(conjunctionRule);
		iterativeUpdate(disjunctionRule);
	}
	
	public static RGraph group(OpGroup arg) {
		Node root = NodeFactory.createBlankNode();
		RGraph ans = new RGraph(root, GraphFactory.createDefaultGraph(), Collections.<Var>emptySet());
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
		ExtendedIterator<Node> vars = GraphUtil.listSubjects(group.graph, typeNode, varNode);
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
		Node o = filterOperator(op);
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
	 * @param s A string that represents a SPARQL function.
	 * @return Returns true if the function is ordered (i.e (f expr1 expr2) != (f expr2 expr1)).
	 */
	public boolean isOrderedFunction(String s){
		if (s.equals("<")){
			return true;
		}
		else if (s.equals(">")){
			return true;
		}
		else if (s.equals("<=")){
			return true;
		}
		else if (s.equals(">=")){
			return true;
		}
		else if (s.equals("-")){
			return true;
		}
		else if (s.equals("/")){
			return true;
		}
		else if (s.equals("regex")) {
			return true;
		}
		else if (s.equals("concat")) {
			return true;
		}
		else if (s.equals("if")) {
			return true;
		}
		else if (s.equals("in")) {
			return true;
		}
		else if (s.equals("notin")) {
			return true;
		}
		else if (s.equals("replace")) {
			return true;
		}
		else if (s.equals("strdt")) {
			return true;
		}
		else if (s.equals("strlang")) {
			return true;
		}
		else if (s.equals("strstarts")) {
			return true;
		}
		else if (s.equals("strends")) {
			return true;
		}
		else if (s.equals("contains")) {
			return true;
		}
		else if (s.equals("strbefore")) {
			return true;
		}
		else if (s.equals("strafter")) {
			return true;
		}
		else if (s.equals("substr")) {
			return true;
		}
		return false;
	}
	
	/**
	 * @param s A string that represents a SPARQL function.
	 * @return Returns true if the string represents a valid SPARQL function.
	 */
	public boolean isOperator(String s){
		if (isOrderedFunction(s)){
			return true;
		}
		else if (s.equals("=")){
			return true;
		}
		else if (s.equals("!=")){
			return true;
		}
		else if (s.equals("*")){
			return true;
		}
		else if (s.equals("+")){
			return true;
		}
		else if (s.equals("-")){
			return true;
		}
		else if (s.equals("/")){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * @param s A string that represents a SPARQL function.
	 * @return Returns a node that represents a SPARQL function.
	 */
	public Node filterOperator(String s){
		s = s.replace("\"", "");
		if (s.equals("=")){
			return NodeFactory.createURI(this.URI+"eq");
		}
		else if (s.equals("!=")){
			return NodeFactory.createURI(this.URI+"neq");
		}
		else if (s.equals("<")){
			return NodeFactory.createURI(this.URI+"lt");
		}
		else if (s.equals(">")){
			return NodeFactory.createURI(this.URI+"gt");
		}
		else if (s.equals("<=")){
			return NodeFactory.createURI(this.URI+"lteq");
		}
		else if (s.equals(">=")){
			return NodeFactory.createURI(this.URI+"gteq");
		}
		else{
			return NodeFactory.createLiteral(s);
		}
	}
	
	public String filterOperatorToString(Node n){
		if (n.equals(NodeFactory.createURI(this.URI+"eq"))){
			return "=";
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"neq"))){
			return "!=";
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"lt"))){
			return "<";
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"gt"))){
			return ">";
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"lteq"))){
			return "<=";
		}
		else if (n.equals(NodeFactory.createURI(this.URI+"gteq"))){
			return ">=";
		}
		else{
			return n.toString();
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
		ExtendedIterator<Node> e = GraphUtil.listSubjects(graph, typeNode, fromNode);
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
	 * @param vars The list of variables according to which the results are ordered.
	 * @param dir A list of integers that indicates if results are ordered ascending or descending.
	 */
	public void orderBy(List<Var> vars, List<Integer> dir){
		Node project = root;
		Node order = NodeFactory.createBlankNode();
		graph.add(Triple.create(project, modNode, order));
		graph.add(Triple.create(order, typeNode, orderByNode));
		for (int i = 0; i < vars.size(); i++){
			Node auxNode = NodeFactory.createBlankNode();
			graph.add(Triple.create(order, argNode, auxNode));
			graph.add(Triple.create(auxNode, varNode, NodeFactory.createBlankNode(vars.get(i).getName())));
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
	
	public boolean tripleExists(Node s, Node p, Node o){
		ExtendedIterator<Node> nodes = GraphUtil.listSubjects(this.graph, subNode, s);
		while (nodes.hasNext()){
			Node n = nodes.next();
			if (this.graph.contains(n, preNode, p) && this.graph.contains(n, objNode, o)){
				return true;
			}
		}
		return false;
	}
	
	public boolean containsProjection(){
		return GraphUtil.listSubjects(graph, typeNode, projectNode).hasNext();
	}
	
	public boolean containsUnion(){
		return GraphUtil.listSubjects(graph, typeNode, unionNode).hasNext();
	}
	
	public boolean containsJoin(){
		return GraphUtil.listSubjects(graph, typeNode, joinNode).hasNext();
	}
	
	public Model asModel(){
		return ModelFactory.createModelForGraph(graph);
	}
	
	public boolean isIsomorphicWith(RGraph e){
		return this.graph.isIsomorphicWith(e.graph);
	}
	
	public String toString(){
		return this.graph.toString().replace(";", "\n");
	}
	
	public void printTurtle(Graph g) {
		ModelFactory.createModelForGraph(g).write(System.out, "TURTLE");
	}
	
	public void print(){
		ExtendedIterator<Triple> e = GraphUtil.findAll(this.graph);
		while (e.hasNext()){
			System.out.println(e.next());
		}
		System.out.println("");
	}
	
	public void printGraph(Graph g){
		ExtendedIterator<Triple> e = GraphUtil.findAll(g);
		while (e.hasNext()){
			System.out.println(e.next() + " .");
		}
	}
	
	/**
	 * @return Returns a set containing all triples in this r-graph.
	 */
	public TreeSet<org.semanticweb.yars.nx.Node[]> getTriples(){
		Model model = this.asModel();
		JenaModelIterator jmi = new JenaModelIterator(model);
		TreeSet<org.semanticweb.yars.nx.Node[]> triples = new TreeSet<org.semanticweb.yars.nx.Node[]>(NodeComparator.NC);
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
	
	public GraphLeaningResult BFSLeaning(Collection<org.semanticweb.yars.nx.Node[]> triples) throws InterruptedException{
		BFSGraphLeaning dfsgl = new BFSGraphLeaning(triples);
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
		GraphLabellingResult glr = gl.call();
		return glr;
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
			System.out.println("CQ Normalisation");
		}
		boolean distinct = this.graph.contains(this.root, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
		removeRedundantOperators();
		if (leaning && distinct){
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
			UpdateAction.execute(duplicatesRule,ans.graph);
			if (verbose){
				System.out.println("Beginning labelling");
			}
			GraphLabellingResult glr = this.label(ans.getTriples());
			
			if (verbose){
				System.out.println("Labelling results: \n");
				System.out.println("Number of blank nodes: "+glr.getBnodeCount());
				System.out.println("Number of colouring iterations: "+glr.getColourIterationCount());
				System.out.println("Number of partitions found: "+glr.getPartitionCount());
			}
			ans = new RGraph(glr.getGraph());
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
			RGraph ans = new RGraph(glr.getGraph());
			return ans;
		}	
	}
	
	/**
	 * @param n A node that represents a literal value.
	 * @return Returns a string representation of the literal value with no datatype.
	 */
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
	
	public String tripleToString(Triple n){
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
	
	public RGraph ucqMinimisation() throws InterruptedException, HashCollisionException{
		ArrayList<RGraph> result = new ArrayList<RGraph>();
		ArrayList<RGraph> redundant = new ArrayList<RGraph>();
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		ExtendedIterator<Node> ucqs = GraphUtil.listSubjects(this.graph, typeNode, unionNode);
		if (!ucqs.hasNext()) {
			if (GraphUtil.listObjects(graph, root, opNode).hasNext()) {
				Node first = GraphUtil.listObjects(graph, root, opNode).next();
				if (GraphUtil.listObjects(graph, first, typeNode).next().equals(joinNode)) {
					return this.getLeanForm();
				}
			}
		}
		while (ucqs.hasNext()){ //Make sure it's a union of conjunctive queries.
			Node union = ucqs.next();
			Node subUnion, preUnion;
			if (GraphUtil.listSubjects(graph, opNode, union).hasNext()){
				subUnion = GraphUtil.listSubjects(graph, opNode, union).next();
				preUnion = opNode;
			}
			else if (GraphUtil.listSubjects(graph, leftNode, union).hasNext()){
				subUnion = GraphUtil.listSubjects(graph, leftNode, union).next();
				preUnion = leftNode;
			}
			else if (GraphUtil.listSubjects(graph, rightNode, union).hasNext()){
				subUnion = GraphUtil.listSubjects(graph, rightNode, union).next();
				preUnion = rightNode;
			}
			else{
				subUnion = GraphUtil.listSubjects(graph, argNode, union).next();
				preUnion = argNode;
			}
			Graph inner = ge.extract(union, graph);
			Graph outer = GraphFactory.createPlainGraph();
			Graph filterGraph = GraphFactory.createPlainGraph();
			ExtendedIterator<Node> filters = GraphUtil.listObjects(inner, union, patternNode);
			ExtendedIterator<Node> cQueries = GraphUtil.listObjects(inner, union, argNode);
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
				ExtendedIterator<Node> orderBy = GraphUtil.listSubjects(graph, typeNode, orderByNode);
				//Extract all variables in ORDER BY clauses
				while (orderBy.hasNext()){
					Node o = orderBy.next();
					ExtendedIterator<Node> orderVars = GraphUtil.listObjects(graph, o, argNode);
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
				List<Node> filterVars = GraphUtil.listSubjects(filterGraph, typeNode, varNode).toList();
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
					ExtendedIterator<Node> cqFilterVars = GraphUtil.listSubjects(e.graph, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
					while(cqFilterVars.hasNext()){
						Node p = cqFilterVars.next();
						e.graph.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
					}
					UpdateAction.execute(tripleRelabelRule,e.graph);
					UpdateAction.execute(branchRelabelRule,e.graph);
					UpdateAction.execute(branchCleanUpRule,e.graph);
					RGraph a = e.getLeanForm();
					result.add(a);
				}
				for (int i = 0; i < result.size(); i++){
					for (int j = i + 1; j < result.size(); j++){
						RGraph e = result.get(i);
						RGraph e1 = result.get(j);
						String askQuery = "ASK\nWHERE{\n";
						String askQuery1 = "ASK\nWHERE{\n";
						Graph g0 = GraphFactory.createDefaultGraph();
						Graph g1 = GraphFactory.createDefaultGraph();
						GraphUtil.addInto(g0, e.graph);
						GraphUtil.addInto(g1, e1.graph);
						UpdateAction.execute(askRule, g0);
						UpdateAction.execute(askRule, g1);
						ExtendedIterator<Triple> t0 = GraphUtil.findAll(g1);
						ExtendedIterator<Triple> t1 = GraphUtil.findAll(g0);
						while(t0.hasNext()){
							askQuery += tripleToString(t0.next()) + "\n";
						}
						while(t1.hasNext()){
							askQuery1 += tripleToString(t1.next()) + "\n";
						}
						askQuery += "}";
						askQuery1 += "}";
						Query q = QueryFactory.create(askQuery);
						Query q1 = QueryFactory.create(askQuery1);
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
						Node eRoot = GraphUtil.listSubjects(e.graph, typeNode, unionNode).next();
						eRoot = GraphUtil.listObjects(e.graph, eRoot, argNode).next();
						GraphUtil.addInto(eg.graph, ge.extract(eRoot, e.graph));				
						eg.graph.add(Triple.create(subUnion, preUnion, eRoot));
						if (!filterGraph.isEmpty()){
							Node fNode = GraphUtil.listSubjects(filterGraph, typeNode, extraNode).next();
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
						if (eg == null){
							eg = e;
							Node r = GraphUtil.listSubjects(eg.graph, typeNode, unionNode).next();
							eg.root = r;
							Node n = GraphUtil.listObjects(eg.graph, eg.root, opNode).next();
							eg.graph.add(Triple.create(eg.root, opNode, uNode));
							eg.graph.delete(Triple.create(eg.root, opNode, n));
							eg.graph.add(Triple.create(uNode, typeNode, unionNode));
							eg.graph.add(Triple.create(uNode, argNode, n));
							GraphUtil.addInto(eg.graph, filterGraph);
						}
						else{
							Node eRoot = GraphUtil.listSubjects(e.graph, typeNode, unionNode).next();
							eRoot = GraphUtil.listObjects(e.graph, eRoot, argNode).next();
							GraphUtil.addInto(eg.graph, ge.extract(eRoot, e.graph));				
							eg.graph.add(Triple.create(uNode, argNode, eRoot));	
						}
					}	
				}
				eg.setDistinctNode(true);
				if (!filterGraph.isEmpty()){
					Node fNode = GraphUtil.listSubjects(filterGraph, typeNode, extraNode).next();
					eg.graph.add(Triple.create(uNode, patternNode, fNode));
					GraphUtil.addInto(eg.graph, filterGraph);
				}
				UpdateAction.execute(branchUnionRule,eg.graph);
				UpdateAction.execute(branchCleanUpRule2,eg.graph);
				UpdateAction.execute(joinRule, eg.graph);
				return eg;
			}
		}	
		return this;
	}
	
	@SuppressWarnings("unused")
	public RGraph uc2rpqMinimisation() {
		ExtendedIterator<Node> ucqs = GraphUtil.listSubjects(this.graph, typeNode, unionNode);
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		if (!ucqs.hasNext()) {
			if (GraphUtil.listObjects(graph, root, opNode).hasNext()) {
				Node first = GraphUtil.listObjects(graph, root, opNode).next();
				ArrayList<PGraph> currentSet = new ArrayList<PGraph>();
				if (GraphUtil.listObjects(graph, first, typeNode).next().equals(joinNode)) {
					//===============================================================================
					Graph g = ge.extract(first, graph);
					Graph outer = GraphFactory.createPlainGraph();
					GraphUtil.addInto(outer, graph);
					GraphUtil.deleteFrom(outer, g);
					Graph filterGraph = GraphFactory.createPlainGraph();
					ExtendedIterator<Node> filters = GraphUtil.listObjects(g, first, patternNode);
					if (filters.hasNext()){
						filterGraph = ge.extract(filters.next(), g);
					}
					GraphUtil.addInto(outer, graph);
					GraphUtil.deleteFrom(outer, g);
					outer.remove(root, opNode, first);
					//Extract all projected variables from projection node.
					ExtendedIterator<Node> pVars = GraphUtil.listObjects(graph, root, argNode);
					while (pVars.hasNext()){
						Node p = pVars.next();
						outer.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
						outer.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
						outer.add(Triple.create(p, typeNode, varNode));
						g.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
						g.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
					}
					ExtendedIterator<Node> orderBy = GraphUtil.listSubjects(graph, typeNode, orderByNode);
					//Extract all variables in ORDER BY clauses
					while (orderBy.hasNext()){
						Node o = orderBy.next();
						ExtendedIterator<Node> orderVars = GraphUtil.listObjects(graph, o, argNode);
						while (orderVars.hasNext()){
							Node v = orderVars.next();
							Node var = GraphUtil.listObjects(graph, v, varNode).next();
							outer.add(Triple.create(var, valueNode, NodeFactory.createLiteral(var.getBlankNodeLabel())));
							outer.add(Triple.create(var, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
							outer.add(Triple.create(var, typeNode, varNode));
							g.add(Triple.create(var, valueNode, NodeFactory.createLiteral(var.getBlankNodeLabel())));
							g.add(Triple.create(var, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
						}
					}
					UpdateAction.execute(filterVarsRule,filterGraph);	
					//printGraph(filterGraph);
					List<Node> filterVars = GraphUtil.listSubjects(filterGraph, typeNode, varNode).toList();
					for (Node f : filterVars){
						filterGraph.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
						filterGraph.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
					}
					//===============================================================================================================
					Graph auxGraph = GraphFactory.createPlainGraph();
					ExtendedIterator<Node> triplePaths = GraphUtil.listObjects(g, first, argNode);
					List<Node> triplePathsList = triplePaths.toList();
					BasicPattern bp = new BasicPattern();
					Map<Node,PGraph> pathMap = new HashMap<Node,PGraph>();
					Map<Node,BasicPattern> bpMap = new HashMap<Node,BasicPattern>();
					Map<Node,String> stringMap = new HashMap<Node,String>();
					ExtendedIterator<Triple> triples = GraphUtil.findAll(g);
//					while (triples.hasNext()) {
//						Triple t = triples.next();
//						Node s = t.getSubject();
//						Node p = t.getPredicate();
//						Node o = t.getObject();
//						if (p.equals(subNode) || p.equals(preNode) || p.equals(objNode)) {
//							continue;
//						}
//						else if (p.equals(tempNode) || p.equals(valueNode)) {
//							auxGraph.add(Triple.create(s, p, o));
//							continue;
//						}
////						if (s.isBlank()) {
////							s = Var.alloc(s.getBlankNodeLabel());
////						}
////						if (p.isBlank()) {
////							p = Var.alloc(p.getBlankNodeLabel());
////						}
////						if (o.isBlank()) {
////							o = Var.alloc(o.getBlankNodeLabel());
////						}
//						bp.add(Triple.create(s, p, o));
//						auxGraph.add(Triple.create(s, p, o));
//					}
					for (Node tp : triplePathsList) {
						Node s = GraphUtil.listObjects(g, tp, subNode).next();
						Node o = GraphUtil.listObjects(g, tp, objNode).next();
						Node p = GraphUtil.listObjects(g, tp, preNode).next();
						Node subject = s, predicate = p, object = o;
						String pathString = GraphUtil.listObjects(g, tp, preNode).next().toString(false);
						Path path;
						BasicPattern bp0 = new BasicPattern();
						if (s.isBlank()) {
							subject = Var.alloc(s.getBlankNodeLabel());
							if (g.contains(s, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean))) {
								bp.add(Triple.create(subject, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								bp.add(Triple.create(subject, valueNode, NodeFactory.createLiteral(s.getBlankNodeLabel())));
								bp0.add(Triple.create(subject, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								bp0.add(Triple.create(subject, valueNode, NodeFactory.createLiteral(s.getBlankNodeLabel())));
								auxGraph.add(Triple.create(s, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								auxGraph.add(Triple.create(s, valueNode, NodeFactory.createLiteral(s.getBlankNodeLabel())));
							}
						}
						if (p.isBlank()) {
							predicate = Var.alloc(p.getBlankNodeLabel());
							if (g.contains(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean))) {
								bp.add(Triple.create(predicate, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								bp.add(Triple.create(predicate, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
								bp0.add(Triple.create(predicate, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								bp0.add(Triple.create(predicate, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
								auxGraph.add(Triple.create(p, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								auxGraph.add(Triple.create(p, valueNode, NodeFactory.createLiteral(p.getBlankNodeLabel())));
							}
						}
						if (o.isBlank()) {
							object = Var.alloc(o.getBlankNodeLabel());
							if (g.contains(o, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean))) {
								bp.add(Triple.create(object, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								bp.add(Triple.create(object, valueNode, NodeFactory.createLiteral(o.getBlankNodeLabel())));
								bp0.add(Triple.create(object, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								bp0.add(Triple.create(object, valueNode, NodeFactory.createLiteral(o.getBlankNodeLabel())));
								auxGraph.add(Triple.create(o, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
								auxGraph.add(Triple.create(o, valueNode, NodeFactory.createLiteral(o.getBlankNodeLabel())));
							}
						}
						if (graph.contains(tp, typeNode, triplePathNode)) {
							path = SSE.parsePath(pathString);
							TriplePath tPath = new TriplePath(s, path, o);
							Node blank = NodeFactory.createBlankNode();
							predicate = Var.alloc(blank.getBlankNodeLabel());
							PGraph pg = new PGraph(tPath);
							currentSet.add(pg);
							pathMap.put(predicate, pg);
							auxGraph.add(Triple.create(tp, preNode, blank));
							auxGraph.add(Triple.create(blank, valueNode, p));
							bp0.add(Triple.create(tp, subNode, subject));
							bp0.add(Triple.create(tp, preNode, predicate));
							bp0.add(Triple.create(tp, objNode, object));
							bpMap.put(predicate, bp0);
							System.out.println("Path: "+tPath);
						}
						else {
							if (p.isURI()) {
								path = PathFactory.pathLink(p);
								TriplePath tPath = new TriplePath(s, path, o);
								Node blank = NodeFactory.createBlankNode();
								predicate = Var.alloc(blank.getBlankNodeLabel());
								PGraph pg = new PGraph(tPath);
								currentSet.add(pg);
								pathMap.put(predicate, pg);
								auxGraph.add(Triple.create(tp, preNode, blank));
								auxGraph.add(Triple.create(blank, valueNode, p));
								bp0.add(Triple.create(tp, subNode, subject));
								bp0.add(Triple.create(tp, preNode, predicate));
								bp0.add(Triple.create(tp, objNode, object));
								bpMap.put(predicate, bp0);
								System.out.println("Path: "+tPath);
							}
							else {
								auxGraph.add(Triple.create(tp, preNode, predicate));
							}
						}	
//						bp.add(Triple.create(subject, predicate, object));
						auxGraph.add(Triple.create(tp, subNode, s));
						auxGraph.add(Triple.create(tp, objNode, o));
						bp.add(Triple.create(Var.alloc(tp.getBlankNodeLabel()), subNode, subject));
						bp.add(Triple.create(Var.alloc(tp.getBlankNodeLabel()), preNode, predicate));
						bp.add(Triple.create(Var.alloc(tp.getBlankNodeLabel()), objNode, object));
					}
					Op op = new OpBGP(bp);
					System.out.println("BGP: "+op+"\n");
					for (Node n : pathMap.keySet()) {
						Op op0 = new OpBGP(bp);
						List<Var> vars = new ArrayList<Var>();
						vars.add(Var.alloc(n));
						op0 = new OpProject(op0, vars);
						Query q = OpAsQuery.asQuery(op0);
						q.setDistinct(true);
						QueryExecution qe = QueryExecutionFactory.create(q, ModelFactory.createModelForGraph(auxGraph));
						ResultSet rs = qe.execSelect();
						while (rs.hasNext()) {
							System.out.println(rs.next());
						}
						System.out.println();
					}
					System.out.println();
					printGraph(auxGraph);
					for (Node n : bpMap.keySet()) {
						Op op0 = new OpBGP(bpMap.get(n));
						List<Var> vars = new ArrayList<Var>();
						//vars.add(Var.alloc(n));
						op0 = new OpProject(op0, vars);
						Query q = OpAsQuery.asQuery(op0);
						//q.setDistinct(true);
						QueryExecution qe = QueryExecutionFactory.create(q, ModelFactory.createModelForGraph(auxGraph));
						ResultSet rs = qe.execSelect();
						while (rs.hasNext()) {
							System.out.println(rs.next());
						}
						System.out.println();
					}
					BasicPattern bp2 = new BasicPattern();
					bp2.add(Triple.create(Var.alloc("s"), Var.alloc("p"), Var.alloc("o")));
					Op op2 = new OpBGP(bp2);
					Query q = OpAsQuery.asQuery(op);
					Query q2 = OpAsQuery.asQuery(op2);
					q.setDistinct(true);
					q2.setDistinct(true);
					QueryExecution qe = QueryExecutionFactory.create(q, ModelFactory.createModelForGraph(auxGraph));
					QueryExecution qe2 = QueryExecutionFactory.create(q2, ModelFactory.createModelForGraph(auxGraph));
					System.out.println();
					printGraph(auxGraph);
					ResultSet rs2 = qe2.execSelect();
					System.out.println();
					while (rs2.hasNext()) {
						System.out.println(rs2.next());
					}
					System.out.println("End of CQ\n");
				}
				for (int i = 0; i < currentSet.size(); i++) {
					for (int j = 0; j < currentSet.size(); j++) {
						PGraph pg1 = currentSet.get(i);
						PGraph pg2 = currentSet.get(j);
						boolean ans = pg1.containedIn(pg2);
					}
				}
			}
		}
		while (ucqs.hasNext()) {
			List<List<PGraph>> graphs = new ArrayList<List<PGraph>>();
			Node union = ucqs.next();
			Node subUnion, preUnion;
			if (GraphUtil.listSubjects(graph, opNode, union).hasNext()){
				subUnion = GraphUtil.listSubjects(graph, opNode, union).next();
				preUnion = opNode;
			}
			else if (GraphUtil.listSubjects(graph, leftNode, union).hasNext()){
				subUnion = GraphUtil.listSubjects(graph, leftNode, union).next();
				preUnion = leftNode;
			}
			else if (GraphUtil.listSubjects(graph, rightNode, union).hasNext()){
				subUnion = GraphUtil.listSubjects(graph, rightNode, union).next();
				preUnion = rightNode;
			}
			else{
				subUnion = GraphUtil.listSubjects(graph, argNode, union).next();
				preUnion = argNode;
			}
			Graph inner = ge.extract(union, graph);
			Graph outer = GraphFactory.createPlainGraph();
			GraphUtil.addInto(outer, graph);
			GraphUtil.deleteFrom(outer, inner);
			Graph filterGraph = GraphFactory.createPlainGraph();
			ExtendedIterator<Node> filters = GraphUtil.listObjects(inner, union, patternNode);
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
			ExtendedIterator<Node> orderBy = GraphUtil.listSubjects(graph, typeNode, orderByNode);
			//Extract all variables in ORDER BY clauses
			while (orderBy.hasNext()){
				Node o = orderBy.next();
				ExtendedIterator<Node> orderVars = GraphUtil.listObjects(graph, o, argNode);
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
			List<Node> filterVars = GraphUtil.listSubjects(filterGraph, typeNode, varNode).toList();
			for (Node f : filterVars){
				filterGraph.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
				filterGraph.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
			}
			ExtendedIterator<Node> rpqs = GraphUtil.listObjects(inner, union, argNode);
			while (rpqs.hasNext()) {
				ArrayList<PGraph> currentSet = new ArrayList<PGraph>();
				Node rpq = rpqs.next();
				Graph g = ge.extract(rpq, inner);
				ExtendedIterator<Node> triplePaths = GraphUtil.listObjects(g, rpq, argNode);
				List<Node> triplePathsList = triplePaths.toList();
				BasicPattern bp = new BasicPattern();
				Map<Node,PGraph> pathMap = new HashMap<Node,PGraph>();
				Map<Node,String> stringMap = new HashMap<Node,String>();
				ExtendedIterator<Triple> triples = GraphUtil.findAll(g);
				while (triples.hasNext()) {
					Triple t = triples.next();
					Node s = t.getSubject();
					Node p = t.getPredicate();
					Node o = t.getObject();
					if (p.equals(subNode) || p.equals(preNode) || p.equals(objNode)) {
						continue;
					}
					if (s.isBlank()) {
						s = Var.alloc(s.getBlankNodeLabel());
					}
					if (p.isBlank()) {
						p = Var.alloc(p.getBlankNodeLabel());
					}
					if (o.isBlank()) {
						o = Var.alloc(o.getBlankNodeLabel());
					}
					bp.add(Triple.create(s, p, o));
				}
				for (Node tp : triplePathsList) {
					Node s = GraphUtil.listObjects(g, tp, subNode).next();
					Node o = GraphUtil.listObjects(g, tp, objNode).next();
					Node p = GraphUtil.listObjects(g, tp, preNode).next();
					Node subject = s, predicate = p, object = o;
					String pathString = GraphUtil.listObjects(g, tp, preNode).next().toString(false);
					Path path;
					if (s.isBlank()) {
						subject = Var.alloc(s.getBlankNodeLabel());
					}
					if (p.isBlank()) {
						predicate = Var.alloc(p.getBlankNodeLabel());
					}
					if (o.isBlank()) {
						object = Var.alloc(o.getBlankNodeLabel());
					}
					if (graph.contains(tp, typeNode, triplePathNode)) {
						path = SSE.parsePath(pathString);
						TriplePath tPath = new TriplePath(s, path, o);
						predicate = Var.alloc(NodeFactory.createBlankNode().getBlankNodeLabel());
						PGraph pg = new PGraph(tPath);
						currentSet.add(pg);
						pathMap.put(predicate, pg);
						System.out.println("Path: "+tPath);
					}
					else {
						if (p.isURI()) {
							path = PathFactory.pathLink(p);
							TriplePath tPath = new TriplePath(s, path, o);
							predicate = Var.alloc(NodeFactory.createBlankNode().getBlankNodeLabel());
							PGraph pg = new PGraph(tPath);
							currentSet.add(pg);
							pathMap.put(predicate, pg);
							System.out.println("Path: "+tPath);
						}
					}	
					bp.add(Triple.create(tp, subNode, subject));
					bp.add(Triple.create(tp, preNode, predicate));
					bp.add(Triple.create(tp, objNode, object));
				}
				Op op = new OpBGP(bp);
				List<Var> vars = new ArrayList<Var>();
				for (Node n : pathMap.keySet()) {
					vars.add(Var.alloc(n));
				}
				op = new OpProject(op, vars);
				Query q = OpAsQuery.asQuery(op);
				q.setDistinct(true);
				QueryExecution qe = QueryExecutionFactory.create(q, ModelFactory.createModelForGraph(g));
				printGraph(g);
				ResultSet rs = qe.execSelect();
				System.out.println();
				while (rs.hasNext()) {
					System.out.println(rs.next());
				}
				System.out.println(bp);
				System.out.println("End of CQ\n");
				graphs.add(currentSet);
			}
			for (int i = 0; i < graphs.size(); i++) {
				for (int j = 0; j < graphs.size(); j++) {
					for (int k = 0; k < graphs.get(i).size(); k++) {
						for (int h = 0; h < graphs.get(j).size(); h++) {
							PGraph pg1 = graphs.get(i).get(k);
							PGraph pg2 = graphs.get(j).get(h);
							boolean ans = pg1.containedIn(pg2);
						}
					}
				}
			}
		}
		System.out.println("Loaded");
		return this;
	}

	
	
	public int getNumberOfNodes(){
		return this.graph.size();
	}
	
	public int getNumberOfTriples(){
		ExtendedIterator<Node> triples = GraphUtil.listSubjects(graph, typeNode, tpNode);
		return triples.toList().size();
	}
	
	public int getNumberOfVars(){
		return GraphUtil.listSubjects(graph, typeNode, varNode).toList().size();
	}
	
	public Model getCQ(RGraph e){
		Graph m = GraphFactory.createDefaultGraph();
		ExtendedIterator<Node> triples = GraphUtil.listSubjects(e.graph, typeNode, tpNode);
		while(triples.hasNext()){
			Node n = triples.next();
			Node s = GraphUtil.listObjects(e.graph, n, subNode).next();
			Node p = GraphUtil.listObjects(e.graph, n, preNode).next();
			Node o = GraphUtil.listObjects(e.graph, n, objNode).next();
			m.add(Triple.create(s, p, o));
		}
		return ModelFactory.createModelForGraph(m);
	}
	
	public String executeQuery(String file){
		String ans = "";
		Query q = QueryFactory.read(file);
		Op op = Algebra.compile(q);
		QueryIterator qIter = Algebra.exec(op, this.asModel());
		for (; qIter.hasNext(); ){
			ans += qIter.next() + "\n";
		}
		return ans;
		
	}
	
	public boolean isWellDesigned() {
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		ExtendedIterator<Node> optionalPatterns = GraphUtil.listSubjects(graph, typeNode, optionalNode);
		if (optionalPatterns.hasNext()) {
			while (optionalPatterns.hasNext()) {
				Set<Node> leftVars = new HashSet<Node>();
				Set<Node> rightVars = new HashSet<Node>();
				Set<Node> outerVars = new HashSet<Node>();
				Graph g = GraphFactory.createPlainGraph();
				GraphUtil.addInto(g, graph);
				Node opt = optionalPatterns.next();
				Graph inner = ge.extract(opt, g);
				Node left = GraphUtil.listObjects(inner, opt, leftNode).next();
				Node right = GraphUtil.listObjects(inner, opt, rightNode).next();
				Graph leftGraph = ge.extract(left, inner);
				Graph rightGraph = ge.extract(right, inner);
				GraphUtil.deleteFrom(g, inner);
				ExtendedIterator<Node> outerTriples = GraphUtil.listSubjects(g, typeNode, tpNode);
				ExtendedIterator<Node> leftTriples = GraphUtil.listSubjects(leftGraph, typeNode, tpNode);
				ExtendedIterator<Node> rightTriples = GraphUtil.listSubjects(rightGraph, typeNode, tpNode);
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
}
