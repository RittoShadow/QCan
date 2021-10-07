package cl.uchile.dcc.qcan.op;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.OpBase;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

public class OpMix extends OpBase {

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public void visit(OpVisitor opVisitor) {

    }

    @Override
    public boolean equalTo(Op op, NodeIsomorphismMap nodeIsomorphismMap) {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }
}
