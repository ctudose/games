package games.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * REST/HTTP implementation of {@link CheckersConnection}.
 *
 * Flow:
 * - POST /join with {roomId, role, name} to obtain a token
 * - Poll GET /state?roomId=...&token=... and redraw only when "version" changes
 * - POST /move with {roomId, token, xFrom,yFrom,xTo,yTo}
 */
public class RestCheckersClientConnection implements CheckersConnection {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long POLL_INTERVAL_MS = 250L;

    private final String host;
    private final int port;
    private final String roomId;
    private final int requestedPlayerIndex; // 0 or 1, or -1 for spectator
    private final String playerName;
    private final Consumer<NetworkGameState> stateListener;
    private final Consumer<String> errorListener;

    private final HttpClient httpClient;

    private volatile boolean closed = false;
    private volatile String token;
    private volatile long lastVersion = -1;

    public RestCheckersClientConnection(String host,
                                        int port,
                                        String roomId,
                                        int requestedPlayerIndex,
                                        String playerName,
                                        Consumer<NetworkGameState> stateListener,
                                        Consumer<String> errorListener) {
        this.host = host;
        this.port = port;
        this.roomId = roomId;
        this.requestedPlayerIndex = requestedPlayerIndex;
        this.playerName = playerName;
        this.stateListener = stateListener;
        this.errorListener = errorListener;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Test/integration helper: create a connection using an already-issued REST session token.
     * This skips the POST /join step and immediately starts polling /state.
     */
    public RestCheckersClientConnection(String host,
                                        int port,
                                        String roomId,
                                        int requestedPlayerIndex,
                                        String playerName,
                                        Consumer<NetworkGameState> stateListener,
                                        Consumer<String> errorListener,
                                        String token) {
        this(host, port, roomId, requestedPlayerIndex, playerName, stateListener, errorListener);
        this.token = token;
    }

    @Override
    public void connectAsync() {
        Thread t = new Thread(this::runConnection, "RestCheckersClientConnection");
        t.setDaemon(true);
        t.start();
    }

    private void runConnection() {
        try {
            if (token == null || token.isEmpty()) {
                this.token = join();
                if (token == null || token.isEmpty()) {
                    notifyError("REST join failed: missing token");
                    return;
                }
            }

            while (!closed) {
                NetworkGameState state = pollState();
                if (state != null) {
                    notifyState(state);
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            if (!closed) {
                notifyError("REST connection error: " + e.getMessage());
            }
        }
    }

    private String join() throws IOException, InterruptedException {
        String role = (requestedPlayerIndex == 0) ? "P1" : (requestedPlayerIndex == 1 ? "P2" : "S");

        var root = MAPPER.createObjectNode();
        root.put("roomId", roomId);
        root.put("role", role);
        root.put("name", playerName);

        String body = MAPPER.writeValueAsString(root);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(buildUri("/join"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IOException("REST join failed with HTTP " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode json = MAPPER.readTree(resp.body());
        if (json.has("ok") && json.get("ok").asBoolean(false) && json.has("token")) {
            return json.get("token").asText("");
        }

        // Server may respond with {type:"error", message:"..."}
        if (json.has("message")) {
            throw new IOException("REST join failed: " + json.get("message").asText(""));
        }

        throw new IOException("REST join failed: unexpected response");
    }

    private NetworkGameState pollState() throws IOException, InterruptedException {
        String tokenLocal = token;
        if (tokenLocal == null) {
            return null;
        }

        // NOTE: roomId is included both in URL and in payload by design.
        // This makes it easier to validate token-room pairing server-side.
        String tokenQ = URLEncoder.encode(tokenLocal, StandardCharsets.UTF_8);
        String roomQ = URLEncoder.encode(roomId == null ? "" : roomId, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(buildUri("/state?roomId=" + roomQ + "&token=" + tokenQ))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IOException("REST state failed with HTTP " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode root = MAPPER.readTree(resp.body());
        long version = root.has("version") ? root.get("version").asLong(-1L) : -1L;
        if (version == lastVersion) {
            return null;
        }

        NetworkGameState state = parseState(root);
        if (state != null) {
            lastVersion = version;
        }
        return state;
    }

    @Override
    public void sendMove(int xFrom, int yFrom, int xTo, int yTo) {
        if (closed) {
            return;
        }
        if (requestedPlayerIndex < 0) {
            // Spectator cannot move.
            return;
        }
        try {
            String tokenLocal = token;
            if (tokenLocal == null || tokenLocal.isEmpty()) {
                return;
            }

            var root = MAPPER.createObjectNode();
            root.put("roomId", roomId);
            root.put("token", tokenLocal);
            root.put("xFrom", xFrom);
            root.put("yFrom", yFrom);
            root.put("xTo", xTo);
            root.put("yTo", yTo);

            String body = MAPPER.writeValueAsString(root);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(buildUri("/move"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                String msg = resp.body();
                try {
                    JsonNode err = MAPPER.readTree(resp.body());
                    if (err.has("message")) {
                        msg = err.get("message").asText(resp.body());
                    }
                } catch (Exception ignored) {
                }
                notifyError("REST move rejected: " + msg);
            }
        } catch (Exception e) {
            notifyError("REST move failed: " + e.getMessage());
        }
    }

    private NetworkGameState parseState(JsonNode root) {
        int playerAtMoveIndex = root.has("playerAtMoveIndex") ? root.get("playerAtMoveIndex").asInt(0) : 0;
        boolean draw = root.has("draw") && root.get("draw").asBoolean(false);
        int winnerIndex = root.has("winnerIndex") ? root.get("winnerIndex").asInt(-1) : -1;

        String[] playerNames = new String[]{"", ""};
        JsonNode players = root.get("players");
        if (players != null && players.isArray()) {
            for (JsonNode p : players) {
                int idx = p.has("index") ? p.get("index").asInt(-1) : -1;
                if (idx >= 0 && idx < playerNames.length) {
                    playerNames[idx] = p.has("name") ? p.get("name").asText("") : "";
                }
            }
        }

        char[][] board = new char[8][8];
        JsonNode rowsNode = root.get("board");
        if (!(rowsNode instanceof com.fasterxml.jackson.databind.node.ArrayNode)) {
            return null;
        }
        var rows = (com.fasterxml.jackson.databind.node.ArrayNode) rowsNode;
        if (rows.size() != 8) {
            return null;
        }

        for (int y = 0; y < 8; y++) {
            String row = rows.get(y).asText("");
            if (row.length() < 8) {
                row = String.format("%-8s", row).replace(' ', '.');
            }
            for (int x = 0; x < 8; x++) {
                board[x][y] = row.charAt(x);
            }
        }

        return new NetworkGameState(playerAtMoveIndex, playerNames, draw, winnerIndex, board);
    }

    private void notifyState(NetworkGameState state) {
        if (stateListener == null) {
            return;
        }
        Platform.runLater(() -> stateListener.accept(state));
    }

    private void notifyError(String message) {
        if (errorListener == null) {
            return;
        }
        Platform.runLater(() -> errorListener.accept(message));
    }

    private URI buildUri(String pathAndQuery) {
        return URI.create("http://" + host + ":" + port + pathAndQuery);
    }

    @Override
    public void close() {
        closed = true;
    }
}

