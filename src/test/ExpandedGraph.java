package test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.graph.GraphFactory;
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

public class ExpandedGraph {
	
	public Graph graph = GraphFactory.createDefaultGraph();
	public Set<Var> vars = new HashSet<Var>();
	public int nTriples;
	public int id = 0;
	public Node root;
	public boolean distinct = false;
	public boolean leaning = true;
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
	private final Node tempNode = NodeFactory.createURI(this.URI+"temp");
	private final UpdateRequest conjunctionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/conjunction.ru"));
	private final UpdateRequest disjunctionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/disjunction.ru"));
	private final UpdateRequest distributionRule1 = UpdateFactory.read(getClass().getResourceAsStream("/rules/distribution.ru"));
	private final UpdateRequest distributionRule2 = UpdateFactory.read(getClass().getResourceAsStream("/rules/distribution2.ru"));
	private final UpdateRequest duplicatesRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/distribution2.ru"));
	private final UpdateRequest joinRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/join.ru"));
	private final UpdateRequest joinTripleRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/joinTriple.ru"));
	private final UpdateRequest redundancyRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/redundancy.ru"));
	private final UpdateRequest unionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/union.ru"));
	private final UpdateRequest branchCleanUpRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/cleanUp.ru"));
	private final UpdateRequest branchCleanUpRule2 = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/branchCleanUp2.ru"));
	private final UpdateRequest branchRelabelRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/branchRelabel.ru"));
	private final UpdateRequest branchUnionRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/branchUnion.ru"));
	private final UpdateRequest filterVarsRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/filterVars.ru"));
	private final UpdateRequest joinLabelRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/joinLabel.ru"));
	private final UpdateRequest tripleRelabelRule = UpdateFactory.read(getClass().getResourceAsStream("/rules/branchLabel/tripleRelabel.ru"));
	
	public ExpandedGraph(List<Triple> triples, List<Var> vars, int id){
		if (vars != null){
			this.vars.addAll(vars);
		}
		int j = 0;
		this.id = id;
		nTriples = triples.size();
		if (triples.size() > 1){
			this.root = NodeFactory.createBlankNode("join"+this.id);
			//Adding typing now.
			graph.add(Triple.create(this.root, typeNode, joinNode));
		}
		for (Triple t : triples){
			String nv = "tp" + this.id + j++;
			Node n = NodeFactory.createBlankNode(nv);
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
	
	public ExpandedGraph(Node root, Graph graph, Collection<Var> vars){
		this.root = root;
		this.graph = graph;
		this.vars.addAll(vars);
	}
	
	public ExpandedGraph(List<Triple> triples, List<Var> vars){
		this(triples,vars,0);
	}
	
	public ExpandedGraph(List<Triple> triples, int id){
		this(triples, null, id);
	}
	
	public ExpandedGraph(Collection<org.semanticweb.yars.nx.Node[]> data){
		for (org.semanticweb.yars.nx.Node[] node : data){
			Node subject = null, predicate = null, object = null;
			if (Pattern.matches("_:.+", node[0].toN3())){
				subject = NodeFactory.createBlankNode(node[0].toN3().substring(2));
			}
			else if (Pattern.matches("<.+://.+>", node[0].toN3())){
				subject = NodeFactory.createURI(node[0].toN3().replaceAll("<|>", ""));
			}
			else{
				subject = createLiteralWithType(node[0].toN3());
			}
			if (Pattern.matches("<.+://.+>", node[1].toN3())){
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
			else if (Pattern.matches("<.+://.+>", node[2].toN3())){
				object = NodeFactory.createURI(node[2].toN3().replaceAll("<|>", ""));
			}
			else{
				object = createLiteralWithType(node[2].toN3());
			}
			if (subject != null){
				graph.add(Triple.create(subject, predicate, object));
			}
			else{
				System.err.println("Invalid blank node label.");
			}
		}
	}
	
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
	
	public void addVars(ExpandedGraph e){
		this.vars.addAll(e.vars);
	}
	
	public void join(ExpandedGraph arg1, int Id){
		Node root = NodeFactory.createBlankNode("join"+Id);
		graph.add(Triple.create(root, typeNode, joinNode));
		boolean a = this.graph.contains(this.root, typeNode, optionalNode);
		boolean b = arg1.graph.contains(arg1.root, typeNode, optionalNode);
		if (!this.root.equals(root)){
			graph.add(Triple.create(root, argNode, this.root));
		}
		if (!arg1.root.equals(root)){
			graph.add(Triple.create(root, argNode, arg1.root));
		}
		if (a && b){  //Joins between operands with optional must be reordered.
			System.out.println("You shouldn't be here");
		}
		else if (a){
			Node left1 = GraphUtil.listObjects(this.graph, this.root, leftNode).next();
			graph.delete(Triple.create(this.root, leftNode, left1));
			graph.delete(Triple.create(root, argNode, this.root));
			graph.add(Triple.create(this.root, leftNode, root));
			graph.add(Triple.create(root, argNode, left1));
			graph.add(Triple.create(root, argNode, arg1.root));
			root = this.root;
		}
		else if (b){
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
	
	public void union(ExpandedGraph arg1, int Id){
		Node root = NodeFactory.createBlankNode("union"+Id);
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
	
	public void optional(ExpandedGraph arg1, int Id){
		Node root = NodeFactory.createBlankNode("optional"+Id);
		graph.add(Triple.create(root, typeNode, optionalNode));
//		boolean a = this.graph.contains(this.root, typeNode, optionalNode);
//		boolean b = arg1.graph.contains(arg1.root, typeNode, optionalNode);
//		if (a && b){
//			ExtendedIterator<Node> nodes = GraphUtil.listObjects(arg1.graph, arg1.root, argNode);
//			while(nodes.hasNext()){
//				Node n = nodes.next();
//				this.graph.add(Triple.create(this.root, argNode, n));
//				GraphUtil.remove(arg1.graph, arg1.root, argNode, n);
//			}
//			GraphUtil.remove(arg1.graph, arg1.root, typeNode, unionNode);
//		}
//		else if (a){
//			this.graph.add(Triple.create(this.root, leftNode, arg1.root));
//		}
//		else if (b){
//			this.graph.add(Triple.create(arg1.root, rightNode, this.root));
//			this.root = arg1.root;
//		}
		graph.add(Triple.create(root, leftNode, this.root));
		graph.add(Triple.create(root, rightNode, arg1.root));
		this.root = root;
		addVars(arg1);
		GraphUtil.addInto(this.graph, arg1.graph);	
	}
	
	public void filter(Node n, int id){
		Node filter = NodeFactory.createBlankNode("filter"+id);
		graph.add(Triple.create(root, modNode, filter));
		graph.add(Triple.create(filter, typeNode, filterNode));
		graph.add(Triple.create(filter, argNode, n));	
	}
	
	public Node filterAnd(Node arg1, Node arg2){
		Node o = NodeFactory.createBlankNode();
		graph.add(Triple.create(o, typeNode, andNode));
		graph.add(Triple.create(o, argNode, arg1));
		graph.add(Triple.create(o, argNode, arg2));
		return o;
	}
	
	public Node filterOr(Node arg1, Node arg2){
		Node o = NodeFactory.createBlankNode();
		graph.add(Triple.create(o, typeNode, orNode));
		graph.add(Triple.create(o, argNode, arg1));
		graph.add(Triple.create(o, argNode, arg2));
		return o;
	}
	
	public Node filterNot(Node arg1){
		Node o = NodeFactory.createBlankNode();
		graph.add(Triple.create(o, typeNode, notNode));
		graph.add(Triple.create(o, argNode, arg1));
		return o;
	}
	
	public Node filterFunction(String op, String arg1){
		Node n = NodeFactory.createBlankNode();
		Node o = NodeFactory.createLiteral(op);
		Node a = NodeFactory.createBlankNode();
		if (arg1.startsWith("?")){
			graph.add(Triple.create(a, valueNode, NodeFactory.createBlankNode(arg1.substring(1))));
		}
		if (Pattern.matches("<.+://.+>", arg1.toString())){
			
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
	
	public Node filterFunction(String op, Node arg1){
		Node n = NodeFactory.createBlankNode();
		Node o = NodeFactory.createLiteral(op);
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
	
	public Node filterFunction(String op, String arg1, String arg2){
		Node n = NodeFactory.createBlankNode();
		Node o = NodeFactory.createLiteral(op);
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
	
	public Node filterFunction(String op, Node arg1, Node arg2){
		Node n = NodeFactory.createBlankNode();
		Node o = NodeFactory.createLiteral(op);
		Node a = NodeFactory.createBlankNode();
		Node b = NodeFactory.createBlankNode();
		boolean x = !GraphUtil.listObjects(graph, arg1, functionNode).hasNext();
		boolean y = !GraphUtil.listObjects(graph, arg2, functionNode).hasNext();
		graph.add(Triple.create(n, functionNode, o));
		if (x){
			graph.add(Triple.create(a, valueNode, arg1));
		}
		else{
			a = arg1;
		}
		if (y){
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
		return false;
	}
	
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
	
	public void project(Collection<Var> vars){
		Node root = NodeFactory.createBlankNode("project");
		graph.add(Triple.create(root, typeNode, projectNode));
		graph.add(Triple.create(root, opNode, this.root));
		this.vars.addAll(vars);
		
		if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)){
			graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
		}
		for (Var v : vars){
			graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));		
		}
		this.root = root;
	}
	
	public void setDistinctNode(boolean isDistinct){
		graph.add(Triple.create(root, distinctNode, NodeFactory.createLiteralByValue(isDistinct, XSDDatatype.XSDboolean)));
	}
	
	public boolean isDistinct(){
		return this.graph.contains(root, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
	}
	
	public void fromGraph(Collection<String> g){
		Node n = NodeFactory.createBlankNode();
		graph.add(Triple.create(root, opNode, n));
		graph.add(Triple.create(n, typeNode, fromNode));
		for (String s : g){
			graph.add(Triple.create(n, argNode, NodeFactory.createURI(s)));
		}
	}
	
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
	
	public void orderBy(List<Var> vars, List<Integer> dir){
		Node project = GraphUtil.listSubjects(graph, typeNode, projectNode).next();
		Node order = NodeFactory.createBlankNode("orderBy");
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
	
	public void slice(int offset, int limit){
		Node order = GraphUtil.listSubjects(graph, typeNode, projectNode).next();
		Node lNode = NodeFactory.createBlankNode("limit");
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
	
	public boolean isIsomorphicWith(ExpandedGraph e){
		return this.graph.isIsomorphicWith(e.graph);
	}
	
	public String toString(){
		return this.graph.toString().replace(";", "\n");
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
			System.out.println(e.next());
		}
	}
	
	public TreeSet<org.semanticweb.yars.nx.Node[]> getTriples(){
		this.update();
		JenaModelIterator jmi = new JenaModelIterator(this.asModel());
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
	
	public GraphLabellingResult label(Collection<org.semanticweb.yars.nx.Node[]> triples) throws InterruptedException, HashCollisionException{
		GraphLabellingArgs gla = new GraphLabellingArgs();
		gla.setDistinguishIsoPartitions(false);
		GraphLabelling gl = new GraphLabelling(triples,gla);	
		GraphLabellingResult glr = gl.call();
		return glr;
	}
	
	public void update(){
		// Delete empty unions.
		UpdateAction.execute(unionRule,this.graph);
		// Distribute union over conjunction Case: (A U B) * C
		UpdateAction.execute(distributionRule1, this.graph);
		// Distribute union over conjunction Case: (A U B) * (C U D)
		UpdateAction.execute(distributionRule2, this.graph);
		UpdateAction.execute(conjunctionRule,this.graph);
		UpdateAction.execute(disjunctionRule,this.graph);
		UpdateAction.execute(joinTripleRule,this.graph);
	}
	
	public void branchRelabelling(){
		UpdateAction.execute(joinLabelRule,this.graph);
	}
	
	public ExpandedGraph getLeanForm() throws InterruptedException{
		GraphLeaningResult glResult = this.DFSLeaning(getTriples());
		return new ExpandedGraph(glResult.getLeanData());
	}
	
	public ExpandedGraph getCanonicalForm(boolean verbose) throws InterruptedException, HashCollisionException{
		Graph before = GraphFactory.createPlainGraph();
		while(!before.isIsomorphicWith(graph)){
			before = GraphFactory.createPlainGraph();
			GraphUtil.addInto(before, graph);
			this.update();
		}
		boolean distinct = this.graph.contains(this.root, distinctNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean));
		if (leaning && distinct){
			try{
				branchRelabelling();
			}
			catch (Exception e){
				e.printStackTrace();
			}
			print();
			ExpandedGraph e = getConjunctiveQueries();
			GraphLeaningResult glResult = this.DFSLeaning(e.getTriples());
			if (verbose){
				System.out.println("DFS Leaning results: \n");
				System.out.println("Core map is: "+glResult.getCoreMap());
				System.out.println("Number of solutions: "+glResult.getSolutionCount());
				System.out.println("Depth of solution tree, I'm guessing: "+glResult.getDepth());
				System.out.println("Number of joins performed: "+glResult.getJoins());
			}
			ExpandedGraph ans = new ExpandedGraph(glResult.getLeanData());
			UpdateAction.execute(duplicatesRule,ans.graph);
			ans.update();
			GraphLabellingResult glr = this.label(ans.getTriples());
			
			if (verbose){
				System.out.println("Labelling results: \n");
				System.out.println("Number of blank nodes is: "+glr.getBnodeCount());
				System.out.println("Number of colouring iterations is: "+glr.getColourIterationCount());
				System.out.println("Number of partitions found is: "+glr.getPartitionCount());
			}
			ans = new ExpandedGraph(glr.getGraph());
			return ans;
		}
		else{
			GraphLabellingResult glr = this.label(this.getTriples());
			if (verbose){
				System.out.println("Labelling results: \n");
				System.out.println("Number of blank nodes is: "+glr.getBnodeCount());
				System.out.println("Number of colouring iterations is: "+glr.getColourIterationCount());
				System.out.println("Number of partitions found is: "+glr.getPartitionCount());
			}
			ExpandedGraph ans = new ExpandedGraph(glr.getGraph());
			return ans;
		}	
	}
	
	//
	
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
	
	public String joinToString(Node n){
		String ans = "";
		ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
		ans += "\t{ \n";
		while (args.hasNext()){
			Node arg = args.next();
			Node type = GraphUtil.listObjects(graph, arg, typeNode).next();
			ans += "\t";
			if (type.equals(unionNode)){
				ans += unionToString(arg);
			}
			else if (type.equals(joinNode)){
				ans += joinToString(arg);
			}
			else if (type.equals(optionalNode)){
				ans += optionalToString(arg);
			}
			else if (type.equals(tpNode)){
				ans += tripleToString(arg);
			}
			else if (type.equals(graphNode)){
				ans += graphToString(arg);
			}
			ans += "\n";
		}
		ans += "\t }";
		if (GraphUtil.listObjects(graph, n, modNode).hasNext()){
			Node f = GraphUtil.listObjects(graph, n, modNode).next();
			ans += "\n\tFILTER" + filterToString(GraphUtil.listObjects(graph, f, argNode).next()) + "\n";
		}
		return ans;
	}
	
	public String tripleToString(Node n){
		String ans = "\t";
		Node subjects = GraphUtil.listObjects(graph, n, subNode).next();
		Node predicates = GraphUtil.listObjects(graph, n, preNode).next();
		Node objects = GraphUtil.listObjects(graph, n, objNode).next();
		String sub = subjects.toString();
		String pre = predicates.toString();
		String obj = objects.toString();
		if (subjects.isBlank()){
			sub = "?"+subjects.getBlankNodeLabel();
		}
		else if (subjects.isURI()){
			sub = "<"+sub+">";
		}
		if (predicates.isBlank()){
			pre = "?"+predicates.getBlankNodeLabel();
		}
		else if (predicates.isURI()){
			pre = "<"+pre+">";
		}
		if (objects.isBlank()){
			obj = "?"+objects.getBlankNodeLabel();
		}
		else if (objects.isURI()){
			obj = "<"+obj+">";
		}
		ans = "\t" + sub +" "+ pre +" "+ obj + " . ";
		return ans;
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
	
	public String unionToString(Node n){
		String ans = "";
		ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
		ans += "\t{ \n";
		while (args.hasNext()){
			Node arg = args.next();
			Node type = GraphUtil.listObjects(graph, arg, typeNode).next();
			ans += "\t{";
			if (type.equals(unionNode)){
				ans += unionToString(arg);
			}
			else if (type.equals(joinNode)){
				ans += joinToString(arg);
			}
			else if (type.equals(optionalNode)){
				ans += optionalToString(arg);
			}
			else if (type.equals(tpNode)){
				ans += tripleToString(arg);
			}
			else if (type.equals(graphNode)){
				ans += graphToString(arg);
			}
			ans += "} \n";
			if (args.hasNext()){
				ans += "\t UNION\n";
			}
		}
		ans += "\t }";
		if (GraphUtil.listObjects(graph, n, modNode).hasNext()){
			Node f = GraphUtil.listObjects(graph, n, modNode).next();
			ans += "\n\tFILTER" + filterToString(GraphUtil.listObjects(graph, f, argNode).next()) + "\n";
		}
		return ans;
	}
	
	public String optionalToString(Node n){
		String ans = "\t";
		Node left = GraphUtil.listObjects(graph, n, leftNode).next();
		if (graph.contains(Triple.create(left, typeNode, unionNode))){
			ans += unionToString(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, joinNode))){
			ans += joinToString(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, tpNode))){
			ans += tripleToString(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, optionalNode))){
			ans += optionalToString(left);
		}
		else if (graph.contains(Triple.create(left, typeNode, graphNode))){
			ans += graphToString(left);
		}
		Node right = GraphUtil.listObjects(graph, n, rightNode).next();
		ans += "\tOPTIONAL { \n\t";
		if (graph.contains(Triple.create(right, typeNode, unionNode))){
			ans += unionToString(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, joinNode))){
			ans += joinToString(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, tpNode))){
			ans += tripleToString(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, optionalNode))){
			ans += optionalToString(right);
		}
		else if (graph.contains(Triple.create(right, typeNode, graphNode))){
			ans += graphToString(right);
		}
		ans += "\t\n";
		ans += "\t }";
		if (GraphUtil.listObjects(graph, n, modNode).hasNext()){
			Node f = GraphUtil.listObjects(graph, n, modNode).next();
			ans += "\n\tFILTER" + filterToString(GraphUtil.listObjects(graph, f, argNode).next()) + "\n";
		}
		return ans;
	}
	
	public String graphToString(Node n){
		String ans = "";
		Node val = GraphUtil.listObjects(graph, n, valueNode).next();
		String uri = "";
		if (val.isURI()){
			uri = val.getURI();
		}
		else{
			uri = "?"+val.getBlankNodeLabel();
		}
		ans += "\tGRAPH " + uri;
		Node next = GraphUtil.listObjects(graph, n, argNode).next();
		ans += "\t { \n";
		if (graph.contains(Triple.create(next, typeNode, unionNode))){
			ans += unionToString(next);
		}
		else if (graph.contains(Triple.create(next, typeNode, joinNode))){
			ans += joinToString(next);
		}
		else if (graph.contains(Triple.create(next, typeNode, tpNode))){
			ans += tripleToString(next);
		}
		else if (graph.contains(Triple.create(next, typeNode, optionalNode))){
			ans += optionalToString(next);
		}
		else if (graph.contains(Triple.create(next, typeNode, graphNode))){
			ans += graphToString(next);
		}
		ans += "\t\n }";
		if (GraphUtil.listObjects(graph, n, modNode).hasNext()){
			Node f = GraphUtil.listObjects(graph, n, modNode).next();
			ans += "\n\tFILTER" + filterToString(GraphUtil.listObjects(graph, f, argNode).next()) + "\n";
		}
		return ans;
	}
	
	public String filterToString(Node n){
		String ans = "";
		if (graph.contains(n, typeNode, andNode)){
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
			ans += "( ";
			while (args.hasNext()){
				ans += filterToString(args.next());
				if (args.hasNext()){
					ans += " && ";
				}
			}
			ans += " )";
		}
		else if (graph.contains(n, typeNode, orNode)){
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
			ans += "( ";
			while (args.hasNext()){
				ans += filterToString(args.next());
				if (args.hasNext()){
					ans += " || ";
				}
			}
			ans += " )";
		}
		else if (graph.contains(n, typeNode, notNode)){
			Node args = GraphUtil.listObjects(graph, n, argNode).next();
			ans += "(! " + filterToString(args) + ")";
		}
		else{
			if (GraphUtil.listObjects(graph, n, functionNode).hasNext()){
				Node function = GraphUtil.listObjects(graph, n, functionNode).next();
				String op = function.toString().replace("\"", "");
				ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
				List<Node> argList = args.toList();
				int nParams = argList.size();
				int i = 0;
				if (nParams == 1){
					ans += op + "(" + filterToString(argList.get(0)) + ")";
					return ans;
				}
				List<String> params = new ArrayList<String>();
				for (int k = 0; k < nParams; k++){
					params.add("");
				}
				ans += "( ";		
				for (Node arg: argList){
					if (GraphUtil.listObjects(graph, arg, valueNode).hasNext()){
						Node value = GraphUtil.listObjects(graph, arg, valueNode).next();
						String argString = "";
						if (value.isBlank()){
							argString = "?"+value.getBlankNodeLabel();
							argString = argString.replace("\"", "");
						}
						else if (value.isLiteral()){
							argString = "\""+value.getLiteralValue().toString()+"\"";
						}
						else{
							argString = "<"+value.getURI()+">";
							argString = argString.replace("\"", "");
						}
						
						op = op.replaceAll("\"", "");
						if (isOrderedFunction(op)){
							int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
							params.set(order, argString);
						}
						else{
							params.set(i, argString);
						}
					}
					if (GraphUtil.listObjects(graph, arg, functionNode).hasNext()){
						op = op.replaceAll("\"", "");
						if (isOrderedFunction(op)){
							int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
							params.set(order, filterToString(arg));
						}
						else{
							params.set(i, filterToString(arg));
						}
					}
					i++;
				}
				if (isOperator(op)){
					ans += params.get(0) + " " + op + " " + params.get(1) + ")";
				}
				else{
					ans += op + "(" + params.get(0) + ", " + params.get(1) + "))";
				}
			}
			if (GraphUtil.listObjects(graph, n, valueNode).hasNext()){
				Node v = GraphUtil.listObjects(graph, n, valueNode).next();
				if (v.isBlank()){
					return "?"+v.getBlankNodeLabel();
				}
				else if (v.isLiteral()){
					return "\""+getCleanLiteral(v).toString()+"\"";
				}
				else{
					return "<"+v.getURI()+">";
				}
			}
		}
		return ans;
	}
	
	public String getQuery(){
		Node project = GraphUtil.listSubjects(this.graph, typeNode, projectNode).next();
		ExtendedIterator<Node> projectVars = GraphUtil.listObjects(this.graph, project, argNode);
		String s = "SELECT ";
		if (distinct){
			s += "DISTINCT ";
		}
		if (projectVars.hasNext()){
			while (projectVars.hasNext()){
				s = s + "?" + projectVars.next().toString() + " ";
				if (projectVars.hasNext()){
					s = s + ", ";
				}
			}
		}
		else{
			s += "* ";
		}
		s += "\n";
		ExtendedIterator<Node> f = GraphUtil.listSubjects(graph, typeNode, fromNode);
		ExtendedIterator<Node> fn = GraphUtil.listSubjects(graph, typeNode, fromNamedNode);
		if (f.hasNext()){
			ExtendedIterator<Node> URIs = GraphUtil.listObjects(graph, f.next(), argNode);
			if (URIs.hasNext()){
				while (URIs.hasNext()){
					s += "FROM " + URIs.next().getURI() + "\n";
				}
			}
		}
		if (fn.hasNext()){
			ExtendedIterator<Node> URIs = GraphUtil.listObjects(graph, fn.next(), argNode);
			if (URIs.hasNext()){
				while (URIs.hasNext()){
					s += "FROM NAMED " + URIs.next().getURI() + "\n";
				}
			}
		}
		s = s + "\nWHERE {\n";
		ExtendedIterator<Node> m = GraphUtil.listObjects(graph, project, opNode);
		Node first = m.next();
		if (graph.contains(Triple.create(first, typeNode, fromNode))){
			first = m.next();
		}
		if (graph.contains(Triple.create(first, typeNode, unionNode))){
			s += unionToString(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, joinNode))){
			s += joinToString(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, optionalNode))){
			s += optionalToString(first);
		}
		else if (graph.contains(Triple.create(first, typeNode, tpNode))){
			s += tripleToString(first);
		}
		if (GraphUtil.listSubjects(graph, typeNode, filterNode).hasNext()){
			Node filter = GraphUtil.listSubjects(graph, typeNode, filterNode).next();
			Node clause = GraphUtil.listObjects(graph, filter, argNode).next();
			s = s + "\n\tFILTER" + filterToString(clause) + "\n";
		}
		s = s + "\n}";
		if (GraphUtil.listSubjects(graph, typeNode, orderByNode).hasNext()){
			Node orderBy = GraphUtil.listSubjects(graph, typeNode, orderByNode).next();
			s = s + "\nORDER BY ";
			List<Node> args = GraphUtil.listObjects(graph, orderBy, argNode).toList();
			List<String> params = new ArrayList<String>();
			for (int i = 0; i < args.size(); i++){
				params.add("");
			}
			for (Node a : args){
				int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, a, orderNode).next()));
				String varName = "?"+GraphUtil.listObjects(graph, a, varNode).next().toString();
				int dir = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, a, dirNode).next()));
				if (dir < 0){
					varName = "DESC("+varName+")";
				}
				params.set(order, varName);
			}
			for (int i = 0; i < params.size(); i++){
				String p = params.get(i);
				if (i == params.size() - 1){
					s = s + p;
				}
				else{
					s = s + p + ", ";
				}
			}
		}
		if (GraphUtil.listSubjects(graph, typeNode, limitNode).hasNext()){
			Node limit = GraphUtil.listSubjects(graph, typeNode, limitNode).next();
			int start = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, limit, offsetNode).next()));
			int finish = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, limit, valueNode).next()));
			if (start > 0){
				s = s + "\nOFFSET "+start;
			}
			s = s + "\nLIMIT "+finish;
		}
		return s;
	}
	
	public ExpandedGraph getConjunctiveQueries() throws InterruptedException, HashCollisionException{
		ArrayList<ExpandedGraph> result = new ArrayList<ExpandedGraph>();
		ArrayList<ExpandedGraph> redundant = new ArrayList<ExpandedGraph>();
		UpdateAction.execute(redundancyRule,this.graph);
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		Node union = GraphUtil.listObjects(this.graph, this.root, opNode).next();
		if (graph.contains(union, typeNode, unionNode)){ //Make sure it's a union of conjunctive queries.
			Graph inner = ge.extract(union, graph);
			Graph outer = GraphFactory.createPlainGraph();
			Graph filterGraph = GraphFactory.createPlainGraph();
			ExtendedIterator<Node> filters = GraphUtil.listObjects(inner, union, modNode);
			ExtendedIterator<Node> cQueries = GraphUtil.listObjects(inner, union, argNode);
			if (GraphUtil.listObjects(inner, union, argNode).toList().size() > 1){
				if (filters.hasNext()){
					filterGraph = ge.extract(filters.next(), inner);
				}
				GraphUtil.addInto(outer, graph);
				GraphUtil.deleteFrom(outer, inner);
				outer.remove(root, opNode, union);
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
				while (orderBy.hasNext()){
					Node o = orderBy.next();
					ExtendedIterator<Node> orderVars = GraphUtil.listObjects(graph, o, argNode);
					while (orderVars.hasNext()){
						Node v = orderVars.next();
						Node var = GraphUtil.listObjects(graph, v, varNode).next();
						outer.add(Triple.create(var, valueNode, NodeFactory.createLiteral(v.getBlankNodeLabel())));
						outer.add(Triple.create(var, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
						outer.add(Triple.create(var, typeNode, varNode));
						inner.add(Triple.create(var, valueNode, NodeFactory.createLiteral(v.getBlankNodeLabel())));
						inner.add(Triple.create(var, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
					}
				}
				UpdateAction.execute(filterVarsRule,filterGraph);	
				List<Node> filterVars = GraphUtil.listSubjects(filterGraph, typeNode, varNode).toList();
				for (Node f : filterVars){
					filterGraph.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
					filterGraph.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
				}
				while (cQueries.hasNext()){
					Node cRoot = cQueries.next();
					Graph cGraph = ge.extract(cRoot, inner);
					for (Node f : filterVars){
						cGraph.add(Triple.create(f, typeNode, varNode));
						cGraph.add(Triple.create(f, valueNode, NodeFactory.createLiteral(f.getBlankNodeLabel())));
						cGraph.add(Triple.create(f, tempNode, NodeFactory.createLiteralByValue(true, XSDDatatype.XSDboolean)));
					}
					ExpandedGraph e = new ExpandedGraph(cRoot,cGraph,this.vars);
					e.graph.add(Triple.create(union, argNode, e.root));
					e.root = union;
					e.graph.add(Triple.create(union, typeNode, unionNode));
					e.project(vars);
					UpdateAction.execute(tripleRelabelRule,e.graph);
					UpdateAction.execute(branchRelabelRule,e.graph);
					UpdateAction.execute(branchCleanUpRule,e.graph);
					ExpandedGraph a = e.getLeanForm();
					result.add(a);
				}
				for (int i = 0; i < result.size(); i++){
					for (int j = i+1; j < result.size(); j++){
						ExpandedGraph e = result.get(i);
						ExpandedGraph e1 = result.get(j);
						String askQuery = "ASK\nWHERE{\n";
						String askQuery1 = "ASK\nWHERE{\n";
						ExtendedIterator<Triple> t0 = GraphUtil.findAll(e1.graph);
						ExtendedIterator<Triple> t1 = GraphUtil.findAll(e.graph);
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
						QueryExecution qexec = QueryExecutionFactory.create(q, e.asModel());
						QueryExecution qexec1 = QueryExecutionFactory.create(q1, e1.asModel());
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
				for (ExpandedGraph e : redundant){
					result.remove(e);
				}
				ExpandedGraph eg = new ExpandedGraph(root, outer, vars);
				Node uNode = NodeFactory.createBlankNode();
				if (result.size() == 1){
					for (ExpandedGraph e : result){
						Node eRoot = GraphUtil.listSubjects(e.graph, typeNode, unionNode).next();
						eRoot = GraphUtil.listObjects(e.graph, eRoot, argNode).next();
						GraphUtil.addInto(eg.graph, ge.extract(eRoot, e.graph));				
						eg.graph.add(Triple.create(eg.root, opNode, eRoot));
						GraphUtil.addInto(eg.graph, filterGraph);
						UpdateAction.execute(joinTripleRule, eg.graph);
					}
				}
				else{
					eg.graph.add(Triple.create(eg.root, opNode, uNode));
					eg.graph.add(Triple.create(uNode, typeNode, unionNode));
					for (ExpandedGraph e : result){
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
						}
						else{
							Node eRoot = GraphUtil.listSubjects(e.graph, typeNode, unionNode).next();
							eRoot = GraphUtil.listObjects(e.graph, eRoot, argNode).next();
							GraphUtil.addInto(eg.graph, ge.extract(eRoot, e.graph));				
							eg.graph.add(Triple.create(uNode, argNode, eRoot));
							GraphUtil.addInto(eg.graph, filterGraph);
						}
					}
				}
				eg.setDistinctNode(true);
				UpdateAction.execute(branchUnionRule,eg.graph);
				UpdateAction.execute(branchCleanUpRule2,eg.graph);
				UpdateAction.execute(joinRule, eg.graph);
				return eg;
			}
		}	
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
	
	public Model getCQ(ExpandedGraph e){
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
	
	public void printQuery(){	
		System.out.println(this.getQuery());
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

}
