package games.fx;

import games.logics.checkers.CheckersGame;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CheckersApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        CheckersGame game = createNewGame();

        BoardView boardView = new BoardView(8);
        Label statusLabel = new Label();

        CheckersController controller = new CheckersController(game, boardView, statusLabel);
        boardView.setCellClickHandler(controller::onCellClicked);

        StackPane centerPane = new StackPane(boardView);
        centerPane.setPadding(new Insets(10));

        // Keep the board square: side = min(width, height) of the center area
        boardView.maxWidthProperty().bind(Bindings.min(centerPane.widthProperty(), centerPane.heightProperty()));
        boardView.maxHeightProperty().bind(boardView.maxWidthProperty());
        boardView.prefWidthProperty().bind(boardView.maxWidthProperty());
        boardView.prefHeightProperty().bind(boardView.maxWidthProperty());

        BorderPane root = new BorderPane();
        root.setCenter(centerPane);
        root.setBottom(createBottomBar(controller, statusLabel));

        Scene scene = new Scene(root, 640, 700);

        primaryStage.setTitle("Luxoft Checkers");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createBottomBar(CheckersController controller, Label statusLabel) {
        Button newGameButton = new Button("New Game");
        newGameButton.setOnAction(e -> controller.setGame(createNewGame()));

        HBox bottom = new HBox(12, newGameButton, statusLabel);
        bottom.setPadding(new Insets(10));

        return bottom;
    }

    private CheckersGame createNewGame() {
        CheckersGame game = new CheckersGame();
        game.setPlayer(0, "Player 1");
        game.setPlayer(1, "Player 2");
        game.start();

        return game;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

