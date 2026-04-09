package games.contracts;

public final class ContractGameState {

    private final int playerAtMoveIndex;
    private final String[] playerNames;
    private final boolean draw;
    private final int winnerIndex;
    private final char[][] boardChars;

    public ContractGameState(int playerAtMoveIndex,
                             String[] playerNames,
                             boolean draw,
                             int winnerIndex,
                             char[][] boardChars) {
        this.playerAtMoveIndex = playerAtMoveIndex;
        this.playerNames = playerNames;
        this.draw = draw;
        this.winnerIndex = winnerIndex;
        this.boardChars = boardChars;
    }

    public int getPlayerAtMoveIndex() {
        return playerAtMoveIndex;
    }

    public String[] getPlayerNames() {
        return playerNames;
    }

    public boolean isDraw() {
        return draw;
    }

    public int getWinnerIndex() {
        return winnerIndex;
    }

    public char[][] getBoardChars() {
        return boardChars;
    }
}
