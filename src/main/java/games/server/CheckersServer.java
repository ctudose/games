package games.server;

import games.config.Config;
import games.logics.GameMove;
import games.logics.GameMoveResult;
import games.logics.checkers.BoardVerifier;
import games.logics.checkers.CheckersGameMove;
import games.logics.checkers.CheckersPiece;
import games.logics.checkers.RobotMove;
import games.server.rooms.GameRoom;
import games.server.rooms.RoomManager;
import games.server.rooms.RoomSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * Simple TCP server that hosts multiple rooms, each containing a CheckersGame.
 * Protocol (line-based, UTF-8):
 * <p>
 * Client -> Server:
 * JOIN P1 name
 * JOIN P2 name
 * JOIN roomId P1 name
 * JOIN roomId P2 name
 * JOIN roomId S name
 * MOVE xFrom yFrom xTo yTo
 * MOVE roomId xFrom yFrom xTo yTo
 * ROLES
 * ROLES roomId
 * CREATE
 * LIST
 * <p>
 * Server -> Client:
 * ERROR message
 * STATE
 * P <playerAtMoveIndex>
 * N 0 name0
 * N 1 name1
 * D <0|1>          // draw flag
 * W <winnerIndex>  // -1 if none, 0 or 1 if winner
 * B
 * <8 lines of 8 chars each, y=0..7>
 * END
 * ROLES p0Taken p1Taken   // each 0 (free) or 1 (taken)
 * ROOM roomId
 * ROOMS count
 * R roomId p1Taken p2Taken spectators
 * ENDROOMS
 */
public class CheckersServer {
    private static final Logger log = LogManager.getLogger(CheckersServer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RoomManager roomManager = new RoomManager();

    public static void main(String[] args) {
        new CheckersServer().runTcp();
    }

    private void runTcp() {
        int textPort = Config.getTextPort();
        int jsonPort = Config.getJsonPort();

        log.info("CheckersServer starting TCP with text port {} and json port {}", textPort, jsonPort);

        Thread textThread = new Thread(() -> acceptLoop(textPort, false), "TextListener-" + textPort);
        Thread jsonThread = new Thread(() -> acceptLoop(jsonPort, true), "JsonListener-" + jsonPort);

        textThread.setDaemon(false);
        jsonThread.setDaemon(false);

        textThread.start();
        jsonThread.start();
    }

    private void acceptLoop(int port, boolean json) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Listener started on port {} (protocol={})", port, json ? "json" : "text");

            while (true) {
                Socket socket = serverSocket.accept();
                log.info("Incoming connection on {} from {}", port, socket.getRemoteSocketAddress());

                ClientHandler handler = json
                        ? new JsonClientHandler(socket)
                        : new TextClientHandler(socket);
                new Thread(handler, (json ? "JsonClientHandler-" : "TextClientHandler-") + socket.getRemoteSocketAddress())
                        .start();
            }
        } catch (IOException e) {
            log.error("Listener IOException on port {}", port, e);
        }
    }

    private GameRoom resolveRoomOrDefault(String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            return roomManager.getOrCreateDefaultRoom();
        }
        GameRoom room = roomManager.get(roomId);
        return (room != null) ? room : roomManager.getOrCreateDefaultRoom();
    }

    private synchronized void joinRoom(String roomId, String role, ClientHandler handler, String name) {
        GameRoom room = resolveRoomOrDefault(roomId);
        handler.roomId = room.getRoomId();

        if ("S".equalsIgnoreCase(role)) {
            handler.spectator = true;
            handler.playerIndex = -1;
            room.joinSpectator(handler);
            log.info("Spectator joined room {} as {}", room.getRoomId(), name);
            sendState(room, handler);
            return;
        }

        int index;
        if ("P1".equalsIgnoreCase(role)) {
            index = 0;
        } else if ("P2".equalsIgnoreCase(role)) {
            index = 1;
        } else {
            handler.sendError("Invalid role: " + role);
            return;
        }

        boolean ok = room.joinPlayer(index, handler, name);
        if (!ok) {
            handler.sendError("Role already taken");
            return;
        }

        handler.spectator = false;
        handler.playerIndex = index;
        log.info("Player {} joined room {} as {}", index, room.getRoomId(), name);
        sendState(room, handler);
        broadcastState(room);
    }

    private synchronized void handleMove(GameRoom room, ClientHandler handler, int xFrom, int yFrom, int xTo, int yTo) {
        if (handler.spectator) {
            handler.sendError("Not allowed to move as spectator");
            return;
        }
        if (handler.playerIndex < 0) {
            handler.sendError("You must JOIN first");
            return;
        }
        if (room.getGame().isFinished()) {
            handler.sendError("Game is finished");
            return;
        }
        if (room.getGame().getPlayerAtMoveIndex() != handler.playerIndex) {
            handler.sendError("Not your turn");
            return;
        }

        GameMoveResult result = room.applyMove(handler.playerIndex, xFrom, yFrom, xTo, yTo);
        if (result == GameMoveResult.CORRECT_MOVE) {
            log.debug("Move by player {} in room {}: {} {} -> {} {}", handler.playerIndex, room.getRoomId(), xFrom, yFrom, xTo, yTo);
            broadcastState(room);
            maybePlayRobotTurn(room);
        } else {
            handler.sendError("Illegal move: " + result);
        }
    }

    private void broadcastState(GameRoom room) {
        CheckersServer.ClientHandler p0 = room.getPlayer(0);
        CheckersServer.ClientHandler p1 = room.getPlayer(1);
        if (p0 != null) {
            sendState(room, p0);
        }
        if (p1 != null) {
            sendState(room, p1);
        }
        for (ClientHandler spectator : room.getSpectators()) {
            if (spectator != null) {
                sendState(room, spectator);
            }
        }
    }

    private void sendState(GameRoom room, ClientHandler client) {
        if (!client.canSend()) {
            return;
        }
        if (client.isJson()) {
            sendStateJson(room, client);
            return;
        }

        client.sendLine("STATE");
        client.sendLine("P " + room.getGame().getPlayerAtMoveIndex());

        Object p0 = room.getGame().getPlayer(0);
        Object p1 = room.getGame().getPlayer(1);
        client.sendLine("N 0 " + (p0 == null ? "" : p0.toString()));
        client.sendLine("N 1 " + (p1 == null ? "" : p1.toString()));

        client.sendLine("D " + (room.getGame().isDraw() ? 1 : 0));

        int winnerIndex = -1;
        if (room.getGame().getWinner() != null) {
            if (room.getGame().getWinner().equals(p0)) {
                winnerIndex = 0;
            } else if (room.getGame().getWinner().equals(p1)) {
                winnerIndex = 1;
            }
        }
        client.sendLine("W " + winnerIndex);

        client.sendLine("B");
        CheckersPiece[][] board = room.getGame().getBoard();
        for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
                CheckersPiece piece = board[x][y];
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
            client.sendLine(line.toString());
        }
        client.sendLine("END");
    }

    private void sendStateJson(GameRoom room, ClientHandler client) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "state");
        root.put("roomId", room.getRoomId());
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

        sendJsonLine(client, root);
    }

    /**
     * If the player whose turn it is has name "robot" (case-insensitive),
     * automatically generate and apply random moves using RobotMove until
     * it is no longer that player's turn, the game finishes, or no legal
     * move is available.
     */
    private synchronized void maybePlayRobotTurn(GameRoom room) {
        long delay = Config.getRobotDelayMillis();

        while (!room.getGame().isFinished()) {
            int currentIndex = room.getGame().getPlayerAtMoveIndex();
            Object currentPlayer = room.getGame().getPlayer(currentIndex);

            if (currentPlayer == null) {
                break;
            }

            String name = currentPlayer.toString();
            if ((name == null) || !name.toLowerCase().startsWith("robot")) {
                break;
            }

            GameMove robotMove = RobotMove.getRobotMove(room.getGame());
            if (!(robotMove instanceof CheckersGameMove)) {
                break;
            }

            CheckersGameMove m = (CheckersGameMove) robotMove;
            ClientHandler robotHandler = room.getPlayer(currentIndex);
            boolean asJson = (robotHandler != null) && robotHandler.isJson();

            if (asJson) {
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
                }
            } else {
                log.trace("RX robot MOVE (text): MOVE {} {} {} {} {}", room.getRoomId(), m.getXFrom(), m.getYFrom(), m.getXTo(), m.getYTo());
            }

            if (delay > 0L) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            GameMoveResult result = room.getGame().checkMove(currentPlayer, robotMove);
            if (result != GameMoveResult.CORRECT_MOVE) {
                break;
            }

            log.debug("Robot move by player {} in room {}: {}", currentIndex, room.getRoomId(), robotMove);
            broadcastState(room);
        }
    }

    private void sendRoles(GameRoom room, ClientHandler client) {
        if (!client.canSend()) {
            return;
        }

        int p0Taken = room.isPlayerSlotTaken(0) ? 1 : 0;
        int p1Taken = room.isPlayerSlotTaken(1) ? 1 : 0;

        if (client.isJson()) {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("type", "roles");
            root.put("roomId", room.getRoomId());
            root.put("p0Taken", p0Taken == 1);
            root.put("p1Taken", p1Taken == 1);
            sendJsonLine(client, root);
            return;
        }

        client.sendLine("ROLES " + p0Taken + " " + p1Taken);
    }

    private void sendJsonLine(ClientHandler client, JsonNode node) {
        if (!client.canSend()) {
            return;
        }

        try {
            client.sendLine(MAPPER.writeValueAsString(node));
        } catch (Exception e) {
            log.error("Failed to write JSON message", e);
        }
    }

    public abstract class ClientHandler implements Runnable {

        private final Socket socket;
        protected BufferedReader in;
        protected PrintWriter out;
        protected int playerIndex = -1;
        protected boolean spectator = false;
        protected String roomId = RoomManager.DEFAULT_ROOM_ID;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        abstract boolean isJson();

        boolean canSend() { return out != null; }

        void sendLine(String line) {
            if (out != null) {
                out.println(line);
                out.flush();
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(socket.getOutputStream(), true);

                handleLoop();
            } catch (IOException e) {
                log.info("Client disconnected: {}", e.getMessage());
            } finally {
                cleanup();
            }
        }

        abstract void handleLoop() throws IOException;

        public abstract void acceptRawMessage(String message);

        final void handleJoinCommand(String roomId, String role, String name) {
            joinRoom(roomId, role, this, name);
        }

        final void handleMoveCommand(String roomId, int xFrom, int yFrom, int xTo, int yTo) {
            GameRoom room = resolveRoomOrDefault(roomId != null ? roomId : this.roomId);
            this.roomId = room.getRoomId();
            handleMove(room, this, xFrom, yFrom, xTo, yTo);
        }

        void sendError(String message) {
            if (!canSend()) {
                return;
            }

            if (isJson()) {
                ObjectNode root = MAPPER.createObjectNode();
                root.put("type", "error");
                root.put("roomId", roomId);
                root.put("message", message);
                sendJsonLine(this, root);
            } else {
                sendLine("ERROR " + message);
            }
        }

        public void cleanup() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ignored) {
            }

            GameRoom room = roomManager.get(roomId);
            if (room != null) {
                room.leavePlayer(this);
                room.leaveSpectator(this);
            }
        }
    }

    private final class TextClientHandler extends ClientHandler {
        TextClientHandler(Socket socket) {
            super(socket);
        }

        @Override
        boolean isJson() {
            return false;
        }

        @Override
        void handleLoop() throws IOException {
            String line;
            while ((line = in.readLine()) != null) {
                acceptRawMessage(line);
            }
        }

        @Override
        public void acceptRawMessage(String message) {
            String line = (message == null) ? "" : message.trim();

            if (line.isEmpty()) {
                return;
            }

            log.trace("RX text: {}", line);

            String[] parts = line.split(" ");

            if ("CREATE".equalsIgnoreCase(parts[0]) && (parts.length == 1)) {
                GameRoom room = roomManager.createRoom();
                if (isJson()) {
                    // unreachable for text handler
                    return;
                }
                sendLine("ROOM " + room.getRoomId());
            } else if ("LIST".equalsIgnoreCase(parts[0]) && (parts.length == 1)) {
                List<RoomSummary> rooms = roomManager.listRooms();
                sendLine("ROOMS " + rooms.size());
                for (RoomSummary r : rooms) {
                    sendLine("R " + r.getRoomId() + " " + (r.isP1Taken() ? 1 : 0) + " " + (r.isP2Taken() ? 1 : 0) + " " + r.getSpectatorCount());
                }
                sendLine("ENDROOMS");
            } else if ("JOIN".equalsIgnoreCase(parts[0]) && (parts.length == 3)) {
                // legacy: JOIN P1 name
                handleJoinCommand(RoomManager.DEFAULT_ROOM_ID, parts[1], parts[2]);
            } else if ("JOIN".equalsIgnoreCase(parts[0]) && (parts.length >= 4)) {
                // room-aware: JOIN roomId P1 name
                handleJoinCommand(parts[1], parts[2], parts[3]);
            } else if ("MOVE".equalsIgnoreCase(parts[0]) && (parts.length == 5)) {
                // legacy: MOVE xFrom yFrom xTo yTo
                log.trace("RX text MOVE: {}", line);
                try {
                    int xFrom = Integer.parseInt(parts[1]);
                    int yFrom = Integer.parseInt(parts[2]);
                    int xTo = Integer.parseInt(parts[3]);
                    int yTo = Integer.parseInt(parts[4]);
                    handleMoveCommand(RoomManager.DEFAULT_ROOM_ID, xFrom, yFrom, xTo, yTo);
                } catch (NumberFormatException ex) {
                    sendError("Invalid MOVE coordinates: " + Arrays.toString(parts));
                }
            } else if ("MOVE".equalsIgnoreCase(parts[0]) && (parts.length == 6)) {
                // room-aware: MOVE roomId xFrom yFrom xTo yTo
                log.trace("RX text MOVE: {}", line);
                try {
                    String roomId = parts[1];
                    int xFrom = Integer.parseInt(parts[2]);
                    int yFrom = Integer.parseInt(parts[3]);
                    int xTo = Integer.parseInt(parts[4]);
                    int yTo = Integer.parseInt(parts[5]);
                    handleMoveCommand(roomId, xFrom, yFrom, xTo, yTo);
                } catch (NumberFormatException ex) {
                    sendError("Invalid MOVE coordinates: " + Arrays.toString(parts));
                }
            } else if ("ROLES".equalsIgnoreCase(parts[0]) && (parts.length == 1)) {
                GameRoom room = resolveRoomOrDefault(RoomManager.DEFAULT_ROOM_ID);
                sendRoles(room, this);
            } else if ("ROLES".equalsIgnoreCase(parts[0]) && (parts.length == 2)) {
                GameRoom room = resolveRoomOrDefault(parts[1]);
                sendRoles(room, this);
            } else {
                sendError("Unknown command: " + line);
            }
        }
    }

    private final class JsonClientHandler extends ClientHandler {
        JsonClientHandler(Socket socket) {
            super(socket);
        }

        @Override
        boolean isJson() {
            return true;
        }

        @Override
        void handleLoop() throws IOException {
            String line;
            while ((line = in.readLine()) != null) {
                acceptRawMessage(line);
            }
        }

        @Override
        public void acceptRawMessage(String message) {
            String line = (message == null) ? "" : message.trim();

            if (line.isEmpty()) {
                return;
            }

            log.trace("RX json: {}", line);

            JsonNode root;
            try {
                root = MAPPER.readTree(line);
            } catch (Exception e) {
                sendError("Invalid JSON");
                return;
            }

            String type = root.has("type") ? root.get("type").asText("") : "";

            if ("join".equalsIgnoreCase(type)) {
                String role = root.has("role") ? root.get("role").asText("") : "";
                String name = root.has("name") ? root.get("name").asText("") : "";
                String roomId = root.has("roomId") ? root.get("roomId").asText(RoomManager.DEFAULT_ROOM_ID) : RoomManager.DEFAULT_ROOM_ID;
                handleJoinCommand(roomId, role, name);
            } else if ("move".equalsIgnoreCase(type)) {
                log.trace("RX json MOVE: {}", line);
                JsonNode from = root.get("from");
                JsonNode to = root.get("to");

                if (from == null || to == null) {
                    sendError("Invalid move payload");
                    return;
                }

                int xFrom = from.get("x").asInt();
                int yFrom = from.get("y").asInt();
                int xTo = to.get("x").asInt();
                int yTo = to.get("y").asInt();
                String roomId = root.has("roomId") ? root.get("roomId").asText(RoomManager.DEFAULT_ROOM_ID) : RoomManager.DEFAULT_ROOM_ID;
                handleMoveCommand(roomId, xFrom, yFrom, xTo, yTo);
            } else if ("roles".equalsIgnoreCase(type)) {
                String roomId = root.has("roomId") ? root.get("roomId").asText(RoomManager.DEFAULT_ROOM_ID) : RoomManager.DEFAULT_ROOM_ID;
                GameRoom room = resolveRoomOrDefault(roomId);
                sendRoles(room, this);
            } else if ("create".equalsIgnoreCase(type)) {
                GameRoom room = roomManager.createRoom();
                ObjectNode resp = MAPPER.createObjectNode();
                resp.put("type", "room");
                resp.put("roomId", room.getRoomId());
                sendJsonLine(this, resp);
            } else if ("list".equalsIgnoreCase(type)) {
                List<RoomSummary> rooms = roomManager.listRooms();
                ObjectNode resp = MAPPER.createObjectNode();
                resp.put("type", "rooms");
                ArrayNode arr = resp.putArray("rooms");
                for (RoomSummary r : rooms) {
                    ObjectNode rn = MAPPER.createObjectNode();
                    rn.put("roomId", r.getRoomId());
                    rn.put("p1Taken", r.isP1Taken());
                    rn.put("p2Taken", r.isP2Taken());
                    rn.put("spectators", r.getSpectatorCount());
                    arr.add(rn);
                }
                sendJsonLine(this, resp);
            } else {
                sendError("Unknown message type: " + type);
            }
        }
    }


}

