package games.logics.checkers;

import games.logics.GameMove;

import static games.logics.checkers.BoardVerifier.lowerLimit;
import static games.logics.checkers.BoardVerifier.upperLimit;
import static games.logics.checkers.MoveVerifier.isAnyMoveAllowed;

public class RobotMove {
    public static GameMove getRobotMove(CheckersGame checkersGame) {
        int moveablePiecesCount = 0;

        for (int x = lowerLimit; x < upperLimit; x++) {
            for (int y = lowerLimit; y < upperLimit; y++) {
                if ((checkersGame.getBoard()[x][y] != null)
                        && (checkersGame.getPlayerAtMoveIndex() == (checkersGame.getBoard()[x][y].getColor()
                        ? 1
                        : 0))) {
                    if (isAnyMoveAllowed(checkersGame, x, y)) {
                        moveablePiecesCount++;
                    }
                }
            }
        }

        int indexPieceToMove = (int) (moveablePiecesCount * Math.random());

        return selectPieceToMove(checkersGame, indexPieceToMove);
    }

    public static GameMove selectPieceToMove(CheckersGame checkersGame, int indexPieceToMove) {
        int pos  = 0;
        int dx[] = {
            1, -1, -1, 1, 2, -2, -2, 2
        };
        int dy[] = {
            1, 1, -1, -1, 2, 2, -2, -2
        };

        for (int x = lowerLimit; x < upperLimit; x++) {
            for (int y = lowerLimit; y < upperLimit; y++) {
                if ((checkersGame.getBoard()[x][y] != null)
                        && (checkersGame.getPlayerAtMoveIndex() == (checkersGame.getBoard()[x][y].getColor()
                        ? 1
                        : 0))) {
                    for (int i = 0; i < dx.length; i++) {
                        if (checkersGame.moveAllowed(x, y, x + dx[i], y + dy[i])) {
                            if (pos == indexPieceToMove) {
                                return new CheckersGameMove(x, y, x + dx[i], y + dy[i]);
                            } else {
                                pos++;

                                continue;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}

