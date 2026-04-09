package games.fx;

import games.config.Config;
import games.logics.GameMove;
import games.logics.GameMoveResult;
import games.logics.checkers.CheckersGame;
import games.logics.checkers.CheckersGameMove;
import games.logics.checkers.CheckersPiece;
import games.logics.checkers.RobotMove;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class CheckersController {

    private CheckersGame game;
    private final BoardView boardView;
    private final Label statusLabel;

    private Integer selectedX;
    private Integer selectedY;

    public CheckersController(CheckersGame game, BoardView boardView, Label statusLabel) {
        this.game = game;
        this.boardView = boardView;
        this.statusLabel = statusLabel;

        refreshBoardAndStatus();
    }

    public void setGame(CheckersGame newGame) {
        this.game = newGame;
        clearSelection();
        refreshBoardAndStatus();
    }

    public void onCellClicked(int x, int y) {
        if (!game.isStarted() || game.isFinished()) {
            return;
        }

        if (selectedX == null) {
            handleFirstClick(x, y);
        } else {
            handleSecondClick(x, y);
        }
    }

    private void handleFirstClick(int x, int y) {
        CheckersPiece piece = game.pieceAt(x, y);

        if (piece == null) {
            return;
        }

        // Ensure this piece belongs to the player whose turn it is
        if (game.wrongPlayer(piece)) {
            return;
        }

        selectedX = x;
        selectedY = y;
        boardView.highlightSelected(x, y);
    }

    private void handleSecondClick(int x, int y) {
        if ((selectedX != null) && (selectedY != null) && (x == selectedX) && (y == selectedY)) {
            clearSelection();

            return;
        }

        CheckersGameMove move = new CheckersGameMove(selectedX, selectedY, x, y);
        GameMoveResult result = game.checkMove(game.getPlayer(game.getPlayerAtMoveIndex()), move);

        if (result == GameMoveResult.CORRECT_MOVE) {
            clearSelection();
            refreshBoardAndStatus();
            maybePlayRobotMove();
        } else {
            statusLabel.setText("Illegal move");
        }
    }

    private void maybePlayRobotMove() {
        if (game.isFinished()) {
            return;
        }

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());

        if (!isRobot(currentPlayer)) {
            return;
        }

        long delay = Config.getRobotDelayMillis();

        if (delay <= 0L) {
            // No delay configured: play all robot moves immediately as before
            while (!game.isFinished()) {
                currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());

                if (!isRobot(currentPlayer)) {
                    break;
                }

                GameMove robotMove = RobotMove.getRobotMove(game);

                if (!(robotMove instanceof CheckersGameMove)) {
                    break;
                }

                GameMoveResult result = game.checkMove(currentPlayer, robotMove);

                if (result != GameMoveResult.CORRECT_MOVE) {
                    break;
                }

                refreshBoardAndStatus();
            }
        } else {
            // Delay configured: schedule the next robot move so the UI remains responsive
            scheduleNextRobotMoveWithDelay(delay);
        }
    }

    private void scheduleNextRobotMoveWithDelay(long delayMillis) {
        PauseTransition pause = new PauseTransition(Duration.millis(delayMillis));
        pause.setOnFinished(e -> {
            if (game.isFinished()) {
                return;
            }

            Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());

            if (!isRobot(currentPlayer)) {
                return;
            }

            GameMove robotMove = RobotMove.getRobotMove(game);

            if (!(robotMove instanceof CheckersGameMove)) {
                return;
            }

            GameMoveResult result = game.checkMove(currentPlayer, robotMove);

            if (result != GameMoveResult.CORRECT_MOVE) {
                return;
            }

            refreshBoardAndStatus();

            // If it is still the robot's turn, schedule another move
            if (!game.isFinished() && isRobot(game.getPlayer(game.getPlayerAtMoveIndex()))) {
                scheduleNextRobotMoveWithDelay(delayMillis);
            }
        });
        pause.play();
    }

    private boolean isRobot(Object player) {
        return (player != null) && player.toString().toLowerCase().startsWith("robot");
    }

    private void clearSelection() {
        selectedX = null;
        selectedY = null;
        boardView.clearSelectionHighlight();
    }

    public void refreshBoardAndStatus() {
        boardView.updateBoard(game);
        updateStatus();
    }

    private void updateStatus() {
        if (game.isFinished()) {
            if (game.isDraw()) {
                statusLabel.setText("Draw");
            } else if (game.getWinner() != null) {
                statusLabel.setText("Winner: " + game.getWinner());
            } else {
                statusLabel.setText("Game finished");
            }
        } else {
            Object player = game.getPlayer(game.getPlayerAtMoveIndex());
            String name = (player != null) ? player.toString() : ("Player #" + game.getPlayerAtMoveIndex());
            statusLabel.setText("Turn: " + name);
        }
    }
}

