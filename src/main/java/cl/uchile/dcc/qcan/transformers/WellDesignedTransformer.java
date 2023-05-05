package cl.uchile.dcc.qcan.transformers;

import cl.uchile.dcc.qcan.tools.OpUtils;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.expr.ExprList;

public class WellDesignedTransformer extends TransformCopy {
    private Op op;

    public WellDesignedTransformer(Op op) {
        this.op = op;
    }

    @Override
    public Op transform(OpFilter opFilter, Op subOp) {
        if (subOp instanceof OpLeftJoin) {
            if (OpUtils.isWellDesigned(opFilter)) {
                return OpLeftJoin.create(OpFilter.filterAlways(opFilter.getExprs(),((OpLeftJoin) subOp).getLeft()),((OpLeftJoin) subOp).getRight(),((OpLeftJoin) subOp).getExprs());
            }
        }
        return OpFilter.filterAlways(opFilter.getExprs(),subOp);
    }

    @Override
    public Op transform(OpJoin opJoin, Op left, Op right) {
        if (left instanceof OpLeftJoin) {
            if (OpUtils.isWellDesigned(opJoin)) {
                if (right instanceof OpLeftJoin) {
                    Op ans = OpJoin.create(((OpLeftJoin) left).getLeft(),((OpLeftJoin) right).getLeft());
                    ExprList exprList = new ExprList();
                    if (((OpLeftJoin) left).getExprs() != null) {
                        exprList.addAll(((OpLeftJoin) left).getExprs());
                    }
                    if (((OpLeftJoin) right).getExprs() != null) {
                        exprList.addAll(((OpLeftJoin) right).getExprs());
                    }
                    ans = OpLeftJoin.create(ans,((OpLeftJoin) left).getRight(),((OpLeftJoin) left).getExprs());
                    ans = OpLeftJoin.create(ans,((OpLeftJoin) right).getRight(),((OpLeftJoin) right).getExprs());
                    return ans;
                }
                else {
                    return OpLeftJoin.create(OpJoin.create(((OpLeftJoin) left).getLeft(),right),((OpLeftJoin) left).getRight(),((OpLeftJoin) left).getExprs());
                }
            }
        }
        else if (right instanceof OpLeftJoin) {
            if (OpUtils.isWellDesigned(opJoin)) {
                return OpLeftJoin.create(OpJoin.create(left,((OpLeftJoin) right).getLeft()),((OpLeftJoin) right).getRight(),((OpLeftJoin) right).getExprs());
            }
        }
        return OpJoin.create(left,right);
    }
}
