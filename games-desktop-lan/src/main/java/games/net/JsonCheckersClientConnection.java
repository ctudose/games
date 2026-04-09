package games.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * JSON line-delimited implementation of CheckersConnection used by the JavaFX UI
 * to talk to CheckersServer when server.protocol=json.
 *
 * Protocol (one JSON object per line):
 * Client -> Server:
 *   {"type":"join","role":"P1","name":"Alice"}
 *   {"type":"move","from":{"x":0,"y":0},"to":{"x":1,"y":1}}
 *   {"type":"roles"}
 *
 * Server -> Client:
 *   {"type":"error","message":"..."}
 *   {"type":"state", ...}
 */
public class JsonCheckersClientConnection implements CheckersConnection {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String host;
    private final int port;
    private final int requestedPlayerIndex; // 0 or 1
    private final String playerName;
    private final String roomId;
    private final Consumer<NetworkGameState> stateListener;
    private final Consumer<String> errorListener;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public JsonCheckersClientConnection(String host,
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
    }

    @Override
    public void connectAsync() {
        Thread t = new Thread(this::runConnection, "JsonCheckersClientConnection");
        t.setDaemon(true);
        t.start();
    }

    private void runConnection() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(socket.getOutputStream(), true);

            sendJoin();

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                JsonNode root;
                try {
                    root = MAPPER.readTree(line);
                } catch (Exception e) {
                    notifyError("Invalid JSON from server");
                    continue;
                }

                String type = root.has("type") ? root.get("type").asText("") : "";

                if ("error".equalsIgnoreCase(type)) {
                    String msg = root.has("message") ? root.get("message").asText("") : "";
                    notifyError(msg);
                } else if ("state".equalsIgnoreCase(type)) {
                    NetworkGameState state = parseState(root);
                    if (state != null) {
                        notifyState(state);
                    }
                }
            }
        } catch (IOException e) {
            notifyError("Connection error: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void sendJoin() {
        String role = (requestedPlayerIndex == 0) ? "P1" : (requestedPlayerIndex == 1 ? "P2" : "S");
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "join");
        if (roomId != null) {
            root.put("roomId", roomId);
        }
        root.put("role", role);
        root.put("name", playerName);
        sendJson(root);
    }

    @Override
    public void sendMove(int xFrom, int yFrom, int xTo, int yTo) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "move");
        if (roomId != null) {
            root.put("roomId", roomId);
        }

        ObjectNode from = root.putObject("from");
        from.put("x", xFrom);
        from.put("y", yFrom);

        ObjectNode to = root.putObject("to");
        to.put("x", xTo);
        to.put("y", yTo);

        sendJson(root);
    }

    private void sendJson(JsonNode node) {
        if (out == null) {
            return;
        }

        try {
            out.println(MAPPER.writeValueAsString(node));
            out.flush();
        } catch (Exception e) {
            notifyError("Failed to send JSON: " + e.getMessage());
        }
    }

    private NetworkGameState parseState(JsonNode root) {
        int playerAtMoveIndex = root.has("playerAtMoveIndex") ? root.get("playerAtMoveIndex").asInt(0) : 0;
        boolean draw = root.has("draw") && root.get("draw").asBoolean(false);
        int winnerIndex = root.has("winnerIndex") ? root.get("winnerIndex").asInt(-1) : -1;

        String[] playerNames = new String[] { "", "" };
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
        ArrayNode rows = (root.get("board") instanceof ArrayNode) ? (ArrayNode) root.get("board") : null;
        if (rows == null || rows.size() != 8) {
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

    @Override
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}

