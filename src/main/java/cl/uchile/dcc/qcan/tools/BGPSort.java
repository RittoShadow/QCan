package cl.uchile.dcc.qcan.tools;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.qcan.main.SingleQuery;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.BasicPattern;

import java.util.*;

public class BGPSort extends TransformCopy {
	public List<HashSet<String>> ucqVars = new ArrayList<>();
	
	public Op transform(OpBGP bgp){
		BasicPattern bp = new BasicPattern();
		List<Triple> tp = bgp.getPattern().getList();
		Collections.sort(tp, new TripleComparator());
		for (Triple t : tp){
			bp.add(t);
		}
		return new OpBGP(bp);
	}
	
	public Op transform(OpUnion union, Op left, Op right){
		if (left instanceof OpBGP){
			List<Triple> triple = ((OpBGP) left).getPattern().getList();
			HashSet<String> cqVars = new HashSet<>();
			for (Triple t : triple){
				if (t.getSubject().isVariable()){
					cqVars.add(t.getSubject().getName());
				}
				if (t.getPredicate().isVariable()){
					cqVars.add(t.getPredicate().getName());
				}
				if (t.getObject().isVariable()){
					cqVars.add(t.getObject().getName());
				}
			}
			ucqVars.add(cqVars);
		}
		if (right instanceof OpBGP){
			List<Triple> triple = ((OpBGP) right).getPattern().getList();
			HashSet<String> cqVars = new HashSet<>();
			for (Triple t : triple){
				if (t.getSubject().isVariable()){
					cqVars.add(t.getSubject().getName());
				}
				if (t.getPredicate().isVariable()){
					cqVars.add(t.getPredicate().getName());
				}
				if (t.getObject().isVariable()){
					cqVars.add(t.getObject().getName());
				}
			}
			ucqVars.add(cqVars);
		}
		return union;	
	}
	
	public class BGPComparator implements Comparator<OpBGP>{

		@Override
		public int compare(OpBGP o1, OpBGP o2) {
			BasicPattern bp1 = o1.getPattern();
			BasicPattern bp2 = o2.getPattern();
			int n = bp1.size() < bp2.size() ? bp1.size() : bp2.size();
			for (int i = 0; i < n; i++){
				if (compare(bp1.get(i),bp2.get(i)) == 0){
					continue;
				}
				else{
					return compare(bp1.get(i),bp2.get(i));
				}
			}
			return bp2.size() - bp1.size();
		}
		
		public int compare(Triple o1, Triple o2) {
			if (compareSubject(o1,o2) == 0){
				if (comparePredicate(o1,o2) == 0){
					return compareObject(o1,o2);
				}
				else{
					return comparePredicate(o1,o2);
				}
			}
			else{
				return compareSubject(o1,o2);
			}
		}
		
		public int compareSubject(Triple o1, Triple o2){
			if (o1.getSubject().isBlank()){
				if (o2.getSubject().isBlank()){
					return o1.getSubject().getBlankNodeLabel().compareTo(o2.getSubject().getBlankNodeLabel());
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getSubject().isBlank()){
					return -1;
				}
				else{
					return o1.getSubject().toString().compareTo(o2.getSubject().toString());
				}
			}
		}
		public int comparePredicate(Triple o1, Triple o2){
			if (o1.getPredicate().isBlank()){
				if (o2.getPredicate().isBlank()){
					return o1.getPredicate().getBlankNodeLabel().compareTo(o2.getPredicate().getBlankNodeLabel());
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getPredicate().isBlank()){
					return -1;
				}
				else{
					return o1.getPredicate().toString().compareTo(o2.getPredicate().toString());
				}
			}
		}
		public int compareObject(Triple o1, Triple o2){
			if (o1.getObject().isBlank()){
				if (o2.getObject().isBlank()){
					return o1.getObject().getBlankNodeLabel().compareTo(o2.getObject().getBlankNodeLabel());
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getObject().isBlank()){
					return -1;
				}
				else{
					return o1.getObject().toString().compareTo(o2.getObject().toString());
				}
			}
		}		
	}
	
	public class TripleComparator implements Comparator<Triple>{

		@Override
		public int compare(Triple o1, Triple o2) {
			if (compareSubject(o1,o2) == 0){
				if (comparePredicate(o1,o2) == 0){
					return compareObject(o1,o2);
				}
				else{
					return comparePredicate(o1,o2);
				}
			}
			else{
				return compareSubject(o1,o2);
			}
		}
		
		public int compareSubject(Triple o1, Triple o2){
			if (o1.getSubject().isBlank()){
				if (o2.getSubject().isBlank()){
					return o1.getSubject().getBlankNodeLabel().compareTo(o2.getSubject().getBlankNodeLabel());
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getSubject().isBlank()){
					return -1;
				}
				else{
					return o1.getSubject().toString().compareTo(o2.getSubject().toString());
				}
			}
		}
		public int comparePredicate(Triple o1, Triple o2){
			if (o1.getPredicate().isBlank()){
				if (o2.getPredicate().isBlank()){
					return o1.getPredicate().getBlankNodeLabel().compareTo(o2.getPredicate().getBlankNodeLabel());
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getPredicate().isBlank()){
					return -1;
				}
				else{
					return o1.getPredicate().toString().compareTo(o2.getPredicate().toString());
				}
			}
		}
		public int compareObject(Triple o1, Triple o2){
			if (o1.getObject().isBlank()){
				if (o2.getObject().isBlank()){
					return o1.getObject().getBlankNodeLabel().compareTo(o2.getObject().getBlankNodeLabel());
				}
				else{
					return 1;
				}
			}
			else{
				if (o2.getObject().isBlank()){
					return -1;
				}
				else{
					return o1.getObject().toString().compareTo(o2.getObject().toString());
				}
			}
		}
	}
	
	public static void main(String[] args) throws InterruptedException, HashCollisionException{
		String q = "PREFIX : <http://example.org/> SELECT DISTINCT ?a WHERE{	{ ?a :p ?b } UNION { ?a :p ?c . ?a :p ?d } .	{ ?a :p ?b } UNION { ?d :p ?c } .	{ ?a :p ?b } UNION { ?e :p ?a } . { ?e :p ?a } UNION { ?e :p ?c } .}";
		@SuppressWarnings("unused")
		SingleQuery sq = new SingleQuery(q, true, true);
	}

}
