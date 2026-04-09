package games.logics.checkers;

import games.logics.Game;
import games.logics.GameMove;
import games.logics.GameMoveResult;
import games.logics.GameSituation;

import static games.logics.checkers.BoardVerifier.getNoOfPieces;
import static games.logics.checkers.BoardVerifier.initBoard;
import static games.logics.checkers.BoardVerifier.isLastLine;
import static games.logics.checkers.BoardVerifier.lowerLimit;
import static games.logics.checkers.BoardVerifier.upperLimit;
import static games.logics.checkers.BoardVerifier.validCoordinates;
import static games.logics.checkers.CaptureVerifier.canMakeACapture;
import static games.logics.checkers.MoveVerifier.canMakeAMove;
import static games.logics.checkers.MoveVerifier.isJumpOverOpponentPiece;
import static games.logics.checkers.MoveVerifier.isLegalDiagonalMove;
import static games.logics.checkers.MoveVerifier.manBackwardMove;
import static games.logics.checkers.MoveVerifier.moveOutsideTable;

import static java.lang.Math.abs;

public class CheckersGame extends Game {
    private CheckersPiece board[][]     = new CheckersPiece[upperLimit][upperLimit];
    private int           intermediateX = -1,    // related to
                          intermediateY = -1;    // multiple captures

    public CheckersGame() {
        playerAtMoveIndex = 0;
        initBoard(board);
    }

    public CheckersGame(CheckersGame other) {
        super(other);

        for (int x = lowerLimit; x < upperLimit; x++) {
            for (int y = lowerLimit; y < upperLimit; y++) {
                board[x][y] = (other.board[x][y] == null)
                              ? null
                              : new CheckersPiece(other.board[x][y]);
            }
        }

        intermediateX = other.intermediateX;
        intermediateY = other.intermediateY;
    }

    public CheckersPiece[][] getBoard() {
        return board;
    }

    public GameMoveResult checkMove(Object player, GameMove move) {
        CheckersGameMove checkersMove = (move instanceof CheckersGameMove)
                                        ? (CheckersGameMove) move
                                        : null;

        if (checkersMove == null) {
            return GameMoveResult.INVALID_DATA_FORMAT;
        }

        int playerIndex = getPlayerIndex(player);

        if (playerIndex == -1) {
            return GameMoveResult.NOT_ONE_OF_PLAYERS;
        }

        if (playerIndex != playerAtMoveIndex) {
            return GameMoveResult.NOT_ALLOWED_TO_MOVE_NOW;
        }

        boolean ok = move(checkersMove.xFrom, checkersMove.yFrom, checkersMove.xTo, checkersMove.yTo);

        return ok
               ? GameMoveResult.CORRECT_MOVE
               : GameMoveResult.WRONG_MOVE;
    }

    public GameSituation getSituation(Object user) {
        return new GameSituation(new CheckersGame(this));
    }

    private boolean move(int xFrom, int yFrom, int xTo, int yTo) {
        if (!moveAllowed(xFrom, yFrom, xTo, yTo)) {
            return false;
        }

        board[xTo][yTo]     = board[xFrom][yFrom];
        board[xFrom][yFrom] = null;

        // Was it a capture?
        if (abs(xFrom - xTo) == 2) {
            board[(xFrom + xTo) / 2][(yFrom + yTo) / 2] = null;
        }

        // It was a capture & there are still possible captures, with the same piece, then the same player will continue the multiple capture
        if ((abs(xFrom - xTo) == 2) && canMakeACapture(board, xTo, yTo)) {
            intermediateX = xTo;
            intermediateY = yTo;
        } else {
            playerAtMoveIndex = 1 - playerAtMoveIndex;
            intermediateX     = -1;
            intermediateY     = -1;
        }

        // Was it a transform?
        if (isLastLine(yTo)) {
            board[xTo][yTo].setKing(true);
        }

        // Victory?
        if (getNoOfPieces(board, !board[xTo][yTo].getColor()) == 0) {
            setWinner(board[xTo][yTo].getColor()
                      ? 1
                      : 0);
        }

        if (!isFinished() &&!canMakeAMove(board, playerAtMoveIndex)) {
            setWinner(1 - playerAtMoveIndex);
        }

        return true;
    }

    boolean moveAllowed(int xFrom, int yFrom, int xTo, int yTo) {
        if (isFinished()) {
            return false;
        }

        if (moveOutsideTable(xFrom, yFrom, xTo, yTo)) {
            return false;
        }

        if (!isLegalDiagonalMove(xFrom, yFrom, xTo, yTo)) {
            return false;
        }

        CheckersPiece movedPiece = pieceAt(xFrom, yFrom);

        if (movedPiece == null) {
            return false;
        }

        if (wrongPlayer(movedPiece)) {
            return false;
        }

        if (pieceAt(xTo, yTo) != null) {
            return false;
        }

        int dx = xFrom - xTo;
        int dy = yFrom - yTo;

        if (manBackwardMove(movedPiece, dy)) {
            return false;
        }

        // Are we during a multiple capture?
        if (mustContinueMultipleCapture()) {
            if ((abs(dx) != 2) || (xFrom != intermediateX) || (yFrom != intermediateY)) {
                return false;
            }
        }

        // If it is a jump (2 squares), there should be over an opponent piece, i.e. a capture
        if (abs(dx) == 2) {
            CheckersPiece capturedPiece = pieceAt((xFrom + xTo) / 2, (yFrom + yTo) / 2);

            return isJumpOverOpponentPiece(movedPiece, capturedPiece);
        } else {
            return !canMakeACapture(board, playerAtMoveIndex);
        }
    }

    public boolean wrongPlayer(CheckersPiece movedPiece) {
        return playerAtMoveIndex == (movedPiece.getColor()
                                     ? 0
                                     : 1);
    }

    public boolean mustContinueMultipleCapture() {
        return intermediateX >= 0;
    }

    public CheckersPiece pieceAt(int x, int y) {
        return (validCoordinates(x, y))
               ? board[x][y]
               : null;
    }
}
