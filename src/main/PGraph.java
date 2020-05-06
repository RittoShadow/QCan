package main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphExtract;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.TripleBoundary;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.yars.nx.NodeComparator;

import cl.uchile.dcc.blabel.jena.JenaModelIterator;
import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.blabel.label.GraphLabelling;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingArgs;
import cl.uchile.dcc.blabel.label.GraphLabelling.GraphLabellingResult;

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
	private int minLength = 0;
	private int maxLength = 0;
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
	
	public PGraph(String path) {
		PathWalker pw = new PathWalker();
		Path p = SSE.parsePath(path);
		p.visit(pw);
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
	
	public PGraph(Path path) {
		PathWalker pw = new PathWalker();
		path.visit(pw);
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
//		try {
//			minimalDFA = label(minimalDFA);
//			Object[] nodes = predicates.toArray();
//			newStartState = GraphUtil.listSubjects(minimalDFA, preNode, (Node) nodes[0]).next();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
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
	
	public PGraph intersection(PGraph pg) { // Computes intersection of A and ¬B
		Graph union = GraphFactory.createPlainGraph();
		GraphUtil.addInto(union, this.complementDFA);
		GraphUtil.addInto(union, pg.minimalDFA);
		if (!this.predicates.equals(pg.predicates)) {
			for (Node p : this.predicates) {
				if (!pg.predicates.contains(p)) {
					return null;
				}
			}
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
	
	public Graph label(Graph g) throws InterruptedException, HashCollisionException{
		Model model = ModelFactory.createModelForGraph(g);
		JenaModelIterator jmi = new JenaModelIterator(model);
		TreeSet<org.semanticweb.yars.nx.Node[]> triples = new TreeSet<org.semanticweb.yars.nx.Node[]>(NodeComparator.NC);
		while(jmi.hasNext()){
			org.semanticweb.yars.nx.Node[] triple = jmi.next();
			triples.add(new org.semanticweb.yars.nx.Node[]{triple[0],triple[1],triple[2]});
		}
		GraphLabellingArgs gla = new GraphLabellingArgs();
		gla.setDistinguishIsoPartitions(false);
		GraphLabelling gl = new GraphLabelling(triples,gla);	
		GraphLabellingResult glr = gl.call();
		RGraph rg = new RGraph(glr.getGraph());
		return rg.graph;
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
	
	public Path getNormalisedPath() {
		return this.propertyPathToOp(this.newStartState, minimalDFA);
	}
	
	public Path propertyPathToOp(Node n, Graph graph) {
		Path ans = null;
		List<Node> predicates = GraphUtil.listObjects(graph, n, preNode).toList();
		GraphExtract ge = new GraphExtract(TripleBoundary.stopNowhere);
		Map<Pair<Node,Node>,Path> transitionTable = new HashMap<Pair<Node,Node>,Path>();
		Graph dfa = ge.extract(n, graph);
		Node startState = n;
		Node newFinalState = null;
		List<Node> states = orderedStates(dfa, n);
		for (Node p : predicates) {
			dfa.remove(n, preNode, p);
		}
		startState = NodeFactory.createBlankNode("start");
		dfa.add(Triple.create(startState, epsilon, n));
		List<Node> finalStates = GraphUtil.listSubjects(dfa, typeNode, finalNode).toList();
		newFinalState = NodeFactory.createBlankNode("final");
		for (Node finalState : finalStates) {
			dfa.add(Triple.create(finalState, epsilon, newFinalState));
			dfa.remove(finalState, typeNode, finalNode);
		}
		dfa.add(Triple.create(newFinalState, typeNode, finalNode));
		ExtendedIterator<Triple> transitions = GraphUtil.findAll(dfa);
		while (transitions.hasNext()) {
			Triple t = transitions.next();
			if (!t.getPredicate().equals(typeNode)) {
				Pair<Node,Node> pair = new Pair<Node,Node>(t.getSubject(),t.getObject());
				List<Node> transitionList = GraphUtil.listPredicates(dfa, pair.getLeft(), pair.getRight()).toList();
				if (!transitionTable.containsKey(pair)) {
					if (transitionList.size() > 1) {
						Path newPath = null;
						List<String> pathList = new ArrayList<String>();
						for (Node path : transitionList) {
							pathList.add(path.toString());
						}
						Collections.sort(pathList);
						for (String path : pathList) {
							if (path.startsWith("\"^")) {
								if (newPath == null) {
									newPath = PathFactory.pathInverse(PathFactory.pathLink(NodeFactory.createURI(path.substring(2, path.length() - 1))));
								}
								else {
									newPath = PathFactory.pathAlt(newPath, PathFactory.pathInverse(PathFactory.pathLink(NodeFactory.createURI(path.substring(2, path.length() - 1)))));
								}
							}
							else {
								if (newPath == null) {
									newPath = PathFactory.pathLink(NodeFactory.createURI(path));
								}
								else {
									newPath = PathFactory.pathAlt(newPath, PathFactory.pathLink(NodeFactory.createURI(path)));
								}
							}
						}
						transitionTable.put(pair, newPath);
					}
					else {
						if (t.getPredicate().toString().startsWith("\"^")) {
							String u = t.getPredicate().toString();
							u = u.substring(2,u.length()-1);
							transitionTable.put(pair, PathFactory.pathInverse(PathFactory.pathLink(NodeFactory.createURI(u))));
						}
						else {
							transitionTable.put(pair, PathFactory.pathLink(t.getPredicate()));
						}
					}
				}
			}
			if (t.getPredicate().equals(epsilon)) {
				Pair<Node,Node> pair = new Pair<Node,Node>(t.getSubject(),t.getObject());
				transitionTable.put(pair, PathFactory.pathLink(epsilon));
			}
		}
//		List<Path> possiblePaths = new ArrayList<Path>();
//		for (List<Node> nodeList : statePerm) {
//			Path p = dfaToPath(startState, newFinalState, transitionTable, nodeList);
//			possiblePaths.add(p);
//		}
		Set<Node> tempNodes = new HashSet<Node>();
		for (Node tempN : states) {
			tempNodes.add(tempN);
		}
		Iterator<Node> tempStates = tempNodes.iterator();
		while (states.size() > 0) { // Should iterate until only the start node and final node remain.
			Node state = tempStates.next();
			states.add(startState);
			states.add(newFinalState);
			path(state,transitionTable,states);
			states.remove(state);
			states.remove(startState);
			states.remove(newFinalState);
		}
		ans = finalState(startState,newFinalState,transitionTable);
		return ans;
	}
	
	public Path ardensTheorem() {
		Path ans = null;
		Set<Node> states = findStates(this.minimalDFA);
		for (Node state : states) {
			if (newStartState.equals(state)) {
				
			}
			else {
				
			}
		}
		return ans;
	}
	
	public List<Node> orderedStates(Graph g, Node n) {
		ArrayList<Node> ans = new ArrayList<Node>();
		Node current = n;
		ArrayList<Node> orderedPredicates = new ArrayList<Node>();
		for (Node p : this.predicates) {
			orderedPredicates.add(p);
		}
		Collections.sort(orderedPredicates, new tools.NodeComparator());
		ans.add(current);
		orderedStates(current, g, orderedPredicates, ans);
		return ans;
	}
	
	public List<Node> orderedStates(Node n, Graph g, List<Node> predicates, List<Node> ordered) {
		Node current = n;
		for (Node p : predicates) {
			if (GraphUtil.listObjects(g, current, p).hasNext()) {
				Node next = GraphUtil.listObjects(g, current, p).next();
				if (!ordered.contains(next)) {
					ordered.add(next);
					orderedStates(next, g, predicates, ordered);
				}
			}
		}
		return ordered;
	}
	
	public Path dfaToPath(Node startState, Node finalState, Map<Pair<Node,Node>,Path> startTable, List<Node> states) {
		Path ans = null;
		Set<Node> statesSet = new HashSet<Node>();
		statesSet.addAll(states);
		statesSet.add(startState);
		statesSet.add(finalState);
		Map<Pair<Node,Node>,Path> transitionTable = new HashMap<Pair<Node,Node>,Path>();
		for (Map.Entry<Pair<Node,Node>, Path> entry : startTable.entrySet()) {
			transitionTable.put(entry.getKey(), entry.getValue());
		}
		for (Node state : states) {
			path(state, transitionTable, statesSet);
			statesSet.remove(state);
		}
		ans = finalState(startState, finalState, transitionTable);
		return ans;
	}
	
	public void path(Node n, Map<Pair<Node,Node>,Path> transitionTable, Collection<Node> states) {
		Set<Pair<Pair<Node,Node>,Path>> toUpdate = new HashSet<Pair<Pair<Node,Node>,Path>>();
		Set<Pair<Node,Node>> toDelete = new HashSet<Pair<Node,Node>>();
		for (Node n0 : states) {
			for (Node n1 : states) {
				if (n0.equals(n) || n1.equals(n)) {
					continue;
				}
				Path ans = null;
				Path regex0 = null;
				Path regex1 = null;
				Path regex2 = null;
				Path regex3 = null;
				Pair<Node,Node> pair0 = new Pair<Node,Node>(n0,n);
				Pair<Node,Node> pair1 = new Pair<Node,Node>(n,n1);
				Pair<Node,Node> pair2 = new Pair<Node,Node>(n0,n1);
				Pair<Node,Node> pair3 = new Pair<Node,Node>(n,n);
				if (transitionTable.containsKey(pair0) && transitionTable.containsKey(pair1)) {
					regex0 = transitionTable.get(pair0);
					regex1 = transitionTable.get(pair1);
					if (transitionTable.containsKey(pair2)) {
						regex2 = transitionTable.get(pair2);
					}
					if (transitionTable.containsKey(pair3)) {
						regex3 = transitionTable.get(pair3);
					}
					ans = newTransition(regex0, regex1, regex2, regex3);
					toUpdate.add(new Pair<Pair<Node,Node>,Path>(pair2, ans));
				}	
				toDelete.add(pair0);
				toDelete.add(pair1);
				toDelete.add(pair3);
			}	
		}
		for (Pair<Pair<Node,Node>,Path> pair : toUpdate) {
			if (transitionTable.containsKey(pair)) {
				Path p = transitionTable.get(pair.getLeft());
				p = new P_Alt(p,pair.getRight());
				transitionTable.put(pair.getLeft(), p);
			}
			else {
				transitionTable.put(pair.getLeft(), pair.getRight());
			}
		}
		for (Pair<Node,Node> pair : toDelete) {
			transitionTable.remove(pair);
		}	
	}
	
	public Path newTransition(Path regex0, Path regex1, Path regex2, Path regex3) {
		Path ans = null;
		if (regex0 != null && !regex0.equals(PathFactory.pathLink(epsilon))) {
			ans = regex0;
		}
		if (regex3 != null) {
			if (ans == null) {
				ans = PathFactory.pathZeroOrMore1(regex3);
			}
			else {
				ans = PathFactory.pathSeq(ans, PathFactory.pathZeroOrMore1(regex3));
			}
		}
		if (regex1 != null) {
			if (ans == null || ans.equals(PathFactory.pathLink(epsilon))) {
				ans = regex1;
			}
			else {
				if (!regex1.equals(PathFactory.pathLink(epsilon))) {
					ans = PathFactory.pathSeq(ans, regex1);
				}
			}
		}
		if (regex2 != null) {
			if (ans == null) {
				ans = regex2;
			}
			else if (ans.equals(PathFactory.pathLink(epsilon))) {
				if (regex2 instanceof P_ZeroOrMore1) {
					ans = regex2;
				}
			}
			else if (regex2.equals(PathFactory.pathLink(epsilon))) {
				if (ans instanceof P_ZeroOrMore1) {
					
				}
				else if (ans instanceof P_Seq) { // p / p* | epsilon = p*
					Path pLeft = ((P_Seq) ans).getLeft();
					Path pRight = ((P_Seq) ans).getRight();
					if (pRight instanceof P_ZeroOrMore1) {
						if (((P_ZeroOrMore1) pRight).getSubPath().equals(pLeft)) {
							ans = pRight;
						}
					}
				}
			}
			else {
				ans = PathFactory.pathAlt(ans, regex2);
			}			
		}
		return ans;
	}
	
	public Path finalState(Node startState, Node endState, Map<Pair<Node, Node>, Path> transitionTable) {
		Pair<Node,Node> pair = new Pair<Node,Node>(startState,endState);
		if (transitionTable.get(pair) == null) {
//			pair.remove(startState);
//			pair.add(endState);
			pair = new Pair<Node,Node>(endState,endState);
			return PathFactory.pathZeroOrMore1(transitionTable.get(pair));
		}
		else {
			Path p0 = transitionTable.get(pair);
//			pair.remove(startState);
//			pair.add(endState);
			pair = new Pair<Node,Node>(endState,endState);
			Path p1 = transitionTable.get(pair);
			if (p1 != null) {
				return PathFactory.pathSeq(p0, PathFactory.pathZeroOrMore1(p1));
			}
			else {
				return p0;
			}
		}
	}
	
	public List<List<Node>> permutations(List<Node> set){
		List<List<Node>> ans = new ArrayList<List<Node>>();
		List<Node> s = new ArrayList<Node>();
		s.addAll(set);
		if (s.size() == 1) {
			ans.add(s);
			return ans;
		}
		else {
			for (Node n : s) {
				List<List<Node>> aux = permutations(s.subList(1, s.size()));
				for (List<Node> list : aux) {
					list.add(n);
					ans.add(list);
				}
			}
		}
		return ans;
	}
	
	public List<List<Node>> permutations2(List<Node> set, List<List<Node>> ans, int k){
		List<Node> s = new ArrayList<Node>();
		s.addAll(set);
		Node temp = null;
		if (k == 1) {
			ans.add(s);
			return ans;
		}
		else {
			permutations2(set, ans, k - 1);
			for (int i = 0; i < k - 1; i++) {
				if (k % 2 == 0) {
					temp = set.get(i);
					set.set(i, set.get(k - 1));
					set.set(k - 1, temp);
				}
				else {
					temp = set.get(0);
					set.set(0, set.get(k - 1));
					set.set(k - 1, temp);
				}
				permutations2(set, ans, k - 1);
			}
		}
		return ans;
	}
	
	public void setMinimumLength(Node n) {
		
	}
	
	public static void main(String[] args) {
		Path path2 = SSE.parsePath("(seq <http://xmlns.com/foaf/0.1/p> (path* <http://xmlns.com/foaf/0.1/p>))");
		Path path = SSE.parsePath("(seq (seq <http://xmlns.com/foaf/0.1/p> (reverse <http://xmlns.com/foaf/0.1/p>) ) <http://xmlns.com/foaf/0.1/p> )");
		PathTransform pt = new PathTransform();
		path = pt.visit(path);
		TriplePath tp = new TriplePath(NodeFactory.createBlankNode(),path,NodeFactory.createBlankNode());
		PGraph pg = new PGraph(tp);
		pg.printMinimalDFA();
		System.out.println(pg.getNormalisedPath());
		System.out.println(pg.orderedStates(pg.minimalDFA, pg.getStartState()));
		PGraph pg1 = new PGraph(path);
		PGraph pg2 = new PGraph(path2);
		System.out.println(pg1.containedIn(pg2));
		System.out.println(pg2.containedIn(pg1));
		pg1.printMinimalDFA();
		System.out.println();
		pg2.printMinimalDFA();
	}
}
