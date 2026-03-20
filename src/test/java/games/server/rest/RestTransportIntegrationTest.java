package games.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import games.net.RestCheckersClientConnection;
import games.test.JfxTestUtil;
import games.net.NetworkGameState;
import games.test.RestTestHttpUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static games.test.RestTestHttpUtil.getJson;
import static org.junit.jupiter.api.Assertions.*;

class RestTransportIntegrationTest {

    @Test
    @DisplayName("Given a running REST server and joined players When polling state and submitting a legal move Then versioning and board updates are observed")
    void restRoomsJoinRolesStateVersionAndMove() throws IOException, InterruptedException {
        JfxTestUtil.initJfx();

        String host = "127.0.0.1";
        int port = JfxTestUtil.freePort();

        CheckersRestServer server = new CheckersRestServer(host, port);
        server.start();

        RestCheckersClientConnection client = null;

        try {
            HttpClient http = HttpClient.newHttpClient();

            String roomId = RestTestHttpUtil.createRoom(http, host, port);
            assertNotNull(roomId);

            // /roles initial
            JsonNode roles = getJson(http, host, port, "/roles?roomId=" + roomId);
            assertAll(
                    () -> assertFalse(roles.get("p0Taken").asBoolean(true), "Expected P1 free"),
                    () -> assertFalse(roles.get("p1Taken").asBoolean(true), "Expected P2 free")
            );

            // /join P1 and P2
            String tokenP1 = RestTestHttpUtil.join(http, host, port, roomId, "P1", "P1");
            String tokenP2 = RestTestHttpUtil.join(http, host, port, roomId, "P2", "P2");
            assertAll(
                    () -> assertNotNull(tokenP1),
                    () -> assertNotNull(tokenP2)
            );

            // initial state
            JsonNode st1 = getJson(http, host, port, "/state?roomId=" + roomId + "&token=" + tokenP1);
            long v1 = st1.get("version").asLong();

            // Opening move for playerIndex=0 uses color=false pieces on row y=2.
            // We'll move (1,2) -> (0,3).
            String row2Before = st1.get("board").get(2).asText();
            String row3Before = st1.get("board").get(3).asText();
            assertAll(
                    () -> assertEquals('w', row2Before.charAt(1), "Expected a white man at (1,2) before move"),
                    () -> assertEquals('.', row3Before.charAt(0), "Expected (0,3) to be empty before move")
            );

            // second poll should not change version
            JsonNode st1b = getJson(http, host, port, "/state?roomId=" + roomId + "&token=" + tokenP1);
            long v1b = st1b.get("version").asLong();
            assertEquals(v1, v1b, "Version should not change without moves");

            // Start polling client; it should notify once (initial version) before we move.
            CountDownLatch initialLatch = new CountDownLatch(1);
            CountDownLatch afterMoveLatch = new CountDownLatch(1);
            AtomicInteger stateCount = new AtomicInteger(0);

            client = new RestCheckersClientConnection(
                    host,
                    port,
                    roomId,
                    0,
                    "P1",
                    (NetworkGameState state) -> {
                        int n = stateCount.incrementAndGet();
                        if (n == 1) {
                            initialLatch.countDown();
                        } else if (n == 2) {
                            afterMoveLatch.countDown();
                        }
                    },
                    err -> fail("REST client error: " + err),
                    tokenP1
            );
            client.connectAsync();

            assertTrue(initialLatch.await(5, TimeUnit.SECONDS), "Expected REST client initial state callback");

            // Make the move: (1,2) -> (0,3)
            RestTestHttpUtil.postMove(http, host, port, roomId, tokenP1, 1, 2, 0, 3);

            // After move, version should increment and board should update.
            JsonNode st2 = getJson(http, host, port, "/state?roomId=" + roomId + "&token=" + tokenP1);
            long v2 = st2.get("version").asLong();
            assertEquals(v1 + 1, v2, "Version should increment by exactly 1 after a legal move");

            String row2After = st2.get("board").get(2).asText();
            String row3After = st2.get("board").get(3).asText();
            assertAll(
                    () -> assertEquals('.', row2After.charAt(1), "Expected source (1,2) to become empty after move"),
                    () -> assertEquals('w', row3After.charAt(0), "Expected destination (0,3) to contain moved piece")
            );

            assertTrue(afterMoveLatch.await(5, TimeUnit.SECONDS), "Expected REST client to notify again after version change");
        } finally {
            if (client != null) {
                client.close();
            }
            server.stop();
        }
    }

}

