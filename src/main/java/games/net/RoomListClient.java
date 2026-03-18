package games.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import games.config.Config;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RoomListClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void queryRoomsAsync(String host,
                                       int port,
                                       Consumer<List<RoomInfo>> onResult,
                                       Consumer<String> onError) {
        Thread t = new Thread(() -> runList(host, port, onResult, onError), "RoomListClient-LIST");
        t.setDaemon(true);
        t.start();
    }

    public static void createRoomAsync(String host,
                                       int port,
                                       Consumer<String> onRoomId,
                                       Consumer<String> onError) {
        Thread t = new Thread(() -> runCreate(host, port, onRoomId, onError), "RoomListClient-CREATE");
        t.setDaemon(true);
        t.start();
    }

    private static void runList(String host,
                                int port,
                                Consumer<List<RoomInfo>> onResult,
                                Consumer<String> onError) {
        String protocol = Config.getClientProtocol();

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            if ("json".equals(protocol)) {
                ObjectNode req = MAPPER.createObjectNode();
                req.put("type", "list");
                out.println(MAPPER.writeValueAsString(req));
                out.flush();
            } else {
                out.println("LIST");
                out.flush();
            }

            List<RoomInfo> rooms = readRooms(protocol, in);

            if (onResult != null) {
                Platform.runLater(() -> onResult.accept(rooms));
            }
        } catch (IOException e) {
            if (onError != null) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }
    }

    private static void runCreate(String host,
                                  int port,
                                  Consumer<String> onRoomId,
                                  Consumer<String> onError) {
        String protocol = Config.getClientProtocol();

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            if ("json".equals(protocol)) {
                ObjectNode req = MAPPER.createObjectNode();
                req.put("type", "create");
                out.println(MAPPER.writeValueAsString(req));
                out.flush();
            } else {
                out.println("CREATE");
                out.flush();
            }

            String roomId = readCreatedRoomId(protocol, in);

            if (roomId != null && onRoomId != null) {
                String finalRoomId = roomId;
                Platform.runLater(() -> onRoomId.accept(finalRoomId));
            }
        } catch (IOException e) {
            if (onError != null) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }
    }

    private static List<RoomInfo> readRooms(String protocol, BufferedReader in) throws IOException {
        if ("json".equals(protocol)) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                JsonNode root = MAPPER.readTree(line);
                String type = root.has("type") ? root.get("type").asText("") : "";
                if (!"rooms".equalsIgnoreCase(type)) {
                    continue;
                }

                List<RoomInfo> rooms = new ArrayList<>();
                JsonNode arr = root.get("rooms");
                if (arr != null && arr.isArray()) {
                    for (JsonNode r : arr) {
                        String roomId = r.has("roomId") ? r.get("roomId").asText("") : "";
                        boolean p1Taken = r.has("p1Taken") && r.get("p1Taken").asBoolean(false);
                        boolean p2Taken = r.has("p2Taken") && r.get("p2Taken").asBoolean(false);
                        int spectators = r.has("spectators") ? r.get("spectators").asInt(0) : 0;
                        if (!roomId.isEmpty()) {
                            rooms.add(new RoomInfo(roomId, p1Taken, p2Taken, spectators));
                        }
                    }
                }
                return rooms;
            }
            return new ArrayList<>();
        }

        // text
        List<RoomInfo> rooms = new ArrayList<>();
        String line;
        boolean inRooms = false;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("ROOMS")) {
                inRooms = true;
                continue;
            }
            if ("ENDROOMS".equalsIgnoreCase(line)) {
                break;
            }
            if (inRooms && line.startsWith("R ")) {
                String[] parts = line.split(" ");
                if (parts.length >= 5) {
                    String roomId = parts[1];
                    int p1Taken = Integer.parseInt(parts[2]);
                    int p2Taken = Integer.parseInt(parts[3]);
                    int spectators = Integer.parseInt(parts[4]);
                    rooms.add(new RoomInfo(roomId, p1Taken == 1, p2Taken == 1, spectators));
                }
            }
        }
        return rooms;
    }

    private static String readCreatedRoomId(String protocol, BufferedReader in) throws IOException {
        if ("json".equals(protocol)) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                JsonNode root = MAPPER.readTree(line);
                String type = root.has("type") ? root.get("type").asText("") : "";
                if ("room".equalsIgnoreCase(type) && root.has("roomId")) {
                    return root.get("roomId").asText(null);
                }
            }
            return null;
        }

        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("ROOM ")) {
                return line.substring("ROOM ".length()).trim();
            }
        }
        return null;
    }
}

