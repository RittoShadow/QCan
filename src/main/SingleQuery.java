package main;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.optimize.TransformExtendCombine;
import org.apache.jena.sparql.algebra.optimize.TransformMergeBGPs;
import org.apache.jena.sparql.algebra.optimize.TransformPathFlatternStd;
import org.apache.jena.sparql.algebra.optimize.TransformSimplify;

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
	private boolean enableLeaning = true;
	private boolean containsUnion = false;
	private boolean containsJoin = false;
	private boolean containsOptional = false;
	private boolean containsFilter = false;
	private boolean containsSolutionMods = false;
	private boolean containsNamedGraphs = false;
	
	
	public SingleQuery(String q) throws InterruptedException, HashCollisionException{
		this(q,true,true);
	}
	
	public SingleQuery(String q, boolean canon, boolean leaning) throws InterruptedException, HashCollisionException{
		this(q,canon,leaning,false);
	}
	
	public SingleQuery(String q, boolean canon, boolean leaning, boolean verbose) throws InterruptedException, HashCollisionException{
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
	
	public SingleQuery(Op op) throws InterruptedException, HashCollisionException {
		String q = OpAsQuery.asQuery(op).toString();
		parseQuery(q);
		canonicalise();
	}
	
	public static Op UCQTransformation(Op op){
		Op op2 = Transformer.transform(new TransformPathFlatternStd(), op);
		op2 = Transformer.transform(new TransformSimplify(), op2);
		op2 = Transformer.transform(new UCQVisitor(), op2);
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
		RGraphBuilder rgb = new RGraphBuilder(query);
		graph = rgb.getResult();
		if (!rgb.isDistinct){
			if (rgb.totalVars.containsAll(rgb.projectionVars) && rgb.projectionVars.containsAll(rgb.totalVars)){
				if (!checkBranchVars(op)){
					graph.setDistinctNode(true);
				}			
				else{
					graph.setDistinctNode(rgb.isDistinct);
				}
			}
			else{
				graph.setDistinctNode(rgb.isDistinct);
			}
		}
		else{
			graph.setDistinctNode(true);
		}
		nTriples = rgb.nTriples;
		containsUnion = rgb.getContainsUnion();
		containsJoin = rgb.getContainsJoin();
		containsOptional = rgb.getContainsOptional();
		containsFilter = rgb.getContainsFilter();
		containsNamedGraphs = rgb.getContainsNamedGraphs();
		containsSolutionMods = rgb.getContainsSolutionMods();
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
		String q = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT DISTINCT ?x WHERE {	{		?x foaf:knows ?y .		?a foaf:knows* ?b .		?n foaf:knows* ?m .		?c (foaf:knows|foaf:knows)* ?d .		?y foaf:knows* ?e . 	}}";
		@SuppressWarnings("unused")
		SingleQuery sq = new SingleQuery(q,true,true,true);
		sq.getCanonicalGraph().print();
		System.out.println(sq.getQuery());
	}
}
