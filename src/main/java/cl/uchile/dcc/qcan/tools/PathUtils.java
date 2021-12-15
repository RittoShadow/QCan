package cl.uchile.dcc.qcan.tools;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.*;
import org.apache.jena.sparql.path.*;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.*;

public class PathUtils {

    public static Path dfaToPath(Graph dfa, Node n, List<Node> predicates) {
        boolean needStartState = false;
        Node startState = n;
        Node newFinalState = null;
        Map<Pair<Node, Node>, Path> transitionTable = new HashMap<>();
        for (Node p : predicates) {
            ExtendedIterator<Node> aux = GraphUtil.listSubjects(dfa, p, n);
            if (aux.hasNext()) {
                needStartState = true;
            }
        }
        for (Node p : predicates) {
            dfa.remove(n, CommonNodes.preNode, p);
        }
        if (needStartState) {
            startState = NodeFactory.createBlankNode("start");
            dfa.add(Triple.create(startState, CommonNodes.epsilon, n));
        }
        List<Node> finalStates = GraphUtil.listSubjects(dfa, CommonNodes.typeNode, CommonNodes.finalNode).toList();
        newFinalState = NodeFactory.createBlankNode("final");
        for (Node finalState : finalStates) {
            dfa.add(Triple.create(finalState, CommonNodes.epsilon, newFinalState));
            dfa.remove(finalState, CommonNodes.typeNode, CommonNodes.finalNode);
        }
        dfa.add(Triple.create(newFinalState, CommonNodes.typeNode, CommonNodes.finalNode));
        Set<Node> states = findStates(dfa);
        ExtendedIterator<Triple> transitions = GraphUtil.findAll(dfa);
        while (transitions.hasNext()) {
            Triple t = transitions.next();
            if (!t.getPredicate().equals(CommonNodes.typeNode)) {
                Pair<Node, Node> pair = new Pair<>(t.getSubject(), t.getObject());
                if (t.getPredicate().toString().startsWith("\"^")) {
                    String u = t.getPredicate().toString();
                    u = u.substring(2, u.length() - 1);
                    transitionTable.put(pair, PathFactory.pathInverse(PathFactory.pathLink(NodeFactory.createURI(u))));
                } else if (t.getPredicate().toString().contains("negatedPropertySet")) {
                    String u = t.getPredicate().toString();
                    u = u.substring(u.lastIndexOf("negatedPropertySet") + 19);
                    String[] links = u.split("&#");
                    P_NegPropSet neg = new P_NegPropSet();
                    for (String link : links) {
                        if (link.startsWith("\"^")) {
                            neg.add((P_Path0) PathFactory.pathInverse(PathFactory.pathLink(NodeFactory.createURI(link.substring(2)))));
                        } else {
                            neg.add((P_Path0) PathFactory.pathLink(NodeFactory.createURI(link)));
                        }
                    }
                    transitionTable.put(pair, neg);
                } else {
                    transitionTable.put(pair, PathFactory.pathLink(t.getPredicate()));
                }

            }
            if (t.getPredicate().equals(CommonNodes.epsilon)) {
                Pair<Node, Node> pair = new Pair<>(t.getSubject(), t.getObject());
                transitionTable.put(pair, PathFactory.pathLink(CommonNodes.epsilon));
            }
        }
        Set<Node> tempNodes = new HashSet<>(states);
        Iterator<Node> tempStates = tempNodes.iterator();
        while (states.size() > 2) { // Should iterate until only the start node and final node remain.
            Node state = tempStates.next();
            if (!state.equals(startState) && !state.equals(newFinalState)) {
                path(state, transitionTable, states);
                states.remove(state);
            }
        }
        return finalState(startState, newFinalState, transitionTable);
    }

    public static Set<Node> findStates(Graph g) {
        Set<Node> ans = new HashSet<>();
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

    public static void path(Node n, Map<Pair<Node, Node>, Path> transitionTable, Set<Node> states) {
        Set<Pair<Pair<Node, Node>, Path>> toUpdate = new HashSet<>();
        Set<Pair<Node, Node>> toDelete = new HashSet<>();
        for (Node n0 : states) {
            for (Node n1 : states) {
                if (n1.equals(n) || n0.equals(n)) {
                    continue;
                }
                Path ans = null;
                Path regex0 = null;
                Path regex1 = null;
                Path regex2 = null;
                Path regex3 = null;
                Pair<Node, Node> pair0 = new Pair<>(n0, n);
                Pair<Node, Node> pair1 = new Pair<>(n, n1);
                Pair<Node, Node> pair2 = new Pair<>(n0, n1);
                Pair<Node, Node> pair3 = new Pair<>(n, n);
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
                    toUpdate.add(new Pair<>(pair2, ans));
                }
                toDelete.add(pair0);
                toDelete.add(pair1);
                toDelete.add(pair3);
            }
        }
        for (Pair<Pair<Node, Node>, Path> pair : toUpdate) {
            if (transitionTable.containsKey(pair.getLeft())) {
                Path p = transitionTable.get(pair.getLeft());
                if (p.equals(PathFactory.pathLink(CommonNodes.epsilon))) {
                    p = pair.getRight();
                } else {
                    p = new P_Alt(p, pair.getRight());
                }
                transitionTable.put(pair.getLeft(), p);
            } else {
                transitionTable.put(pair.getLeft(), pair.getRight());
            }
        }
        for (Pair<Node, Node> pair : toDelete) {
            transitionTable.remove(pair);
        }
    }

    public static Path newTransition(Path regex0, Path regex1, Path regex2, Path regex3) {
        Path ans = null;
        if (regex0 != null && !regex0.equals(PathFactory.pathLink(CommonNodes.epsilon))) {
            ans = regex0;
        }
        if (regex3 != null) {
            if (ans == null) {
                ans = PathFactory.pathZeroOrMore1(regex3);
            } else {
                ans = PathFactory.pathSeq(ans, PathFactory.pathZeroOrMore1(regex3));
            }
        }
        if (regex1 != null) {
            if (ans == null || ans.equals(PathFactory.pathLink(CommonNodes.epsilon))) {
                ans = regex1;
            } else {
                if (!regex1.equals(PathFactory.pathLink(CommonNodes.epsilon))) {
                    ans = PathFactory.pathSeq(ans, regex1);
                }
            }
        }
        if (regex2 != null) {
            if (ans == null) {
                ans = regex2;
            } else if (ans.equals(PathFactory.pathLink(CommonNodes.epsilon))) {
                if (regex2 instanceof P_ZeroOrMore1) {
                    ans = regex2;
                } else {
                    ans = PathFactory.pathAlt(ans, regex2);
                }
            } else if (regex2.equals(PathFactory.pathLink(CommonNodes.epsilon))) {
                if (ans instanceof P_Seq) { // p / p* | epsilon = p*
                    Path pLeft = ((P_Seq) ans).getLeft();
                    Path pRight = ((P_Seq) ans).getRight();
                    if (pRight instanceof P_ZeroOrMore1) {
                        if (((P_ZeroOrMore1) pRight).getSubPath().equals(pLeft)) {
                            ans = pRight;
                        } else {
                            ans = PathFactory.pathAlt(ans, regex2);
                        }
                    } else {
                        ans = PathFactory.pathAlt(ans, regex2);
                    }
                }
            } else {
                ans = PathFactory.pathAlt(ans, regex2);
            }
        }
        return ans;
    }

    public static Path finalState(Node startState, Node endState, Map<Pair<Node, Node>, Path> transitionTable) {
        Pair<Node, Node> pair = new Pair<>(startState, endState);
        if (transitionTable.get(pair) == null) {
            pair = new Pair<>(endState, endState);
            return PathFactory.pathZeroOrMore1(transitionTable.get(pair));
        } else {
            Path p0 = transitionTable.get(pair);
            pair = new Pair<>(endState, endState);
            Path p1 = transitionTable.get(pair);
            if (p1 != null) {
                return PathFactory.pathSeq(p0, PathFactory.pathZeroOrMore1(p1));
            } else {
                return p0;
            }
        }
    }
}
