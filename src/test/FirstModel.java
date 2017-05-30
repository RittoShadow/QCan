package test;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;

public class FirstModel {
	
	String personURI = "http://somewhere/JohnSmith";
	String givenName = "John";
	String familyName = "Smith";
	String fullName = givenName + " " + familyName;
	
	Model model = ModelFactory.createDefaultModel();
	
	public FirstModel(){
		model.read("datos.ttl");
	}
	public static void main(String[] args){
		FirstModel fm = new FirstModel();
		StmtIterator ni = fm.model.listStatements();
		while(ni.hasNext()){
			System.out.println(ni.next().toString());
		}
		ni.close();
	}

}
