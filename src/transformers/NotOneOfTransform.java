package transformers;

import java.util.List;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpUnion;

public class NotOneOfTransform extends TransformCopy {
	
	public Op transform(OpSequence op, List<Op> ops) {
		OpSequence ans = OpSequence.create();
		for (Op o : ops) {
			if (o instanceof OpUnion) {
				if (((OpUnion) o).getLeft() instanceof OpPath && ((OpUnion) o).getRight() == null) {
					ans.add(((OpUnion) o).getLeft());
				}
				else if (((OpUnion) o).getRight() instanceof OpPath && ((OpUnion) o).getLeft() == null) {
					ans.add(((OpUnion) o).getRight());
				}
			}
			else {
				ans.add(o);
			}
		}
		return ans;
	}
}
