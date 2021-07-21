package cl.uchile.dcc.visitors;

import org.apache.jena.atlas.lib.RandomLib;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.NodeTransform;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OpRenamer implements NodeTransform {
    public int id = 0;
    public Collection<Var> vars;
    public Map<Var,Var> varMap = new HashMap<>();

    public OpRenamer(Collection<Var> vars) {
        this.id = RandomLib.qrandom.nextInt();
        this.vars = vars;
    }

    public OpRenamer(Map<Var,Var> varMap) {
        this.varMap = varMap;
    }

    @Override
    public Node apply(Node n) {
        if (n.isVariable()) {
            if (varMap.containsKey(Var.alloc(n))) {
                return varMap.get(Var.alloc(n));
            } else if (vars != null && !vars.contains(n)) {
                return Var.alloc(n.getName() + id);
            }
        }
        return n;
    }
}
