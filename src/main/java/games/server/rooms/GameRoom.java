package games.server.rooms;

import games.logics.GameMoveResult;
import games.logics.checkers.CheckersGame;
import games.logics.checkers.CheckersGameMove;
import games.server.CheckersServer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One logical game room: a single CheckersGame with up to 2 players and many spectators.
 */
public final class GameRoom {

    private final String roomId;
    private final CheckersGame game;
    private final CheckersServer.ClientHandler[] players = new CheckersServer.ClientHandler[2];
    private final List<CheckersServer.ClientHandler> spectators = new CopyOnWriteArrayList<>();

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.game = new CheckersGame();
        game.setPlayer(0, "Player 1");
        game.setPlayer(1, "Player 2");
        game.start();
    }

    public String getRoomId() {
        return roomId;
    }

    public CheckersGame getGame() {
        return game;
    }

    public CheckersServer.ClientHandler getPlayer(int index) {
        return players[index];
    }

    public List<CheckersServer.ClientHandler> getSpectators() {
        return spectators;
    }

    public boolean joinPlayer(int index, CheckersServer.ClientHandler handler, String name) {
        if (index < 0 || index > 1) {
            return false;
        }
        synchronized (players) {
            if (players[index] != null) {
                return false;
            }
            players[index] = handler;
            game.setPlayer(index, name);
        }
        return true;
    }

    public void leavePlayer(CheckersServer.ClientHandler handler) {
        synchronized (players) {
            for (int i = 0; i < players.length; i++) {
                if (players[i] == handler) {
                    players[i] = null;
                }
            }
        }
    }

    public void joinSpectator(CheckersServer.ClientHandler handler) {
        spectators.add(handler);
    }

    public void leaveSpectator(CheckersServer.ClientHandler handler) {
        spectators.remove(handler);
    }

    public GameMoveResult applyMove(int playerIndex, int xFrom, int yFrom, int xTo, int yTo) {
        if (playerIndex < 0 || playerIndex > 1) {
            return GameMoveResult.NOT_ONE_OF_PLAYERS;
        }
        Object player = game.getPlayer(playerIndex);
        if (player == null) {
            return GameMoveResult.NOT_ONE_OF_PLAYERS;
        }
        CheckersGameMove move = new CheckersGameMove(xFrom, yFrom, xTo, yTo);
        return game.checkMove(player, move);
    }

    public boolean isPlayerSlotTaken(int index) {
        return players[index] != null;
    }

    public int getSpectatorCount() {
        return spectators.size();
    }
}

