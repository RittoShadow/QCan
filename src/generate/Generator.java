package generate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;

import test.ExpandedGraph;

public class Generator {
	protected BufferedReader br;
	protected HashMap<Integer,Node> hm = new HashMap<Integer,Node>();
	protected List<Triple> triples = new ArrayList<Triple>();
	protected Random rng = new Random();
	protected Node predicate;
	protected List<Var> vars = new ArrayList<Var>();
	
	public Generator(File f) throws FileNotFoundException{
		br = new BufferedReader(new FileReader(f));
		predicate = NodeFactory.createURI("p");
	}
	
	public void generateTriples() throws IOException{
		String s = br.readLine();
		while ((s = br.readLine()) != null){
			String[] coords = s.split(" ");
			int r = rng.nextInt();
			int v0 = new Integer(coords[1]);
			int v1 = new Integer(coords[2]);
			if (!hm.containsKey(v0)){
				hm.put(v0, NodeFactory.createVariable("v"+(v0+r)));
				vars.add(Var.alloc("v"+(v0+r)));		
			}
			r = rng.nextInt();
			if (!hm.containsKey(v1)){
				hm.put(v1, NodeFactory.createVariable("v"+(v1+r)));
				vars.add(Var.alloc("v"+(v1+r)));
			}
			triples.add(Triple.create(hm.get(v0), predicate, hm.get(v1)));
		}
		br.close();
	}
	
	public ExpandedGraph generateGraph(){
		return new ExpandedGraph(this.triples,this.vars);
	}
	
	public static void main(String[] args) throws IOException{
		Generator g = new Generator(new File("src/eval/k/k-4"));
		g.generateTriples();
		ExpandedGraph eg = g.generateGraph();
		eg.print();
	}

}
