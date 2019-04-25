package generate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import main.RGraphBuilder;
import main.RGraph;

public class UCQGenerator {
	protected BufferedReader br;
	protected HashMap<Integer,Node> hm = new HashMap<Integer,Node>();
	protected List<Triple> triples = new ArrayList<Triple>();
	protected List<Triple> conjunctions = new ArrayList<Triple>();
	protected List<Triple> unions = new ArrayList<Triple>();
	protected Random rng = new Random();
	protected Node predicate;
	protected List<Var> vars = new ArrayList<Var>();
	
	public UCQGenerator(File f) throws FileNotFoundException{
		br = new BufferedReader(new FileReader(f));
		predicate = NodeFactory.createURI("http://example.org/p");
	}
	
	public void generateTriples() throws IOException{
		String s = br.readLine();
		while ((s = br.readLine()) != null){
			String[] coords = s.split(" ");
			int r = Math.abs(rng.nextInt((int) Math.pow(2, 20)));
			int v0 = new Integer(coords[1]);
			int v1 = new Integer(coords[2]);
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
	
	public void selectTriples(int a, int b){
		if (conjunctions.isEmpty()){
			for (Triple t : triples){
				conjunctions.add(t);
				unions.add(t);
			}
		}
		Collections.shuffle(conjunctions);
		Collections.shuffle(unions);
		conjunctions = conjunctions.subList(0, a);
		unions = unions.subList(0, b);
	}
	
	public List<Triple> generateUnion(int x){
		Collections.shuffle(triples);
		List<Triple> ans = triples.subList(0, x);
		return ans;
	}
	
	public RGraph generateGraph(int x, int y){
		Op op = null;
		for (int i = 0; i < x; i++){   
			List<Triple> u = generateUnion(y);
			BasicPattern b = new BasicPattern();
			b.add(u.get(0));
			Op op1 = new OpBGP(b);
			for (int k = 1; k < y; k++){
				BasicPattern b1 = new BasicPattern();
				b1.add(u.get(k));
				op1 = new OpUnion(op1, new OpBGP(b1));
			}   
			if (i == 0){
				op = op1;
			}
			else{
				op = OpJoin.create(op, op1);
			}
		}
		op = new OpProject(op, Arrays.asList(Var.alloc(triples.get(0).getSubject())));
		op = new OpDistinct(op);
		Query q = OpAsQuery.asQuery(op); 
		RGraphBuilder visitor = new RGraphBuilder(q);
		OpWalker.walk(op, visitor);       
		RGraph ans = visitor.getResult();
		
		return ans;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, HashCollisionException{
		UCQGenerator g = new UCQGenerator(new File("eval/k/k-6"));
		g.generateTriples();
		RGraph e = g.generateGraph(4,2);
		e.print();
		RGraph a = e.getCanonicalForm(false);
		a.print();
		System.out.println("");
	}

}
