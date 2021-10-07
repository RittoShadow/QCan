package cl.uchile.dcc.qcan.tools;

import org.apache.jena.graph.Triple;

import java.util.Comparator;

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