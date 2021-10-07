package cl.uchile.dcc.qcan.generate;

import cl.uchile.dcc.blabel.label.GraphColouring;
import cl.uchile.dcc.qcan.main.RGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public abstract class Generator {
    BufferedReader br;
    protected HashMap<Integer, Node> hm = new HashMap<>();
    List<Triple> triples = new ArrayList<>();
    Random rng = new Random();
    List<Var> vars = new ArrayList<>();

    Generator(File f) throws FileNotFoundException {
        this.br = new BufferedReader(new FileReader(f));
    }

    abstract void generateTriples() throws IOException;

    abstract RGraph generateGraph() throws GraphColouring.HashCollisionException, InterruptedException;


}
