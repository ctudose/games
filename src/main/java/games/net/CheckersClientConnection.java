package games.net;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP implementation of CheckersConnection used by the JavaFX UI
 * to talk to CheckersServer.
 */
public class CheckersClientConnection implements CheckersConnection {

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

    public CheckersClientConnection(String host,
                                    int port,
                                    String roomId,
                                    int requestedPlayerIndex,
                                    String playerName,
                                    Consumer<NetworkGameState> stateListener,
                                    Consumer<String> errorListener) {
        this.host                = host;
        this.port                = port;
        this.roomId              = roomId;
        this.requestedPlayerIndex = requestedPlayerIndex;
        this.playerName          = playerName;
        this.stateListener       = stateListener;
        this.errorListener       = errorListener;
    }

    @Override
    public void connectAsync() {
        Thread t = new Thread(this::runConnection, "CheckersClientConnection");
        t.setDaemon(true);
        t.start();
    }

    private void runConnection() {
        try {
            socket = new Socket(host, port);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out    = new PrintWriter(socket.getOutputStream(), true);

            sendJoin();

            String line;

            while ((line = in.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("ERROR")) {
                    String msg = line.substring("ERROR".length()).trim();
                    notifyError(msg);
                } else if ("STATE".equalsIgnoreCase(line)) {
                    NetworkGameState state = readState();

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
        if (roomId != null && !roomId.isEmpty()) {
            sendLine("JOIN " + roomId + " " + role + " " + playerName);
        } else {
            sendLine("JOIN " + role + " " + playerName);
        }
    }

    @Override
    public void sendMove(int xFrom, int yFrom, int xTo, int yTo) {
        if (roomId != null && !roomId.isEmpty()) {
            sendLine("MOVE " + roomId + " " + xFrom + " " + yFrom + " " + xTo + " " + yTo);
        } else {
            sendLine("MOVE " + xFrom + " " + yFrom + " " + xTo + " " + yTo);
        }
    }

    private void sendLine(String line) {
        if (out != null) {
            out.println(line);
            out.flush();
        }
    }

    private NetworkGameState readState() throws IOException {
        int playerAtMoveIndex = 0;
        String[] playerNames  = new String[] { "", "" };
        boolean draw          = false;
        int winnerIndex       = -1;
        char[][] board        = new char[8][8];

        String line;

        while ((line = in.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("P ")) {
                playerAtMoveIndex = Integer.parseInt(line.substring(2).trim());
            } else if (line.startsWith("N ")) {
                String[] parts = line.split(" ", 3);

                if (parts.length >= 3) {
                    int idx = Integer.parseInt(parts[1]);

                    if ((idx >= 0) && (idx < playerNames.length)) {
                        playerNames[idx] = parts[2];
                    }
                }
            } else if (line.startsWith("D ")) {
                draw = "1".equals(line.substring(2).trim());
            } else if (line.startsWith("W ")) {
                winnerIndex = Integer.parseInt(line.substring(2).trim());
            } else if ("B".equalsIgnoreCase(line)) {
                for (int y = 0; y < 8; y++) {
                    String row = in.readLine();

                    if (row == null) {
                        return null;
                    }

                    if (row.length() < 8) {
                        row = String.format("%-8s", row).replace(' ', '.');
                    }

                    for (int x = 0; x < 8; x++) {
                        board[x][y] = row.charAt(x);
                    }
                }
            } else if ("END".equalsIgnoreCase(line)) {
                break;
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

