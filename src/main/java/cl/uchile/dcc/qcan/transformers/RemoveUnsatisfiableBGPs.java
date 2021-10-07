package cl.uchile.dcc.qcan.transformers;

import cl.uchile.dcc.qcan.tools.OpUtils;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.*;

import java.util.HashSet;
import java.util.Set;

public class RemoveUnsatisfiableBGPs extends TransformCopy {

    @Override
    public Op transform(OpUnion union, Op left, Op right) {
        Op ans = OpNull.create();
        Set<Op> opsInUnion = OpUtils.opsInUnion(union);
        Set<Op> newOps = new HashSet<>();
        for (Op op : opsInUnion) {
            if (OpUtils.isSatisfiable(op)) {
                newOps.add(op);
            }
        }
        if (!newOps.isEmpty()) {
            for (Op op : newOps) {
                if (ans instanceof OpNull) {
                    ans = op;
                }
                else {
                    ans = OpUnion.create(ans,op);
                }
            }
        }
        return ans;
    }

    @Override
    public Op transform(OpJoin join, Op left, Op right) {
        if (OpUtils.isSatisfiable(join)) {
            return OpJoin.create(left, right);
        }
        else {
            return OpNull.create();
        }
    }

    @Override
    public Op transform(OpBGP bgp) {
        if (OpUtils.isSatisfiable(bgp)) {
            return bgp;
        }
        else {
            return OpNull.create();
        }
    }

    @Override
    public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) {
        if (OpUtils.isSatisfiable(left)) {
            if (OpUtils.isSatisfiable(right)) {
                return OpLeftJoin.create(left,right,opLeftJoin.getExprs());
            }
            else {
                return left;
            }
        }
        else {
            return OpNull.create();
        }
    }

    @Override
    public Op transform(OpFilter opFilter, Op subOp) {
        if (OpUtils.isSatisfiable(subOp)) {
            return OpFilter.filterAlways(opFilter.getExprs(),subOp);
        }
        else {
            return OpNull.create();
        }
    }
}
