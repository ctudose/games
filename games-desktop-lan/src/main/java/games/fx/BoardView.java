package games.fx;

import games.logics.checkers.BoardVerifier;
import games.logics.checkers.CheckersGame;
import games.logics.checkers.CheckersPiece;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.function.BiConsumer;

public class BoardView extends GridPane {

    private final int size;
    private final StackPane[][] cells;
    private BiConsumer<Integer, Integer> cellClickHandler;
    private int dragFromX = -1;
    private int dragFromY = -1;

    public BoardView(int size) {
        this.size = size;
        this.cells = new StackPane[size][size];

        setHgap(0);
        setVgap(0);
        setAlignment(Pos.CENTER);

        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        setupConstraints();
        buildGrid();
    }

    public void setCellClickHandler(BiConsumer<Integer, Integer> cellClickHandler) {
        this.cellClickHandler = cellClickHandler;
    }

    private void setupConstraints() {
        getColumnConstraints().clear();
        getRowConstraints().clear();

        double percent = 100.0 / size;

        for (int i = 0; i < size; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(percent);
            getColumnConstraints().add(col);

            RowConstraints row = new RowConstraints();
            row.setPercentHeight(percent);
            getRowConstraints().add(row);
        }
    }

    private void buildGrid() {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int row = size - 1 - y;
                int col = x;
                StackPane cell = createCell(x, y);
                cells[col][row] = cell;
                add(cell, col, row);
            }
        }
    }

    private StackPane createCell(int boardX, int boardY) {
        StackPane cell = new StackPane();
        cell.setMinSize(60, 60);
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setPadding(new Insets(4));
        cell.setAlignment(Pos.CENTER);

        boolean dark = (boardX + boardY) % 2 == 0;
        String baseColor = dark ? "#8B5A2B" : "#F0D9B5";
        cell.setStyle("-fx-background-color: " + baseColor + ";");
        cell.setUserData(cell.getStyle());

        cell.setCursor(Cursor.HAND);

        cell.setOnMouseClicked(e -> {
            if (cellClickHandler != null) {
                cellClickHandler.accept(boardX, boardY);
            }
        });

        cell.setOnDragDetected(e -> {
            dragFromX = boardX;
            dragFromY = boardY;

            if (cellClickHandler != null) {
                // Select the piece as if it were clicked
                cellClickHandler.accept(boardX, boardY);
            }

            cell.startFullDrag();
            e.consume();
        });

        cell.setOnMouseDragReleased(e -> {
            if (cellClickHandler != null && dragFromX >= 0 && dragFromY >= 0) {
                // Treat drop as clicking the target square
                cellClickHandler.accept(boardX, boardY);
            }

            dragFromX = -1;
            dragFromY = -1;
            e.consume();
        });

        return cell;
    }

    public void updateBoard(CheckersGame game) {
        CheckersPiece[][] board = game.getBoard();
        updateBoard(board);
    }

    public void updateBoard(CheckersPiece[][] board) {
        for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
            for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
                int row = size - 1 - y;
                int col = x;
                StackPane cell = cells[col][row];

                cell.getChildren().removeIf(node -> node instanceof Circle);

                CheckersPiece piece = board[x][y];

                if (piece != null) {
                    Circle disk = new Circle();
                    // Radius scales with the current cell size to keep the same proportion
                    disk.radiusProperty().bind(Bindings.min(cell.widthProperty(), cell.heightProperty()).multiply(0.35));

                    if (piece.getColor()) {
                        // Player 1 pieces (originally black)
                        disk.setFill(Color.web("#FFFFFF"));
                        disk.setStroke(Color.web("#CCCCCC"));
                    } else {
                        // Player 0 pieces (originally white)
                        disk.setFill(Color.web("#B22222"));
                        disk.setStroke(Color.web("#660000"));
                    }

                    if (piece.isKing()) {
                        disk.setStrokeWidth(3.0);
                        disk.setStroke(Color.GOLD);
                    } else {
                        disk.setStrokeWidth(2.0);
                    }

                    cell.getChildren().add(disk);
                }
            }
        }
    }

    public void clearSelectionHighlight() {
        for (int col = 0; col < size; col++) {
            for (int row = 0; row < size; row++) {
                StackPane cell = cells[col][row];
                Object userData = cell.getUserData();

                if (userData instanceof String) {
                    cell.setStyle((String) userData);
                }
            }
        }
    }

    public void highlightSelected(int boardX, int boardY) {
        clearSelectionHighlight();

        int row = size - 1 - boardY;
        int col = boardX;
        StackPane cell = cells[col][row];

        Object userData = cell.getUserData();
        String baseStyle = (userData instanceof String) ? (String) userData : cell.getStyle();
        cell.setStyle(baseStyle + " -fx-border-color: #FFD700; -fx-border-width: 3;");
    }
}

