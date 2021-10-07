package cl.uchile.dcc.qcan.paths;

import com.google.common.collect.HashBiMap;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.*;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.List;
import java.util.Set;

public class PropertyAutomata {
    public Automaton automaton;
    public Path path;
    public HashBiMap<String,String> charToURIMap = HashBiMap.create();

    public PropertyAutomata(Path path) {
        this.path = path;
        this.automaton = getAutomaton(path);
    }

    public PropertyAutomata(Graph graph) {
        ExtendedIterator<Triple> triples = GraphUtil.findAll(graph);
        automaton = new Automaton();
        while (triples.hasNext()) {
            Triple triple = triples.next();
        }
    }

    public Automaton getAutomaton(Path path) {
        String pathString = path.toString();
        pathString = pathString.replace("^<","<^");
        String[] split = pathString.split("<|>");
        String newPathString = "";
        for (int i = 0; i < split.length; i++) {
            if (split[i].contains(":")) {
                String chars = String.valueOf((char)(i + 80));
                if (charToURIMap.containsValue(split[i])) {
                    chars = charToURIMap.inverse().get(split[i]);
                }
                else {
                    charToURIMap.put(chars,split[i]);
                }
                newPathString += "\"" + chars + "\"";
            }
            else {
                newPathString += split[i].replace("/","");
            }
        }
        RegExp regExp = new RegExp(newPathString);
        Automaton automaton = regExp.toAutomaton();
        automaton.minimize();
        return automaton;
    }

    public Node getURINode(char c) {
        String URI = charToURIMap.get(String.valueOf(c));
        Node node = NodeFactory.createURI(URI);
        return node;
    }

    public Node stateToNode(State state) {
        String label = state.toString();
        label = label.substring(0,label.indexOf("["));
        label = label.replace(" ","");
        Node node = NodeFactory.createBlankNode(label);
        return node;
    }

    public int stateId(State state) {
        String label = state.toString();
        label = label.substring(0,label.indexOf("["));
        String[] split = label.split(" ");
        if (split.length == 2) {
            if (StringUtils.isNumeric(split[1])) {
                return Integer.valueOf(split[1]);
            }
        }
        return -1;
    }

    public Graph toGraph() {
        Graph ans = GraphFactory.createPlainGraph();
        Set<State> states = automaton.getStates();
        for (State state : states) {
            List<Transition> transitions = state.getSortedTransitions(true);
            for (Transition transition : transitions) {
                Triple t = Triple.create(stateToNode(state),getURINode(transition.getMin()),stateToNode(transition.getDest()));
                ans.add(t);
            }
        }
        return ans;
    }

    public TwoWayAutomaton get2FA() {
        TwoWayAutomaton ans = new TwoWayAutomaton();
        Set<State> states = automaton.getStates();
        for (State state : states) {
            int stateId = stateId(state);
            List<Transition> transitions = state.getSortedTransitions(true);
            if (state.isAccept()) {
                ans.addAcceptState(stateId);
            }
            for (Transition transition : transitions) {
                ans.addTransition(stateId,charToURIMap.get(String.valueOf(transition.getMin())),stateId(transition.getDest()));
            }
        }
        return ans;
    }



    public static void main(String[] args) {
        Path path = SSE.parsePath("(path* (seq (path* <http://xmlns.com/foaf/0.1/b>) (reverse <http://xmlns.com/foaf/0.1/b>)) )");
        Path path1 = SSE.parsePath("(path* (seq (path* <http://xmlns.com/foaf/0.1/b>) (path* <http://xmlns.com/foaf/0.1/b>)) )");
        PropertyAutomata propertyAutomata = new PropertyAutomata(path);
        PropertyAutomata propertyAutomata1 = new PropertyAutomata(path1);
        propertyAutomata.get2FA().toNDFA();

    }
}
