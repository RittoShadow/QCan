package cl.uchile.dcc.op;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.Transform;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

public class OpEpsilon extends OpTriple {
    private Node starPredicate;

    public OpEpsilon(Node n0, Node predicate, Node n1) {
        super(Triple.create(n0,predicate,n1));
        this.starPredicate = predicate;
        if (predicate.isURI()) {
            this.starPredicate = NodeFactory.createURI(predicate.getURI() + "-star");
        }

    }

    @Override
    public Op apply(Transform transform) {
        return transform.transform(this);
    }

    @Override
    public OpEpsilon copy() {
        return new OpEpsilon(getN0(),super.getTriple().getPredicate(),getN1());
    }

    @Override
    public int hashCode() {
        return getN0().hashCode() + 97*getN1().hashCode();
    }

    @Override
    public void visit(OpVisitor opVisitor) {
        opVisitor.visit(this);
    }

    @Override
    public boolean equalTo(Op op, NodeIsomorphismMap nodeIsomorphismMap) {
        if (op instanceof OpEpsilon) {
            return nodeIsomorphismMap.makeIsomorphic(getN0(),getN1());
        }
        else {
            return false;
        }
    }

    @Override
    public String getName() {
        return "epsilon " + getN0().toString() + " " + getN0().toString();
    }

    public Node getN0() {
        return super.getTriple().getSubject();
    }

    public Node getN1() {
        return super.getTriple().getObject();
    }

    public Node getPredicate() {
        return starPredicate;
    }

    public Triple asTriple() {
        return Triple.create(getN0(),getPredicate(),getN1());
    }
}
