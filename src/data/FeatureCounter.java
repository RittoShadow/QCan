package data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.Op1;
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
import org.apache.jena.sparql.path.Path;

import main.PGraph;

public class FeatureCounter implements OpVisitor {
	
	public int nTriples = 0;
	public int nPaths = 0;
	private boolean containsUnion = false;
	private boolean containsJoin = false;
	private boolean containsOptional = false;
	private boolean containsFilter = false;
	private boolean containsSolutionMods = false;
	private boolean containsNamedGraphs = false;
	private boolean containsPaths = false;
	private boolean isCQ = false;
	private boolean isUCQ = false;
	private boolean isMonotone = false;
	private boolean isC2RPQ = false;
	private boolean isUC2RPQ = false;
	private boolean isM2RPQ = false;
	private HashSet<String> features = new HashSet<String>();
	private HashSet<String> pathFeatures = new HashSet<String>();
	private HashSet<String> pathStats = new HashSet<String>();
	
	public FeatureCounter(){
		
	}
	
	public FeatureCounter(Query query){

	}
	
	public FeatureCounter(Op op) {
		Op firstOp = firstOp(op);
		isCQ = isCQ(firstOp);
		isUCQ = isUCQ(firstOp);
		isC2RPQ = isC2RPQ(firstOp);
		isUC2RPQ = isUC2RPQ(firstOp);
		isMonotone = isMonotone(firstOp);
		isM2RPQ = isM2RPQ(firstOp);
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
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpQuadBlock arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpTriple arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpQuad arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpPath arg0) {
		features.add(arg0.getName());
		containsPaths = true;
		Path p = arg0.getTriplePath().getPath();
		PropertyPathFeatureCounter ppfc = new PropertyPathFeatureCounter(p);
		pathFeatures.addAll(ppfc.features);
		nPaths++;
		boolean path = true;
		if (path) {
			String pFeatures = "[";
			for (String f : ppfc.features) {
				pFeatures += f + ", ";
			}
			pFeatures = pFeatures.substring(0, pFeatures.length() - 1) + "]\t";
			pFeatures += ppfc.maxLength + "\t";
			if (ppfc.features.contains("P_NegPropSet") || ppfc.features.contains("P_Mod") || ppfc.features.contains("P_Distinct") || ppfc.features.contains("P_ZeroOrOne") || ppfc.features.contains("P_Multi") || ppfc.features.contains("P_Shortest")) {
				pFeatures += "unsupported";
				return;
			}
			else {
				PGraph pg = new PGraph(p);
				long t = System.nanoTime();
				Path normal = pg.getNormalisedPath();
				PropertyPathFeatureCounter ppfc2 = new PropertyPathFeatureCounter(normal);
				t = System.nanoTime() - t;
				pFeatures += "[";
				for (String f : ppfc2.features) {
					pFeatures += f + ", ";
				}
				pFeatures = pFeatures.substring(0, pFeatures.length() - 1) + "]\t";
				pFeatures += ppfc2.maxLength + "\t";
				pFeatures += t;
				System.out.println(t);
				pathStats.add(pFeatures);
			}
			
		}
	}

	@Override
	public void visit(OpTable arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpNull arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpProcedure arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpPropFunc arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
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
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpDatasetNames arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpLabel arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpAssign arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpExtend arg0) {
		features.add(arg0.getName());
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
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpMinus arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpConditional arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpSequence arg0) {
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpDisjunction arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpList arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
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
		features.add(arg0.getName());
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
		features.add(arg0.getName());
	}

	@Override
	public void visit(OpTopN arg0) {
		features.add("Unsupported");
		features.add(arg0.getName());
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
	
	public boolean getContainsPaths() {
		return this.containsPaths;
	}
	
	public boolean isMonotone() {
		return isMonotone;
	}
	
	public boolean isCQ() {
		return isCQ;
	}
	
	public boolean isUCQ() {
		return isUCQ;
	}
	
	public boolean isC2RPQ() {
		return isC2RPQ;
	}
	
	public boolean isUC2RPQ() {
		return isUC2RPQ;
	}
	
	public boolean isM2RPQ() {
		return isM2RPQ;
	}
	
	public HashSet<String> getFeatures(){
		return this.features;
	}
	
	public HashSet<String> getPathFeatures() {
		return this.pathFeatures;
	}
	
	public boolean isCQ(Op op) {
		if (op instanceof OpJoin) {
			boolean ans = true;
			List<Op> ops = opsInJoin(op);
			if (ops.isEmpty()) {
				return false;
			}
			for (Op o : ops) {
				if (!(o instanceof OpTriple || o instanceof OpPath || o instanceof OpSequence || o instanceof OpBGP)) {
					ans = false;
				}
				else if (o instanceof OpSequence || o instanceof OpPath) {
					ans = false;
				}
			}
			return ans;
		}
		else {
			return false;
		}
	}
	
	public boolean isC2RPQ(Op op) {
		if (op instanceof OpJoin) {
			boolean ans = true;
			List<Op> ops = opsInJoin(op);
			if (ops.isEmpty()) {
				return false;
			}
			for (Op o : ops) {
				if (!(o instanceof OpTriple || o instanceof OpPath || o instanceof OpSequence || o instanceof OpBGP)) {
					ans = false;
				}
				else if (o instanceof OpSequence) {
					if (!isC2RPQ(o)) {
						return false;
					}
				}
			}
			return ans;
		}
		else if (op instanceof OpSequence) {
			boolean ans = true;
			List<Op> ops = ((OpSequence) op).getElements();
			if (ops == null) {
				return false;
			}
			for (Op o : ops) {
				if (!(o instanceof OpTriple || o instanceof OpJoin || o instanceof OpPath || o instanceof OpSequence || o instanceof OpBGP)) {
					ans = false;
				}
				else if (o instanceof OpJoin || o instanceof OpSequence) {
					if (!isC2RPQ(o)) {
						return false;
					}
				}
			}
			return ans;
		}
		else {
			return false;
		}
	}
	
	public boolean isUCQ(Op op) {
		if (op instanceof OpUnion) {
			boolean ans = true;
			List<Op> ops = opsInUnion(op);
			if (ops.isEmpty()) {
				return false;
			}
			for (Op o : ops) {
				if (!(o instanceof OpJoin || o instanceof OpTriple || o instanceof OpPath || o instanceof OpSequence || o instanceof OpBGP)) {
					ans = false;
				}
				else if (o instanceof OpSequence || o instanceof OpPath) {
					ans = false;
				}
			}
			return ans;
		}
		else {
			return false;
		}
	}
	
	public boolean isUC2RPQ(Op op) {
		if (op instanceof OpUnion) {
			boolean ans = true;
			List<Op> ops = opsInUnion(op);
			if (ops.isEmpty()) {
				return false;
			}
			for (Op o : ops) {
				if (!(o instanceof OpJoin || o instanceof OpTriple || o instanceof OpPath || o instanceof OpSequence || o instanceof OpBGP)) {
					ans = false;
				}
				else if (o instanceof OpJoin || o instanceof OpSequence) {
					if (!isC2RPQ(o)) {
						return false;
					}
				}
			}
			return ans;
		}
		else {
			return false;
		}
	}
	
	public boolean isMonotone(Op op) {
		if (op instanceof OpJoin) {
			List<Op> ops = opsInJoin(op);
			if (ops.isEmpty()) {
				return false;
			}
			for (Op o : ops) {
				if (!(o instanceof OpJoin || o instanceof OpTriple || o instanceof OpBGP || o instanceof OpUnion)) {
					return false;
				}
				else if (o instanceof OpUnion || o instanceof OpJoin) {
					if (!isMonotone(o)) {
						return false;
					}
				}
			}
			return true;
		}
		else if (op instanceof OpUnion) {
			List<Op> ops = opsInUnion(op);
			if (ops == null) {
				return false;
			}
			for (Op o : ops) {
				if (!(o instanceof OpJoin || o instanceof OpTriple || o instanceof OpBGP || o instanceof OpUnion)) {
					return false;
				}
				else if (o instanceof OpUnion || o instanceof OpJoin) {
					if (!isMonotone(o)) {
						return false;
					}
				}
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean isM2RPQ(Op op) {
		if (op instanceof OpJoin) {
			List<Op> ops = opsInJoin(op);
			if (ops.isEmpty()) {
				return false;
			}
			for (Op o : ops) {
				if (!(o instanceof OpJoin || o instanceof OpTriple || o instanceof OpBGP || o instanceof OpUnion || o instanceof OpPath || o instanceof OpSequence)) {
					return false;
				}
				else if (o instanceof OpUnion || o instanceof OpJoin || o instanceof OpSequence) {
					if (!isM2RPQ(o)) {
						return false;
					}
				}
			}
			return true;
		}
		else if (op instanceof OpUnion) {
			List<Op> ops = opsInUnion(op);
			if (ops.isEmpty()) {
				return false;
			}
			for (Op o : ops) {
				if (!(o instanceof OpJoin || o instanceof OpTriple || o instanceof OpBGP || o instanceof OpUnion)) {
					return false;
				}
				else if (o instanceof OpUnion || o instanceof OpJoin) {
					if (!isM2RPQ(o)) {
						return false;
					}
				}
			}
			return true;
		}
		else if (op instanceof OpSequence) {
			List<Op> ops = ((OpSequence) op).getElements();
			for (Op o : ops) {
				if (!(o instanceof OpJoin || o instanceof OpTriple || o instanceof OpBGP || o instanceof OpUnion || o instanceof OpPath)) {
					return false;
				}
				else if (o instanceof OpUnion || o instanceof OpJoin || o instanceof OpSequence) {
					if (!isM2RPQ(o)) {
						return false;
					}
				}
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	public List<Op> opsInJoin(Op op) {
		List<Op> ans = new ArrayList<Op>();
		if (op instanceof OpJoin) {
			Op left = ((OpJoin) op).getLeft();
			Op right = ((OpJoin) op).getRight();
			if (left instanceof OpJoin) {
				ans.addAll(opsInJoin(left));
			}
			else {
				ans.add(left);
			}
			if (right instanceof OpJoin) {
				ans.addAll(opsInJoin(right));
			}
			else {
				ans.add(right);
			}
		}
		else {
			return ans;
		}
		return ans;
	}
	
	public List<Op> opsInUnion(Op op){
		List<Op> ans = new ArrayList<Op>();
		if (op instanceof OpUnion) {
			Op leftOp = ((OpUnion) op).getLeft();
			Op rightOp = ((OpUnion) op).getRight();
			if (leftOp instanceof OpUnion) {
				ans.addAll(opsInUnion(leftOp));
			}
			else {
				ans.add(leftOp);
			}
			if (rightOp instanceof OpUnion) {
				ans.addAll(opsInUnion(rightOp));
			}
			else {
				ans.add(rightOp);
			}
		}
		else {
			return ans;
		}
		return ans;
	}
	
	public Op firstOp(Op op) {
		if (op instanceof Op1) {
			if (op instanceof OpDistinct) {
				return firstOp(((Op1) op).getSubOp());
			}
			else if (op instanceof OpProject) {
				return firstOp(((Op1) op).getSubOp());
			}
			else {
				return op;
			}
		}
		else {
			return op;
		}
	}
	
	public HashSet<String> getPathStats() { 
		return this.pathStats;
	}
	
	public static void main(String[] args) {
		Query q = QueryFactory.create("SELECT  *\r\n" + 
				"WHERE\r\n" + 
				"  { ?var1 (<http://www.wikidata.org/prop/direct/P31>)*/(<http://www.wikidata.org/prop/direct/P279>)* <http://www.wikidata.org/entity/Q16917> .\r\n" + 
				"    ?var1  <http://www.wikidata.org/prop/direct/P625>  ?var2\r\n" + 
				"  }");
		Op op = Algebra.compile(q);
		FeatureCounter fc = new FeatureCounter(op);
		OpWalker.walk(op, fc);
		System.out.println(fc.features);
		System.out.println(fc.pathStats);
	}

}
