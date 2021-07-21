package cl.uchile.dcc.visitors;

import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.walker.OpVisitorByTypeAndExpr;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.ExprList;

public class QcanVisitor implements OpVisitorByTypeAndExpr {

    private static QcanVisitor singleton = new QcanVisitor();

    public static QcanVisitor get() {
        return singleton;
    }

    public static void set(QcanVisitor visitor) {
        singleton = visitor;
    }

    @Override
    public void visit0(Op0 op0) {

    }

    @Override
    public void visit1(Op1 op1) {

    }

    @Override
    public void visit2(Op2 op2) {

    }

    @Override
    public void visitN(OpN opN) {

    }

    @Override
    public void visitExpr(ExprList exprList) {

    }

    @Override
    public void visitVarExpr(VarExprList varExprList) {

    }

    public void visit(OpLeftJoin opLeftJoin) {

    }
}
