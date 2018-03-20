package data;

import java.util.HashSet;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.OpAssign;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpDatasetNames;
import org.apache.jena.sparql.algebra.op.OpDiff;
import org.apache.jena.sparql.algebra.op.OpDisjunction;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLabel;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpList;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpNull;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpProcedure;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpPropFunc;
import org.apache.jena.sparql.algebra.op.OpQuad;
import org.apache.jena.sparql.algebra.op.OpQuadBlock;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.algebra.op.OpReduced;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.op.OpTopN;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.op.OpUnion;

public class FeatureCounter implements OpVisitor {
	
	public int nTriples = 0;
	private boolean containsUnion = false;
	private boolean containsJoin = false;
	private boolean containsOptional = false;
	private boolean containsFilter = false;
	private boolean containsSolutionMods = false;
	private boolean containsNamedGraphs = false;
	private HashSet<String> features = new HashSet<String>();
	
	public FeatureCounter(){
		
	}
	
	public FeatureCounter(Query query){

	}

	@Override
	public void visit(OpBGP arg0) {
		features.add(arg0.getName());
		if (arg0.getPattern().getList().size() > 1){
			features.add("join");
		}
	}

	@Override
	public void visit(OpQuadPattern arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpQuadBlock arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpTriple arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpQuad arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpPath arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpTable arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpNull arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpProcedure arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpPropFunc arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpFilter arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpGraph arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpService arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpDatasetNames arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpLabel arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpAssign arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpExtend arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpJoin arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpLeftJoin arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpUnion arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpDiff arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpMinus arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpConditional arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpSequence arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpDisjunction arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpList arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpOrder arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpProject arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpReduced arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpDistinct arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpSlice arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpGroup arg0) {
		features.add("Unsupported");
	}

	@Override
	public void visit(OpTopN arg0) {
		features.add("Unsupported");
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
	
	public HashSet<String> getFeatures(){
		return this.features;
	}

}
