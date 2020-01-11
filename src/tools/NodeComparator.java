package tools;

import java.util.Comparator;

import org.apache.jena.graph.Node;

public class NodeComparator implements Comparator<Node>{

	@Override
	public int compare(Node o1, Node o2) {
		return o1.toString().compareTo(o2.toString());
	}
}
