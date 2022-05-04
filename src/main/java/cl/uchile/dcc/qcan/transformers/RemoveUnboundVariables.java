package cl.uchile.dcc.qcan.transformers;

import cl.uchile.dcc.qcan.tools.OpUtils;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.core.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RemoveUnboundVariables extends TransformCopy {

    @Override
    public Op transform(OpProject opProject, Op subOp) {
        List<Var> vars = opProject.getVars();
        List<Var> newVars = new ArrayList<>();
        Set<Var> subVars = OpUtils.allVarsContainedIn(subOp);
        for (Var var : vars) {
            if (subVars.contains(var)) {
                newVars.add(var);
            }
        }
        return new OpProject(subOp,newVars);
    }
}
