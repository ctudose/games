package games.logics.checkers;

import games.logics.GameMove;
import games.logics.GameMoveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CheckersGameTest {

    private CheckersGame game;

    @BeforeEach
    void setUp() {
        game = new CheckersGame();
        game.setPlayer(0, "Player0");
        game.setPlayer(1, "Player1");
        game.start();
    }

    @Test
    void initialBoardHasTwelvePiecesPerColor() {
        CheckersPiece[][] board = game.getBoard();

        int countColorFalse = 0;
        int countColorTrue  = 0;

        for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
            for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
                CheckersPiece piece = board[x][y];

                if (piece != null) {
                    if (piece.getColor()) {
                        countColorTrue++;
                    } else {
                        countColorFalse++;
                    }
                }
            }
        }

        int finalCountColorFalse = countColorFalse;
        int finalCountColorTrue = countColorTrue;
        assertAll(
                () -> assertEquals(12, finalCountColorFalse, "Expected 12 pieces for color=false"),
                () -> assertEquals(12, finalCountColorTrue, "Expected 12 pieces for color=true")
        );
    }

    @Test
    void checkMoveRejectsUnknownPlayer() {
        GameMove move = new CheckersGameMove(0, 0, 1, 1);

        GameMoveResult result = game.checkMove("Unknown", move);

        assertEquals(GameMoveResult.NOT_ONE_OF_PLAYERS, result);
    }

    @Test
    void robotGeneratedMoveIsAppliedByCheckersGame() {
        // Arrange: robot chooses a legal move for the current player
        GameMove robotMove = RobotMove.getRobotMove(game);
        assertNotNull(robotMove, "RobotMove should return a move from the initial position");

        CheckersPiece[][] before = copyBoard(game.getBoard());

        // Act
        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMoveResult result = game.checkMove(currentPlayer, robotMove);

        // Assert
        CheckersPiece[][] after = game.getBoard();
        boolean changed = boardsDiffer(before, after);

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result, "Robot move should be legal"),
                () -> assertEquals(true, changed, "Board should change after applying a legal move")
        );
    }

    private static CheckersPiece[][] copyBoard(CheckersPiece[][] original) {
        CheckersPiece[][] copy = new CheckersPiece[BoardVerifier.upperLimit][BoardVerifier.upperLimit];

        for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
            for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
                CheckersPiece piece = original[x][y];
                copy[x][y] = (piece == null) ? null : new CheckersPiece(piece);
            }
        }

        return copy;
    }

    private static boolean boardsDiffer(CheckersPiece[][] a, CheckersPiece[][] b) {
        for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
            for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
                CheckersPiece p1 = a[x][y];
                CheckersPiece p2 = b[x][y];

                if (p1 == null && p2 == null) {
                    continue;
                }
                if (p1 == null || p2 == null) {
                    return true;
                }
                if (p1.getColor() != p2.getColor() || p1.isKing() != p2.isKing()) {
                    return true;
                }
            }
        }
        return false;
    }
}

