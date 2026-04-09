package games.contracts;

public final class ContractRoles {

    private final boolean p0Taken;
    private final boolean p1Taken;

    public ContractRoles(boolean p0Taken, boolean p1Taken) {
        this.p0Taken = p0Taken;
        this.p1Taken = p1Taken;
    }

    public boolean isP0Taken() {
        return p0Taken;
    }

    public boolean isP1Taken() {
        return p1Taken;
    }
}
