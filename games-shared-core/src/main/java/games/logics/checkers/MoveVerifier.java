package games.logics.checkers;

import static games.logics.checkers.BoardVerifier.*;

import static java.lang.Math.abs;

public class MoveVerifier {
    public static boolean moveOutsideTable(int xFrom, int yFrom, int xTo, int yTo) {
        return !validCoordinates(xFrom, yFrom) || !validCoordinates(xTo, yTo);
    }

    public static boolean isJumpOverOpponentPiece(CheckersPiece movedPiece, CheckersPiece capturedPiece) {
        if ((movedPiece == null) || (capturedPiece == null)) {
            return false;
        }

        return (capturedPiece.getColor() != movedPiece.getColor());
    }

    public static boolean manBackwardMove(CheckersPiece movedPiece, int dy) {
        if (!movedPiece.isKing()) {
            return (movedPiece.getColor() ? dy < 0 : dy > 0);
        }
        return false;
    }

    public static boolean allowedMoveType(CheckersPiece board[][], int x, int y, int dy) {
        return board[x][y].isKing() || isForwardMove(board, x, y, dy);
    }

    public static boolean isForwardMove(CheckersPiece board[][], int x, int y, int dy) {
        return board[x][y].getColor()
               ? dy < 0
               : dy > 0;
    }

    public static boolean differentColorToJumpOver(CheckersPiece board[][], int x, int y, int dx, int dy) {
        return board[x + dx / 2][y + dy / 2].getColor() != board[x][y].getColor();
    }

    public static boolean allowedSimpleMove(CheckersPiece board[][], int x, int y, int dx, int dy) {
        return allowedMoveType(board, x, y, dy) && validCoordinates(x + dx, y + dy)
               && isEmptySquare(board, x + dx, y + dy);
    }

    public static boolean allowedCapture(CheckersPiece board[][], int x, int y, int dx, int dy) {
        return allowedMoveType(board, x, y, dy) && validCoordinates(x + dx, y + dy)
               && isEmptySquare(board, x + dx, y + dy) && !isEmptySquare(board, x + dx / 2, y + dy / 2)
               && differentColorToJumpOver(board, x, y, dx, dy);
    }

    public static boolean isAnyMoveAllowed(CheckersGame checkersGame, int x, int y) {
        return checkersGame.moveAllowed(x, y, x + 1, y + 1) || checkersGame.moveAllowed(x, y, x - 1, y + 1)
               || checkersGame.moveAllowed(x, y, x - 1, y - 1) || checkersGame.moveAllowed(x, y, x + 1, y - 1)
               || checkersGame.moveAllowed(x, y, x + 2, y + 2) || checkersGame.moveAllowed(x, y, x - 2, y + 2)
               || checkersGame.moveAllowed(x, y, x - 2, y - 2) || checkersGame.moveAllowed(x, y, x + 2, y - 2);
    }

    public static boolean isLegalDiagonalMove(int xFrom, int yFrom, int xTo, int yTo) {
        return ((abs(xFrom - xTo) == 1) && (abs(yFrom - yTo) == 1))
               || ((abs(xFrom - xTo) == 2) && (abs(yFrom - yTo) == 2));
    }

    public static boolean canMakeAMove(CheckersPiece board[][], int playerAtMoveIndex) {
        for (int x = lowerLimit; x < upperLimit; x++) {
            for (int y = lowerLimit; y < upperLimit; y++) {
                if (!isEmptySquare(board, x, y) && (playerAtMoveIndex == (board[x][y].getColor()
                        ? 1
                        : 0)) && canMakeAMove(board, x, y)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean canMakeAMove(CheckersPiece board[][], int x, int y) {
        if (!validCoordinates(x, y)) {
            return false;
        }

        if (isEmptySquare(board, x, y)) {
            return false;
        }

        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dy = -1; dy <= 1; dy += 2) {
                if (allowedSimpleMove(board, x, y, dx, dy)) {
                    return true;
                }
            }
        }

        for (int dx = -2; dx <= 2; dx += 4) {
            for (int dy = -2; dy <= 2; dy += 4) {
                if (allowedCapture(board, x, y, dx, dy)) {
                    return true;
                }
            }
        }

        return false;
    }
}

