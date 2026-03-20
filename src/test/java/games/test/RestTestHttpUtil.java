package games.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RestTestHttpUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RestTestHttpUtil() {
    }

    public static String createRoom(HttpClient http, String host, int port) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://" + host + ":" + port + "/rooms/create"))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = MAPPER.readTree(resp.body());
        assertAllRoomResponse(resp, root);
        return root.get("roomId").asText();
    }

    public static String join(HttpClient http, String host, int port, String roomId, String role, String name) throws IOException, InterruptedException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("roomId", roomId);
        body.put("role", role);
        body.put("name", name);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://" + host + ":" + port + "/join"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = MAPPER.readTree(resp.body());
        assertTrue(resp.statusCode() >= 200 && resp.statusCode() < 300);
        assertTrue(root.get("ok").asBoolean(false));
        return root.get("token").asText();
    }

    public static JsonNode getJson(HttpClient http, String host, int port, String pathAndQuery) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://" + host + ":" + port + pathAndQuery))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(resp.statusCode() >= 200 && resp.statusCode() < 300);
        return MAPPER.readTree(resp.body());
    }

    public static void postMove(HttpClient http,
                                String host,
                                int port,
                                String roomId,
                                String token,
                                int xFrom,
                                int yFrom,
                                int xTo,
                                int yTo) throws IOException, InterruptedException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("roomId", roomId);
        body.put("token", token);
        body.put("xFrom", xFrom);
        body.put("yFrom", yFrom);
        body.put("xTo", xTo);
        body.put("yTo", yTo);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://" + host + ":" + port + "/move"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(resp.statusCode() >= 200 && resp.statusCode() < 300, "Move should be accepted");
    }

    private static void assertAllRoomResponse(HttpResponse<String> resp, JsonNode root) {
        assertAll(
                () -> assertTrue(resp.statusCode() >= 200 && resp.statusCode() < 300),
                () -> assertEquals("room", root.get("type").asText())
        );
    }
}

