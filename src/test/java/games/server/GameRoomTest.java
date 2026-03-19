package games.server;

import games.logics.GameMoveResult;
import games.logics.GameMove;
import games.logics.checkers.CheckersGameMove;
import games.logics.checkers.RobotMove;
import games.server.rooms.GameRoom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

public class GameRoomTest {

    private static final class DummyHandler extends CheckersServer.ClientHandler {
        DummyHandler(CheckersServer server) {
            server.super(null);
        }

        @Override
        boolean isJson() {
            return false;
        }

        @Override
        void handleLoop() {
            // not used in unit tests
        }

        @Override
        public void acceptRawMessage(String message) {
            // not used in unit tests
        }
    }

    @Test
    void joiningPlayerSlotsWorks() {
        GameRoom room = new GameRoom("r1");
        CheckersServer server = new CheckersServer();

        DummyHandler p1 = new DummyHandler(server);
        DummyHandler p2 = new DummyHandler(server);

        assertAll(
                () -> assertTrue(room.joinPlayer(0, p1, "A")),
                () -> assertTrue(room.joinPlayer(1, p2, "B")),
                () -> assertFalse(room.joinPlayer(0, new DummyHandler(server), "C")),
                () -> assertFalse(room.joinPlayer(1, new DummyHandler(server), "D"))
        );
    }

    @Test
    void spectatorJoinDoesNotBlockPlayers() {
        GameRoom room = new GameRoom("r2");
        CheckersServer server = new CheckersServer();

        DummyHandler s1 = new DummyHandler(server);
        DummyHandler s2 = new DummyHandler(server);
        room.joinSpectator(s1);
        room.joinSpectator(s2);

        DummyHandler p1 = new DummyHandler(server);
        assertTrue(room.joinPlayer(0, p1, "Player"));
    }

    @Test
    void applyMoveReturnsEngineResult() {
        GameRoom room = new GameRoom("r3");
        CheckersServer server = new CheckersServer();

        DummyHandler p1 = new DummyHandler(server);
        DummyHandler p2 = new DummyHandler(server);
        assertTrue(room.joinPlayer(0, p1, "P1"));
        assertTrue(room.joinPlayer(1, p2, "P2"));

        // Typical opening move for player 0 (white): (2,5)->(3,4) should be legal in standard checkers setup
        GameMoveResult result = room.applyMove(0, 2, 5, 3, 4);
        assertNotNull(result);
    }

    @Test
    void stateVersionIncrementsOnCorrectMove() {
        GameRoom room = new GameRoom("r4");
        CheckersServer server = new CheckersServer();

        DummyHandler p1 = new DummyHandler(server);
        DummyHandler p2 = new DummyHandler(server);
        assertTrue(room.joinPlayer(0, p1, "P1"));
        assertTrue(room.joinPlayer(1, p2, "P2"));

        long v0 = room.getStateVersion();

        GameMove robotMove = RobotMove.getRobotMove(room.getGame());
        assertNotNull(robotMove);
        assertTrue(robotMove instanceof CheckersGameMove);

        CheckersGameMove m = (CheckersGameMove) robotMove;
        int playerIndex = room.getGame().getPlayerAtMoveIndex();

        GameMoveResult result = room.applyMove(
                playerIndex,
                m.getXFrom(),
                m.getYFrom(),
                m.getXTo(),
                m.getYTo()
        );

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result),
                () -> assertEquals(v0 + 1, room.getStateVersion()));
    }
}

