package games.net;

import games.contracts.ContractGameState;
import games.logics.checkers.CheckersPiece;

/**
 * Immutable snapshot of the game state coming from the server.
 */
public class NetworkGameState {

    private final int playerAtMoveIndex;
    private final String[] playerNames; // length 2
    private final boolean draw;
    private final int winnerIndex;      // -1 if none
    private final char[][] boardChars;  // [8][8], y = 0..7, x = 0..7

    public NetworkGameState(int playerAtMoveIndex,
                            String[] playerNames,
                            boolean draw,
                            int winnerIndex,
                            char[][] boardChars) {
        this.playerAtMoveIndex = playerAtMoveIndex;
        this.playerNames       = playerNames;
        this.draw              = draw;
        this.winnerIndex       = winnerIndex;
        this.boardChars        = boardChars;
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

    public ContractGameState toContract() {
        return new ContractGameState(playerAtMoveIndex, playerNames, draw, winnerIndex, boardChars);
    }

    public static NetworkGameState fromContract(ContractGameState state) {
        if (state == null) {
            return null;
        }
        return new NetworkGameState(
                state.getPlayerAtMoveIndex(),
                state.getPlayerNames(),
                state.isDraw(),
                state.getWinnerIndex(),
                state.getBoardChars()
        );
    }

    /**
     * Convert the char board into a CheckersPiece matrix for rendering, applying orientation.
     *
     * @param viewAsPlayerIndex 0 or 1; 0 = normal, 1 = rotated 180 degrees
     */
    public CheckersPiece[][] toPieceMatrix(int viewAsPlayerIndex) {
        int size = boardChars.length;
        CheckersPiece[][] matrix = new CheckersPiece[size][size];

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int logicalX = x;
                int logicalY = y;

                if (viewAsPlayerIndex == 1) {
                    logicalX = size - 1 - x;
                    logicalY = size - 1 - y;
                }

                char c = boardChars[logicalX][logicalY];

                CheckersPiece piece = fromChar(c);
                matrix[x][y] = piece;
            }
        }

        return matrix;
    }

    private static CheckersPiece fromChar(char c) {
        switch (c) {
            case 'w':
                return new CheckersPiece(false, false);
            case 'W':
                return new CheckersPiece(false, true);
            case 'b':
                return new CheckersPiece(true, false);
            case 'B':
                return new CheckersPiece(true, true);
            default:
                return null;
        }
    }
}

