package main;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Distinct;
import org.apache.jena.sparql.path.P_FixedLength;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_OneOrMoreN;
import org.apache.jena.sparql.path.P_Path0;
import org.apache.jena.sparql.path.P_Path1;
import org.apache.jena.sparql.path.P_Path2;
import org.apache.jena.sparql.path.P_ReverseLink;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMoreN;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.sse.SSE;

public class PathTransform {
	
	public int numberOfInverses = 0;
	public int totalNumberofPaths = 0;
	public int firstInverse = 0;
	
	public Path visit(Path path) {
		if (path instanceof P_Inverse) {
			Path sub = ((P_Inverse) path).getSubPath();
			if (sub instanceof P_Seq) {
				Path left = ((P_Seq) sub).getLeft();
				Path right = ((P_Seq) sub).getRight();
				return new P_Seq(visit(new P_Inverse(right)),visit(new P_Inverse(left)));
			}
			else if (sub instanceof P_Alt) {
				Path left = ((P_Alt) sub).getLeft();
				Path right = ((P_Alt) sub).getRight();
				return new P_Alt(visit(new P_Inverse(left)),visit(new P_Inverse(right)));
			}
			else if (sub instanceof P_Inverse) {
				return ((P_Inverse) sub).getSubPath();
			}
			else if (sub instanceof P_ZeroOrMore1) {
				return new P_ZeroOrMore1(visit(new P_Inverse(((P_ZeroOrMore1) sub).getSubPath())));
			}
			else if (sub instanceof P_ZeroOrMoreN) {
				return new P_ZeroOrMoreN(visit(new P_Inverse(((P_ZeroOrMoreN) sub).getSubPath())));
			}
			else if (sub instanceof P_OneOrMore1) {
				return new P_Seq(visit(new P_Inverse(new P_ZeroOrMore1(((P_OneOrMore1) sub).getSubPath()))),visit(new P_Inverse(((P_OneOrMore1) sub).getSubPath())));
			}
			else if (sub instanceof P_OneOrMoreN) {
				return new P_Seq(visit(new P_Inverse(new P_ZeroOrMoreN(((P_OneOrMore1) sub).getSubPath()))),visit(new P_Inverse(((P_OneOrMoreN) sub).getSubPath())));
			}
			else if (sub instanceof P_ZeroOrOne) {
				return new P_ZeroOrOne(visit(new P_Inverse(((P_ZeroOrOne) sub).getSubPath())));
			}
			else if (sub instanceof P_FixedLength) {
				long l = ((P_FixedLength) sub).getCount();
				return new P_FixedLength(visit(new P_Inverse(((P_FixedLength) sub).getSubPath())),l);
			}
			else if (sub instanceof P_Distinct) {
				return new P_Distinct(visit(new P_Inverse(((P_Distinct) sub).getSubPath())));
			}
			else if (sub instanceof P_Link) {
				return new P_Inverse(((P_Link) sub));
			}
			else if (sub instanceof P_ReverseLink) {
				return new P_Link(((P_ReverseLink) sub).getNode());
			}
		}
		else {
			if (path instanceof P_Path1) {
				Path sub = ((P_Path1) path).getSubPath();
				if (path instanceof P_ZeroOrMore1) {
					return new P_ZeroOrMore1(visit(sub));
				}
				else if (path instanceof P_ZeroOrMoreN) {
					return new P_ZeroOrMoreN(visit(sub));
				}
				else if (path instanceof P_OneOrMore1) {
					return new P_OneOrMore1(visit(sub));
				}
				else if (path instanceof P_OneOrMoreN) {
					return new P_OneOrMoreN(visit(sub));
				}
				else if (path instanceof P_ZeroOrOne) {
					return new P_ZeroOrOne(visit(sub));
				}
				else if (path instanceof P_Distinct) {
					return new P_Distinct(visit(sub));
				}
				else if (path instanceof P_FixedLength) {
					long l = ((P_FixedLength) path).getCount();
					return new P_FixedLength(visit(sub),l);
				}
			}
			else if (path instanceof P_Path2) {
				Path left = ((P_Path2) path).getLeft();
				Path right = ((P_Path2) path).getRight();
				if (path instanceof P_Seq) {
					return new P_Seq(visit(left),visit(right));
				}
				else if (path instanceof P_Alt) {
					return new P_Alt(visit(left),visit(right));
				}
			}
			else if (path instanceof P_Path0) {
				if (path instanceof P_Link) {
					return path;
				}
				else if (path instanceof P_ReverseLink) {
					return path;
				}
			}
		}
		return path;
	}
	
	public Path fold(Path path) {
		if (path instanceof P_Seq) {
			List<Path> seq = sequence(path);
			Path previous = null;
			int m = seq.size();
			int[] i = new int[m+1];
			i[0] = 0;
			for (int j = 0; j < m; j++) {
				if (seq.get(j) instanceof P_Inverse) {
					
				}
				else {
					
				}
			}
			for (Path p : seq) {
				if (previous == null) {
					previous = p;
				}
				else {
					if (p.equals(previous)) {
						
					}
				}
			}
		}
		else {
			return path;
		}
		return path;
	}
	
	public List<Path> sequence(Path path) {
		ArrayList<Path> ans = new ArrayList<Path>();
		if (path instanceof P_Seq) {
			Path left = ((P_Seq) path).getLeft();
			Path right = ((P_Seq) path).getRight();
			if (left instanceof P_Seq) {
				ans.addAll(sequence(left));
			}
			else {
				ans.add(left);
			}
			if (right instanceof P_Seq) {
				ans.addAll(sequence(right));
			}
			else {
				ans.add(right);
			}
		}
		return ans;
	}
	
	public int minLength(Path path) {
		if (path instanceof P_Path0) {
			return 1;
		}
		else if (path instanceof P_Path1) {
			if (path instanceof P_Inverse) {
				return 0;
			}
			else if (path instanceof P_ZeroOrMore1) {
				return 0;
			}
			else {
				return minLength(((P_Path1) path).getSubPath());
			}
		}
		else if (path instanceof P_Path2) {
			Path left = ((P_Path2) path).getLeft();
			Path right = ((P_Path2) path).getRight();
			if (path instanceof P_Seq) {
				return minLength(left) + minLength(right);
			}
			else if (path instanceof P_Alt) {
				return Math.min(minLength(left), minLength(right));
			}
		}
		return 0;
	}
	
	public int maxLength(Path path) {
		if (path instanceof P_Path0) {
			return 1;
		}
		else if (path instanceof P_Path1) {
			return maxLength(((P_Path1) path).getSubPath());
		}
		else if (path instanceof P_Path2) {
			Path left = ((P_Path2) path).getLeft();
			Path right = ((P_Path2) path).getRight();
			if (path instanceof P_Seq) {
				return maxLength(left) + maxLength(right);
			}
			else if (path instanceof P_Alt) {
				return Math.max(maxLength(left), maxLength(right));
			}
		}
		else {
			return 0;
		}
		return 0;
	}
	
	public void count(Path path) {
		if (path instanceof P_Path0) {
			totalNumberofPaths++;
		}
		else if (path instanceof P_Path1) {
			Path sub = ((P_Path1) path).getSubPath();
			if (path instanceof P_Inverse) {
				if (sub instanceof P_Link) {
					numberOfInverses++;
					totalNumberofPaths++;
					if (firstInverse == 0) {
						firstInverse = totalNumberofPaths;
					}
				}
			}
			else {
				count(sub);
			}
		}
		else if (path instanceof P_Path2) {
			Path left = ((P_Path2) path).getLeft();
			Path right = ((P_Path2) path).getRight();
			count(left);
			count(right);
		}
	}
	
	public Op getResult(TriplePath tp) {
		Node s = tp.getSubject();
		Node o = tp.getObject();
		Path p = visit(tp.getPath());
		count(p);
		System.out.println(maxLength(p));
		if (numberOfInverses < (totalNumberofPaths - numberOfInverses)) {
			if (tp.getPath() instanceof P_Link) {
				return new OpTriple(Triple.create(s, tp.getPredicate(), o));
			}
			else {
				return new OpPath(new TriplePath(s,p,o));
			}
		}
		else if (numberOfInverses > (totalNumberofPaths - numberOfInverses)) {
			Path p1 = PathFactory.pathInverse(tp.getPath());
			if (tp.getPath() instanceof P_Link) { // i.e a triple pattern
				return new OpTriple(Triple.create(o, tp.getPredicate(), s));
			}
			else {
				p = visit(p1);
				return new OpPath(new TriplePath(o,p,s));
			}
		}
		else {
			PathTransform pt = new PathTransform();
			Path p1 = PathFactory.pathInverse(tp.getPath());
			p1 = pt.visit(p1);
			pt.count(p1);
			if (firstInverse < pt.firstInverse) {
				return new OpPath(new TriplePath(s,p,o));
			}
			else if (firstInverse > pt.firstInverse) {
				return new OpPath(new TriplePath(o,p1,s));
			}
			else {
				TriplePath tp1 = new TriplePath(s,p,o);
				TriplePath tp2 = new TriplePath(o,p1,s);
				return OpJoin.create(new OpPath(tp1), new OpPath(tp2));
			}
		}
	}
	
	public static void main(String[] args) {
		String q = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT DISTINCT ?x ?name WHERE {  ?x foaf:name/foaf:name/^foaf:name/foaf:name/^foaf:name ?y }";
		Op op = Algebra.compile(QueryFactory.create(q));
		System.out.println(op);
		Path path = SSE.parsePath("(seq (seq (seq (seq <http://xmlns.com/foaf/0.1/name> <http://xmlns.com/foaf/0.1/name>) (reverse <http://xmlns.com/foaf/0.1/name>)) <http://xmlns.com/foaf/0.1/name>) (reverse <http://xmlns.com/foaf/0.1/name>))");
		PathTransform pt = new PathTransform();
		TriplePath tp = new TriplePath(Var.alloc("x"), path, Var.alloc("y"));
		pt.getResult(tp);
		System.out.println(pt.numberOfInverses);
		System.out.println(pt.totalNumberofPaths);
		System.out.println(pt.firstInverse);
		System.out.println(pt.sequence(path));
		System.out.println(pt.minLength(path));
		System.out.println(pt.maxLength(path));
	}
}
