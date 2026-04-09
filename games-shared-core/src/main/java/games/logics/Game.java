package games.logics;

public abstract class Game {
    private boolean    draw   = false;
    private Object     winner = null;
    protected Object[] players;
    protected int      playerAtMoveIndex;
    protected boolean  isStarted;

    public Game() {
        allocatePlayersArray();
        isStarted = false;
    }

    public Game(Game other) {
        players = new Object[other.players.length];
        System.arraycopy(other.players, 0, players, 0, other.players.length);
        isStarted         = other.isStarted;
        playerAtMoveIndex = other.playerAtMoveIndex;
        draw              = other.draw;
        winner            = other.winner;
    }

    public abstract GameMoveResult checkMove(Object player, GameMove move);

    public abstract GameSituation getSituation(Object user);

    protected void allocatePlayersArray() {
        players = new Object[2];
    }

    public Object getPlayer(int index) {
        return players[index];
    }

    public Object getPlayerAtMove() {
        return players[playerAtMoveIndex];
    }

    public int getPlayerAtMoveIndex() {
        return playerAtMoveIndex;
    }

    public int getPlayerIndex(Object player) {
        if (player != null) {
            for (int i = 0; i < players.length; i++) {
                if (player.equals(players[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    public Object getWinner() {
        return winner;
    }

    public void setWinner(int winnerIndex) {
        winner = players[winnerIndex];
    }

    public boolean isDraw() {
        return draw;
    }

    public void setDraw() {
        draw = true;
    }

    public boolean isFinished() {
        return (winner != null) || draw;
    }

    public boolean isPlayer(Object user) {
        return getPlayerIndex(user) >= 0;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void start() {
        isStarted = true;
    }

    public void setPlayer(int index, Object player) {
        players[index] = player;
    }

    public void setPlayerAtMoveIndex(int index) {
        playerAtMoveIndex = index;
    }

    public void setPlayers(Object[] newPlayers) {
        players = newPlayers;
    }
}

