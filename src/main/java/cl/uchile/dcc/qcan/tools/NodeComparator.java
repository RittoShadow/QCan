package cl.uchile.dcc.qcan.tools;

import org.apache.jena.graph.Node;

import java.util.Comparator;

public class NodeComparator implements Comparator<Node>{

	@Override
	public int compare(Node o1, Node o2) {
		return o1.toString().compareTo(o2.toString());
	}
}
