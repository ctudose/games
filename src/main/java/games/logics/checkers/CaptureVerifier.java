package games.logics.checkers;

import static games.logics.checkers.BoardVerifier.isEmptySquare;
import static games.logics.checkers.BoardVerifier.lowerLimit;
import static games.logics.checkers.BoardVerifier.upperLimit;
import static games.logics.checkers.MoveVerifier.allowedCapture;

public class CaptureVerifier {
    public static boolean canMakeACapture(CheckersPiece board[][], int playerAtMoveIndex) {
        boolean color = (playerAtMoveIndex == 1);

        for (int x = lowerLimit; x < upperLimit; x++) {
            for (int y = lowerLimit; y < upperLimit; y++) {
                if ((!isEmptySquare(board, x, y)) && (board[x][y].getColor() == color)
                        && canMakeACapture(board, x, y)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean canMakeACapture(CheckersPiece board[][], int x, int y) {
        if (isEmptySquare(board, x, y)) {
            return false;
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

