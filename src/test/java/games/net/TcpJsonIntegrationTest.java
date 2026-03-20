package games.net;

import games.server.CheckersServer;
import games.test.JfxTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TcpJsonIntegrationTest {

    @Test
    @DisplayName("Given a running TCP JSON server and two clients When player 1 makes a legal move and then moves again out of turn Then state updates and wrong-turn error is returned")
    void tcpJsonJoinMoveAndWrongTurn() throws InterruptedException {
        JfxTestUtil.initJfx();

        String host = "127.0.0.1";
        int textPort = JfxTestUtil.freePort();
        int jsonPort = JfxTestUtil.freePort();
        String roomId = "default";

        CheckersServer server = new CheckersServer();
        server.startTcp(host, textPort, jsonPort);

        JsonCheckersClientConnection p1 = null;
        JsonCheckersClientConnection p2 = null;
        try {
            AtomicReference<NetworkGameState> latestP1State = new AtomicReference<>();
            AtomicReference<String> wrongTurnError = new AtomicReference<>();

            CountDownLatch initialP1StateLatch = new CountDownLatch(1);
            CountDownLatch afterMoveLatch = new CountDownLatch(1);
            CountDownLatch wrongTurnErrorLatch = new CountDownLatch(1);

            AtomicInteger p1StateCount = new AtomicInteger(0);

            p1 = new JsonCheckersClientConnection(
                    host,
                    jsonPort,
                    roomId,
                    0,
                    "P1",
                    state -> {
                        latestP1State.set(state);
                        int n = p1StateCount.incrementAndGet();
                        if (n == 1) {
                            initialP1StateLatch.countDown();
                        }
                        if (state.getPlayerAtMoveIndex() == 1 && n >= 2) {
                            afterMoveLatch.countDown();
                        }
                    },
                    err -> {
                        wrongTurnError.set(err);
                        wrongTurnErrorLatch.countDown();
                    }
            );

            p2 = new JsonCheckersClientConnection(
                    host,
                    jsonPort,
                    roomId,
                    1,
                    "P2",
                    state -> {
                        // we only need P2 to be connected so the move broadcast reaches both players
                    },
                    err -> {
                        // ignore in this test; assertions are based on P1's wrong-turn behavior
                    }
            );

            p1.connectAsync();
            p2.connectAsync();

            assertTrue(initialP1StateLatch.await(5, TimeUnit.SECONDS), "Expected initial state callback for P1");

            // A known opening move for playerIndex=0 (color=false pieces on the initial board).
            // (1,2) -> (0,3) should be a legal simple move and no capture is available initially.
            p1.sendMove(1, 2, 0, 3);

            assertTrue(afterMoveLatch.await(5, TimeUnit.SECONDS), "Expected state callback after P1's move");

            // Wrong turn: P1 tries to move again while playerAtMoveIndex == 1 (P2's turn).
            p1.sendMove(1, 2, 0, 3);

            NetworkGameState st = latestP1State.get();
            assertAll(
                    () -> assertTrue(wrongTurnErrorLatch.await(5, TimeUnit.SECONDS), "Expected wrong-turn error callback"),
                    () -> assertNotNull(wrongTurnError.get(), "Expected an error message"),
                    () -> assertTrue(wrongTurnError.get().toLowerCase().contains("not your turn"), "Expected 'Not your turn' error"),
                    () -> assertNotNull(st),
                    () -> assertEquals(1, st.getPlayerAtMoveIndex(), "After P1's move, it should be P2's turn")
            );
        } finally {
            if (p1 != null) {
                p1.close();
            }
            if (p2 != null) {
                p2.close();
            }
            server.stopTcp();
        }
    }
}

