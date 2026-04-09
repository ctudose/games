package games.logics.checkers;

import games.logics.GameMove;

public class CheckersGameMove extends GameMove {
    int xFrom, yFrom, xTo, yTo;

    public CheckersGameMove(int xFrom, int yFrom, int xTo, int yTo) {
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

    public String toString() {
        return "Checkers move from " + xFrom + " " + yFrom + " to " + xTo + " " + yTo;
    }
}

