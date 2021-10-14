package cl.uchile.dcc.qcan.generate;

import cl.uchile.dcc.qcan.builder.RGraphBuilder;
import cl.uchile.dcc.qcan.main.RGraph;
import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.qcan.tools.Utils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;

import java.io.*;
import java.util.*;

public class  UCQGenerator extends Generator {
	protected List<Triple> conjunctions = new ArrayList<>();
	protected List<Triple> unions = new ArrayList<>();
	protected Node predicate;
	protected List<Node> predicateList = new ArrayList<>();
	protected int x = 0;
	protected int y = 0;
	protected int numberOfPredicates = 0;
	public long graphTime;
	public long rewriteTime;
	public long leaningTime;
	public long canonTime;
	
	public UCQGenerator(File f, int x, int y, int numberOfPredicates) throws FileNotFoundException{
		super(f);
		br = new BufferedReader(new FileReader(f));
		predicate = NodeFactory.createURI("http://example.org/p");
		this.numberOfPredicates = numberOfPredicates;
		predicateList.add(predicate);
		for (int i = 0; i < numberOfPredicates; i++) {
			predicateList.add(NodeFactory.createURI(predicate.getURI() + i));
		}
		this.x = x;
		this.y = y;
	}
	
	public void generateTriples() throws IOException{
		String s = br.readLine();
		while ((s = br.readLine()) != null){
			String[] coords = s.split(" ");
			int r = Math.abs(rng.nextInt((int) Math.pow(2, 20)));
			int v0 = Integer.valueOf(coords[1]);
			int v1 = Integer.valueOf(coords[2]);
			if (!hm.containsKey(v0)){
				hm.put(v0, NodeFactory.createVariable("v"+(v0+r)));
				vars.add(Var.alloc("v"+(v0+r)));		
			}
			r = Math.abs(rng.nextInt((int) Math.pow(2, 20)));
			if (!hm.containsKey(v1)){
				hm.put(v1, NodeFactory.createVariable("v"+(v1+r)));
				vars.add(Var.alloc("v"+(v1+r)));
			}
			int p = Math.abs(rng.nextInt(numberOfPredicates));
			triples.add(Triple.create(hm.get(v0), predicateList.get(p), hm.get(v1)));
		}
		br.close();
	}

	@Override
	RGraph generateGraph() {
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
		op = new OpProject(op, Utils.randomSample(vars));
		op = new OpDistinct(op);
		Query q = OpAsQuery.asQuery(op);
		RGraphBuilder visitor = new RGraphBuilder(q);
		RGraph ans = visitor.getResult();
		graphTime = visitor.graphTime;
		rewriteTime = visitor.rewriteTime;
		return ans;
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
	
	public static void main(String[] args) throws IOException, InterruptedException, HashCollisionException{
		UCQGenerator g = new UCQGenerator(new File("eval/k/k-12"),4,8,2);
		g.generateTriples();
		RGraph e = g.generateGraph();
		System.out.println(e.graph.size());
		System.out.println(e.getNumberOfTriples());
		RGraph a = e.getCanonicalForm(false);
		a.print();
		System.out.println(e.getNumberOfTriples());
		System.out.println(e.graph.size());
	}

}
