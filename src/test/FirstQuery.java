package test;

import java.util.Iterator;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

public class FirstQuery {
	
	String qString = "";
	Query query;
	Model model;
	
	public FirstQuery(String q){	
		this.model = new FirstModel().model;
		Map<String,String> pm = model.getNsPrefixMap();
		for (String i : pm.keySet()){
			this.qString = this.qString + "PREFIX " + i + ":<" + pm.get(i) + ">\n";
		}
		this.qString = this.qString + q;
		this.query = QueryFactory.create(qString);
//		for (String s : op.toString().replace("\n", "").split("[()]")){
//			System.out.println("+ " + s);
//		}
		ElementWalker.walk(this.query.getQueryPattern(),
			    // For each element...
			    new ElementVisitorBase() {
			        // ...when it's a block of triples...
			        public void visit(ElementPathBlock el) {
			            // ...go through all the triples...
			            Iterator<TriplePath> triples = el.patternElts();
			            while (triples.hasNext()) {
			                // ...and grab the subject
			                System.out.println(triples.next());
			            }
			        }
			    }
			);
	}
	
	public void execQuery(boolean b){	
		try (QueryExecution qexec = QueryExecutionFactory.create(query,model)){
			ResultSet results = qexec.execSelect();
			while(results.hasNext()){
				QuerySolution soln = results.nextSolution();
				if (b){
					System.out.println(soln.toString());
				}
			}
			qexec.close();
		}
	}
	
	public static void main(String[] args){
		FirstQuery fq = new FirstQuery("SELECT ?s ?m WHERE {?s :auxiliarOf ?o . OPTIONAL {?s :hasAdvisor ?t} . ?m :studentOf ?o }");
		fq.execQuery(false);
	}
}
