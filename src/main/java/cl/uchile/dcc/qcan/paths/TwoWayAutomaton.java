package cl.uchile.dcc.qcan.paths;

import org.apache.jena.atlas.lib.Pair;

import java.util.*;

public class TwoWayAutomaton {

    public Map<TwoWayState,Set<TwoWayTransition>> transitionSet = new HashMap<>();
    public TwoWayState initialState;
    public Set<String> predicates = new HashSet<>();
    public Set<TwoWayState> stateSet = new HashSet<>();
    public Set<TwoWayState> acceptSet = new HashSet<>();
    public static int numberOfStates = 0;

    public TwoWayAutomaton() {
        this.initialState = new TwoWayState();
        numberOfStates++;
    }

    public void addTransition(int id, String predicate, int targetId) {
        TwoWayState state = new TwoWayState(id);
        if (!predicates.contains(predicate)) {
            predicates.add(predicate);
            predicates.add(inverseOf(predicate));
        }
        if (!stateSet.contains(state)) {
            stateSet.add(state);
            transitionSet.put(state,new HashSet<>());
        }
        TwoWayState otherState = new TwoWayState(state,inverseOf(predicate));
        if (!stateSet.contains(otherState)) {
            stateSet.add(otherState);
            transitionSet.put(otherState,new HashSet<>());
        }
        TwoWayState targetState = new TwoWayState(targetId);
        if (!stateSet.contains(targetState)) {
            stateSet.add(targetState);
            transitionSet.put(targetState,new HashSet<>());
        }
        TwoWayTransition transition = new TwoWayTransition(predicate,targetState,1);
        TwoWayTransition inverseTransition = new TwoWayTransition(inverseOf(predicate),otherState,-1);
        TwoWayTransition neutralTransition = new TwoWayTransition(inverseOf(predicate),targetState,0);
        Set<TwoWayTransition> transitions = transitionSet.get(state);
        Set<TwoWayTransition> inverseTransitions = transitionSet.get(otherState);
        transitions.add(transition);
        transitions.add(inverseTransition);
        inverseTransitions.add(neutralTransition);
        transitionSet.put(state,transitions);
        transitionSet.put(otherState,inverseTransitions);
    }

    public void addAcceptState(int id) {
        TwoWayState state = new TwoWayState(id);
        if (!stateSet.contains(state)) {
            stateSet.add(state);
            transitionSet.put(state,new HashSet<>());
        }
        acceptSet.add(state);
    }

    public void toNDFA() {
        List<Set<TwoWayState>> combinations = new ArrayList<>(powerset());
        List<TwoWayState> initialStates = new ArrayList<>();
        List<TwoWayState> newAcceptStates = new ArrayList<>();
        List<Pair<Set<TwoWayState>,Set<TwoWayState>>> pairs = new ArrayList<>();
        HashMap<Set<TwoWayState>,TwoWayState> combinationSet = new HashMap<>();
        HashMap<Pair<Set<TwoWayState>,Set<TwoWayState>>,TwoWayState> pairStates = new HashMap<>();
        HashMap<TwoWayState,Set<TwoWayTransition>> newTransitions = new HashMap<>();
        for (Set<TwoWayState> c : combinations) {
            TwoWayState singleState = new TwoWayState();
            combinationSet.put(c,singleState);
            if (c.contains(initialState)) {
                initialStates.add(singleState);
            }
            if (Collections.disjoint(c,acceptSet)){
                newAcceptStates.add(singleState);
            }
            for (Set<TwoWayState> d : combinations) {
                Pair<Set<TwoWayState>,Set<TwoWayState>> pair = new Pair<>(c, d);
                TwoWayState pairState = new TwoWayState();
                pairs.add(pair);
                pairStates.put(pair, pairState);
                if (Collections.disjoint(d,acceptSet)) {
                    newAcceptStates.add(pairState);
                }
            }
        }
        for (String predicate : predicates) {
            for (Set<TwoWayState> T : combinations) {
                for (Set<TwoWayState> U : combinations) {
                    for (Set<TwoWayState> V : combinations) {
                        boolean condition1 = false;
                        boolean condition2 = false;
                        boolean condition3 = false;
                        for (TwoWayState s : T) {
                            Set<TwoWayTransition> transitions = transitionSet.get(s);
                            Set<TwoWayState> targets = new HashSet<>();
                            for (TwoWayTransition transition : transitions) {
                                targets.add(transition.getTarget());
                            }
                            for (TwoWayState t : targets) {
                                if (transitions.contains(new TwoWayTransition(predicate,t,0))) {
                                    if (T.contains(t)) {
                                        condition1 = true;
                                    }
                                }
                                else {
                                    condition1 = true;
                                }
                                if (transitions.contains(new TwoWayTransition(predicate,t,1))) {
                                    if (U.contains(t)) {
                                        condition2 = true;
                                    }
                                }
                                else {
                                    condition2 = true;
                                }
                            }
                            if (condition1 && condition2) {
                                TwoWayState n = combinationSet.get(T);
                                TwoWayState target = pairStates.get(new Pair<>(T,U));
                                if (!newTransitions.containsKey(n)) {
                                    newTransitions.put(n, new HashSet<>());
                                }
                                Set<TwoWayTransition> transitions1 = newTransitions.get(n);
                                transitions1.add(new TwoWayTransition(predicate,target,1));
                                newTransitions.put(n,transitions1);
                                condition1 = false;
                                condition2 = false;
                                break;
                            }
                        }
                        for (TwoWayState s : U) {
                            Set<TwoWayTransition> transitions = transitionSet.get(s);
                            Set<TwoWayState> targets = new HashSet<>();
                            for (TwoWayTransition transition : transitions) {
                                targets.add(transition.getTarget());
                            }
                            for (TwoWayState t : targets) {
                                if (transitions.contains(new TwoWayTransition(predicate,t,-1))) {
                                    if (T.contains(t)) {
                                        condition1 = true;
                                    }
                                }
                                else {
                                    condition1 = true;
                                }
                                if (transitions.contains(new TwoWayTransition(predicate,t,0))) {
                                    if (U.contains(t)) {
                                        condition2 = true;
                                    }
                                }
                                else {
                                    condition2 = true;
                                }
                                if (transitions.contains(new TwoWayTransition(predicate,t,1))) {
                                    if (V.contains(t)) {
                                        condition3 = true;
                                    }
                                }
                                else {
                                    condition3 = true;
                                }
                            }
                        }
                        if (condition1 && condition2 && condition3) {
                            TwoWayState n = pairStates.get(new Pair<>(T, U));
                            TwoWayState target = pairStates.get(new Pair<>(U,V));
                            if (newTransitions.containsKey(n)) {
                                Set<TwoWayTransition> set = newTransitions.get(n);
                                set.add(new TwoWayTransition(predicate, target, 1));
                                newTransitions.put(n, set);
                            } else {
                                Set<TwoWayTransition> nSet = new HashSet<>();
                                nSet.add(new TwoWayTransition(predicate, target, 1));
                                newTransitions.put(n, nSet);
                            }
                            condition1 = false;
                            condition2 = false;
                            condition3 = false;
                            break;
                        }
                    }
                }
            }
        }
        System.out.println(newTransitions.size());
        for (TwoWayState state : initialStates) {
            Set<TwoWayState> endStates = DFSLoop(state,newTransitions);
            if (!Collections.disjoint(endStates,newAcceptStates)) {
                System.out.println(state + " : " + endStates);
            }
        }
    }

    public void DFS(TwoWayState state, Map<TwoWayState,Set<TwoWayTransition>> transitionSet, Set<TwoWayState> visited, Set<TwoWayState> result) {
        Set<TwoWayTransition> transitions = transitionSet.get(state);
        for (TwoWayTransition transition : transitions) {
            boolean end = true;
            if (!visited.contains(transition.getTarget()) && !state.equals(transition.getTarget())) {
                end = false;
                visited.add(state);
                DFS(transition.getTarget(),transitionSet,visited,result);
            }
            if (end) {
                result.add(state);
            }
        }
    }

    public Set<TwoWayState> DFSLoop(TwoWayState state, Map<TwoWayState,Set<TwoWayTransition>> transitionSet) {
        Set<TwoWayState> result = new HashSet<>();
        DFS(state,transitionSet,new HashSet<>(),result);
        return result;
    }

    public static void main(String[] args) {

    }

    public Set<Set<TwoWayState>> powerset() {
        return powerset(new HashSet<>());
    }

    public Set<Set<TwoWayState>> powerset(Set<TwoWayState> current) {
        Set<Set<TwoWayState>> ans = new HashSet<>();
        if (current.size() == stateSet.size()) {
            return ans;
        }
        else {
            for (TwoWayState state : stateSet) {
                if (!current.contains(state)) {
                    Set<TwoWayState> newSet = new HashSet<>(current);
                    newSet.add(state);
                    ans.add(newSet);
                    ans.addAll(powerset(newSet));
                }
            }
        }
        return ans;
    }

    public String inverseOf(String predicate) {
        if (predicate.startsWith("^")) {
            return predicate.substring(predicate.indexOf("^")+1);
        }
        else {
            return "^" + predicate;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TwoWayState state : transitionSet.keySet()) {
            sb.append(state);
            if (acceptSet.contains(state)) {
                sb.append(" [accept]");
            }
            sb.append(": \n");
            for (TwoWayTransition transition : transitionSet.get(state)) {
                sb.append("\t").append(transition).append("\n");
            }
        }
        return sb.toString();
    }
}