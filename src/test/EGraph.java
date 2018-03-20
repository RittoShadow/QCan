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
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
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

public class EGraph {
	
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
	
	public EGraph(List<Triple> triples, List<Var> vars, int id){
		if (vars != null){
			this.vars.addAll(vars);
		}
		int j = 0;
		this.id = id;
		nTriples = triples.size();
		this.root = NodeFactory.createBlankNode("join"+this.id);
		//Adding typing now.
		graph.add(Triple.create(this.root, typeNode, joinNode));
		for (Triple t : triples){
			createTriple(t, j++);
		}	
	}
	
	public EGraph(List<Triple> triples, List<Var> vars){
		this(triples,vars,0);
	}
	
	public EGraph(List<Triple> triples, int id){
		this(triples, null, id);
	}
	
	public EGraph(Collection<org.semanticweb.yars.nx.Node[]> data){
		for (org.semanticweb.yars.nx.Node[] node : data){
			Node subject = null, predicate = null, object = null;
			if (Pattern.matches("_:.+", node[0].toN3())){
				subject = NodeFactory.createBlankNode(node[0].toN3().substring(2));
			}
			else if (Pattern.matches("<http://.+>", node[0].toN3())){
				subject = NodeFactory.createURI(node[0].toN3().replaceAll("<|>", ""));
			}
			else{
				subject = createLiteralWithType(node[0].toN3());
			}
			if (Pattern.matches("<http://.+>", node[1].toN3())){
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
			else if (Pattern.matches("<http://.+>", node[2].toN3())){
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
	
	public void addVars(EGraph e){
		this.vars.addAll(e.vars);
	}
	
	public Node createTriple(Triple t, int j){
		Triple s, p, o;
		String nv = "tp" + this.id + j++;
		Node n = NodeFactory.createBlankNode(nv);
		graph.add(Triple.create(root, argNode, n));
		graph.add(Triple.create(n, typeNode, tpNode));
		if (t.getSubject().isVariable()){
			Node temp = NodeFactory.createBlankNode(t.getSubject().getName());
			s = Triple.create(n, subNode, temp);
			graph.add(Triple.create(temp, typeNode, varNode));
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
			graph.add(Triple.create(temp, typeNode, varNode));
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
			graph.add(Triple.create(temp, typeNode, varNode));
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
		return n;
	}
	
	public Node join(EGraph arg1, int Id){
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
			graph.add(Triple.create(this.root, leftNode, root));
			graph.add(Triple.create(root, argNode, left1));
			graph.add(Triple.create(root, argNode, arg1.root));
			root = this.root;
		}
		else if (b){
			System.out.println("Left arg is: "+this.toString());
			System.out.println("Right arg is: "+arg1.toString());
			Node left2 = GraphUtil.listObjects(arg1.graph, arg1.root, leftNode).next();
			arg1.graph.delete(Triple.create(arg1.root, leftNode, left2));
			arg1.graph.add(Triple.create(arg1.root, leftNode, root));
			arg1.graph.add(Triple.create(root, argNode, left2));
			arg1.graph.add(Triple.create(root, argNode, this.root));
			root = arg1.root;
		}
		GraphUtil.addInto(this.graph, arg1.graph);
		addVars(arg1);
		this.root = root;
		return root;
	}
	
	public Node union(EGraph arg1, int Id){
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
		return root;
	}
	
	public void optional(EGraph arg1, int Id){
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
	
	public void filter(Node n){
		Node filter = NodeFactory.createBlankNode("filter");
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
		else{
			Node aux = NodeFactory.createLiteral(arg1);
			graph.add(Triple.create(a, valueNode, aux));
		}
		graph.add(Triple.create(n, functionNode, o));
		graph.add(Triple.create(n, argNode, a));
		graph.add(Triple.create(n, valueNode, NodeFactory.createLiteral(arg1.toString())));
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
	
	public void project(List<Var> vars){
		Node root = NodeFactory.createBlankNode("project");
		graph.add(Triple.create(root, typeNode, projectNode));
		graph.add(Triple.create(root, opNode, this.root));
		if (graph.contains(NodeFactory.createBlankNode("orderBy"), typeNode, orderByNode)){
			graph.add(Triple.create(root, modNode, NodeFactory.createBlankNode("orderBy")));
		}
		for (Var v : vars){
			graph.add(Triple.create(root, argNode, NodeFactory.createBlankNode(v.getName())));		
		}
		this.root = root;
	}
	
	public void orderBy(List<Var> vars, List<Integer> dir){
		Node order = NodeFactory.createBlankNode("orderBy");
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
	
	
	
	public Op asAlgebra(){
		Op op;
		BasicPattern bp = new BasicPattern();
		ExtendedIterator<Triple> triples = GraphUtil.findAll(this.graph); // O(n)
//		ExtendedIterator<Node> unions = GraphUtil.listObjects(this.graph, nodeType, unionNode);
//		ExtendedIterator<Node> joins = GraphUtil.listObjects(this.graph, nodeType, joinNode);
		List<Node> subjectList = new ArrayList<Node>();
		while (triples.hasNext()){
			Node a = triples.next().getMatchSubject();
			if (!subjectList.contains(a)){
				subjectList.add(a);
			}
		}
		for (Node n : subjectList){
			Node subjects = GraphUtil.listObjects(graph, n, subNode).next();
			Node predicates = GraphUtil.listObjects(graph, n, preNode).next();
			Node objects = GraphUtil.listObjects(graph, n, objNode).next();
			bp.add(Triple.create(subjects, predicates, objects));
		}
		op = new OpBGP(bp);
//		Transformer.transform(new TransformBase(){
//			public Op transform(OpProject op){
//				List<Var> v = op.getVars();
//				Op op1 = op.getSubOp();
//				if (OpBGP.isBGP(op1)){
//				}
//				return op;		
//			}
//		}, op);
//		OpWalker.walk(op, 
//				new OpVisitorBase(){
//			
//					public void visit(OpProject opp){
//					}
//					
//					public void visit(OpBGP op){
//						System.out.println(op.getPattern());
//					}
//				}
//			);
		return op;
	}
	
	public Model asModel(){
		return ModelFactory.createModelForGraph(graph);
	}
	
	public boolean isIsomorphicWith(EGraph e){
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
		this.print();
		UpdateRequest request = UpdateFactory.read("rules/join.ru", Syntax.syntaxSPARQL_11);
		UpdateAction.execute(request,this.graph);
		request = UpdateFactory.read("rules/union.ru", Syntax.syntaxSPARQL_11);
		UpdateAction.execute(request,this.graph);
		request = UpdateFactory.read("rules/distribution.ru", Syntax.syntaxSPARQL_11);
		UpdateAction.execute(request, this.graph);
		request = UpdateFactory.read("rules/distribution2.ru", Syntax.syntaxSPARQL_11);
		UpdateAction.execute(request, this.graph);
		request = UpdateFactory.read("rules/join.ru", Syntax.syntaxSPARQL_11);
		UpdateAction.execute(request,this.graph);
		request = UpdateFactory.read("rules/conjunction.ru", Syntax.syntaxSPARQL_11);
		UpdateAction.execute(request,this.graph);
		request = UpdateFactory.read("rules/disjunction.ru", Syntax.syntaxSPARQL_11);
		UpdateAction.execute(request,this.graph);
		request = UpdateFactory.read("rules/duplicates.ru", Syntax.syntaxSPARQL_11);
		UpdateAction.execute(request,this.graph);
	}
	
	public EGraph getCanonicalForm(boolean verbose) throws InterruptedException, HashCollisionException{
		this.update();
		if (leaning && distinct){
		GraphLeaningResult glResult = this.DFSLeaning(this.getTriples());
			if (verbose){
				System.out.println("DFS Leaning results: \n");
				System.out.println("Core map is: "+glResult.getCoreMap());
				System.out.println("Number of solutions: "+glResult.getSolutionCount());
				System.out.println("Depth of solution tree, I'm guessing: "+glResult.getDepth());
				System.out.println("Number of joins performed: "+glResult.getJoins());
			}
			GraphLabellingResult glr = this.label(glResult.getLeanData());
			if (verbose){
				System.out.println("Labelling results: \n");
				System.out.println("Number of blank nodes is: "+glr.getBnodeCount());
				System.out.println("Number of colouring iterations is: "+glr.getColourIterationCount());
				System.out.println("Number of partitions found is: "+glr.getPartitionCount());
			}
			return new EGraph(glr.getGraph());
		}
		else{
			GraphLabellingResult glr = this.label(this.getTriples());
			if (verbose){
				System.out.println("Labelling results: \n");
				System.out.println("Number of blank nodes is: "+glr.getBnodeCount());
				System.out.println("Number of colouring iterations is: "+glr.getColourIterationCount());
				System.out.println("Number of partitions found is: "+glr.getPartitionCount());
			}
			return new EGraph(glr.getGraph());
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
			Node function = GraphUtil.listObjects(graph, n, functionNode).next();
			String op = function.toString();
			ExtendedIterator<Node> args = GraphUtil.listObjects(graph, n, argNode);
			List<Node> argList = args.toList();
			int nParams = argList.size();
			int i = 0;
			if (nParams == 1){
				ans += op + "(" + argList.get(0).toString() + ")";
				return ans;
			}
			List<String> params = new ArrayList<String>();
			for (int k = 0; k < nParams; k++){
				params.add("");
			}
			ans += "( ";		
			for (Node arg: argList){
				Node value = GraphUtil.listObjects(graph, arg, valueNode).next();
				String argString = value.isBlank() ? "?"+value.getBlankNodeLabel() : value.getLiteralValue().toString();
				argString = argString.replace("\"", "");
				op = op.replaceAll("\"", "");
				if (isOrderedFunction(op)){
					int order = new Integer(getCleanLiteral(GraphUtil.listObjects(graph, arg, orderNode).next()));
					params.set(order, argString);
				}
				else{
					params.set(i, argString);
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
		s = s + "\nWHERE {\n";
		ExtendedIterator<Node> unions = GraphUtil.listSubjects(this.graph, typeNode, unionNode);
		if (unions.hasNext()){
			while (unions.hasNext()){
				Node u = unions.next();
				ExtendedIterator<Node> joins = GraphUtil.listObjects(this.graph, u, argNode);
				while (joins.hasNext()){
					Node j = joins.next();
					s = s + "\n\t{";
					if (this.graph.contains(j, typeNode, joinNode)){
						ExtendedIterator<Node> tp = GraphUtil.listObjects(this.graph, j, argNode);
						while (tp.hasNext()){
							Node n = tp.next();
							if (this.graph.contains(n, typeNode, tpNode)){
								Node subjects = GraphUtil.listObjects(graph, n, subNode).next();
								Node predicates = GraphUtil.listObjects(graph, n, preNode).next();
								Node objects = GraphUtil.listObjects(graph, n, objNode).next();
								String sub = subjects.toString();
								String pre = predicates.toString();
								String obj = objects.toString();
								if (subjects.isBlank()){
									sub = "?"+sub;
								}
								else if (subjects.isURI()){
									sub = "<"+sub+">";
								}
								if (predicates.isBlank()){
									pre = "?"+pre;
								}
								else if (predicates.isURI()){
									pre = "<"+pre+">";
								}
								if (objects.isBlank()){
									obj = "?"+obj;
								}
								else if (objects.isURI()){
									obj = "<"+obj+">";
								}
								s = s +"\n\t\t "+ sub +" "+ pre +" "+ obj + " .\n";
							}
						}
					}
					if (joins.hasNext()){
						s = s + "\t}\n\tUNION";
					}
					else{
						s = s + "\t}\n";
					}
				}
			}
		}
		else{
			ExtendedIterator<Node> joins = GraphUtil.listSubjects(this.graph, typeNode, joinNode);
			while (joins.hasNext()){
				Node j = joins.next();
				s = s + "\n\t{";
				if (this.graph.contains(j, typeNode, joinNode)){
					ExtendedIterator<Node> tp = GraphUtil.listObjects(this.graph, j, argNode);
					while (tp.hasNext()){
						Node n = tp.next();
						if (this.graph.contains(n, typeNode, tpNode)){
							Node subjects = GraphUtil.listObjects(graph, n, subNode).next();
							Node predicates = GraphUtil.listObjects(graph, n, preNode).next();
							Node objects = GraphUtil.listObjects(graph, n, objNode).next();
							String sub = subjects.toString();
							String pre = predicates.toString();
							String obj = objects.toString();
							if (subjects.isBlank()){
								sub = "?"+sub;
							}
							else if (subjects.isURI()){
								sub = "<"+sub+">";
							}
							if (predicates.isBlank()){
								pre = "?"+pre;
							}
							else if (predicates.isURI()){
								pre = "<"+pre+">";
							}
							if (objects.isBlank()){
								obj = "?"+obj;
							}
							else if (objects.isURI()){
								obj = "<"+obj+">";
							}
							s = s +"\n\t\t "+ sub +" "+ pre +" "+ obj + " .\n";
						}
					}
				}
				s = s + "\n\t}";
			}
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
	
	public void printQuery(){	
		System.out.println(this.getQuery());
	}

}
