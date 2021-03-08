package transformers;

import org.apache.jena.base.Sys;
import org.apache.jena.sparql.expr.*;

public class ExprTransformer extends ExprTransformCopy {

    @Override
    public Expr transform(ExprFunction2 func, Expr expr1, Expr expr2){
        Expr ans = func;
        if (func instanceof E_LogicalAnd) {
            if (expr1 instanceof E_LogicalOr  && !(expr2 instanceof E_LogicalOr)) {
                Expr left = new E_LogicalAnd(((E_LogicalOr) expr1).getArg1(),expr2);
                Expr right = new E_LogicalAnd(((E_LogicalOr) expr1).getArg2(),expr2);
                ans = new E_LogicalOr(left,right);
            }
            else if (!(expr1 instanceof E_LogicalOr) && expr2 instanceof E_LogicalOr) {
                Expr left = new E_LogicalAnd(((E_LogicalOr) expr2).getArg1(),expr1);
                Expr right = new E_LogicalAnd(((E_LogicalOr) expr2).getArg2(),expr1);
                ans = new E_LogicalOr(left,right);
            }
            else if (expr1 instanceof E_LogicalOr && expr2 instanceof E_LogicalOr) {
                Expr left1 = new E_LogicalAnd(((E_LogicalOr) expr1).getArg1(),((E_LogicalOr) expr2).getArg1());
                Expr right1 = new E_LogicalAnd(((E_LogicalOr) expr1).getArg2(),((E_LogicalOr) expr2).getArg1());
                Expr left2 = new E_LogicalAnd(((E_LogicalOr) expr1).getArg1(),((E_LogicalOr) expr2).getArg2());
                Expr right2 = new E_LogicalAnd(((E_LogicalOr) expr1).getArg2(),((E_LogicalOr) expr2).getArg2());
                ans = new E_LogicalOr(left1,right1);
                ans = new E_LogicalOr(ans,new E_LogicalOr(left2,right2));
            }
            else {
                ans = new E_LogicalAnd(expr1,expr2);
            }
            return ans;
        }
        else{
            return func;
        }
    }
}
