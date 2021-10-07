package cl.uchile.dcc.qcan.generate;

import cl.uchile.dcc.blabel.label.GraphColouring;
import cl.uchile.dcc.qcan.builder.QueryBuilder;
import cl.uchile.dcc.qcan.builder.RGraphBuilder;
import cl.uchile.dcc.qcan.main.RGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class WellDesignedPatternGenerator extends Generator {
    protected Node predicate = NodeFactory.createURI("p");

    public WellDesignedPatternGenerator(File f) throws FileNotFoundException {
        super(f);
    }

    @Override
    void generateTriples() throws IOException {
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

    @Override
    RGraph generateGraph() throws GraphColouring.HashCollisionException, InterruptedException {
        Op op = null;
        for (Triple t : triples) {
            Node subject = t.getSubject();
            Node object = t.getObject();
            BasicPattern bp = new BasicPattern();
            bp.add(t);
            bp.add(Triple.create(subject,NodeFactory.createURI("q"),object));
            Op leftJoin = OpLeftJoin.createLeftJoin(new OpBGP(bp),new OpTriple(Triple.create(subject,NodeFactory.createURI("r"),object)),null);
            if (op == null) {
                op = leftJoin;
            }
            else {
                op = OpJoin.create(op,leftJoin);
            }
        }
        RGraphBuilder rGraphBuilder = new RGraphBuilder(op);
        RGraph e = rGraphBuilder.getResult();
        return e.getCanonicalForm(false);
    }

    public static void main(String[] args) throws IOException, GraphColouring.HashCollisionException, InterruptedException {
        WellDesignedPatternGenerator wellDesignedPatternGenerator = new WellDesignedPatternGenerator(new File("eval/k/k-8"));
        wellDesignedPatternGenerator.generateTriples();
        RGraph rGraph = wellDesignedPatternGenerator.generateGraph();
        QueryBuilder queryBuilder = new QueryBuilder(rGraph);
        System.out.println(queryBuilder.getOp());
        System.exit(1);
    }
}
