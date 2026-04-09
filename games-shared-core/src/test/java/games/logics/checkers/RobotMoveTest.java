package games.logics.checkers;

import games.logics.GameMove;
import games.logics.GameMoveResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RobotMoveTest {

    private CheckersGame game;

    @BeforeEach
    void setUp() {
        game = new CheckersGame();
        game.setPlayer(0, "Player0");
        game.setPlayer(1, "Player1");
        game.start();
    }

    @Test
    @DisplayName("Given a game state with a legal move for the current player When RobotMove selects a move Then the move is accepted as CORRECT_MOVE")
    void getRobotMoveReturnsLegalMoveFromInitialPosition() {
        // Arrange
        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());

        // Act
        GameMove move = RobotMove.getRobotMove(game);

        // Assert
        GameMoveResult result = game.checkMove(currentPlayer, move);

        assertAll(
                () -> assertNotNull(move, "RobotMove should return a move from the initial position"),
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result, "Robot move should be legal for current player")
        );
    }

    @Test
    @DisplayName("Given a board where only one capture is legal for the current player When RobotMove selects a move Then it returns that capture and checkMove accepts it")
    void getRobotMovePrefersOnlyAvailableCaptureWhenCaptureIsMandatory() {
        BoardVerifier.resetBoard(game.getBoard());
        game.getBoard()[2][2] = new CheckersPiece(false, false);
        game.getBoard()[3][3] = new CheckersPiece(true, false);

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMove move = RobotMove.getRobotMove(game);
        GameMoveResult result = game.checkMove(currentPlayer, move);

        CheckersGameMove checkersMove = (CheckersGameMove) move;
        assertAll(
                () -> assertNotNull(move),
                () -> assertEquals(2, checkersMove.getXFrom()),
                () -> assertEquals(2, checkersMove.getYFrom()),
                () -> assertEquals(4, checkersMove.getXTo()),
                () -> assertEquals(4, checkersMove.getYTo()),
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result)
        );
    }

    @Test
    @DisplayName("Given a finished board with no legal moves for the current player When RobotMove selects a move Then it returns null")
    void getRobotMoveReturnsNullWhenNoLegalMoveExists() {
        BoardVerifier.resetBoard(game.getBoard());
        game.getBoard()[0][0] = new CheckersPiece(false, false);
        game.setWinner(1);

        GameMove move = RobotMove.getRobotMove(game);
        assertNull(move);
    }
}

