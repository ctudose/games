package games.logics.checkers;

import games.logics.GameMove;
import games.logics.GameMoveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}

