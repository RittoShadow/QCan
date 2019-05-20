package main;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.optimize.TransformExtendCombine;
import org.apache.jena.sparql.algebra.optimize.TransformMergeBGPs;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;

/**
 * A class that takes a query as an input, performs a canonicalisation and measures how long it takes.
 * @author Jaime
 *
 */
public class SingleQuery {
	
	private long time = 0;
	private long canonTime = 0;
	private int nTriples = 0;
	private RGraph graph;
	private RGraph canonGraph;
	private boolean enableFilter = true;
	private boolean enableOptional = true;
	private boolean enableLeaning = true;
	private boolean containsUnion = false;
	private boolean containsJoin = false;
	private boolean containsOptional = false;
	private boolean containsFilter = false;
	private boolean containsSolutionMods = false;
	private boolean containsNamedGraphs = false;
	
	
	public SingleQuery(String q, boolean enableFilter, boolean enableOptional) throws InterruptedException, HashCollisionException{
		this(q,enableFilter,enableOptional,true,true);
	}
	
	public SingleQuery(String q, boolean enableFilter, boolean enableOptional, boolean canon, boolean leaning) throws InterruptedException, HashCollisionException{
		this(q,enableFilter,enableOptional,canon,leaning,false);
	}
	
	public SingleQuery(String q, boolean enableFilter, boolean enableOptional, boolean canon, boolean leaning, boolean verbose) throws InterruptedException, HashCollisionException{
		setEnableFilter(enableFilter);
		setEnableOptional(enableOptional);
		setLeaning(leaning);
		long t = System.nanoTime();
		parseQuery(q);
		time = System.nanoTime() - t;
		if (canon){
			t = System.nanoTime();
			canonicalise(verbose);
			canonTime = System.nanoTime() - t;
		}
		else{
			canonGraph = this.graph;
			canonTime = time;
		}
	}
	
	public Op UCQTransformation(Op op){
		Op op2 = Transformer.transform(new UCQVisitor(), op);
		while (!op.equals(op2)){
			op = op2;
			op2 = Transformer.transform(new UCQVisitor(), op2);
		}
		op2 = Transformer.transform(new FilterTransform(), op2);
		op2 = Transformer.transform(new TransformMergeBGPs(), op2);
		op2 = Transformer.transform(new TransformExtendCombine(), op2);
		op2 = Transformer.transform(new BGPSort(), op2);
		return op2;
	}
	
	public boolean checkBranchVars(Op op){
		BGPSort bgps = new BGPSort();
		@SuppressWarnings("unused")
		Op op2 = Transformer.transform(bgps, op);
		for (int i = 0; i < bgps.ucqVars.size(); i++){
			for (int j = i + 1; j < bgps.ucqVars.size(); j++){
				if (bgps.ucqVars.get(i).equals(bgps.ucqVars.get(j))){
					return true;
				}
			}
		}
		return false;
	}
	
	public void parseQuery(String q) throws UnsupportedOperationException, QueryParseException{
		Query query = QueryFactory.create(q);
		Op op = Algebra.compile(query);
		Op op2 = UCQTransformation(op);
		RGraphBuilder visitor = new RGraphBuilder(query);
		visitor.setEnableFilter(enableFilter);
		visitor.setEnableOptional(enableOptional);
		System.out.println(op2);
		OpWalker.walk(op2, visitor);
		graph = visitor.getResult();
		graph.print();
		if (!visitor.isDistinct){
			if (visitor.totalVars.containsAll(visitor.projectionVars) && visitor.projectionVars.containsAll(visitor.totalVars)){
				if (!checkBranchVars(op2)){
					graph.setDistinctNode(true);
				}			
				else{
					graph.setDistinctNode(visitor.isDistinct);
				}
			}
			else{
				graph.setDistinctNode(visitor.isDistinct);
			}
		}
		else{
			graph.setDistinctNode(true);
		}
		nTriples = visitor.nTriples;
		containsUnion = visitor.getContainsUnion();
		containsJoin = visitor.getContainsJoin();
		containsOptional = visitor.getContainsOptional();
		containsFilter = visitor.getContainsFilter();
		containsNamedGraphs = visitor.getContainsNamedGraphs();
		containsSolutionMods = visitor.getContainsSolutionMods();
	}
	
	public void canonicalise() throws InterruptedException, HashCollisionException{
		this.graph.setLeaning(enableLeaning);
		canonGraph = this.graph.getCanonicalForm(false);
	}
	
	public void canonicalise(boolean verbose) throws InterruptedException, HashCollisionException{
		this.graph.setLeaning(enableLeaning);
		canonGraph = this.graph.getCanonicalForm(verbose);
	}
	
	public boolean determineSemantics(){
		
		return true;
	}
	
	public void setLeaning(boolean b){
		this.enableLeaning = b;
	}
	
	public long getGraphCreationTime(){
		return this.time;
	}
	
	public long getCanonicalisationTime(){
		return this.canonTime;
	}
	
	public int getInitialTriples(){
		return this.nTriples;
	}
	
	public int triplePatternsIn(){
		return graph.getNumberOfTriples();
	}
	
	public int triplePatternsOut(){
		return canonGraph.getNumberOfTriples();
	}
	
	public int graphSizeIn(){
		return graph.getNumberOfNodes();
	}
	
	public int graphSizeOut(){
		return canonGraph.getNumberOfNodes();
	}
	
	public int getVarsIn(){
		return graph.getNumberOfVars();
	}
	
	public int getVarsOut(){
		return canonGraph.getNumberOfVars();
	}
	
	public String getQuery(){
		QueryBuilder qb = new QueryBuilder(this.canonGraph);
		return qb.getQuery();
	}
	
	public boolean isDistinct(){
		return graph.isDistinct();
	}
	
	public boolean hasUnion(){
		return canonGraph.containsUnion();
	}
	
	public boolean hasJoin(){
		return canonGraph.containsJoin();
	}
	
	public RGraph getOriginalGraph(){
		return this.graph;
	}
	
	public RGraph getCanonicalGraph(){
		return this.canonGraph;
	}
	
	public void setEnableFilter(boolean b){
		this.enableFilter = b;
	}
	
	public void setEnableOptional(boolean b){
		this.enableOptional = b;
	}
	
	public boolean getContainsUnion(){
		return this.containsUnion;
	}
	
	public boolean getContainsJoin(){
		return this.containsJoin;
	}
	
	public boolean getContainsOptional(){
		return this.containsOptional;
	}
	
	public boolean getContainsFilter(){
		return this.containsFilter;
	}
	
	public boolean getContainsSolutionMods(){
		return this.containsSolutionMods;
	}
	
	public boolean getContainsNamedGraphs(){
		return this.containsNamedGraphs;
	}
	
	public static void main(String[] args) throws InterruptedException, HashCollisionException{
		String q = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?x ?name WHERE { ?x foaf:mbox <mailto:alice@example> .    ?x (foaf:knows|foaf:name)+ ?name .}";
		@SuppressWarnings("unused")
		SingleQuery sq = new SingleQuery(q, true, true, true, true, true);
		sq.getCanonicalGraph().print();
		System.out.println(sq.getQuery());
	}
}
