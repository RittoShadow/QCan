package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.optimize.TransformExtendCombine;
import org.apache.jena.sparql.algebra.optimize.TransformMergeBGPs;
import org.apache.jena.sparql.algebra.optimize.TransformSimplify;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.util.iterator.ExtendedIterator;
import transformers.FilterTransform;
import transformers.NotOneOfTransform;
import transformers.TransformPath;
import transformers.UCQTransformer;

public class Tools {

	public static void printGraph(Graph g){
		ExtendedIterator<Triple> e = GraphUtil.findAll(g);
		while (e.hasNext()){
			System.out.println(e.next() + " .");
		}
		System.out.println();
	}
	
	public static void extractQueries(File in, File out, String split, int n, String beginAt, String end) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(in));
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		String s;
		while ((s = br.readLine()) != null) {
			try {
				String query = s.split(split)[n];
				if (!query.contains(beginAt)) {
					continue;
				}
				if (query.contains(end) && end.length() > 0) {
					query = query.substring(query.indexOf(beginAt) + beginAt.length(), query.indexOf(end));
				}
				else {
					query = query.substring(query.indexOf(beginAt) + beginAt.length());
				}
				query = URLDecoder.decode(query, String.valueOf(StandardCharsets.UTF_8));
				query = query.replace("\n", " ").trim();
				query = query.replace("\r", " ").trim();
				Query q = QueryFactory.create(query);
				if (q != null) {
					bw.write(query);
					bw.newLine();
				}
			}
			catch (Exception e) {
			}
		}
		br.close();
		bw.close();
	}

	public static Set<Var> safeVars(Op op) {
		Set<Var> ans = new HashSet<>();
		if (op instanceof OpBGP) {
			BasicPattern bp = ((OpBGP) op).getPattern();
			for (Triple t : bp.getList()) {
				ans.addAll(safeVars(new OpTriple(t)));
			}
		}
		else if (op instanceof OpTriple) {
			ans.addAll(varsContainedIn(op));
		}
		else if (op instanceof OpUnion) {
			Op left = ((OpUnion) op).getLeft();
			Op right = ((OpUnion) op).getRight();
			Set<Var> leftVars = safeVars(left);
			Set<Var> rightVars = safeVars(right);
			for (Var v : leftVars) {
				if (rightVars.contains(v)) {
					ans.add(v);
				}
			}
		}
		else if (op instanceof OpMinus) {
			return safeVars(((OpMinus) op).getLeft());
		}
		else if (op instanceof OpLeftJoin) {
			return safeVars(((OpLeftJoin) op).getLeft());
		}
		else if (op instanceof OpProject) {
			Op sub = ((OpProject) op).getSubOp();
			Set<Var> subVars = safeVars(sub);
			List<Var> pVars = ((OpProject) op).getVars();
			for (Var v : subVars) {
				if (pVars.contains(v)) {
					ans.add(v);
				}
			}
		}
		else if (op instanceof OpFilter) {
			return safeVars(((OpFilter) op).getSubOp());
		}
		return ans;
	}

	public static Set<Var> varsContainedIn(Op op) {
		Set<Var> ans = new HashSet<>();
		if (op instanceof OpTriple) {
			Triple t = ((OpTriple) op).getTriple();
			if (t.getSubject().isVariable()) {
				if (!t.getSubject().getName().startsWith("?")) {
					ans.add(Var.alloc(t.getSubject().getName()));
				}
			}
			if (t.getPredicate().isVariable()) {
				if (!t.getPredicate().getName().startsWith("?")) {
					ans.add(Var.alloc(t.getPredicate().getName()));
				}
			}
			if (t.getObject().isVariable()) {
				if (!t.getObject().getName().startsWith("?")) {
					ans.add(Var.alloc(t.getObject().getName()));
				}
			}
		}
		else if (op instanceof OpBGP) {
			for (Triple t : ((OpBGP) op).getPattern().getList()) {
				if (t.getSubject().isVariable()) {
					if (!t.getSubject().getName().startsWith("?")) {
						ans.add(Var.alloc(t.getSubject().getName()));
					}
				}
				if (t.getPredicate().isVariable()) {
					if (!t.getPredicate().getName().startsWith("?")) {
						ans.add(Var.alloc(t.getPredicate().getName()));
					}
				}
				if (t.getObject().isVariable()) {
					if (!t.getObject().getName().startsWith("?")) {
						ans.add(Var.alloc(t.getObject().getName()));
					}
				}
			}
		}
		else if (op instanceof OpProject) {
			ans.addAll(((OpProject) op).getVars());
			ans.addAll(varsContainedIn(((OpProject) op).getSubOp()));
		}
		else if (op instanceof OpPath) {
			TriplePath tp = ((OpPath) op).getTriplePath();
			if (tp.getSubject().isVariable()) {
				ans.add(Var.alloc(tp.getSubject().getName()));
			}
			if (tp.getObject().isVariable()) {
				ans.add(Var.alloc(tp.getObject().getName()));
			}
		}
		else if (op instanceof OpGraph) {
			Node n = ((OpGraph) op).getNode();
			if (n.isVariable()) {
				ans.add(Var.alloc(n.getName()));
			}
			ans.addAll(varsContainedIn(((OpGraph) op).getSubOp()));
		}
		else if (op instanceof OpFilter) {
			ExprList eList = ((OpFilter) op).getExprs();
			ans.addAll(eList.getVarsMentioned());
			ans.addAll(varsContainedIn(((OpFilter) op).getSubOp()));
		}
		else if (op instanceof OpExtend) {
			Map<Var, Expr> map = ((OpExtend) op).getVarExprList().getExprs();
			for (Map.Entry<Var, Expr> entry : map.entrySet()) {
				ans.add(entry.getKey());
				ans.addAll(entry.getValue().getVarsMentioned());
			}
			ans.addAll(varsContainedIn(((OpExtend) op).getSubOp()));
		}
		else if (op instanceof OpAssign) {
			Map<Var,Expr> map = ((OpAssign) op).getVarExprList().getExprs();
			for (Map.Entry<Var, Expr> entry : map.entrySet()) {
				ans.add(entry.getKey());
				ans.addAll(entry.getValue().getVarsMentioned());
			}
			ans.addAll(varsContainedIn(((OpAssign) op).getSubOp()));
		}
		else if (op instanceof OpTable) {
			ans.addAll(((OpTable) op).getTable().getVars());
		}
		else if (op instanceof Op1) {
			Op subOp = ((Op1) op).getSubOp();
			ans.addAll(varsContainedIn(subOp));
		}
		else if (op instanceof Op2) {
			Op left = ((Op2) op).getLeft();
			Op right = ((Op2) op).getRight();
			ans.addAll(varsContainedIn(left));
			ans.addAll(varsContainedIn(right));
		}
		else if (op instanceof OpN) {
			for (Op o : ((OpN) op).getElements()) {
				ans.addAll(varsContainedIn(o));
			}
		}
		return ans;
	}

	public static Op UCQNormalisation(Op op) {
		Op op1 = op;
		Op op2 = op;
		do {
			op1 = op2;
			op2 = Transformer.transform(new FilterTransform(), op2);
			op2 = Transformer.transform(new UCQTransformer(), op2);
			op2 = Transformer.transform(new TransformPath(), op2);
			op2 = Transformer.transform(new NotOneOfTransform(), op2);
		}
		while (!op1.equals(op2));
		return op2;
	}

	public static Op UCQTransformation(Op op) {
		Op op2 = UCQNormalisation(op);
//		op2 = uC2RPQCollapse(op2);
//		op2 = Transformer.transform(new BGPCollapser(op2, this.projectionVars, true), op2); // transform all sequences
//		op2 = Transformer.transform(new BGPCollapser(op2,this.projectionVars,false), op2); // transform BGPs
		op2 = Transformer.transform(new TransformSimplify(), op2);
		op2 = Transformer.transform(new TransformMergeBGPs(), op2);
		op2 = Transformer.transform(new TransformExtendCombine(), op2);
		op2 = Transformer.transform(new BGPSort(), op2);
		return op2;
	}
	
	public static void main(String[] args) throws IOException {
		File out = new File("RKBExplorerQueries.txt");
		if (!out.exists()) {
			out.createNewFile();
		}
		Tools.extractQueries(new File("RKBExplorer.log"), out, "\t", 3, "", "");
	}
}
