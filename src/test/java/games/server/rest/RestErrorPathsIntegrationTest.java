package games.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import games.test.JfxTestUtil;
import games.test.RestTestHttpUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RestErrorPathsIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Given REST API calls with invalid token and spectator role When state and move endpoints are called Then they return error payloads with expected status codes")
    void restRejectsInvalidTokenAndSpectatorMove() throws IOException, InterruptedException {
        String host = "127.0.0.1";
        int port = JfxTestUtil.freePort();

        CheckersRestServer server = new CheckersRestServer(host, port);
        server.start();

        try {
            HttpClient http = HttpClient.newHttpClient();

            String roomId = RestTestHttpUtil.createRoom(http, host, port);
            assertNotNull(roomId);

            // invalid token on /state
            HttpResponse<String> stateResp = http.send(
                    HttpRequest.newBuilder()
                            .uri(java.net.URI.create("http://" + host + ":" + port + "/state?roomId=" + roomId + "&token=badtoken"))
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode stateRoot = MAPPER.readTree(stateResp.body());
            assertAll(
                    () -> assertEquals(401, stateResp.statusCode()),
                    () -> assertEquals("error", stateRoot.get("type").asText()),
                    () -> assertEquals("Invalid token", stateRoot.get("message").asText())
            );

            // join as spectator
            String spectatorToken = RestTestHttpUtil.join(http, host, port, roomId, "S", "Spectator");
            assertNotNull(spectatorToken);

            // spectator tries to move
            ObjectNode moveBody = MAPPER.createObjectNode();
            moveBody.put("roomId", roomId);
            moveBody.put("token", spectatorToken);
            moveBody.put("xFrom", 0);
            moveBody.put("yFrom", 5);
            moveBody.put("xTo", 1);
            moveBody.put("yTo", 4);

            HttpResponse<String> moveResp = http.send(
                    HttpRequest.newBuilder()
                            .uri(java.net.URI.create("http://" + host + ":" + port + "/move"))
                            .timeout(Duration.ofSeconds(10))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(moveBody)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode moveRoot = MAPPER.readTree(moveResp.body());
            assertAll(
                    () -> assertEquals(403, moveResp.statusCode()),
                    () -> assertEquals("error", moveRoot.get("type").asText()),
                    () -> assertEquals("Spectators cannot move", moveRoot.get("message").asText())
            );
        } finally {
            server.stop();
        }
    }
}

