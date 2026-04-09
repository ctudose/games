package games.fx;

import games.logics.checkers.CheckersGame;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardViewUiIT extends ApplicationTest {
    private BoardView boardView;

    @Override
    public void start(Stage stage) {
        boardView = new BoardView(8);
        CheckersGame game = new CheckersGame();
        game.setPlayer(0, "P1");
        game.setPlayer(1, "P2");
        game.start();
        boardView.updateBoard(game);

        stage.setScene(new Scene(new StackPane(boardView), 640, 640));
        stage.show();
    }

    @Test
    @DisplayName("Given a board view with initial checkers state When the scene is shown Then all board cells and initial pieces are rendered")
    void boardViewRendersGridAndInitialPieces() throws InterruptedException {
        AtomicInteger pieceCount = new AtomicInteger();
        AtomicInteger cellCount = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            pieceCount.set((int) boardView.getChildren().stream()
                    .filter(StackPane.class::isInstance)
                    .map(StackPane.class::cast)
                    .flatMap(cell -> cell.getChildren().stream())
                    .filter(Circle.class::isInstance)
                    .count());
            cellCount.set(boardView.getChildren().size());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertAll(
                () -> assertEquals(64, cellCount.get(), "8x8 grid should render 64 cells"),
                () -> assertEquals(24, pieceCount.get(), "Initial checkers board should render 24 pieces")
        );
    }

    @Test
    @DisplayName("Given a rendered board When a legal opening move is applied Then source becomes empty and destination shows the moved piece")
    void boardViewReflectsLegalMoveOnRenderedCells() throws InterruptedException {
        CheckersGame game = new CheckersGame();
        game.setPlayer(0, "P1");
        game.setPlayer(1, "P2");
        game.start();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger totalPieces = new AtomicInteger();
        AtomicInteger sourcePieces = new AtomicInteger();
        AtomicInteger destinationPieces = new AtomicInteger();

        // Apply a known legal opening move for player 0 and refresh UI.
        game.checkMove("P1", new games.logics.checkers.CheckersGameMove(1, 2, 0, 3));

        Platform.runLater(() -> {
            boardView.updateBoard(game);

            totalPieces.set((int) boardView.getChildren().stream()
                    .filter(StackPane.class::isInstance)
                    .map(StackPane.class::cast)
                    .flatMap(cell -> cell.getChildren().stream())
                    .filter(Circle.class::isInstance)
                    .count());

            // Grid row index is flipped in BoardView: row = size - 1 - y
            sourcePieces.set(countPieceNodesAtCell(1, 5));
            destinationPieces.set(countPieceNodesAtCell(0, 4));
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertAll(
                () -> assertEquals(24, totalPieces.get(), "Move should not change total piece count for non-capture move"),
                () -> assertEquals(0, sourcePieces.get(), "Source cell should be empty after move"),
                () -> assertEquals(1, destinationPieces.get(), "Destination cell should contain the moved piece")
        );
    }

    private int countPieceNodesAtCell(int gridCol, int gridRow) {
        return (int) boardView.getChildren().stream()
                .filter(node -> GridPosition.isAt(node, gridCol, gridRow))
                .filter(StackPane.class::isInstance)
                .map(StackPane.class::cast)
                .flatMap(cell -> cell.getChildren().stream())
                .filter(Circle.class::isInstance)
                .count();
    }

    private static final class GridPosition {
        private GridPosition() {
        }

        static boolean isAt(Node node, int col, int row) {
            Integer nodeCol = javafx.scene.layout.GridPane.getColumnIndex(node);
            Integer nodeRow = javafx.scene.layout.GridPane.getRowIndex(node);
            return (nodeCol != null && nodeCol == col) && (nodeRow != null && nodeRow == row);
        }
    }
}

