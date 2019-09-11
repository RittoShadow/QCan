package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathCompiler;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.util.iterator.ExtendedIterator;

public class PGraph {
	private Graph nfa;
	private Graph dfa = GraphFactory.createPlainGraph();
	private Graph complementDFA = GraphFactory.createPlainGraph();
	private Graph minimalDFA = GraphFactory.createPlainGraph();
	private Node startState;
	private Node endState;
	private Node newStartState;
	private Node epsilon = NodeFactory.createURI("http://example.org/epsilon");
	private Set<Node> predicates;
	private Set<Node> endStates = new HashSet<Node>();
	private Map<Node,Set<Node>> eClosures = new HashMap<Node,Set<Node>>();
	private Map<Set<Node>,Node> newNodes = new HashMap<Set<Node>,Node>();
	private List<List<Set<Node>>> partitions = new ArrayList<List<Set<Node>>>();
	final String URI = "http://example.org/";
	private final Node preNode = NodeFactory.createURI(this.URI+"predicate");
	private final Node typeNode = NodeFactory.createURI(this.URI+"type");
	@SuppressWarnings("unused")
	private final Node pathNode = NodeFactory.createURI(this.URI+"path");
	private final Node finalNode = NodeFactory.createURI(this.URI+"final");
	@SuppressWarnings("unused")
	private final Node argNode = NodeFactory.createURI(this.URI+"arg");
	
	public PGraph(TriplePath tp) {
		PathWalker pw = new PathWalker();
		tp.getPath().visit(pw);
		this.nfa = pw.graph;
		this.startState = pw.getStartState();
		this.endState = pw.getEndState();
		this.predicates = pw.predicates;
		this.newStartState = NodeFactory.createBlankNode();		
		determineEClosures();
		powerset(eClosures.get(startState));
		minimisation();
		complement();
	}
	
	private PGraph(Graph g, Node start, Node end, Set<Node> predicates) {
		this.nfa = g;
		this.startState = start;
		this.endState = end;
		this.predicates = predicates;
		this.newStartState = NodeFactory.createBlankNode();
		determineEClosures();
		powerset(eClosures.get(startState));
		minimisation();
		complement();
	}
	
	private void powerset(Set<Node> nodes) {
		if (!newNodes.containsKey(nodes)) {
			Node aux = NodeFactory.createBlankNode();
			if (nodes.equals(eClosures.get(startState))) {
				aux = newStartState;
			}
			if (nodes.contains(endState)) {
				endStates.add(aux);
			}
			newNodes.put(nodes, aux);
		}
		else {
			return;
		}
		for (Node p : predicates) {
			Set<Node> auxSet = new HashSet<Node>();
			for (Node v : nodes) {
				auxSet.addAll(eClosedTransition(v,p));
			}
			powerset(auxSet);
			Node f = newNodes.get(auxSet);
			dfa.add(Triple.create(newNodes.get(nodes), p, f));
		}
	}
	
	public Set<Node> findStates(Graph g){
		Set<Node> ans = new HashSet<Node>();
		ExtendedIterator<Triple> triples = GraphUtil.findAll(g);
		while (triples.hasNext()) {
			Triple triple = triples.next();
			Node subject = triple.getSubject();
			Node object = triple.getObject();
			if (subject.isBlank()) {
				ans.add(subject);
			}
			if (object.isBlank()) {
				ans.add(object);
			}
		}
		return ans;
	}
	
	public void determineEClosures() {
		Set<Node> states = findStates(nfa);
		Map<Node,Set<Node>> startClosures = new HashMap<Node,Set<Node>>();
		for (Node state : states) {
			Set<Node> closure = new HashSet<Node>();
			ExtendedIterator<Node> eNodes = GraphUtil.listObjects(nfa, state, epsilon);
			while (eNodes.hasNext()) {
				Node e = eNodes.next();
				closure.add(e);
			}
			closure.add(state);
			startClosures.put(state, closure);
		}
		boolean anyChanges = true;
		while (anyChanges) {
			anyChanges = false;
			for (Node state : states) {
				Set<Node> startClosure = startClosures.get(state);
				Set<Node> closure = new HashSet<Node>();
				for (Node c : startClosure) {
					closure.addAll(startClosures.get(c));
				}
				eClosures.put(state, closure);
				startClosures.put(state,closure);
				if (!startClosure.equals(closure)) {
					anyChanges = true;
				}
			}
		}		
	}
	
	public void determineEClosure(Graph g) {
		Set<Node> states = findStates(g);
		Map<Node,Set<Node>> startClosures = new HashMap<Node,Set<Node>>();
		for (Node state : states) {
			Set<Node> closure = new HashSet<Node>();
			ExtendedIterator<Node> eNodes = GraphUtil.listObjects(g, state, epsilon);
			while (eNodes.hasNext()) {
				Node e = eNodes.next();
				closure.add(e);
			}
			closure.add(state);
			startClosures.put(state, closure);
		}
		boolean anyChanges = true;
		while (anyChanges) {
			anyChanges = false;
			for (Node state : states) {
				Set<Node> startClosure = startClosures.get(state);
				Set<Node> closure = new HashSet<Node>();
				for (Node c : startClosure) {
					closure.addAll(startClosures.get(c));
				}
				eClosures.put(state, closure);
				startClosures.put(state,closure);
				if (!startClosure.equals(closure)) {
					anyChanges = true;
				}
			}
		}		
	}
	
	public Set<Node> eClosedTransition(Node n, Node p) {
		Set<Node> ans = new HashSet<Node>();
		Set<Node> nodes = eClosures.get(n);
		for (Node s : nodes) {
			ExtendedIterator<Node> states = GraphUtil.listObjects(nfa, s, p);
			while (states.hasNext()) {
				Node v = states.next();
				ans.addAll(eClosures.get(v));
			}
		}
		return ans;
	}
	
	public void eliminateDeadStates(Graph g) {
		Set<Node> states = findStates(g);
		Set<Triple> triplesToDelete = new HashSet<Triple>();
		for (Node state : states) {
			if (!endStates.contains(state)) {
				Set<Node> transitions = new HashSet<Node>();
				for (Node p : predicates) {
					transitions.add(GraphUtil.listObjects(g, state, p).next());
				}
				if (transitions.size() == 1) {
					if (transitions.contains(state)) {
						for (Node p : predicates) {
							ExtendedIterator<Node> subjects = GraphUtil.listSubjects(g, p, state);
							ExtendedIterator<Node> objects = GraphUtil.listObjects(g, state, p);
							while (subjects.hasNext()) {
								Node s = subjects.next();
								triplesToDelete.add(Triple.create(s, p, state));
							}
							while (objects.hasNext()) {
								Node o = objects.next();
								triplesToDelete.add(Triple.create(state, p, o));
							}
						}
					}
				}
			}
		}
		for (Triple t : triplesToDelete) {
			g.delete(t);
		}
	}
	
	private void minimisation() {
		Set<Node> nonEndStates = findStates(dfa);
		nonEndStates.removeAll(endStates);
		List<Set<Node>> partition = new ArrayList<Set<Node>>();
		partition.add(nonEndStates);
		partition.add(endStates);
		partitions.add(partition);
		int k = 0;
		List<Set<Node>> previousPartition = new ArrayList<Set<Node>>();
		while (!partitions.get(k).equals(previousPartition)) {
			previousPartition = partitions.get(k);
			partitions.add(nextPartition(partitions.get(k),k++));
		}
		for (Set<Node> nodes : partitions.get(k)) {
			Node n = NodeFactory.createBlankNode();
			newNodes.put(nodes, n);
		}
		ExtendedIterator<Triple> dfaTriples = GraphUtil.findAll(dfa);
		while (dfaTriples.hasNext()) {
			Triple t = dfaTriples.next();
			minimalDFA.add(Triple.create(findPartition(t.getSubject()), t.getPredicate(), findPartition(t.getObject())));
		}
		this.newStartState = findPartition(this.newStartState);
		Set<Node> newEndStates = new HashSet<Node>();
		for (Node n : endStates) {
			newEndStates.add(findPartition(n));
		}
		this.endStates = newEndStates;
		for (Node end : newEndStates) {
			minimalDFA.add(Triple.create(end, typeNode, finalNode));
		}
		for (Node p : predicates) {
			minimalDFA.add(Triple.create(newStartState, preNode, p));
		}
		eliminateDeadStates(minimalDFA);		
	}
	
	public boolean distinguishable(Node a, Node b, List<Set<Node>> list) {
		boolean ans = false;
		for (Node p : predicates) {
			Node n0 = GraphUtil.listObjects(dfa, a, p).next();
			Node n1 = GraphUtil.listObjects(dfa, b, p).next();
			Set<Node> partition0 = null, partition1 = null;
			for (Set<Node> partition : list) {
				if (partition.contains(n0)) {
					partition0 = partition;
				}
				if (partition.contains(n1)) {
					partition1 = partition;
				}
			}
			if (!partition0.equals(partition1)) {
				return true;
			}
		}
		return ans;
	}
	
	public List<Set<Node>> nextPartition(List<Set<Node>> p, int k) {
		List<Set<Node>> ans = new ArrayList<Set<Node>>();
		for (Set<Node> partition : p) {
			ArrayList<Node> partitionList = new ArrayList<Node>();
			partitionList.addAll(partition);
			boolean[][] partitionTable = new boolean[partition.size()][partition.size()];
			String[] keys = new String[partition.size()];
			for (int i = 0; i < partition.size(); i++) {	
				partitionTable[i][i] = false;
				keys[i] = "";
				for (int j = i + 1; j < partition.size(); j++) {
					Node n0 = partitionList.get(i);
					Node n1 = partitionList.get(j);	
					boolean b = distinguishable(n0,n1,partitions.get(k));
					partitionTable[i][j] = b;
					partitionTable[j][i] = b;
				}
				for (int j = 0; j < partition.size(); j++) {
					if (partitionTable[i][j]) {
						keys[i] = keys[i] + "1";
					}
					else {
						keys[i] = keys[i] + "0";
					}
				}
			}
			Map<String,Set<Node>> f = new HashMap<String,Set<Node>>();
			for (int i = 0; i < partition.size(); i++) {
				f.put(keys[i], new HashSet<Node>());
			}
			for (int i = 0; i < partition.size(); i++) {
				Set<Node> current = f.get(keys[i]);
				current.add(partitionList.get(i));
			}
			ans.addAll(f.values());
		}
		return ans;
	}
	
	private Node findPartition(Node n) {
		for (Set<Node> partition : partitions.get(partitions.size() - 1)) {
			if (partition.contains(n)) {
				return newNodes.get(partition);
			}
		}
		return null;
	}
	
	private void complement() {
		GraphUtil.addInto(complementDFA, minimalDFA);
		Node f = NodeFactory.createBlankNode();
		Node dead = NodeFactory.createBlankNode();
		Set<Node> states = findStates(complementDFA);
		for (Node p : predicates) {
			complementDFA.add(Triple.create(dead, p, dead));
			for (Node n : states) {
				if (!GraphUtil.listObjects(complementDFA, n, p).hasNext()) {
					complementDFA.add(Triple.create(n, p, dead));
				}
			}
		}
		for (Node n : states) {
			if (!endStates.contains(n)) {
				complementDFA.add(Triple.create(n, epsilon, f));
			}
			else {
				complementDFA.remove(n, typeNode, finalNode);
			}
		}
		complementDFA.add(Triple.create(dead, epsilon, f));
		complementDFA.add(Triple.create(f, typeNode, finalNode));
	}
	
//	public static PGraph union(Graph g1, Graph g2) {
//		Graph g = GraphFactory.createPlainGraph();
//		PGraph ans = new PGraph(g);
//		GraphUtil.addInto(ans.nfa, g1);
//		GraphUtil.addInto(ans.nfa, g2);
//		ExtendedIterator<Node> states = GraphUtil.listSubjects(ans.nfa, ans.typeNode, ans.pathNode);
//		List<List<Node>> predicates = new ArrayList<List<Node>>();
//		Node newStart = NodeFactory.createBlankNode();
//		Node newFinalState = NodeFactory.createBlankNode();
//		while (states.hasNext()) {
//			Node s = states.next();
//			Node start = GraphUtil.listObjects(ans.nfa, s, ans.argNode).next();
//			ans.nfa.add(Triple.create(newStart, ans.epsilon, start));
//			List<Node> p = GraphUtil.listObjects(ans.nfa, start, ans.preNode).toList();
//			predicates.add(p);
//			for (Node n : p) {
//				ans.nfa.delete(Triple.create(start, ans.preNode, n));
//			}
//		}
//		if (!predicates.get(0).equals(predicates.get(1))) { // No containment possible.
//			return null;
//		}
//		ExtendedIterator<Node> finalStates = GraphUtil.listSubjects(ans.nfa, ans.typeNode, ans.finalNode);
//		while (finalStates.hasNext()) {
//			Node f = finalStates.next();
//			ans.nfa.add(Triple.create(f, ans.epsilon, newFinalState));
//			ans.nfa.delete(Triple.create(f, ans.typeNode, ans.finalNode));
//		}
//		return ans;
//	}
	
	public PGraph intersection(PGraph pg) { // Computes intersection of ¬A and B
		Graph union = GraphFactory.createPlainGraph();
		GraphUtil.addInto(union, this.complementDFA);
		GraphUtil.addInto(union, pg.minimalDFA);
		if (!this.predicates.equals(pg.predicates)) {
			return null;
		}
		Node start = NodeFactory.createBlankNode();
		Node end = NodeFactory.createBlankNode();
		union.add(Triple.create(start, epsilon, this.newStartState));
		union.add(Triple.create(start, epsilon, pg.newStartState));
		for (Node p : predicates) {
			ExtendedIterator<Node> nodes = GraphUtil.listSubjects(union, preNode, p);
			while (nodes.hasNext()) {
				Node n = nodes.next();
				union.delete(Triple.create(n, preNode, p));
			}
		}
		Node end0 = GraphUtil.listSubjects(this.complementDFA, typeNode, finalNode).next();
		union.add(Triple.create(end0, epsilon, end));
		union.remove(end0, typeNode, finalNode);
		for (Node f : pg.endStates) {
			union.remove(f, typeNode, finalNode);
			union.add(Triple.create(f, epsilon, end));
		}
		PGraph pg1 = new PGraph(union,start,end,predicates);
		return pg1;
	}
	
	public PGraph difference(PGraph pg) {
		PGraph p = this.intersection(pg);
		if (p == null) {
			return null;
		}
		Graph g = p.getMinimalDFA();
		Set<Node> states = p.findStates(p.getMinimalDFA());
		Node deadState = NodeFactory.createBlankNode();
		for (Node n : states) {
			for (Node pre : p.predicates) {
				if (!GraphUtil.listObjects(p.getMinimalDFA(), n, pre).hasNext()) {
					g.add(Triple.create(n, pre, deadState));
				}
			}
		}
		return p;
	}
	
	public boolean containedIn(PGraph pg) {
		PGraph p = this.difference(pg);
		if (p == null) {
			return false;
		}
		Set<Node> r = p.reachableStates(p.newStartState, p.getMinimalDFA(), p.predicates);
		Set<Node> nonEndStates = p.findStates(p.getMinimalDFA());
		nonEndStates.removeAll(p.getEndStates());
		for (Node n : r) {
			if (nonEndStates.contains(n)) {
				return false;
			}
		}
		return true;
	}
	
	public void printNFA() {
		ExtendedIterator<Triple> et = GraphUtil.findAll(nfa);
		while (et.hasNext()) {
			System.out.println(et.next());
		}
	}
	
	public void printDFA() {
		ExtendedIterator<Triple> et = GraphUtil.findAll(dfa);
		while (et.hasNext()) {
			System.out.println(et.next());
		}
	}
	
	public void printMinimalDFA() {
		ExtendedIterator<Triple> et = GraphUtil.findAll(minimalDFA);
		while (et.hasNext()) {
			System.out.println(et.next());
		}
	}
	
	public void printComplement() {
		ExtendedIterator<Triple> et = GraphUtil.findAll(complementDFA);
		while (et.hasNext()) {
			System.out.println(et.next());
		}
	}
	
	public Set<Node> reachableStates(Node n, Graph g, Set<Node> predicates, Set<Node> reachable) {
		Node current = n;
		reachable.add(current);
		for (Node p : predicates) {
			if (GraphUtil.listObjects(g, current, p).hasNext()) {
				Node next = GraphUtil.listObjects(g, current, p).next();
				if (!reachable.contains(next)) {
					reachable.add(next);
					reachableStates(next, g, predicates, reachable);
				}	
			}
		}
		return reachable;
	}
	
	public Set<Node> reachableStates(Node n, Graph g, Set<Node> predicates){
		return reachableStates(n, g, predicates, new HashSet<Node>());
	}
	
	public Graph getNFA() {
		return this.nfa;
	}
	
	public Graph getDFA() {
		return this.dfa;
	}
	
	public Graph getMinimalDFA() {
		return this.minimalDFA;
	}
	
	public Node getStartState() {
		return this.newStartState;
	}
	
	public Set<Node> getEndStates() {
		return this.endStates;
	}
	
	public static void main(String[] args) {
		Path path = SSE.parsePath("(path* <http://xmlns.com/foaf/0.1/mbox>)");
		Path path2 = SSE.parsePath("(path <http://xmlns.com/foaf/0.1/mbox>)");
		PathTransform pt = new PathTransform();
		path = pt.visit(path);
		path2 = pt.visit(path2);
		TriplePath tp = new TriplePath(NodeFactory.createBlankNode(),path,NodeFactory.createBlankNode());
		TriplePath tp2 = new TriplePath(NodeFactory.createBlankNode(),path2,NodeFactory.createBlankNode());
		PGraph pg = new PGraph(tp);
		PGraph pg2 = new PGraph(tp2);
		System.out.println(pg.containedIn(pg2));
		System.out.println(pg2.containedIn(pg));
		System.out.println();
		System.out.println(pg2.containedIn(pg2));
	}
}
