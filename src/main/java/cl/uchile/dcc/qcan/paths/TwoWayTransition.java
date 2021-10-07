package cl.uchile.dcc.qcan.paths;

public class TwoWayTransition {

    private final String predicate;
    private final TwoWayState target;
    private final int direction;

    public TwoWayTransition(String predicate, TwoWayState target, int direction) {
        this.predicate = predicate;
        this.target = target;
        this.direction = direction;
    }

    public String getPredicate() {
        return this.predicate;
    }

    public TwoWayState getTarget() {
        return this.target;
    }

    public int getDirection() {
        return this.direction;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null | !getClass().equals(o.getClass())) {
            return false;
        }
        else {
            return (((TwoWayTransition) o).getPredicate().equals(getPredicate()) && ((TwoWayTransition) o).getTarget().equals(getTarget()) && ((TwoWayTransition) o).getDirection() == getDirection());
        }
    }

    @Override
    public int hashCode() {
        return predicate.hashCode() + 53*target.hashCode() + 67*direction;
    }

    @Override
    public String toString() {
        return predicate + " -> (" + target + "," + direction + ")";
    }



}
