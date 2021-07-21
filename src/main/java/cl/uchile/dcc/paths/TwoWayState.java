package cl.uchile.dcc.paths;

public class TwoWayState {

    private int stateId;
    private String predicate;
    private static int numberOfStates = 0;

    public TwoWayState() {
        this.stateId = numberOfStates++;
        this.predicate = "";
    }

    public TwoWayState(int id) {
        this.stateId = id;
        this.predicate = "";
        numberOfStates++;
    }

    public TwoWayState(TwoWayState state, String predicate) {
        this.stateId = state.stateId;
        this.predicate = predicate;
        numberOfStates++;
    }

    @Override
    public String toString() {
        String ans = "state ";
        if (!predicate.isEmpty()) {
            ans = ans + "(" + stateId + "," + predicate + ")";
        }
        else {
            ans = ans + stateId;
        }
        return ans;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null | getClass() != object.getClass()) {
            return false;
        }
        TwoWayState state = (TwoWayState) object;
        return (stateId == state.stateId && predicate.equals(predicate));
    }

    @Override
    public int hashCode() {
        if (predicate.isEmpty()) {
            return stateId;
        }
        else {
            return predicate.hashCode() + stateId;
        }
    }
}
