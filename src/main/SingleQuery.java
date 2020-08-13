package main;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.optimize.TransformExtendCombine;
import org.apache.jena.sparql.algebra.optimize.TransformMergeBGPs;
import org.apache.jena.sparql.algebra.optimize.TransformPathFlatternStd;
import org.apache.jena.sparql.algebra.optimize.TransformSimplify;
import org.apache.jena.sparql.core.Var;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import data.FeatureCounter;
import transformers.FilterTransform;
import transformers.UCQTransformer;

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
	private Set<Var> vars = new HashSet<Var>();
	private Set<Var> canonVars = new HashSet<Var>();
	private boolean enableLeaning = true;
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
		parseQuery(q);
		graphTime = System.nanoTime() - t;
		if (canon){
			canonicalise(verbose);
		}
		else{
			canonGraph = this.graph;
			minimisationTime = graphTime;
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
		op2 = Transformer.transform(new UCQTransformer(), op2);
		while (!op.equals(op2)){
			op = op2;
			op2 = Transformer.transform(new UCQTransformer(), op2);
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
		RGraphBuilder rgb = new RGraphBuilder(query,pathNormalisation);
		graph = rgb.getResult();
		graphTime = rgb.graphTime;
		rewriteTime = rgb.rewriteTime;
		vars = rgb.varsContainedIn(op);
		if (!rgb.isDistinct){
			if (vars.containsAll(rgb.projectionVars) && rgb.projectionVars.containsAll(vars)){
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
		setContainsMinus(fc.getPathFeatures().contains("minus"));
		setContainsBind(fc.getPathFeatures().contains("extend"));
		setContainsGroupBy(fc.getPathFeatures().contains("group"));
		setContainsTable(fc.getPathFeatures().contains("table"));
	}
	
	public void canonicalise() throws InterruptedException, HashCollisionException{
		canonicalise(false);
	}
	
	public void canonicalise(boolean verbose) throws InterruptedException, HashCollisionException{
		this.graph.setLeaning(enableLeaning);
		canonGraph = this.graph.getCanonicalForm(verbose);
		this.labelTime = canonGraph.getLabelTime();
		this.minimisationTime = canonGraph.getMinimisationTime();
	}
	
	public boolean determineSemantics(){
		
		return true;
	}
	
	public void setLeaning(boolean b){
		this.enableLeaning = b;
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
	
	public String getQuery(){
		QueryBuilder qb = new QueryBuilder(this.canonGraph);
		this.canonVars = qb.getVars();
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
		String q = "SELECT * WHERE {  {    ?var1  <http://www.wikidata.org/prop/direct/P179>  ?var2 .  }   UNION  {    ?var3  <http://www.wikidata.org/prop/direct/P179>  ?var2 .  }   UNION  {    ?var4  <http://www.wikidata.org/prop/direct/P179>  ?var2 .  }   UNION  {    ?var5  <http://www.wikidata.org/prop/direct/P179>  ?var2 .  }   UNION  {    ?var6  <http://www.wikidata.org/prop/direct/P179>  ?var2 .  }   UNION  {    ?var7  <http://www.wikidata.org/prop/direct/P179>  ?var2 .  }   VALUES (  ?var1  ) {    (  <http://www.wikidata.org/entity/Q43361>  )    (  <http://www.wikidata.org/entity/Q46751>  )    (  <http://www.wikidata.org/entity/Q46750>  )    (  <http://www.wikidata.org/entity/Q47209>  )   }   VALUES (  ?var3  ) {    (  <http://www.wikidata.org/entity/Q46751>  )   }   VALUES (  ?var4  ) {    (  <http://www.wikidata.org/entity/Q46758>  )   }   VALUES (  ?var5  ) {    (  <http://www.wikidata.org/entity/Q46887>  )   }   VALUES (  ?var6  ) {    (  <http://www.wikidata.org/entity/Q47210>  )   }   VALUES (  ?var7  ) {    (  <http://www.wikidata.org/entity/Q47598>  )   } } ";
		SingleQuery sq = new SingleQuery(q,true,true,true,true);
		sq.getCanonicalGraph().print();
		System.out.println(sq.getQuery());
		System.out.println(sq.graphTime/Math.pow(10, 9));
		System.out.println(sq.rewriteTime/Math.pow(10, 9));
		System.out.println(sq.labelTime/Math.pow(10, 9));
		System.out.println(sq.minimisationTime/Math.pow(10, 9));
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
