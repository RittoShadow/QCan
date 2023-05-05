package cl.uchile.dcc.qcan.generate;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.qcan.builder.RGraphBuilder;
import cl.uchile.dcc.qcan.main.RGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

public class BGPGenerator extends Generator {
	protected Node predicate;
	
	public BGPGenerator(File f) throws FileNotFoundException{
		super(f);
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
		BasicPattern bp = new BasicPattern();
		for (Triple triple : triples) {
			bp.add(triple);
		}
		Op op = new OpBGP(bp);
		Collections.shuffle(vars);
		op = OpDistinct.create(new OpProject(op,Collections.singletonList(vars.get(0))));
		RGraphBuilder rGraphBuilder = new RGraphBuilder(op);
		RGraph ans = rGraphBuilder.getResult();
//		ans.project(Utils.randomSample(vars));
//		ans.setDistinctNode(true);
//		ans.turnDistinctOn();
		return ans;
	}
	
	public void printStats(String string) throws InterruptedException, HashCollisionException, IOException{
		int initialNodes, finalNodes, initialVars, finalVars, initialTriples, finalTriples;
		generateTriples();
		RGraph e = generateGraph();	
		String output = string + "\t";
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
	
	public static void main(String[] args){
		try {
			File file = new File(args[0]);
			BGPGenerator g = new BGPGenerator(file);
			g.printStats(file.getName());
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

}
