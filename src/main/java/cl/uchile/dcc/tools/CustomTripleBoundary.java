package cl.uchile.dcc.tools;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.TripleBoundary;

import java.util.List;

public class CustomTripleBoundary implements TripleBoundary {
    List<Node> projectionNodes;
    Node current;

    public CustomTripleBoundary(List<Node> list, Node n) {
        this.projectionNodes = list;
        this.current = n;
    }
    @Override
    public boolean stopAt(Triple triple) {
        if (projectionNodes.contains(triple.getObject()) && !triple.getObject().equals(current)) {
            return true;
        }
        else {
            return false;
        }
    }
}
