package games.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @DisplayName("Given a REST state request with an invalid token When the endpoint is called Then unauthorized error payload is returned")
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

        } finally {
            server.stop();
        }
    }
}

