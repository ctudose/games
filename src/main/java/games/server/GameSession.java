package games.server;

import games.logics.GameMoveResult;
import games.logics.checkers.CheckersGame;
import games.logics.checkers.CheckersGameMove;

/**
 * Small facade around CheckersGame used by the server layer and tests.
 *
 * It exposes a simple API in terms of player indices and coordinates,
 * without any socket or protocol concerns.
 */
public class GameSession {

    private final CheckersGame game = new CheckersGame();

    public void setPlayer(int index, Object name) {
        game.setPlayer(index, name);
    }

    public void start() {
        game.start();
    }

    public int getPlayerAtMoveIndex() {
        return game.getPlayerAtMoveIndex();
    }

    public boolean isFinished() {
        return game.isFinished();
    }

    public Object getWinner() {
        return game.getWinner();
    }

    /**
     * Apply a move coming from a client identified by playerIndex.
     *
     * @param playerIndex index of the player (0 or 1)
     * @param xFrom       move source x
     * @param yFrom       move source y
     * @param xTo         move target x
     * @param yTo         move target y
     * @return GameMoveResult as returned by the underlying CheckersGame
     */
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

    // Package-private accessor for tests that need direct access to the engine.
    CheckersGame getGame() {
        return game;
    }
}

