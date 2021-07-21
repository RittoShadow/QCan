package cl.uchile.dcc.transformers;

import cl.uchile.dcc.tools.Tools;
import com.github.jsonldjava.shaded.com.google.common.collect.Sets;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprList;

import java.util.Set;

public class WellDesignedTransformer extends TransformCopy {
    private Op op;

    public WellDesignedTransformer(Op op) {
        this.op = op;
    }

    @Override
    public Op transform(OpFilter opFilter, Op subOp) {
        if (subOp instanceof OpLeftJoin) {
            if (isWellDesigned(opFilter)) {
                return OpLeftJoin.create(OpFilter.filterAlways(opFilter.getExprs(),((OpLeftJoin) subOp).getLeft()),((OpLeftJoin) subOp).getRight(),((OpLeftJoin) subOp).getExprs());
            }
        }
        return OpFilter.filterAlways(opFilter.getExprs(),subOp);
    }

    @Override
    public Op transform(OpJoin opJoin, Op left, Op right) {
        if (left instanceof OpLeftJoin) {
            if (isWellDesigned(opJoin)) {
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
                    return OpLeftJoin.create(OpJoin.create(((OpLeftJoin) left).getLeft(),right),opJoin.getRight(),((OpLeftJoin) left).getExprs());
                }
            }
        }
        else if (right instanceof OpLeftJoin) {
            if (isWellDesigned(opJoin)) {
                return OpLeftJoin.create(OpJoin.create(left,((OpLeftJoin) right).getLeft()),((OpLeftJoin) right).getRight(),((OpLeftJoin) right).getExprs());
            }
        }
        return OpJoin.create(left,right);
    }

    public boolean isWellDesigned(Op op) {
        final boolean[] ans = {true};
        OpWalker.walk(op, new OpVisitor() {
            @Override
            public void visit(OpBGP opBGP) {

            }

            @Override
            public void visit(OpQuadPattern opQuadPattern) {

            }

            @Override
            public void visit(OpQuadBlock opQuadBlock) {

            }

            @Override
            public void visit(OpTriple opTriple) {

            }

            @Override
            public void visit(OpQuad opQuad) {

            }

            @Override
            public void visit(OpPath opPath) {

            }

            @Override
            public void visit(OpFind opFind) {

            }

            @Override
            public void visit(OpTable opTable) {

            }

            @Override
            public void visit(OpNull opNull) {

            }

            @Override
            public void visit(OpProcedure opProcedure) {

            }

            @Override
            public void visit(OpPropFunc opPropFunc) {

            }

            @Override
            public void visit(OpFilter opFilter) {
                Set<Var> subVars = Tools.varsContainedIn(opFilter.getSubOp());
                if (opFilter.getExprs() != null) {
                    if (opFilter.getExprs().getVarsMentioned() != null) {
                        Set<Var> filterVars = opFilter.getExprs().getVarsMentioned();
                        for (Var var : filterVars) {
                            if (!subVars.contains(var)) {
                                ans[0] = false;
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void visit(OpGraph opGraph) {

            }

            @Override
            public void visit(OpService opService) {

            }

            @Override
            public void visit(OpDatasetNames opDatasetNames) {

            }

            @Override
            public void visit(OpLabel opLabel) {

            }

            @Override
            public void visit(OpAssign opAssign) {

            }

            @Override
            public void visit(OpExtend opExtend) {

            }

            @Override
            public void visit(OpJoin opJoin) {

            }

            @Override
            public void visit(OpLeftJoin opLeftJoin) {
                Set<Var> outerVars = Tools.varsContainedInExcept(op,opLeftJoin);
                Set<Var> leftVars = Tools.varsContainedIn(opLeftJoin.getLeft());
                Set<Var> rightVars = Tools.varsContainedIn(opLeftJoin.getRight());
                Set<Var> intersection = Sets.intersection(outerVars,rightVars);
                for (Var var : intersection) {
                    if (!leftVars.contains(var)) {
                        ans[0] = false;
                        break;
                    }
                }
            }

            @Override
            public void visit(OpUnion opUnion) {
                ans[0] = false;
            }

            @Override
            public void visit(OpDiff opDiff) {

            }

            @Override
            public void visit(OpMinus opMinus) {

            }

            @Override
            public void visit(OpConditional opConditional) {

            }

            @Override
            public void visit(OpSequence opSequence) {

            }

            @Override
            public void visit(OpDisjunction opDisjunction) {

            }

            @Override
            public void visit(OpList opList) {

            }

            @Override
            public void visit(OpOrder opOrder) {

            }

            @Override
            public void visit(OpProject opProject) {

            }

            @Override
            public void visit(OpReduced opReduced) {

            }

            @Override
            public void visit(OpDistinct opDistinct) {

            }

            @Override
            public void visit(OpSlice opSlice) {

            }

            @Override
            public void visit(OpGroup opGroup) {

            }

            @Override
            public void visit(OpTopN opTopN) {

            }
        });
        return ans[0];
    }

}
