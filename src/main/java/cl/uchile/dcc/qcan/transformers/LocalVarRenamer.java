package cl.uchile.dcc.qcan.transformers;

import cl.uchile.dcc.qcan.tools.OpUtils;
import cl.uchile.dcc.qcan.visitors.OpRenamer;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Exists;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.graph.NodeTransformLib;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class LocalVarRenamer extends TransformCopy {
    Collection<Var> pVars = Collections.emptyList();

    public LocalVarRenamer() {

    }

    public LocalVarRenamer(Collection<Var> pVars) {
        this.pVars = pVars;
    }

    public Op transform(OpFilter op, Op subOp) {
        Op ans = op;
        if (!op.getExprs().isEmpty()) {
            Set<Var> vars = OpUtils.varsContainedIn(subOp);
            Expr expr = op.getExprs().get(0);
            if (expr instanceof E_Exists) {
                Op op1 = ((E_Exists) expr).getGraphPattern();
                OpRenamer mnt = new OpRenamer(vars);
                op1 = NodeTransformLib.transform(mnt,op1);
                return OpFilter.filter(new E_Exists(op1),subOp);
            }
            else if (expr instanceof E_NotExists) {
                Op op1 = ((E_NotExists) expr).getGraphPattern();
                OpRenamer mnt = new OpRenamer(vars);
                op1 = NodeTransformLib.transform(mnt,op1);
                return OpFilter.filter(new E_NotExists(op1),subOp);
            }
        }

        return ans;
    }

    public Op transform(OpMinus op, Op left, Op right) {
        Op ans = op;
        boolean rename = false;
        Set<Var> leftVars = OpUtils.varsContainedIn(left);
        Set<Var> rightVars = OpUtils.varsContainedIn(right);
        for (Var v : rightVars) {
            if (!leftVars.contains(v)) {
                rename = true;
            }
        }
        if (rename) {
            OpRenamer mnt = new OpRenamer(leftVars);
            return OpMinus.create(left,NodeTransformLib.transform(mnt,right));
        }
        return ans;
    }
}
