package cl.uchile.dcc.op;

import cl.uchile.dcc.builder.RGraphBuilder;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.Transform;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.walker.OpVisitorByTypeAndExpr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OpWDLeftJoin extends OpLeftJoin {
    private Op left = null;
    private List<Op> rightOps = new ArrayList<>();
    private ExprList exprList = null;

    protected OpWDLeftJoin(Op left, Op right, ExprList exprs) {
        super(left,right,exprs);
        this.left = left;
    }

    public OpWDLeftJoin(Op left, List<Op> rightOps, ExprList exprs) {
        super(left,rightOps.get(0),exprs);
        this.left = left;
        this.rightOps.addAll(rightOps);
        this.exprList = exprs;
    }

    public Op apply(Transform transform) {
        Op ans = null;
        Op leftOp = getLeft();
        if (leftOp instanceof OpBGP) {
            leftOp = transform.transform((OpBGP) leftOp);
        }
        else if (leftOp instanceof OpTriple) {
            leftOp = transform.transform((OpTriple)leftOp);
        }
        else if (leftOp instanceof OpFilter) {
            leftOp = transform.transform((OpFilter)leftOp,((OpFilter) leftOp).getSubOp());
        }
        List<Op> opList = new ArrayList<>();
        for (Op op : rightOps) {
            opList.add(op);
        }
        ans = new OpWDLeftJoin(left,opList,exprList);
        return ans;
    }

    public void add(Op op) {
        this.rightOps.add(op);
    }

    public void addAll(Collection<Op> ops) {
        this.rightOps.addAll(ops);
    }

    public Op getLeft() {
        return left;
    }

    public List<Op> getRightOps() {
        return rightOps;
    }

    public ExprList getExprs() {
        return exprList;
    }

    public static OpWDLeftJoin create(Op left, List<Op> rightOps, ExprList exprList) {
        return new OpWDLeftJoin(left,rightOps,exprList);
    }

    @Override
    public void visit(OpVisitor opVisitor) {
        if (opVisitor instanceof RGraphBuilder) {
            ((RGraphBuilder)opVisitor).visit(this);
        }
        else if (opVisitor instanceof OpVisitorByTypeAndExpr) {
            ((OpVisitorByTypeAndExpr)opVisitor).visitExpr(this.getExprs());
            getLeft().visit(opVisitor);
            for (Op op : getRightOps()) {
                op.visit(opVisitor);
            }

        }
        else {
            opVisitor.visit(this);
        }

    }

    public void visit2() {

    }

    @Override
    public boolean equalTo(Op op, NodeIsomorphismMap nodeIsomorphismMap) {
        return false;
    }

    @Override
    public String getName() {
        return "wd-leftjoin";
    }
}
