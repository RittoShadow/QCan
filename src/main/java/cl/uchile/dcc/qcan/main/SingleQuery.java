package cl.uchile.dcc.qcan.main;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.qcan.builder.QueryBuilder;
import cl.uchile.dcc.qcan.builder.RGraphBuilder;
import cl.uchile.dcc.qcan.data.FeatureCounter;
import cl.uchile.dcc.qcan.tools.OpUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.core.Var;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class that takes a query as an input, performs a canonicalisation and measures how long it takes.
 * @author Jaime
 *
 */
public class SingleQuery {
	
	private long graphTime = 0;
	private long labelTime = 0;
	private long rewriteTime = 0;
	private long minimisationTime = 0;
	private int nTriples = 0;
	private RGraph graph;
	private RGraph canonGraph;
	private Set<Var> vars = new HashSet<>();
	private Set<Var> canonVars = new HashSet<>();
	private boolean minimisation = true;
	private boolean containsUnion = false;
	private boolean containsJoin = false;
	private boolean containsOptional = false;
	private boolean containsFilter = false;
	private boolean containsSolutionMods = false;
	private boolean containsNamedGraphs = false;
	private boolean pathNormalisation = false;
	private boolean containsPaths = false;
	private boolean containsMinus = false;
	private boolean containsBind = false;
	private boolean containsGroupBy = false;
	private boolean containsTable = false;
	private Op op = null;
	private Op canonOp = null;
	private Map<String, String> varMap = new HashMap<>();

	public SingleQuery(String q) throws InterruptedException, HashCollisionException{
		this(q,true,true);
	}
	
	public SingleQuery(String q, boolean canon, boolean leaning) throws InterruptedException, HashCollisionException{
		this(q,canon,leaning,false);
	}
	
	public SingleQuery(String q, boolean canon, boolean leaning, boolean verbose) throws InterruptedException, HashCollisionException{
		this(q,canon,leaning,verbose,false);
	}
	
	public SingleQuery(String q, boolean canon, boolean leaning, boolean verbose, boolean pathNormalisation) throws InterruptedException, HashCollisionException{
		setLeaning(leaning);
		this.pathNormalisation  = pathNormalisation;
		long t = System.nanoTime();
		parseQuery(q,true);
		graphTime = System.nanoTime() - t;
		setFeatures();
		if (canon || leaning){
			canonicalise(verbose);
		}
		else{
			canonGraph = this.graph;
			minimisationTime = graphTime;
		}
	}

	public SingleQuery(String q, boolean canon, boolean rewrite, boolean minimise, boolean pathNormalisation, boolean verbose) throws HashCollisionException, InterruptedException {
		setLeaning(minimise);
		this.pathNormalisation = pathNormalisation;
		long t = System.nanoTime();
		parseQuery(q,rewrite);
		graphTime = System.nanoTime() - t;
		setFeatures();
		this.graph.canonical = canon;
		if (canon || minimise){
			canonicalise(verbose);
		}
		else{
			canonGraph = this.graph;
			minimisationTime = graphTime;
		}

	}
	
	public SingleQuery(Op op) throws InterruptedException, HashCollisionException {
		this.op = op;
		parseQuery(op);
		canonicalise();
		buildQuery();
	}
	
	public void parseQuery(String q, boolean rewrite) throws UnsupportedOperationException, QueryParseException{
		Query query = QueryFactory.create(q);
		this.op = Algebra.compile(query);
		RGraphBuilder rgb = new RGraphBuilder(query,rewrite,pathNormalisation);
		graph = rgb.getResult();
		graphTime = rgb.graphTime;
		rewriteTime = rgb.rewriteTime;
		vars = OpUtils.varsContainedIn(op);
	}

	public void parseQuery(Op op) {
		this.op = op;
		Query query = OpAsQuery.asQuery(op);
		RGraphBuilder rgb = new RGraphBuilder(query);
		graph = rgb.getResult();
		vars = OpUtils.varsContainedIn(op);
	}

	public void setFeatures() {
		FeatureCounter fc = new FeatureCounter(op);
		OpWalker.walk(op, fc);
		nTriples = graph.getNumberOfTriples();
		containsUnion = fc.getContainsUnion();
		containsJoin = fc.getContainsJoin();
		containsOptional = fc.getContainsOptional();
		containsFilter = fc.getContainsFilter();
		containsNamedGraphs = fc.getContainsNamedGraphs();
		containsSolutionMods = fc.getContainsSolutionMods();
		setContainsPaths(fc.getContainsPaths());
		setContainsMinus(fc.getFeatures().contains("minus"));
		setContainsBind(fc.getFeatures().contains("extend"));
		setContainsGroupBy(fc.getFeatures().contains("group"));
		setContainsTable(fc.getFeatures().contains("table"));
	}
	
	public void canonicalise() throws InterruptedException, HashCollisionException{
		canonicalise(false);
	}
	
	public void canonicalise(boolean verbose) throws InterruptedException, HashCollisionException{
		this.graph.setLeaning(minimisation);
		canonGraph = this.graph.getCanonicalForm(verbose);
		this.labelTime = canonGraph.getLabelTime();
		this.minimisationTime = canonGraph.getMinimisationTime();
	}
	
	public void setLeaning(boolean b){
		this.minimisation = b;
	}
	
	public long getGraphCreationTime(){
		return this.graphTime;
	}
	
	public long getRewriteTime() {
		return this.rewriteTime;
	}
	
	public long getLabelTime() {
		return this.labelTime;
	}
	
	public long getCanonicalisationTime(){
		return this.minimisationTime;
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
		return this.vars.size();
	}
	
	public int getVarsOut(){
		return this.canonVars.size();
	}

	public void buildQuery() {
		QueryBuilder qb = new QueryBuilder(this.canonGraph);
		this.canonVars = qb.getVars();
		this.varMap = qb.finalVarMap;
		this.canonOp = qb.getOp();
	}
	
	public String getQuery(){
		QueryBuilder qb = new QueryBuilder(this.canonGraph);
		this.canonVars = qb.getVars();
		this.varMap = qb.finalVarMap;
		String query = qb.getQuery();
		return query;
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

	public Op getOp() {
		return this.op;
	}

	public Op getCanonOp() {
		return this.canonOp;
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
	
	public static void main(String[] args) throws InterruptedException, HashCollisionException, IOException {
		String q = "SELECT DISTINCT ?var1  ?var1Label  WHERE {   ?var1  <http://www.wikidata.org/prop/direct/P31>  <http://www.wikidata.org/entity/Q6256> .   ?var1  <http://www.wikidata.org/prop/P1081>  ?var2 .   ?var2  <http://www.wikidata.org/prop/statement/P1081>  ?var3 .   ?var2  <http://www.wikidata.org/prop/qualifier/P585>  ?var4 .  FILTER (  (  NOT EXISTS  {    ?var1 ( <http://www.wikidata.org/prop/P1081> / <http://www.wikidata.org/prop/qualifier/P585> ) ?var5 .  FILTER (   ( (  ?var5  >  ?var4  ) )  ) .  }  )  ) .  FILTER (  (  EXISTS  {    ?var1  <http://www.wikidata.org/prop/P1081>  ?var6 .    ?var6  <http://www.wikidata.org/prop/statement/P1081>  ?var7 .    ?var6  <http://www.wikidata.org/prop/qualifier/P585>  ?var8 .  FILTER (   ( (  ?var7  >  ?var3  ) )  ) .  }  )  ) .  SERVICE  <http://wikiba.se/ontology#label>   {     <http://www.bigdata.com/rdf#serviceParam>  <http://wikiba.se/ontology#language>  \"en\".   } }\n";
		SingleQuery sq = new SingleQuery(q,true,true, true,true,false);
		sq.getCanonicalGraph().print();
		String query1 = sq.getQuery();
		System.out.println(query1);
		System.out.println(sq.getVarMap());
		System.out.println(sq.graphTime/Math.pow(10, 9));
		System.out.println(sq.rewriteTime/Math.pow(10, 9));
		System.out.println(sq.labelTime/Math.pow(10, 9));
		System.out.println(sq.minimisationTime/Math.pow(10, 9));
		System.exit(0);
	}

	public Map<String, String> getVarMap() {
		return this.varMap;
	}

	public boolean containsPaths() {
		return containsPaths;
	}

	public void setContainsPaths(boolean containsPaths) {
		this.containsPaths = containsPaths;
	}

	public boolean containsMinus() {
		return containsMinus;
	}

	public void setContainsMinus(boolean containsMinus) {
		this.containsMinus = containsMinus;
	}

	public boolean containsBind() {
		return containsBind;
	}

	public void setContainsBind(boolean containsBind) {
		this.containsBind = containsBind;
	}

	public boolean containsGroupBy() {
		return containsGroupBy;
	}

	public void setContainsGroupBy(boolean containsGroupBy) {
		this.containsGroupBy = containsGroupBy;
	}

	public boolean containsTable() {
		return containsTable;
	}

	public void setContainsTable(boolean containsTable) {
		this.containsTable = containsTable;
	}
}
