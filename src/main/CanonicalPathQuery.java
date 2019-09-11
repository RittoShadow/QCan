package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphExtract;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.TripleBoundary;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

public class CanonicalPathQuery {
	Graph graph;
	Graph canonicalGraph = GraphFactory.createPlainGraph();
	final String URI = "http://example.org/";
	private final Node typeNode = NodeFactory.createURI(this.URI+"type");
	private final Node tpNode = NodeFactory.createURI(this.URI+"TP");
	private final Node argNode = NodeFactory.createURI(this.URI+"arg");
	private final Node subNode = NodeFactory.createURI(this.URI+"subject");
	private final Node preNode = NodeFactory.createURI(this.URI+"predicate");
	private final Node objNode = NodeFactory.createURI(this.URI+"object");
	private final Node joinNode = NodeFactory.createURI(this.URI+"join");
	private final Node unionNode = NodeFactory.createURI(this.URI+"union");
	private final Node projectNode = NodeFactory.createURI(this.URI+"projection");
	private final Node pathNode = NodeFactory.createURI(this.URI+"path");
	private final Node triplePathNode = NodeFactory.createURI(this.URI+"triplePath");
	private final Node epsilon = NodeFactory.createURI(this.URI+"epsilon");
	private final Node finalNode = NodeFactory.createURI(this.URI+"final");
	
	public CanonicalPathQuery(Graph g) {
		this.graph = g;
	}
	
	public void paths(Node join) {
		ExtendedIterator<Node> paths = GraphUtil.listObjects(graph, join, argNode);
		while (paths.hasNext()) {
			Node path = paths.next();
			if (graph.contains(Triple.create(path, typeNode, tpNode))) {
				Node s = GraphUtil.listObjects(graph, path, subNode).next();
				Node p = GraphUtil.listObjects(graph, path, preNode).next();
				Node o = GraphUtil.listObjects(graph, path, objNode).next();
				canonicalGraph.add(Triple.create(s, p, o));
			}
			if (graph.contains(Triple.create(path, typeNode, triplePathNode))) {
				Node s = GraphUtil.listObjects(graph, path, subNode).next();
				Node p = GraphUtil.listObjects(graph, path, preNode).next();
				Node o = GraphUtil.listObjects(graph, path, objNode).next();
				semiPath(s,p,o);
			}
		}
	}
	
	public void semiPath(Node subject, Node path, Node object) {
		Node start = GraphUtil.listObjects(graph, path, argNode).next();
		List<Node> predicates = GraphUtil.listObjects(graph, start, preNode).toList();
		Node current = subject;
		if (graph.contains(Triple.create(start, typeNode, finalNode))) {
			for (Node p : predicates) {
				if (GraphUtil.listObjects(graph, start, p).hasNext()) {
					System.out.println(p);
					canonicalGraph.add(Triple.create(subject, p, object));
					break;
				}
			}
		}
		else {
			while (!graph.contains(Triple.create(start, typeNode, finalNode))) {
				for (Node p : predicates) {
					if (GraphUtil.listObjects(graph, start, p).hasNext()) {
						Node next = GraphUtil.listObjects(graph, start, p).next();
						canonicalGraph.add(Triple.create(current, p, next));
						current = next;
						start = current;
						break;
					}
				}
			}
		}
	}

}
