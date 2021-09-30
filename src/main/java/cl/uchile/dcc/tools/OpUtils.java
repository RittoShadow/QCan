package cl.uchile.dcc.tools;

import cl.uchile.dcc.transformers.FilterTransform;
import cl.uchile.dcc.transformers.NotOneOfTransform;
import cl.uchile.dcc.transformers.TransformPath;
import cl.uchile.dcc.transformers.UCQTransformer;
import com.google.common.collect.Sets;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;

import java.util.*;

public class OpUtils {

    public static Set<Var> safeVars(Op op) {
        Set<Var> ans = new HashSet<>();
        if (op instanceof OpBGP) {
            BasicPattern bp = ((OpBGP) op).getPattern();
            for (Triple t : bp.getList()) {
                ans.addAll(safeVars(new OpTriple(t)));
            }
        }
        else if (op instanceof OpTriple) {
            ans.addAll(varsContainedIn(op));
        }
        else if (op instanceof OpUnion) {
            Op left = ((OpUnion) op).getLeft();
            Op right = ((OpUnion) op).getRight();
            Set<Var> leftVars = safeVars(left);
            Set<Var> rightVars = safeVars(right);
            for (Var v : leftVars) {
                if (rightVars.contains(v)) {
                    ans.add(v);
                }
            }
        }
        else if (op instanceof OpMinus) {
            return safeVars(((OpMinus) op).getLeft());
        }
        else if (op instanceof OpLeftJoin) {
            return safeVars(((OpLeftJoin) op).getLeft());
        }
        else if (op instanceof OpProject) {
            Op sub = ((OpProject) op).getSubOp();
            Set<Var> subVars = safeVars(sub);
            List<Var> pVars = ((OpProject) op).getVars();
            for (Var v : subVars) {
                if (pVars.contains(v)) {
                    ans.add(v);
                }
            }
        }
        else if (op instanceof OpGroup) {
            Op sub = ((OpGroup) op).getSubOp();
            Set<Var> subVars = safeVars(sub);
            List<Var> gVars = ((OpGroup) op).getGroupVars().getVars();
            for (Var v : subVars) {
                if (gVars.contains(v)) {
                    ans.add(v);
                }
            }
        }
        else if (op instanceof OpFilter) {
            return safeVars(((OpFilter) op).getSubOp());
        }
        else if (op instanceof OpExtend) {
            return safeVars(((OpExtend) op).getSubOp());
        }
        else if (op instanceof OpTable) {
            ans.addAll(((OpTable) op).getTable().getVars());
        }
        else if (op instanceof OpGraph) {
            Op sub = ((OpGraph) op).getSubOp();
            if (((OpGraph) op).getNode().isVariable()) {
                ans.add(Var.alloc(((OpGraph) op).getNode()));
            }
            ans.addAll(safeVars(sub));
        }
        else if (op instanceof OpService) {
            if (((OpService) op).getSilent()) {
                return safeVars(((OpService) op).getSubOp());
            }
            else {
                //TODO
            }
        }
        return ans;
    }

    public static Set<Var> varsContainedInExcept(Op op, Op exception) {
        Set<Var> ans = new HashSet<>();
        if (op.equals(exception)) {
            return ans;
        }
        if (op instanceof OpTriple) {
            Triple t = ((OpTriple) op).getTriple();
            if (t.getSubject().isVariable()) {
                if (!t.getSubject().getName().startsWith("?")) {
                    ans.add(Var.alloc(t.getSubject().getName()));
                }
            }
            if (t.getPredicate().isVariable()) {
                if (!t.getPredicate().getName().startsWith("?")) {
                    ans.add(Var.alloc(t.getPredicate().getName()));
                }
            }
            if (t.getObject().isVariable()) {
                if (!t.getObject().getName().startsWith("?")) {
                    ans.add(Var.alloc(t.getObject().getName()));
                }
            }
        } else if (op instanceof OpBGP) {
            for (Triple t : ((OpBGP) op).getPattern().getList()) {
                if (t.getSubject().isVariable()) {
                    if (!t.getSubject().getName().startsWith("?")) {
                        ans.add(Var.alloc(t.getSubject().getName()));
                    }
                }
                if (t.getPredicate().isVariable()) {
                    if (!t.getPredicate().getName().startsWith("?")) {
                        ans.add(Var.alloc(t.getPredicate().getName()));
                    }
                }
                if (t.getObject().isVariable()) {
                    if (!t.getObject().getName().startsWith("?")) {
                        ans.add(Var.alloc(t.getObject().getName()));
                    }
                }
            }
        } else if (op instanceof OpProject) {
            ans.addAll(((OpProject) op).getVars());
            ans.addAll(varsContainedInExcept(((OpProject) op).getSubOp(),exception));
        } else if (op instanceof OpPath) {
            TriplePath tp = ((OpPath) op).getTriplePath();
            if (tp.getSubject().isVariable()) {
                if (!tp.getSubject().getName().startsWith("?")) {
                    ans.add(Var.alloc(tp.getSubject().getName()));
                }
            }
            if (tp.getObject().isVariable()) {
                if (tp.getObject().getName().startsWith("?")) {
                    ans.add(Var.alloc(tp.getObject().getName()));
                }
            }
        } else if (op instanceof OpGraph) {
            Node n = ((OpGraph) op).getNode();
            if (n.isVariable()) {
                ans.add(Var.alloc(n.getName()));
            }
            ans.addAll(varsContainedInExcept(((OpGraph) op).getSubOp(),exception));
        } else if (op instanceof OpFilter) {
            ExprList eList = ((OpFilter) op).getExprs();
            ans.addAll(eList.getVarsMentioned());
            ans.addAll(varsContainedInExcept(((OpFilter) op).getSubOp(),exception));
        } else if (op instanceof OpExtend) {
            Map<Var, Expr> map = ((OpExtend) op).getVarExprList().getExprs();
            for (Map.Entry<Var, Expr> entry : map.entrySet()) {
                ans.add(entry.getKey());
                ans.addAll(entry.getValue().getVarsMentioned());
            }
            ans.addAll(varsContainedInExcept(((OpExtend) op).getSubOp(),exception));
        } else if (op instanceof OpAssign) {
            Map<Var, Expr> map = ((OpAssign) op).getVarExprList().getExprs();
            for (Map.Entry<Var, Expr> entry : map.entrySet()) {
                ans.add(entry.getKey());
                ans.addAll(entry.getValue().getVarsMentioned());
            }
            ans.addAll(varsContainedInExcept(((OpAssign) op).getSubOp(),exception));
        } else if (op instanceof OpTable) {
            ans.addAll(((OpTable) op).getTable().getVars());
        } else if (op instanceof Op1) {
            Op subOp = ((Op1) op).getSubOp();
            ans.addAll(varsContainedInExcept(subOp,exception));
        } else if (op instanceof Op2) {
            Op left = ((Op2) op).getLeft();
            Op right = ((Op2) op).getRight();
            ans.addAll(varsContainedInExcept(left,exception));
            ans.addAll(varsContainedInExcept(right,exception));
        } else if (op instanceof OpN) {
            for (Op o : ((OpN) op).getElements()) {
                ans.addAll(varsContainedInExcept(o,exception));
            }
        }
        return ans;
    }

    public static Set<Var> varsContainedIn(Op op) {
        Set<Var> ans = new HashSet<>();
        if (op instanceof OpTriple) {
            Triple t = ((OpTriple) op).getTriple();
            if (t.getSubject().isVariable()) {
                if (!t.getSubject().getName().startsWith("?")) {
                    ans.add(Var.alloc(t.getSubject().getName()));
                }
            }
            if (t.getPredicate().isVariable()) {
                if (!t.getPredicate().getName().startsWith("?")) {
                    ans.add(Var.alloc(t.getPredicate().getName()));
                }
            }
            if (t.getObject().isVariable()) {
                if (!t.getObject().getName().startsWith("?")) {
                    ans.add(Var.alloc(t.getObject().getName()));
                }
            }
        }
        else if (op instanceof OpBGP) {
            for (Triple t : ((OpBGP) op).getPattern().getList()) {
                if (t.getSubject().isVariable()) {
                    if (!t.getSubject().getName().startsWith("?")) {
                        ans.add(Var.alloc(t.getSubject().getName()));
                    }
                }
                if (t.getPredicate().isVariable()) {
                    if (!t.getPredicate().getName().startsWith("?")) {
                        ans.add(Var.alloc(t.getPredicate().getName()));
                    }
                }
                if (t.getObject().isVariable()) {
                    if (!t.getObject().getName().startsWith("?")) {
                        ans.add(Var.alloc(t.getObject().getName()));
                    }
                }
            }
        }
        else if (op instanceof OpProject) {
            ans.addAll(((OpProject) op).getVars());
            ans.addAll(varsContainedIn(((OpProject) op).getSubOp()));
        }
        else if (op instanceof OpPath) {
            TriplePath tp = ((OpPath) op).getTriplePath();
            if (tp.getSubject().isVariable()) {
                if (!tp.getSubject().getName().startsWith("?")) {
                    ans.add(Var.alloc(tp.getSubject().getName()));
                }
            }
            if (tp.getObject().isVariable()) {
                if (tp.getObject().getName().startsWith("?")) {
                    ans.add(Var.alloc(tp.getObject().getName()));
                }
            }
        }
        else if (op instanceof OpGraph) {
            Node n = ((OpGraph) op).getNode();
            if (n.isVariable()) {
                ans.add(Var.alloc(n.getName()));
            }
            ans.addAll(varsContainedIn(((OpGraph) op).getSubOp()));
        }
        else if (op instanceof OpFilter) {
            ExprList eList = ((OpFilter) op).getExprs();
            ans.addAll(eList.getVarsMentioned());
            ans.addAll(varsContainedIn(((OpFilter) op).getSubOp()));
        }
        else if (op instanceof OpExtend) {
            Map<Var, Expr> map = ((OpExtend) op).getVarExprList().getExprs();
            for (Map.Entry<Var, Expr> entry : map.entrySet()) {
                ans.add(entry.getKey());
                ans.addAll(entry.getValue().getVarsMentioned());
            }
            ans.addAll(varsContainedIn(((OpExtend) op).getSubOp()));
        }
        else if (op instanceof OpAssign) {
            Map<Var,Expr> map = ((OpAssign) op).getVarExprList().getExprs();
            for (Map.Entry<Var, Expr> entry : map.entrySet()) {
                ans.add(entry.getKey());
                ans.addAll(entry.getValue().getVarsMentioned());
            }
            ans.addAll(varsContainedIn(((OpAssign) op).getSubOp()));
        }
        else if (op instanceof OpTable) {
            ans.addAll(((OpTable) op).getTable().getVars());
        }
        else if (op instanceof Op1) {
            Op subOp = ((Op1) op).getSubOp();
            ans.addAll(varsContainedIn(subOp));
        }
        else if (op instanceof Op2) {
            Op left = ((Op2) op).getLeft();
            Op right = ((Op2) op).getRight();
            ans.addAll(varsContainedIn(left));
            ans.addAll(varsContainedIn(right));
        }
        else if (op instanceof OpN) {
            for (Op o : ((OpN) op).getElements()) {
                ans.addAll(varsContainedIn(o));
            }
        }
        return ans;
    }

    /**
     * Same as above but includes blank node variables.
     */
    public static Set<Var> allVarsContainedIn(Op op) {
        Set<Var> ans = new HashSet<>();
        if (op instanceof OpTriple) {
            Triple t = ((OpTriple) op).getTriple();
            if (t.getSubject().isVariable()) {
                ans.add(Var.alloc(t.getSubject().getName()));
            }
            if (t.getPredicate().isVariable()) {
                ans.add(Var.alloc(t.getPredicate().getName()));
            }
            if (t.getObject().isVariable()) {
                ans.add(Var.alloc(t.getObject().getName()));
            }
        }
        else if (op instanceof OpBGP) {
            for (Triple t : ((OpBGP) op).getPattern().getList()) {
                if (t.getSubject().isVariable()) {
                    ans.add(Var.alloc(t.getSubject().getName()));
                }
                if (t.getPredicate().isVariable()) {
                    ans.add(Var.alloc(t.getPredicate().getName()));
                }
                if (t.getObject().isVariable()) {
                    ans.add(Var.alloc(t.getObject().getName()));
                }
            }
        }
        else if (op instanceof OpProject) {
            ans.addAll(((OpProject) op).getVars());
            ans.addAll(allVarsContainedIn(((OpProject) op).getSubOp()));
        }
        else if (op instanceof OpPath) {
            TriplePath tp = ((OpPath) op).getTriplePath();
            if (tp.getSubject().isVariable()) {
                ans.add(Var.alloc(tp.getSubject().getName()));
            }
            if (tp.getObject().isVariable()) {
                ans.add(Var.alloc(tp.getObject().getName()));
            }
        }
        else if (op instanceof OpGraph) {
            Node n = ((OpGraph) op).getNode();
            if (n.isVariable()) {
                ans.add(Var.alloc(n.getName()));
            }
            ans.addAll(allVarsContainedIn(((OpGraph) op).getSubOp()));
        }
        else if (op instanceof OpFilter) {
            ExprList eList = ((OpFilter) op).getExprs();
            ans.addAll(eList.getVarsMentioned());
            ans.addAll(allVarsContainedIn(((OpFilter) op).getSubOp()));
        }
        else if (op instanceof OpExtend) {
            Map<Var, Expr> map = ((OpExtend) op).getVarExprList().getExprs();
            for (Map.Entry<Var, Expr> entry : map.entrySet()) {
                ans.add(entry.getKey());
                ans.addAll(entry.getValue().getVarsMentioned());
            }
            ans.addAll(allVarsContainedIn(((OpExtend) op).getSubOp()));
        }
        else if (op instanceof OpAssign) {
            Map<Var,Expr> map = ((OpAssign) op).getVarExprList().getExprs();
            for (Map.Entry<Var, Expr> entry : map.entrySet()) {
                ans.add(entry.getKey());
                ans.addAll(entry.getValue().getVarsMentioned());
            }
            ans.addAll(allVarsContainedIn(((OpAssign) op).getSubOp()));
        }
        else if (op instanceof OpTable) {
            ans.addAll(((OpTable) op).getTable().getVars());
        }
        else if (op instanceof Op1) {
            Op subOp = ((Op1) op).getSubOp();
            ans.addAll(allVarsContainedIn(subOp));
        }
        else if (op instanceof Op2) {
            Op left = ((Op2) op).getLeft();
            Op right = ((Op2) op).getRight();
            ans.addAll(allVarsContainedIn(left));
            ans.addAll(allVarsContainedIn(right));
        }
        else if (op instanceof OpN) {
            for (Op o : ((OpN) op).getElements()) {
                ans.addAll(allVarsContainedIn(o));
            }
        }
        return ans;
    }

    public static boolean isWellDesigned(Op op) {
        return isWellDesigned(op,new HashSet<>());
    }

    public static boolean isWellDesigned(Op op, Set<Var> outerVars) {
        Set<Var> currentVars = new HashSet<>(outerVars);
        if (op instanceof OpProject) {
            currentVars.addAll(((OpProject) op).getVars());
            return isWellDesigned(((OpProject) op).getSubOp(),currentVars);
        }
        else if (op instanceof OpGraph) {
            Node n = ((OpGraph) op).getNode();
            if (n.isVariable()) {
                currentVars.add(Var.alloc(n.getName()));
            }
            return isWellDesigned(((OpGraph) op).getSubOp(),currentVars);
        }
        else if (op instanceof OpFilter) {
            ExprList eList = ((OpFilter) op).getExprs();
            Set<Var> filterVars = eList.getVarsMentioned();
            Set<Var> subVars = varsContainedIn(((OpFilter) op).getSubOp());
            for (Var var : filterVars) {
                if (!subVars.contains(var)) {
                    return false;
                }
            }
            currentVars.addAll(filterVars);
            return isWellDesigned(((OpFilter) op).getSubOp(),currentVars);
        }
        else if (op instanceof OpExtend) {
            Map<Var, Expr> map = ((OpExtend) op).getVarExprList().getExprs();
            for (Map.Entry<Var, Expr> entry : map.entrySet()) {
                currentVars.add(entry.getKey());
                currentVars.addAll(entry.getValue().getVarsMentioned());
            }
            return isWellDesigned(((OpExtend) op).getSubOp(),currentVars);
        }
        else if (op instanceof OpAssign) {
            Map<Var,Expr> map = ((OpAssign) op).getVarExprList().getExprs();
            for (Map.Entry<Var, Expr> entry : map.entrySet()) {
                currentVars.add(entry.getKey());
                currentVars.addAll(entry.getValue().getVarsMentioned());
            }
            return isWellDesigned(((OpAssign) op).getSubOp(),currentVars);
        }
        else if (op instanceof Op1) {
            return isWellDesigned(((Op1) op).getSubOp(),currentVars);
        }
        else if (op instanceof Op2) {
            Op left = ((Op2) op).getLeft();
            Op right = ((Op2) op).getRight();
            if (op instanceof OpLeftJoin) {
                Set<Var> rightVars = varsContainedIn(right);
                Set<Var> leftVars = varsContainedIn(left);
                Set<Var> intersection = Sets.intersection(rightVars,currentVars);
                for (Var var : intersection) {
                    if (!leftVars.contains(var)) {
                        return false;
                    }
                }
            }
            return isWellDesigned(left,currentVars) && isWellDesigned(right,currentVars);
        }
        else if (op instanceof OpN) {
            boolean ans = true;
            for (Op o : ((OpN) op).getElements()) {
                ans = ans && isWellDesigned(o,currentVars);
            }
            return ans;
        }
        else {
            return true;
        }
    }

    public static boolean checkBranchVars(Op op){
        BGPSort bgps = new BGPSort();
        Transformer.transform(bgps, op);
        for (int i = 0; i < bgps.ucqVars.size(); i++){
            for (int j = i + 1; j < bgps.ucqVars.size(); j++){
                if (bgps.ucqVars.get(i).equals(bgps.ucqVars.get(j))){
                    return true;
                }
            }
        }
        return false;
    }

    public static Set<Op> opsInUnion(Op op){
        Set<Op> ans = new HashSet<>();
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

    public static Op joinOps(Op left, Op right) {
        if (left instanceof OpBGP) {
            Set<Triple> tripleSet = new HashSet<>(((OpBGP) left).getPattern().getList());
            if (right instanceof OpBGP) {
                tripleSet.addAll(((OpBGP) right).getPattern().getList());
            }
            else if (right instanceof OpTriple) {
                tripleSet.add(((OpTriple) right).getTriple());
            }
            else {
                return OpJoin.create(left, right);
            }
            BasicPattern bp = new BasicPattern();
            for (Triple triple : tripleSet) {
                bp.add(triple);
            }
            return new OpBGP(bp);
        }
        else if (right instanceof OpBGP) {
            Set<Triple> tripleSet = new HashSet<>(((OpBGP) right).getPattern().getList());
            if (left instanceof OpBGP) {
                tripleSet.addAll(((OpBGP) left).getPattern().getList());
            }
            else if (left instanceof OpTriple) {
                tripleSet.add(((OpTriple) left).getTriple());
            }
            else {
                return OpJoin.create(left,right);
            }
            BasicPattern bp = new BasicPattern();
            for (Triple triple : tripleSet) {
                bp.add(triple);
            }
            return new OpBGP(bp);
        }
        return OpJoin.create(left,right);
    }

    public static Op UCQNormalisation(Op op) {
        Op op1 = op;
        Op op2 = op;
        do {
            op1 = op2;
            op2 = Transformer.transform(new FilterTransform(), op2);
            op2 = Transformer.transform(new UCQTransformer(), op2);
            op2 = Transformer.transform(new TransformPath(), op2);
            op2 = Transformer.transform(new NotOneOfTransform(), op2);
        }
        while (!op1.equals(op2));
        return op2;
    }

    public static boolean isSatisfiable(Op op) {
        if (op instanceof OpUnion) {
            Op left = ((OpUnion) op).getLeft();
            Op right = ((OpUnion) op).getRight();
            return isSatisfiable(left) || isSatisfiable(right);
        }
        else if (op instanceof OpJoin) {
            return isSatisfiable(((OpJoin) op).getLeft()) && isSatisfiable(((OpJoin) op).getRight());
        }
        else if (op instanceof OpLeftJoin) {
            return isSatisfiable(((OpLeftJoin) op).getLeft());
        }
        else if (op instanceof OpMinus) {
            return isSatisfiable(((OpMinus) op).getLeft());
        }
        else if (op instanceof OpBGP) {
            BasicPattern bp = ((OpBGP) op).getPattern();
            for (Triple triple : bp.getList()) {
                if (triple.getSubject().isLiteral()) {
                    return false;
                }
            }
        }
        else if (op instanceof OpTriple) {
            Triple triple = ((OpTriple) op).getTriple();
            if (triple.getSubject().isLiteral()) {
                return false;
            }
        }
        else if (op instanceof Op1) {
            return isSatisfiable(((Op1) op).getSubOp());
        }
        return true;
    }

    public static boolean isNull(Op op) {
        if (op instanceof OpNull) {
            return true;
        }
        else if (op instanceof Op1) {
            return isNull(((Op1) op).getSubOp());
        }
        else if (op instanceof Op2) {
            return isNull(((Op2) op).getLeft()) || isNull(((Op2) op).getRight());
        }
        else if (op instanceof OpN) {
            for (Op o : ((OpN) op).getElements()) {
                if (isNull(o)) {
                    return true;
                }
            }
        }
        return false;
    }
}
