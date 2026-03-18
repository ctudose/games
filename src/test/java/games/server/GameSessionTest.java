package games.server;

import games.logics.GameMove;
import games.logics.GameMoveResult;
import games.logics.checkers.CheckersGame;
import games.logics.checkers.CheckersGameMove;
import games.logics.checkers.RobotMove;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameSessionTest {

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession();
        session.setPlayer(0, "Player0");
        session.setPlayer(1, "Player1");
        session.start();
    }

    @Test
    void applyMoveRejectsUnknownPlayerIndex() {
        GameMoveResult result = session.applyMove(2, 0, 0, 1, 1);
        assertEquals(GameMoveResult.NOT_ONE_OF_PLAYERS, result);
    }

    @Test
    void moveFromWrongTurnPlayerIsRejected() {
        CheckersGame engine = session.getGame();
        GameMove robotMove = RobotMove.getRobotMove(engine);
        assertNotNull(robotMove, "RobotMove should find a move from the initial position");

        CheckersGameMove move = (CheckersGameMove) robotMove;

        // Try to apply the move as the wrong player (1 instead of 0)
        GameMoveResult result = session.applyMove(1, move.getXFrom(), move.getYFrom(), move.getXTo(), move.getYTo());
        assertEquals(GameMoveResult.NOT_ALLOWED_TO_MOVE_NOW, result);
    }

    @Test
    void legalMoveFromCorrectPlayerIsAccepted() {
        CheckersGame engine = session.getGame();
        GameMove robotMove = RobotMove.getRobotMove(engine);
        assertNotNull(robotMove, "RobotMove should find a move from the initial position");

        CheckersGameMove move = (CheckersGameMove) robotMove;

        GameMoveResult result = session.applyMove(0, move.getXFrom(), move.getYFrom(), move.getXTo(), move.getYTo());

        assertEquals(GameMoveResult.CORRECT_MOVE, result);
    }
}

