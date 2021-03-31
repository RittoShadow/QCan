package visitors;

import org.apache.jena.atlas.lib.RandomLib;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.NodeTransform;

import java.util.Collection;

public class OpRenamer implements NodeTransform {
    public int id = 0;
    public Collection<Var> vars;

    public OpRenamer(Collection<Var> vars) {
        this.id = RandomLib.qrandom.nextInt();
        this.vars = vars;
    }

    @Override
    public Node apply(Node n) {
        if (n.isVariable() && !vars.contains(n)) {
            return Var.alloc(n.getName() + id);
        }
        else {
            return n;
        }
    }
}
