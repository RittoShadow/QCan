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

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import main.RGraph;

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
			int r = Math.abs(rng.nextInt((int) Math.pow(2, 20)));
			int v0 = Integer.parseInt(coords[1]);
			int v1 = Integer.parseInt(coords[2]);
			if (!hm.containsKey(v0)){
				hm.put(v0, NodeFactory.createVariable("v"+(v0+r)));
				vars.add(Var.alloc("v"+(v0+r)));		
			}
			r = Math.abs(rng.nextInt((int) Math.pow(2, 20)));
			if (!hm.containsKey(v1)){
				hm.put(v1, NodeFactory.createVariable("v"+(v1+r)));
				vars.add(Var.alloc("v"+(v1+r)));
			}
			triples.add(Triple.create(hm.get(v0), predicate, hm.get(v1)));
		}
		br.close();
	}
	
	public RGraph generateGraph(){
		RGraph ans = new RGraph(this.triples,this.vars);
		ans.project(vars.subList(0, 1));
		ans.setDistinctNode(true);
		ans.turnDistinctOn();
		return ans;
	}
	
	public void printStats() throws InterruptedException, HashCollisionException, IOException{
		int initialNodes, finalNodes, initialVars, finalVars, initialTriples, finalTriples;
		generateTriples();
		RGraph e = generateGraph();	
		String output = "";
		initialNodes = e.getNumberOfNodes();
		initialVars = e.getNumberOfVars();
		initialTriples = e.getNumberOfTriples();
		long t = System.nanoTime();
		RGraph a = e.getCanonicalForm(false);
		t = System.nanoTime() - t;
		finalNodes = a.getNumberOfNodes();
		finalVars = a.getNumberOfVars();
		finalTriples = a.getNumberOfTriples();
		output += t + "\t";
		output += initialNodes + "\t";
		output += finalNodes + "\t";
		output += initialVars + "\t";
		output += finalVars + "\t";
		output += initialTriples +"\t";
		output += finalTriples;
		System.out.println(output);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, HashCollisionException{
		Generator g = new Generator(new File("eval/lattice/lattice-4"));
		g.printStats();
	}

}
