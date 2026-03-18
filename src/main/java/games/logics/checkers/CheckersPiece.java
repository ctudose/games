package games.logics.checkers;

public class CheckersPiece {
    private boolean color;
    private boolean isKing;

    public CheckersPiece(CheckersPiece other) {
        color  = other.color;
        isKing = other.isKing;
    }

    public CheckersPiece(boolean color, boolean isKing) {
        this.color  = color;
        this.isKing = isKing;
    }

    public boolean getColor() {
        return color;
    }

    public void setColor(boolean color) {
        this.color = color;
    }

    public boolean isKing() {
        return isKing;
    }

    public void setKing(boolean isKing) {
        this.isKing = isKing;
    }

    public String toString() {
        return (color
                ? "Black "
                : "White ") + (isKing
                               ? "King"
                               : "Man");
    }
}

