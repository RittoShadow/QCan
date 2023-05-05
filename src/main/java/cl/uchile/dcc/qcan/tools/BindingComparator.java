package cl.uchile.dcc.qcan.tools;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingMap;

import java.util.*;

public class BindingComparator implements Comparator<BindingMap> {
    @Override
    public int compare(BindingMap o1, BindingMap o2) {
        List<Var> vars1 = new ArrayList<>();
        List<Var> vars2 = new ArrayList<>();
        Iterator<Var> varIterator1 = o1.vars();
        Iterator<Var> varIterator2 = o2.vars();
        while (varIterator1.hasNext()) {
            vars1.add(varIterator1.next());
        }
        while (varIterator2.hasNext()) {
            vars2.add(varIterator2.next());
        }
        vars1.sort(new NodeComparator());
        vars2.sort(new NodeComparator());
        return 0;
    }
}
