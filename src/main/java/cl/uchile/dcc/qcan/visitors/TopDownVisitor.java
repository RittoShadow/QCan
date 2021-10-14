package cl.uchile.dcc.qcan.visitors;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;

import java.util.ArrayList;
import java.util.List;

public abstract class TopDownVisitor {
    public TopDownVisitor() {

    }

    public Op visit(Op op) {
        if (op instanceof Op1) {
            return visit((Op1) op);
        }
        else if (op instanceof Op2) {
            return this.visit((Op2) op);
        }
        else if (op instanceof OpN) {
            return this.visit((OpN) op);
        }
        else if (op instanceof Op0) {
            return this.visit((Op0) op);
        }
        else {
            return op;
        }
    }

    public Op visit(Op1 op) {
        if (op instanceof OpExtendAssign) {
            return visit((OpExtendAssign) op);
        }
        else if (op instanceof OpFilter) {
            return visit((OpFilter) op);
        }
        else if (op instanceof OpGraph) {
            return visit((OpGraph) op);
        }
        else if (op instanceof OpGroup) {
            return visit((OpGroup) op);
        }
        else if (op instanceof OpLabel) {
            return visit((OpLabel) op);
        }
        else if (op instanceof OpProcedure) {
            return visit((OpProcedure) op);
        }
        else if (op instanceof OpPropFunc) {
            return visit((OpPropFunc) op);
        }
        else if (op instanceof OpService) {
            return visit((OpService) op);
        }
        else if (op instanceof OpDistinct) {
            return visit((OpDistinct) op);
        }
        else if (op instanceof OpReduced) {
            return visit((OpReduced) op);
        }
        else if (op instanceof OpList) {
            return visit((OpList) op);
        }
        else if (op instanceof OpProject) {
            return visit((OpProject) op);
        }
        else if (op instanceof OpSlice) {
            return visit((OpSlice) op);
        }
        else if (op instanceof OpTopN) {
            return visit((OpTopN) op);
        }
        return op;
    }

    public Op visit(Op2 op) {
        if (op instanceof OpUnion) {
            return visit((OpUnion) op);
        }
        else if (op instanceof OpConditional) {
            return visit((OpConditional) op);
        }
        else if (op instanceof OpDiff) {
            return visit((OpDiff) op);
        }
        else if (op instanceof OpLeftJoin) {
           return visit((OpLeftJoin) op);
        }
        else if (op instanceof OpMinus) {
            return visit((OpMinus) op);
        }
        else if (op instanceof OpJoin) {
            return visit((OpJoin) op);
        }
        else {
            return op;
        }
    }

    public Op visit(Op0 op) {
        if (op instanceof OpBGP) {
            return visit((OpBGP) op);
        }
        else if (op instanceof OpPath) {
            return visit((OpPath) op);
        }
        else if (op instanceof OpTable) {
            return visit((OpTable) op);
        }
        else if (op instanceof OpTriple) {
            return visit((OpTriple) op);
        }
        else {
            return op;
        }
    }

    public Op visit(OpBGP op) {
        return new OpBGP(op.getPattern());
    }

    public Op visit(OpTriple op) {
        return new OpTriple(op.getTriple());
    }

    public Op visit(OpPath op) {
        return new OpPath(op.getTriplePath());
    }

    public Op visit(OpTable op) {
        return OpTable.create(op.getTable());
    }

    public Op visit(OpN op) {
        if (op instanceof OpSequence) {
            return visit((OpSequence) op);
        }
        else if (op instanceof OpDisjunction) {
            return visit((OpDisjunction) op);
        }
        else {
            return op;
        }
    }

    public Op visit(OpSequence op) {
        List<Op> ops = op.getElements();
        List<Op> newOps = new ArrayList<>();
        for (Op o : ops) {
            newOps.add(visit(o));
        }
        Op ans = OpSequence.create();
        for (Op o : newOps) {
            ((OpSequence) ans).add(o);
        }
        return ans;
    }

    public Op visit(OpConditional op) {
        return new OpConditional(visit(op.getLeft()),visit(op.getRight()));
    }

    public Op visit(OpDisjunction op) {
        List<Op> ops = op.getElements();
        List<Op> newOps = new ArrayList<>();
        for (Op o : ops) {
            newOps.add(visit(o));
        }
        Op ans = OpDisjunction.create();
        for (Op o : newOps) {
            ((OpDisjunction) ans).add(o);
        }
        return ans;
    }

    public Op visit(OpProject op) {
        return new OpProject(visit(op.getSubOp()),op.getVars());
    }

    public Op visit(OpExtendAssign op) {
        if (op instanceof OpExtend) {
            return visit((OpExtend) op);
        }
        else if (op instanceof OpAssign) {
           return visit((OpAssign) op);
        }
        return op;
    }

    public Op visit(OpExtend op) {
        return OpExtend.create(visit(op.getSubOp()),op.getVarExprList());
    }

    public Op visit(OpAssign op) {
        return OpAssign.create(visit(op.getSubOp()), op.getVarExprList());
    }

    public Op visit(OpFilter op) {
        return OpFilter.filterAlways(op.getExprs(),visit(op.getSubOp()));
    }

    public Op visit(OpGraph op) {
        return new OpGraph(op.getNode(),visit(op.getSubOp()));
    }

    public Op visit(OpGroup op) {
        return new OpGroup(visit(op.getSubOp()), op.getGroupVars(), op.getAggregators());
    }

    public Op visit(OpLabel op) {
        return OpLabel.create(op.getObject(),visit(op.getSubOp()));
    }

    public Op visit(OpProcedure op) {
        return new OpProcedure(op.getProcId(),op.getArgs(),visit(op.getSubOp()));
    }

    public Op visit(OpPropFunc op) {
        return new OpPropFunc(op.getProperty(), op.getSubjectArgs(), op.getObjectArgs(), visit(op.getSubOp()));
    }

    public Op visit(OpService op) {
        if (op.getServiceElement() != null) {
            return new OpService(op.getService(),visit(op.getSubOp()), op.getServiceElement(), op.getSilent());
        }
        else {
            return new OpService(op.getService(),visit(op.getSubOp()), op.getSilent());
        }
    }

    public Op visit(OpDistinct op) {
        return new OpDistinct(visit(op.getSubOp()));
    }

    public Op visit(OpReduced op) {
        return OpReduced.create(visit(op.getSubOp()));
    }

    public Op visit(OpList op) {
        return new OpList(visit(op.getSubOp()));
    }

    public Op visit(OpSlice op) {
        return new OpSlice(visit(op.getSubOp()), op.getStart(), op.getLength());
    }

    public Op visit(OpTopN op) {
        return new OpTopN(visit(op.getSubOp()), op.getLimit(), op.getConditions());
    }

    public Op visit(OpUnion op) {
        return new OpUnion(visit(op.getLeft()), visit(op.getRight()));
    }

    public Op visit(OpJoin op){
        return OpJoin.create(visit(op.getLeft()),visit(op.getRight()));
    }

    public Op visit(OpDiff op) {
        return OpDiff.create(visit(op.getLeft()), visit(op.getRight()));
    }

    public Op visit(OpLeftJoin op) {
        return OpLeftJoin.create(visit(op.getLeft()), visit(op.getRight()), op.getExprs());
    }

    public Op visit(OpMinus op) {
        return OpMinus.create(visit(op.getLeft()), visit(op.getRight()));
    }
}
