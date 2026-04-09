package games.contracts;

public final class ContractMove {

    private final int xFrom;
    private final int yFrom;
    private final int xTo;
    private final int yTo;

    public ContractMove(int xFrom, int yFrom, int xTo, int yTo) {
        this.xFrom = xFrom;
        this.yFrom = yFrom;
        this.xTo = xTo;
        this.yTo = yTo;
    }

    public int getXFrom() {
        return xFrom;
    }

    public int getYFrom() {
        return yFrom;
    }

    public int getXTo() {
        return xTo;
    }

    public int getYTo() {
        return yTo;
    }
}
