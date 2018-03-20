package test;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;

public class SingleQuery {
	
	private long time = 0;
	private long canonTime = 0;
	private int nTriples = 0;
	private ExpandedGraph graph;
	private ExpandedGraph canonGraph;
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
		setEnableFilter(enableFilter);
		setEnableOptional(enableOptional);
		setLeaning(leaning);
		long t = System.nanoTime();
		parseQuery(q);
		time = System.nanoTime() - t;
		if (canon){
			t = System.nanoTime();
			canonicalise();
			canonTime = System.nanoTime() - t;
		}
		else{
			canonGraph = this.graph;
			canonTime = time;
		}
	}
	
	public void parseQuery(String q) throws UnsupportedOperationException, QueryParseException{
		Query query = QueryFactory.create(q);
		Op op = Algebra.compile(query);
		CustomOpVisitor visitor = new CustomOpVisitor(query);
		visitor.setEnableFilter(enableFilter);
		visitor.setEnableOptional(enableOptional);
		OpWalker.walk(op, visitor);
		graph = visitor.getResult();
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
		return this.canonGraph.getQuery();
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
	
	public ExpandedGraph getOriginalGraph(){
		return this.graph;
	}
	
	public ExpandedGraph getCanonicalGraph(){
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
}
