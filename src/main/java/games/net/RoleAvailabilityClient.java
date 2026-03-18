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
import java.util.function.Consumer;

/**
 * Helper to query the server for which roles (Player 1 / Player 2) are already taken.
 *
 * Protocol:
 *   Client: ROLES
 *   Server: ROLES p0Taken p1Taken   // 0 = free, 1 = taken
 */
public class RoleAvailabilityClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void queryRolesAsync(String host,
                                       int port,
                                       String roomId,
                                       Consumer<boolean[]> onResult,
                                       Consumer<String> onError) {
        Thread t = new Thread(() -> run(host, port, roomId, onResult, onError), "RoleAvailabilityClient");
        t.setDaemon(true);
        t.start();
    }

    private static void run(String host,
                            int port,
                            String roomId,
                            Consumer<boolean[]> onResult,
                            Consumer<String> onError) {
        String protocol = Config.getClientProtocol();

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            if ("json".equals(protocol)) {
                ObjectNode req = MAPPER.createObjectNode();
                req.put("type", "roles");
                if (roomId != null) {
                    req.put("roomId", roomId);
                }
                out.println(MAPPER.writeValueAsString(req));
                out.flush();
            } else {
                if (roomId != null) {
                    out.println("ROLES " + roomId);
                } else {
                    out.println("ROLES");
                }
                out.flush();
            }

            String line;

            while ((line = in.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if ("json".equals(protocol)) {
                    JsonNode root = MAPPER.readTree(line);
                    String type = root.has("type") ? root.get("type").asText("") : "";
                    if ("roles".equalsIgnoreCase(type)) {
                        boolean p0Taken = root.has("p0Taken") && root.get("p0Taken").asBoolean(false);
                        boolean p1Taken = root.has("p1Taken") && root.get("p1Taken").asBoolean(false);

                        boolean[] available = new boolean[2];
                        available[0] = !p0Taken;
                        available[1] = !p1Taken;

                        if (onResult != null) {
                            Platform.runLater(() -> onResult.accept(available));
                        }
                        break;
                    }
                } else {
                    if (line.startsWith("ROLES")) {
                        String[] parts = line.split(" ");

                        if (parts.length == 3) {
                            int p0Taken = Integer.parseInt(parts[1]);
                            int p1Taken = Integer.parseInt(parts[2]);

                            boolean[] available = new boolean[2];
                            available[0] = (p0Taken == 0);
                            available[1] = (p1Taken == 0);

                            if (onResult != null) {
                                Platform.runLater(() -> onResult.accept(available));
                            }
                        }

                        break;
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            if (onError != null) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }
    }

    // Backwards-compatible overload (default room)
    public static void queryRolesAsync(String host,
                                       int port,
                                       Consumer<boolean[]> onResult,
                                       Consumer<String> onError) {
        queryRolesAsync(host, port, null, onResult, onError);
    }
}

