package test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.graph.GraphFactory;
import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;


public class SparqlModel {
	
	Model model;
	Graph graph = GraphFactory.createDefaultGraph();
	List<ExpandedGraph> graphs;
	
	public SparqlModel(ParseQuery pq){
		graphs = pq.getGraphList();
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException, HashCollisionException{
		File f = new File("testFiles/test09.txt");
		SparqlModel spqm = new SparqlModel(new ParseQuery(f, ""));
		for (ExpandedGraph e : spqm.graphs){
			ExpandedGraph e1 = e.getCanonicalForm(false);
			e1.print();
			System.out.println("\n");
			e1.printQuery();
			System.out.println("\n");
			
		}
//		System.out.println("Let's see what happens with a union: \n");
//		spqm.graphs[1].union(spqm.graphs[0]);
//		System.out.println(spqm.graphs[1]);
	}

}
