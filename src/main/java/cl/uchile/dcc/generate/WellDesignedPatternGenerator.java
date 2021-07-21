package cl.uchile.dcc.generate;

import cl.uchile.dcc.main.RGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Var;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class WellDesignedPatternGenerator extends Generator {
    protected Node predicate;

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
    RGraph generateGraph() {
        Op op = null;
        
        return null;
    }
}
