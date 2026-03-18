package games.fx;

import games.config.Config;
import games.net.CheckersClientConnection;
import games.net.CheckersConnection;
import games.net.JsonCheckersClientConnection;
import games.net.NetworkGameState;
import games.net.RoleAvailabilityClient;
import games.net.RoomInfo;
import games.net.RoomListClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX client application for networked checkers.
 *
 * Start the server separately (CheckersServer), then run two instances of this app.
 */
public class NetworkCheckersApp extends Application {

    private static final String DEFAULT_HOST = Config.getNetworkHost();
    private static final int DEFAULT_PORT    = Config.getClientPort();

    @Override
    public void start(Stage primaryStage) {
        Scene scene = createRoomSelectionScene(primaryStage);

        primaryStage.setTitle("Network Checkers - Rooms");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Scene createRoomSelectionScene(Stage stage) {
        Label title = new Label("Rooms");

        TextField nameField = new TextField();
        nameField.setPromptText("Your name");

        ObservableList<String> roomItems = FXCollections.observableArrayList();
        ListView<String> roomsList = new ListView<>(roomItems);
        roomsList.setPrefHeight(200);

        Button refreshButton = new Button("Refresh");
        Button createButton = new Button("Create room");
        Button nextButton = new Button("Next");
        nextButton.setDisable(true);

        roomsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            nextButton.setDisable(newV == null || newV.trim().isEmpty());
        });

        Runnable refresh = () -> RoomListClient.queryRoomsAsync(
                DEFAULT_HOST,
                DEFAULT_PORT,
                rooms -> {
                    roomItems.clear();
                    for (RoomInfo r : rooms) {
                        roomItems.add(formatRoom(r));
                    }
                },
                err -> {
                    roomItems.clear();
                    title.setText("Rooms (server unavailable)");
                }
        );

        refreshButton.setOnAction(e -> refresh.run());
        createButton.setOnAction(e -> RoomListClient.createRoomAsync(
                DEFAULT_HOST,
                DEFAULT_PORT,
                roomId -> {
                    // refresh and auto-select created room
                    refresh.run();
                    roomsList.getSelectionModel().select(findItemByRoomId(roomItems, roomId));
                },
                err -> title.setText("Failed to create room: " + err)
        ));

        nextButton.setOnAction(e -> {
            String selected = roomsList.getSelectionModel().getSelectedItem();
            String roomId = extractRoomId(selected);
            Scene scene = createRoleSelectionScene(stage, roomId, nameOrDefault(nameField.getText(), "Player"));
            stage.setTitle("Network Checkers - Join Room");
            stage.setScene(scene);
        });

        HBox buttons = new HBox(10, refreshButton, createButton, nextButton);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(12, title, nameField, roomsList, buttons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(16));

        Scene scene = new Scene(root, 520, 420);
        refresh.run();
        return scene;
    }

    private Scene createRoleSelectionScene(Stage stage, String roomId, String playerNameDefault) {
        Label title = new Label("Room: " + roomId);

        TextField nameField = new TextField(playerNameDefault);
        nameField.setPromptText("Your name");

        Button p1Button = new Button("Player 1");
        Button p2Button = new Button("Player 2");
        Button spectatorButton = new Button("Spectator");

        // Disable until we know which roles are free
        p1Button.setDisable(true);
        p2Button.setDisable(true);

        p1Button.setOnAction(e -> startGameAs(stage, roomId, 0, nameOrDefault(nameField.getText(), "Player 1")));
        p2Button.setOnAction(e -> startGameAs(stage, roomId, 1, nameOrDefault(nameField.getText(), "Player 2")));
        spectatorButton.setOnAction(e -> startGameAsSpectator(stage, roomId, nameOrDefault(nameField.getText(), "Spectator")));

        HBox buttons = new HBox(10, p1Button, p2Button, spectatorButton);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(15, title, nameField, buttons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 420, 220);

        // Query server for role availability for this room
        RoleAvailabilityClient.queryRolesAsync(
                DEFAULT_HOST,
                DEFAULT_PORT,
                roomId,
                available -> {
                    boolean p1Available = available[0];
                    boolean p2Available = available[1];

                    p1Button.setDisable(!p1Available);
                    p2Button.setDisable(!p2Available);
                },
                errorMsg -> {
                    // Fallback: allow both buttons so existing error handling still works
                    p1Button.setDisable(false);
                    p2Button.setDisable(false);
                }
        );

        return scene;
    }

    private void startGameAs(Stage stage, String roomId, int playerIndex, String playerName) {
        BoardView boardView = new BoardView(8);
        Label statusLabel   = new Label("Connecting...");
        Label headerLabel   = new Label("Room: " + roomId + " — " +
                roleAndName(playerIndex == 0 ? "Player 1" : "Player 2", playerName));

        NetworkCheckersController controller = new NetworkCheckersController(
                playerIndex,
                false,
                boardView,
                statusLabel,
                null
        );

        boardView.setCellClickHandler(controller::onCellClicked);

        StackPane centerPane = new StackPane(boardView);
        centerPane.setPadding(new Insets(10));

        // Keep the board square
        boardView.maxWidthProperty().bind(
                Bindings.min(centerPane.widthProperty(), centerPane.heightProperty())
        );
        boardView.maxHeightProperty().bind(boardView.maxWidthProperty());
        boardView.prefWidthProperty().bind(boardView.maxWidthProperty());
        boardView.prefHeightProperty().bind(boardView.maxWidthProperty());

        HBox bottom = new HBox(12, statusLabel);
        bottom.setPadding(new Insets(10));

        HBox top = new HBox(headerLabel);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(centerPane);
        root.setBottom(bottom);

        Scene gameScene = new Scene(root, 640, 700);

        stage.setTitle("Network Checkers - " + (playerIndex == 0 ? "Player 1" : "Player 2"));
        stage.setScene(gameScene);
        stage.show();

        // Now create the real connection with callbacks wired to the controller
        CheckersConnection connection;

        String protocol = Config.getClientProtocol();
        if ("json".equals(protocol)) {
            connection = new JsonCheckersClientConnection(
                    DEFAULT_HOST,
                    DEFAULT_PORT,
                    roomId,
                    playerIndex,
                    playerName,
                    (NetworkGameState state) -> controller.onStateUpdate(state),
                    controller::onError
            );
        } else {
            connection = new CheckersClientConnection(
                    DEFAULT_HOST,
                    DEFAULT_PORT,
                    roomId,
                    playerIndex,
                    playerName,
                    (NetworkGameState state) -> controller.onStateUpdate(state),
                    controller::onError
            );
        }
        controller.setConnection(connection);

        // Start background networking
        connection.connectAsync();
    }

    private void startGameAsSpectator(Stage stage, String roomId, String name) {
        BoardView boardView = new BoardView(8);
        Label statusLabel   = new Label("Connecting...");
        Label headerLabel   = new Label("Room: " + roomId + " — " +
                roleAndName("Spectator", name));

        // Spectator sees Player 1 orientation
        NetworkCheckersController controller = new NetworkCheckersController(
                0,
                true,
                boardView,
                statusLabel,
                null
        );

        // No input for spectator, but still render updates
        boardView.setCellClickHandler(controller::onCellClicked);

        StackPane centerPane = new StackPane(boardView);
        centerPane.setPadding(new Insets(10));

        // Keep the board square
        boardView.maxWidthProperty().bind(
                Bindings.min(centerPane.widthProperty(), centerPane.heightProperty())
        );
        boardView.maxHeightProperty().bind(boardView.maxWidthProperty());
        boardView.prefWidthProperty().bind(boardView.maxWidthProperty());
        boardView.prefHeightProperty().bind(boardView.maxWidthProperty());

        HBox bottom = new HBox(12, statusLabel);
        bottom.setPadding(new Insets(10));

        HBox top = new HBox(headerLabel);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(centerPane);
        root.setBottom(bottom);

        Scene gameScene = new Scene(root, 640, 700);

        stage.setTitle("Network Checkers - Spectator");
        stage.setScene(gameScene);
        stage.show();

        // Use requestedPlayerIndex=-1 which maps to JOIN S
        CheckersConnection connection;

        String protocol = Config.getClientProtocol();
        if ("json".equals(protocol)) {
            connection = new JsonCheckersClientConnection(
                    DEFAULT_HOST,
                    DEFAULT_PORT,
                    roomId,
                    -1,
                    name,
                    (NetworkGameState state) -> controller.onStateUpdate(state),
                    controller::onError
            );
        } else {
            connection = new CheckersClientConnection(
                    DEFAULT_HOST,
                    DEFAULT_PORT,
                    roomId,
                    -1,
                    name,
                    (NetworkGameState state) -> controller.onStateUpdate(state),
                    controller::onError
            );
        }
        controller.setConnection(connection);
        connection.connectAsync();
    }

    private static String nameOrDefault(String text, String fallback) {
        return (text != null && !text.trim().isEmpty()) ? text.trim() : fallback;
    }

    private static String roleAndName(String role, String name) {
        String n = (name != null) ? name.trim() : "";
        if (n.isEmpty()) {
            return role;
        }
        return role + " — " + n;
    }

    private static String formatRoom(RoomInfo r) {
        return r.getRoomId()
                + " — P1:" + (r.isP1Taken() ? "taken" : "free")
                + " P2:" + (r.isP2Taken() ? "taken" : "free")
                + " S:" + r.getSpectators();
    }

    private static String extractRoomId(String item) {
        if (item == null) return "";
        int idx = item.indexOf(" —");
        return (idx >= 0) ? item.substring(0, idx).trim() : item.trim();
    }

    private static String findItemByRoomId(ObservableList<String> items, String roomId) {
        if (roomId == null) return null;
        for (String it : items) {
            if (roomId.equals(extractRoomId(it))) {
                return it;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

