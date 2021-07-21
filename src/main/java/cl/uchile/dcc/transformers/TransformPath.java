package cl.uchile.dcc.transformers;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.optimize.TransformPathFlatternStd;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.Path;

public class TransformPath extends TransformPathFlatternStd {
    @Override
    public Op transform(OpPath opPath) {
        Path path = opPath.getTriplePath().getPath();
        if (path instanceof P_NegPropSet) {
            return opPath;
        }
        else {
           return super.transform(opPath);
        }
    }
}
