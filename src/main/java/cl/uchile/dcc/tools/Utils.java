package cl.uchile.dcc.tools;

import cl.uchile.dcc.transformers.*;
import com.google.common.collect.Sets;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.*;
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

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Utils {

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
		else if (op instanceof OpGroup) {
			Op sub = ((OpGroup) op).getSubOp();
			Set<Var> subVars = safeVars(sub);
			List<Var> gVars = ((OpGroup) op).getGroupVars().getVars();
			for (Var v : subVars) {
				if (gVars.contains(v)) {
					ans.add(v);
				}
			}
		}
		else if (op instanceof OpFilter) {
			return safeVars(((OpFilter) op).getSubOp());
		}
		else if (op instanceof OpExtend) {
			return safeVars(((OpExtend) op).getSubOp());
		}
		else if (op instanceof OpTable) {
			ans.addAll(((OpTable) op).getTable().getVars());
		}
		else if (op instanceof OpGraph) {
			Op sub = ((OpGraph) op).getSubOp();
			if (((OpGraph) op).getNode().isVariable()) {
				ans.add(Var.alloc(((OpGraph) op).getNode()));
			}
			ans.addAll(safeVars(sub));
		}
		else if (op instanceof OpService) {
			if (((OpService) op).getSilent()) {
				return safeVars(((OpService) op).getSubOp());
			}
			else {
				//TODO
			}
		}
		return ans;
	}

	public static Set<Var> varsContainedInExcept(Op op, Op exception) {
		Set<Var> ans = new HashSet<>();
		if (op.equals(exception)) {
			return ans;
		}
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
		} else if (op instanceof OpBGP) {
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
		} else if (op instanceof OpProject) {
			ans.addAll(((OpProject) op).getVars());
			ans.addAll(varsContainedInExcept(((OpProject) op).getSubOp(),exception));
		} else if (op instanceof OpPath) {
			TriplePath tp = ((OpPath) op).getTriplePath();
			if (tp.getSubject().isVariable()) {
				if (!tp.getSubject().getName().startsWith("?")) {
					ans.add(Var.alloc(tp.getSubject().getName()));
				}
			}
			if (tp.getObject().isVariable()) {
				if (tp.getObject().getName().startsWith("?")) {
					ans.add(Var.alloc(tp.getObject().getName()));
				}
			}
		} else if (op instanceof OpGraph) {
			Node n = ((OpGraph) op).getNode();
			if (n.isVariable()) {
				ans.add(Var.alloc(n.getName()));
			}
			ans.addAll(varsContainedInExcept(((OpGraph) op).getSubOp(),exception));
		} else if (op instanceof OpFilter) {
			ExprList eList = ((OpFilter) op).getExprs();
			ans.addAll(eList.getVarsMentioned());
			ans.addAll(varsContainedInExcept(((OpFilter) op).getSubOp(),exception));
		} else if (op instanceof OpExtend) {
			Map<Var, Expr> map = ((OpExtend) op).getVarExprList().getExprs();
			for (Map.Entry<Var, Expr> entry : map.entrySet()) {
				ans.add(entry.getKey());
				ans.addAll(entry.getValue().getVarsMentioned());
			}
			ans.addAll(varsContainedInExcept(((OpExtend) op).getSubOp(),exception));
		} else if (op instanceof OpAssign) {
			Map<Var, Expr> map = ((OpAssign) op).getVarExprList().getExprs();
			for (Map.Entry<Var, Expr> entry : map.entrySet()) {
				ans.add(entry.getKey());
				ans.addAll(entry.getValue().getVarsMentioned());
			}
			ans.addAll(varsContainedInExcept(((OpAssign) op).getSubOp(),exception));
		} else if (op instanceof OpTable) {
			ans.addAll(((OpTable) op).getTable().getVars());
		} else if (op instanceof Op1) {
			Op subOp = ((Op1) op).getSubOp();
			ans.addAll(varsContainedInExcept(subOp,exception));
		} else if (op instanceof Op2) {
			Op left = ((Op2) op).getLeft();
			Op right = ((Op2) op).getRight();
			ans.addAll(varsContainedInExcept(left,exception));
			ans.addAll(varsContainedInExcept(right,exception));
		} else if (op instanceof OpN) {
			for (Op o : ((OpN) op).getElements()) {
				ans.addAll(varsContainedInExcept(o,exception));
			}
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
				if (!tp.getSubject().getName().startsWith("?")) {
					ans.add(Var.alloc(tp.getSubject().getName()));
				}
			}
			if (tp.getObject().isVariable()) {
				if (tp.getObject().getName().startsWith("?")) {
					ans.add(Var.alloc(tp.getObject().getName()));
				}
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

	/**
	 * Same as above but includes blank node variables.
	 */
	public static Set<Var> allVarsContainedIn(Op op) {
		Set<Var> ans = new HashSet<>();
		if (op instanceof OpTriple) {
			Triple t = ((OpTriple) op).getTriple();
			if (t.getSubject().isVariable()) {
				ans.add(Var.alloc(t.getSubject().getName()));
			}
			if (t.getPredicate().isVariable()) {
				ans.add(Var.alloc(t.getPredicate().getName()));
			}
			if (t.getObject().isVariable()) {
				ans.add(Var.alloc(t.getObject().getName()));
			}
		}
		else if (op instanceof OpBGP) {
			for (Triple t : ((OpBGP) op).getPattern().getList()) {
				if (t.getSubject().isVariable()) {
					ans.add(Var.alloc(t.getSubject().getName()));
				}
				if (t.getPredicate().isVariable()) {
					ans.add(Var.alloc(t.getPredicate().getName()));
				}
				if (t.getObject().isVariable()) {
					ans.add(Var.alloc(t.getObject().getName()));
				}
			}
		}
		else if (op instanceof OpProject) {
			ans.addAll(((OpProject) op).getVars());
			ans.addAll(allVarsContainedIn(((OpProject) op).getSubOp()));
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
			ans.addAll(allVarsContainedIn(((OpGraph) op).getSubOp()));
		}
		else if (op instanceof OpFilter) {
			ExprList eList = ((OpFilter) op).getExprs();
			ans.addAll(eList.getVarsMentioned());
			ans.addAll(allVarsContainedIn(((OpFilter) op).getSubOp()));
		}
		else if (op instanceof OpExtend) {
			Map<Var, Expr> map = ((OpExtend) op).getVarExprList().getExprs();
			for (Map.Entry<Var, Expr> entry : map.entrySet()) {
				ans.add(entry.getKey());
				ans.addAll(entry.getValue().getVarsMentioned());
			}
			ans.addAll(allVarsContainedIn(((OpExtend) op).getSubOp()));
		}
		else if (op instanceof OpAssign) {
			Map<Var,Expr> map = ((OpAssign) op).getVarExprList().getExprs();
			for (Map.Entry<Var, Expr> entry : map.entrySet()) {
				ans.add(entry.getKey());
				ans.addAll(entry.getValue().getVarsMentioned());
			}
			ans.addAll(allVarsContainedIn(((OpAssign) op).getSubOp()));
		}
		else if (op instanceof OpTable) {
			ans.addAll(((OpTable) op).getTable().getVars());
		}
		else if (op instanceof Op1) {
			Op subOp = ((Op1) op).getSubOp();
			ans.addAll(allVarsContainedIn(subOp));
		}
		else if (op instanceof Op2) {
			Op left = ((Op2) op).getLeft();
			Op right = ((Op2) op).getRight();
			ans.addAll(allVarsContainedIn(left));
			ans.addAll(allVarsContainedIn(right));
		}
		else if (op instanceof OpN) {
			for (Op o : ((OpN) op).getElements()) {
				ans.addAll(allVarsContainedIn(o));
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
		BranchRenamer br = new BranchRenamer();
		op2 = br.visit(op2);
		op2 = Transformer.transform(new LocalVarRenamer(), op2);
		op2 = Transformer.transform(new TransformSimplify(), op2);
		op2 = Transformer.transform(new TransformMergeBGPs(), op2);
		op2 = Transformer.transform(new TransformExtendCombine(), op2);
		op2 = Transformer.transform(new BGPSort(), op2);
		return op2;
	}

	public static boolean isWellDesigned(Op op) {
		return isWellDesigned(op,new HashSet<>());
	}

	public static boolean isWellDesigned(Op op, Set<Var> outerVars) {
		Set<Var> currentVars = new HashSet<>(outerVars);
		if (op instanceof OpProject) {
			currentVars.addAll(((OpProject) op).getVars());
			return isWellDesigned(((OpProject) op).getSubOp(),currentVars);
		}
		else if (op instanceof OpGraph) {
			Node n = ((OpGraph) op).getNode();
			if (n.isVariable()) {
				currentVars.add(Var.alloc(n.getName()));
			}
			return isWellDesigned(((OpGraph) op).getSubOp(),currentVars);
		}
		else if (op instanceof OpFilter) {
			ExprList eList = ((OpFilter) op).getExprs();
			Set<Var> filterVars = eList.getVarsMentioned();
			Set<Var> subVars = varsContainedIn(((OpFilter) op).getSubOp());
			for (Var var : filterVars) {
				if (!subVars.contains(var)) {
					return false;
				}
			}
			currentVars.addAll(filterVars);
			return isWellDesigned(((OpFilter) op).getSubOp(),currentVars);
		}
		else if (op instanceof OpExtend) {
			Map<Var, Expr> map = ((OpExtend) op).getVarExprList().getExprs();
			for (Map.Entry<Var, Expr> entry : map.entrySet()) {
				currentVars.add(entry.getKey());
				currentVars.addAll(entry.getValue().getVarsMentioned());
			}
			return isWellDesigned(((OpExtend) op).getSubOp(),currentVars);
		}
		else if (op instanceof OpAssign) {
			Map<Var,Expr> map = ((OpAssign) op).getVarExprList().getExprs();
			for (Map.Entry<Var, Expr> entry : map.entrySet()) {
				currentVars.add(entry.getKey());
				currentVars.addAll(entry.getValue().getVarsMentioned());
			}
			return isWellDesigned(((OpAssign) op).getSubOp(),currentVars);
		}
		else if (op instanceof Op1) {
			return isWellDesigned(((Op1) op).getSubOp(),currentVars);
		}
		else if (op instanceof Op2) {
			Op left = ((Op2) op).getLeft();
			Op right = ((Op2) op).getRight();
			if (op instanceof OpLeftJoin) {
				Set<Var> rightVars = varsContainedIn(right);
				Set<Var> leftVars = varsContainedIn(left);
				Set<Var> intersection = Sets.intersection(rightVars,currentVars);
				for (Var var : intersection) {
					if (!leftVars.contains(var)) {
						return false;
					}
				}
			}
			return isWellDesigned(left,currentVars) && isWellDesigned(right,currentVars);
		}
		else if (op instanceof OpN) {
			boolean ans = true;
			for (Op o : ((OpN) op).getElements()) {
				ans = ans && isWellDesigned(o,currentVars);
			}
			return ans;
		}
		else {
			return true;
		}
	}

	public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
		Set<Set<T>> sets = new HashSet<>();
		if (originalSet.isEmpty()) {
			sets.add(new HashSet<>());
			return sets;
		}
		List<T> list = new ArrayList<>(originalSet);
		T head = list.get(0);
		Set<T> rest = new HashSet<>(list.subList(1, list.size()));
		for (Set<T> set : powerSet(rest)) {
			Set<T> newSet = new HashSet<>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}
		return sets;
	}

	/**
	 * @param t A triple pattern.
	 * @return A triple where all variable nodes have been replaced with blank nodes.
	 */
	public static Triple getTripleWithVars(Triple t) {
		Node s = t.getSubject();
		if (s.isBlank()) {
			s = Var.alloc(s.getBlankNodeLabel());
		}
		Node p = t.getPredicate();
		if (p.isBlank()) {
			p = Var.alloc(p.getBlankNodeLabel());
		}
		Node o = t.getObject();
		if (o.isBlank()) {
			o = Var.alloc(o.getBlankNodeLabel());
		}
		return Triple.create(s, p, o);
	}

	/**
	 * @param s An RDF literal with a datatype.
	 * @return A node that represents a literal with a datatype. If no datatype is specified, it is assumed to be a string.
	 */
	public static Node createLiteralWithType(String s) {
		Node ans;
		s = s.replaceAll("\"", "");
		if (s.contains("^^")) {
			ans = NodeFactory.createLiteralByValue(s.substring(0, s.indexOf("^^")), NodeFactory.getType(s.substring(1 + s.lastIndexOf("^")).replaceAll("[<>]", "")));
		} else {
			ans = NodeFactory.createLiteralByValue(s, XSDDatatype.XSDstring);
		}
		return ans;
	}

	/**
	 * @param s A string that represents a SPARQL function.
	 * @return Returns true if the function is ordered (i.e (f expr1 expr2) != (f expr2 expr1)).
	 */
	public static boolean isOrderedFunction(String s) {
		switch (s) {
			case "<":
			case "concat":
			case ">":
			case "<=":
			case ">=":
			case "-":
			case "/":
			case "regex":
			case "if":
			case "in":
			case "notin":
			case "replace":
			case "strdt":
			case "strlang":
			case "strstarts":
			case "strends":
			case "contains":
			case "strbefore":
			case "strafter":
			case "substr":
				return true;
			default:
				return false;
		}
	}

	public static void main(String[] args) throws IOException {
		File out = new File("RKBExplorerQueries.txt");
		if (!out.exists()) {
			out.createNewFile();
		}
		Utils.extractQueries(new File("RKBExplorer.log"), out, "\t", 3, "", "");
	}
}
