package games.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import games.config.Config;
import games.logics.GameMove;
import games.logics.GameMoveResult;
import games.logics.checkers.BoardVerifier;
import games.logics.checkers.CheckersGameMove;
import games.logics.checkers.CheckersPiece;
import games.logics.checkers.RobotMove;
import games.server.CheckersServer;
import games.server.rooms.GameRoom;
import games.server.rooms.RoomManager;
import games.server.rooms.RoomSummary;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HTTP REST server with polling:
 * - POST /join
 * - GET  /state
 * - POST /move
 * - GET  /rooms, /roles
 * - POST /rooms/create
 */
public class CheckersRestServer {

    // Use the same logger category as the TCP server so REST robot logs mirror the TCP prefix.
    private static final Logger log = LogManager.getLogger(CheckersRestServer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final long SESSION_TTL_MS = 30000L;
    private static final long SESSION_CLEANUP_INTERVAL_MS = 5000L;

    private final RoomManager roomManager = new RoomManager();
    private final HttpServer server;

    private final Map<String, RestSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public CheckersRestServer() throws IOException {
        int port = Config.getRestPort();
        String host = Config.getNetworkHost();

        // HttpServer doesn't support binding to "localhost" for some environments, but it's fine for this local setup.
        this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());

        setupRoutes();
        startCleanup();
    }

    public void start() {
        log.info("CheckersRestServer starting on port {}", Config.getRestPort());
        server.start();
    }

    private void setupRoutes() {
        server.createContext("/rooms", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleListRooms(exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        });

        server.createContext("/rooms/create", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCreateRoom(exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        });

        server.createContext("/roles", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleRoles(exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        });

        server.createContext("/join", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleJoin(exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        });

        server.createContext("/move", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleMove(exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        });

        server.createContext("/state", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleState(exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        });
    }

    private void startCleanup() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (RestSession session : sessions.values()) {
                if (now - session.lastSeenMillis > SESSION_TTL_MS) {
                    sessions.remove(session.token);
                    if (session.playerIndex >= 0) {
                        GameRoom room = roomManager.get(session.roomId);
                        room.leavePlayer(session.playerIndex);
                    }
                    log.info("REST session expired: token={} role={} roomId={}", session.token, session.role, session.roomId);
                }
            }
        }, SESSION_CLEANUP_INTERVAL_MS, SESSION_CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void handleListRooms(HttpExchange exchange) throws IOException {
        log.trace("RX json: {\"type\":\"list\"}");
        List<RoomSummary> rooms = roomManager.listRooms();

        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "rooms");
        ArrayNode arr = root.putArray("rooms");

        for (RoomSummary r : rooms) {
            ObjectNode rn = MAPPER.createObjectNode();
            rn.put("roomId", r.getRoomId());
            rn.put("p1Taken", r.isP1Taken());
            rn.put("p2Taken", r.isP2Taken());
            rn.put("spectators", r.getSpectatorCount());
            arr.add(rn);
        }

        sendJson(exchange, 200, root);
    }

    private void handleCreateRoom(HttpExchange exchange) throws IOException {
        log.trace("RX json: {\"type\":\"create\"}");
        GameRoom room = roomManager.createRoom();

        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "room");
        root.put("roomId", room.getRoomId());
        sendJson(exchange, 200, root);
    }

    private void handleRoles(HttpExchange exchange) throws IOException {
        Map<String, String> q = parseQuery(exchange);
        String roomId = q.get("roomId");
        ObjectNode req = MAPPER.createObjectNode();
        req.put("type", "roles");
        if (roomId != null) {
            req.put("roomId", roomId);
        }
        log.trace("RX json: {}", toJsonString(req));

        GameRoom room = resolveRoom(roomId);

        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "roles");
        root.put("roomId", room.getRoomId());
        root.put("p0Taken", room.isPlayerSlotTaken(0));
        root.put("p1Taken", room.isPlayerSlotTaken(1));

        sendJson(exchange, 200, root);
    }

    private void handleJoin(HttpExchange exchange) throws IOException {
        JsonNode req = readJson(exchange);
        if (req == null) {
            sendError(exchange, 400, "Invalid JSON");
            return;
        }
        log.trace("RX json: {}", toJsonString(req));

        String roomId = textOrNull(req.get("roomId"));
        String role = textOrNull(req.get("role"));
        String name = textOrNull(req.get("name"));

        if (role == null) {
            sendError(exchange, 400, "Missing role");
            return;
        }
        if (name == null) {
            name = "";
        }

        role = role.trim().toUpperCase();
        GameRoom room = resolveRoom(roomId);

        int playerIndex;
        if ("P1".equals(role)) {
            playerIndex = 0;
        } else if ("P2".equals(role)) {
            playerIndex = 1;
        } else if ("S".equals(role)) {
            playerIndex = -1;
        } else {
            sendError(exchange, 400, "Invalid role: " + role);
            return;
        }

        if (playerIndex >= 0) {
            boolean ok = room.joinPlayer(playerIndex, name);
            if (!ok) {
                sendError(exchange, 409, "Role already taken");
                return;
            }
            log.info("Player {} joined room {} as {}", playerIndex, room.getRoomId(), name);
        } else {
            log.info("Spectator joined room {} as {}", room.getRoomId(), name);
        }

        String token = UUID.randomUUID().toString();
        RestSession session = new RestSession(token, room.getRoomId(), role, playerIndex, System.currentTimeMillis());
        sessions.put(token, session);

        ObjectNode root = MAPPER.createObjectNode();
        root.put("ok", true);
        root.put("token", token);
        root.put("playerIndex", playerIndex);
        sendJson(exchange, 200, root);
    }

    private void handleMove(HttpExchange exchange) throws IOException {
        JsonNode req = readJson(exchange);
        if (req == null) {
            sendError(exchange, 400, "Invalid JSON");
            return;
        }
        log.trace("RX json: {}", toJsonString(req));
        log.trace("RX json MOVE: {}", toJsonString(req));

        String roomId = textOrNull(req.get("roomId"));
        String token = textOrNull(req.get("token"));

        if (token == null || token.isEmpty()) {
            sendError(exchange, 401, "Missing token");
            return;
        }

        RestSession session = sessions.get(token);
        if (session == null) {
            sendError(exchange, 401, "Invalid token");
            return;
        }

        if (roomId == null || roomId.isEmpty()) {
            roomId = session.roomId;
        }
        if (!session.roomId.equals(roomId)) {
            sendError(exchange, 403, "Token does not match roomId");
            return;
        }

        if (session.playerIndex < 0) {
            sendError(exchange, 403, "Spectators cannot move");
            return;
        }

        int xFrom = req.has("xFrom") ? req.get("xFrom").asInt() : 0;
        int yFrom = req.has("yFrom") ? req.get("yFrom").asInt() : 0;
        int xTo = req.has("xTo") ? req.get("xTo").asInt() : 0;
        int yTo = req.has("yTo") ? req.get("yTo").asInt() : 0;

        // Refresh session lastSeen on activity
        session.lastSeenMillis = System.currentTimeMillis();

        GameRoom room = roomManager.get(session.roomId);

        GameMoveResult result;
        synchronized (room) {
            result = room.applyMove(session.playerIndex, xFrom, yFrom, xTo, yTo);
        }

        if (result == GameMoveResult.CORRECT_MOVE) {
            log.debug("Move by player {} in room {}: {} {} -> {} {}", session.playerIndex, room.getRoomId(), xFrom, yFrom, xTo, yTo);
            playRobotTurns(room);
            ObjectNode root = MAPPER.createObjectNode();
            root.put("ok", true);
            root.put("result", "CORRECT_MOVE");
            sendJson(exchange, 200, root);
        } else {
            sendError(exchange, 400, "Illegal move: " + result);
        }
    }

    private void handleState(HttpExchange exchange) throws IOException {
        Map<String, String> q = parseQuery(exchange);
        String roomId = q.get("roomId");
        String token = q.get("token");
        ObjectNode req = MAPPER.createObjectNode();
        req.put("type", "state");
        if (roomId != null) {
            req.put("roomId", roomId);
        }
        if (token != null) {
            req.put("token", token);
        }
        log.trace("RX json: {}", toJsonString(req));

        if (token == null || token.isEmpty()) {
            sendError(exchange, 401, "Missing token");
            return;
        }

        RestSession session = sessions.get(token);
        if (session == null) {
            sendError(exchange, 401, "Invalid token");
            return;
        }

        if (roomId == null || roomId.isEmpty()) {
            roomId = session.roomId;
        }
        if (!session.roomId.equals(roomId)) {
            sendError(exchange, 403, "Token does not match roomId");
            return;
        }

        // Refresh session on polling.
        session.lastSeenMillis = System.currentTimeMillis();

        GameRoom room = roomManager.get(session.roomId);
        ObjectNode root;
        synchronized (room) {
            root = encodeState(room);
        }
        sendJson(exchange, 200, root);
    }

    private void playRobotTurns(GameRoom room) {
        long delay = Config.getRobotDelayMillis();

        while (true) {
            boolean finished;
            int currentIndex;
            Object currentPlayer;

            synchronized (room) {
                finished = room.getGame().isFinished();
                if (finished) {
                    break;
                }

                currentIndex = room.getGame().getPlayerAtMoveIndex();
                currentPlayer = room.getGame().getPlayer(currentIndex);
            }

            if (currentPlayer == null) {
                break;
            }

            String name = currentPlayer.toString();
            if ((name == null) || !name.toLowerCase().startsWith("robot")) {
                break;
            }

            GameMove robotMove;
            synchronized (room) {
                robotMove = RobotMove.getRobotMove(room.getGame());
            }

            if (!(robotMove instanceof CheckersGameMove)) {
                break;
            }

            CheckersGameMove m = (CheckersGameMove) robotMove;

            // Mirror the TCP JSON robot move trace so logs look identical.
            ObjectNode jsonMove = MAPPER.createObjectNode();
            jsonMove.put("type", "move");
            jsonMove.put("roomId", room.getRoomId());
            ObjectNode from = jsonMove.putObject("from");
            from.put("x", m.getXFrom());
            from.put("y", m.getYFrom());
            ObjectNode to = jsonMove.putObject("to");
            to.put("x", m.getXTo());
            to.put("y", m.getYTo());
            try {
                log.trace("RX robot MOVE (json): {}", MAPPER.writeValueAsString(jsonMove));
            } catch (Exception ignored) {
                // ignore serialization errors in logs
            }

            GameMoveResult result;
            synchronized (room) {
                result = room.applyMove(
                        currentIndex,
                        m.getXFrom(),
                        m.getYFrom(),
                        m.getXTo(),
                        m.getYTo()
                );
            }

            if (result != GameMoveResult.CORRECT_MOVE) {
                break;
            }

            // Mirror the TCP robot move debug so logs match.
            log.debug("Robot move by player {} in room {}: {}", currentIndex, room.getRoomId(), robotMove);

            if (delay > 0L) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private ObjectNode encodeState(GameRoom room) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "state");
        root.put("roomId", room.getRoomId());
        root.put("version", room.getStateVersion());
        root.put("playerAtMoveIndex", room.getGame().getPlayerAtMoveIndex());

        ArrayNode players = root.putArray("players");
        for (int i = 0; i < 2; i++) {
            Object p = room.getGame().getPlayer(i);
            ObjectNode pNode = MAPPER.createObjectNode();
            pNode.put("index", i);
            pNode.put("name", p == null ? "" : p.toString());
            players.add(pNode);
        }

        root.put("draw", room.getGame().isDraw());

        int winnerIndex = -1;
        Object p0 = room.getGame().getPlayer(0);
        Object p1 = room.getGame().getPlayer(1);
        if (room.getGame().getWinner() != null) {
            if (room.getGame().getWinner().equals(p0)) {
                winnerIndex = 0;
            } else if (room.getGame().getWinner().equals(p1)) {
                winnerIndex = 1;
            }
        }
        root.put("winnerIndex", winnerIndex);

        ArrayNode board = root.putArray("board");
        CheckersPiece[][] matrix = room.getGame().getBoard();
        for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
                CheckersPiece piece = matrix[x][y];
                if (piece == null) {
                    line.append('.');
                } else {
                    boolean color = piece.getColor();
                    boolean king = piece.isKing();
                    if (!color) {
                        line.append(king ? 'W' : 'w');
                    } else {
                        line.append(king ? 'B' : 'b');
                    }
                }
            }
            board.add(line.toString());
        }

        return root;
    }

    private GameRoom resolveRoom(String roomId) {
        return roomManager.get(roomId);
    }

    private Map<String, String> parseQuery(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> result = new ConcurrentHashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }

        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    private JsonNode readJson(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            if (in == null) {
                return null;
            }
            return MAPPER.readTree(in);
        }
    }

    private void sendJson(HttpExchange exchange, int status, JsonNode node) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(node);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "error");
        root.put("message", message);
        log.trace("TX error {}: {}", status, message);
        sendJson(exchange, status, root);
    }

    private String toJsonString(JsonNode node) {
        try {
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(node);
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String s = node.asText(null);
        if (s == null) {
            return null;
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static final class RestSession {
        private final String token;
        private final String roomId;
        private final String role;
        private final int playerIndex;
        private volatile long lastSeenMillis;

        private RestSession(String token, String roomId, String role, int playerIndex, long lastSeenMillis) {
            this.token = token;
            this.roomId = roomId;
            this.role = role;
            this.playerIndex = playerIndex;
            this.lastSeenMillis = lastSeenMillis;
        }
    }
}

