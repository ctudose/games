package games.fx;

import games.logics.checkers.CheckersPiece;
import games.net.CheckersConnection;
import games.net.NetworkGameState;
import javafx.scene.control.Label;

/**
 * Controller used by the networked JavaFX client.
 */
public class NetworkCheckersController {

    private final int myPlayerIndex; // 0 or 1
    private final boolean spectator;
    private final BoardView boardView;
    private final Label statusLabel;
    private CheckersConnection connection;

    private NetworkGameState currentState;

    private Integer selectedX;
    private Integer selectedY;

    public NetworkCheckersController(int myPlayerIndex,
                                     boolean spectator,
                                     BoardView boardView,
                                     Label statusLabel,
                                     CheckersConnection connection) {
        this.myPlayerIndex = myPlayerIndex;
        this.spectator     = spectator;
        this.boardView     = boardView;
        this.statusLabel   = statusLabel;
        this.connection    = connection;
    }

    public void setConnection(CheckersConnection connection) {
        this.connection = connection;
    }

    public void onStateUpdate(NetworkGameState state) {
        this.currentState = state;
        clearSelection();

        if (state != null) {
            CheckersPiece[][] matrix = state.toPieceMatrix(myPlayerIndex);
            boardView.updateBoard(matrix);
            updateStatus();
        }
    }

    public void onError(String message) {
        statusLabel.setText("Error: " + message);
    }

    public void onCellClicked(int displayX, int displayY) {
        if (spectator) {
            return;
        }

        if (currentState == null) {
            return;
        }

        if (currentState.getWinnerIndex() != -1 || currentState.isDraw()) {
            return;
        }

        if (currentState.getPlayerAtMoveIndex() != myPlayerIndex) {
            return;
        }

        if (selectedX == null) {
            handleFirstClick(displayX, displayY);
        } else {
            handleSecondClick(displayX, displayY);
        }
    }

    private void handleFirstClick(int displayX, int displayY) {
        char[][] board = currentState.getBoardChars();

        int logicalX = toLogicalCoordinate(displayX);
        int logicalY = toLogicalCoordinate(displayY);

        char c = board[logicalX][logicalY];

        if (!belongsToMe(c)) {
            return;
        }

        selectedX = displayX;
        selectedY = displayY;
        boardView.highlightSelected(displayX, displayY);
    }

    private void handleSecondClick(int displayX, int displayY) {
        if ((selectedX != null) && (selectedY != null)
                && (displayX == selectedX) && (displayY == selectedY)) {
            clearSelection();

            return;
        }

        int fromLogicalX = toLogicalCoordinate(selectedX);
        int fromLogicalY = toLogicalCoordinate(selectedY);
        int toLogicalX   = toLogicalCoordinate(displayX);
        int toLogicalY   = toLogicalCoordinate(displayY);

        if (connection != null) {
            connection.sendMove(fromLogicalX, fromLogicalY, toLogicalX, toLogicalY);
        }
        // Actual validation and board update will come via server STATE
    }

    private boolean belongsToMe(char c) {
        if (c == '.') {
            return false;
        }

        boolean isPlayer0Piece = (c == 'w') || (c == 'W');
        boolean isPlayer1Piece = (c == 'b') || (c == 'B');

        return (myPlayerIndex == 0 && isPlayer0Piece)
               || (myPlayerIndex == 1 && isPlayer1Piece);
    }

    private int toLogicalCoordinate(int displayCoord) {
        return (myPlayerIndex == 0) ? displayCoord : 7 - displayCoord;
    }

    private void clearSelection() {
        selectedX = null;
        selectedY = null;
        boardView.clearSelectionHighlight();
    }

    private void updateStatus() {
        if (currentState == null) {
            statusLabel.setText(spectator ? "Spectating — Connecting..." : "Connecting...");

            return;
        }

        if (currentState.isDraw()) {
            statusLabel.setText(spectator ? "Spectating — Draw" : "Draw");

            return;
        }

        int winnerIndex = currentState.getWinnerIndex();

        if (winnerIndex >= 0) {
            String name = nameOrDefault(winnerIndex, "Player " + (winnerIndex + 1));
            String message = "Winner: " + name;
            statusLabel.setText(spectator ? "Spectating — " + message : message);

            return;
        }

        int playerAtMove = currentState.getPlayerAtMoveIndex();
        String name      = nameOrDefault(playerAtMove, "Player " + (playerAtMove + 1));

        String message = "Turn: " + name;
        statusLabel.setText(spectator ? "Spectating — " + message : message);
    }

    private String nameOrDefault(int index, String fallback) {
        String[] names = currentState.getPlayerNames();

        if ((names != null) && (index >= 0) && (index < names.length) && (names[index] != null)
                && !names[index].isEmpty()) {
            return names[index];
        }

        return fallback;
    }
}

