package cl.uchile.dcc.qcan.transformers;

import cl.uchile.dcc.qcan.visitors.TopDownVisitor;
import org.apache.jena.query.*;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.graph.NodeTransformLib;

import java.util.*;

public class BranchRenamer extends TopDownVisitor {
    Set<Var> varsInScope = new HashSet<>();
    private int projectionDepth = 0;

    public Op visit(OpProject project) {
        varsInScope.addAll(project.getVars());
        projectionDepth++;
        if (projectionDepth > 1) {
            OpRenamer or = new OpRenamer(varsInScope);
            Op op = visit(NodeTransformLib.transform(or,project.getSubOp()));
            return new OpProject(op,project.getVars());
        }
        else {
            return new OpProject(visit(project.getSubOp()), project.getVars());
        }

    }

    public Op visit(OpOrder order) {
        for (SortCondition sortCondition : order.getConditions()) {
            varsInScope.addAll(sortCondition.getExpression().getVarsMentioned());
        }
        return new OpOrder(visit(order.getSubOp()), order.getConditions());
    }

    public Op visit(OpFilter filter) {
        for (Expr expr : filter.getExprs()) {
            varsInScope.addAll(expr.getVarsMentioned());
        }
        return OpFilter.filterAlways(filter.getExprs(),visit(filter.getSubOp()));
    }

    public Op visit(OpExtend extend) {
        varsInScope.addAll(extend.getVarExprList().getVars());
        return OpExtend.create(visit(extend.getSubOp()), extend.getVarExprList());
    }

    public Op visit(OpGroup group) {
        for (ExprAggregator expr : group.getAggregators()) {
            if (expr.getAggregator().getExprList() != null) {
                Set<Var> vars = expr.getAggregator().getExprList().getVarsMentioned();
                varsInScope.addAll(vars);
            }
        }
        varsInScope.addAll(group.getGroupVars().getVars());
        return new OpGroup(visit(group.getSubOp()),group.getGroupVars(), group.getAggregators());
    }

    public Op visit(OpGraph graph) {
        if (graph.getNode().isVariable()) {
            varsInScope.add(Var.alloc(graph.getNode()));
        }
        return new OpGraph(graph.getNode(),visit(graph.getSubOp()));
    }

    public Op visit(OpService service) {
        if (service.getServiceElement() != null) {
            if (service.getService().isVariable()) {
                varsInScope.add(Var.alloc(service.getService()));
            }
            if (service.getServiceElement().getServiceNode().isVariable()) {
                varsInScope.add(Var.alloc(service.getServiceElement().getServiceNode()));
            }
            return new OpService(service.getService(),visit(service.getSubOp()),service.getServiceElement(),service.getSilent());
        }
        else {
            if (service.getService().isVariable()) {
                varsInScope.add(Var.alloc(service.getService()));
            }
            return new OpService(service.getService(),visit(service.getSubOp()),service.getSilent());
        }
    }

    public Op visit(OpLeftJoin leftJoin) {
        Op leftOp = leftJoin.getLeft();
        Op rightOp = leftJoin.getRight();
        return leftJoin;
    }

    public Op visit(OpUnion union) {
        List<Op> opsInUnion = opsInUnion(union);
        List<Op> newOps = new ArrayList<>();
        for (Op op : opsInUnion) {
            OpRenamer or = new OpRenamer(varsInScope);
            Op op1 = NodeTransformLib.transform(or,op);
            newOps.add(op1);
        }
        Op ans = newOps.get(0);
        for (int i = 1; i < newOps.size(); i++) {
            ans = OpUnion.create(ans,newOps.get(i));
        }
        return ans;
    }

    public List<Op> opsInUnion(Op op){
        List<Op> ans = new ArrayList<>();
        if (op instanceof OpUnion) {
            Op leftOp = ((OpUnion) op).getLeft();
            Op rightOp = ((OpUnion) op).getRight();
            if (leftOp instanceof OpUnion) {
                ans.addAll(opsInUnion(leftOp));
            }
            else {
                ans.add(leftOp);
            }
            if (rightOp instanceof OpUnion) {
                ans.addAll(opsInUnion(rightOp));
            }
            else {
                ans.add(rightOp);
            }
        }
        else {
            return ans;
        }
        return ans;
    }
}
