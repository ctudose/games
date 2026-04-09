package games.logics.checkers;

import games.logics.GameMove;
import games.logics.GameMoveResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckersGameTest {

    private CheckersGame game;

    @BeforeEach
    void setUp() {
        game = new CheckersGame();
        game.setPlayer(0, "Player0");
        game.setPlayer(1, "Player1");
        game.start();
    }

    @Test
    @DisplayName("Given a new game with initialized players When the initial board is inspected Then it contains twelve pieces per color")
    void initialBoardHasTwelvePiecesPerColor() {
        CheckersPiece[][] board = game.getBoard();

        int countColorFalse = 0;
        int countColorTrue  = 0;

        for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
            for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
                CheckersPiece piece = board[x][y];

                if (piece != null) {
                    if (piece.getColor()) {
                        countColorTrue++;
                    } else {
                        countColorFalse++;
                    }
                }
            }
        }

        int finalCountColorFalse = countColorFalse;
        int finalCountColorTrue = countColorTrue;
        assertAll(
                () -> assertEquals(12, finalCountColorFalse, "Expected 12 pieces for color=false"),
                () -> assertEquals(12, finalCountColorTrue, "Expected 12 pieces for color=true")
        );
    }

    @Test
    @DisplayName("Given an unknown player When checkMove is called Then it returns NOT_ONE_OF_PLAYERS")
    void checkMoveRejectsUnknownPlayer() {
        GameMove move = new CheckersGameMove(0, 0, 1, 1);

        GameMoveResult result = game.checkMove("Unknown", move);

        assertEquals(GameMoveResult.NOT_ONE_OF_PLAYERS, result);
    }

    @Test
    @DisplayName("Given a null player object When checkMove is called Then it returns NOT_ONE_OF_PLAYERS")
    void checkMoveRejectsNullPlayer() {
        GameMoveResult result = game.checkMove(null, new CheckersGameMove(0, 0, 1, 1));
        assertEquals(GameMoveResult.NOT_ONE_OF_PLAYERS, result);
    }

    @Test
    @DisplayName("Given a game state with a legal move for the current player When RobotMove selects it Then checkMove accepts it and updates the board")
    void robotGeneratedMoveIsAppliedByCheckersGame() {
        // Arrange: robot chooses a legal move for the current player
        GameMove robotMove = RobotMove.getRobotMove(game);
        assertNotNull(robotMove, "RobotMove should return a move from the initial position");

        CheckersPiece[][] before = copyBoard(game.getBoard());

        // Act
        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMoveResult result = game.checkMove(currentPlayer, robotMove);

        // Assert
        CheckersPiece[][] after = game.getBoard();
        boolean changed = boardsDiffer(before, after);

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result, "Robot move should be legal"),
                () -> assertEquals(true, changed, "Board should change after applying a legal move")
        );
    }

    @Test
    @DisplayName("Given a non-checkers move object When checkMove is called Then it returns INVALID_DATA_FORMAT")
    void checkMoveReturnsInvalidDataFormatWhenMoveIsNotCheckersGameMove() {
        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMove invalidMove = new GameMove() {
        };

        GameMoveResult result = game.checkMove(currentPlayer, invalidMove);
        assertEquals(GameMoveResult.INVALID_DATA_FORMAT, result);
    }

    @Test
    @DisplayName("Given a board where a capture exists When a simple move is attempted Then the move is rejected as WRONG_MOVE")
    void mandatoryCaptureRejectsSimpleMoveEvenFromAnotherPiece() {
        // Build: white has a capture somewhere, so all simple moves are rejected.
        BoardVerifier.resetBoard(game.getBoard());

        // Capture-capable white man
        game.getBoard()[2][2] = new CheckersPiece(false, false);
        // Opponent to capture
        game.getBoard()[3][3] = new CheckersPiece(true, false);

        // Another white man with a potential simple move (but must be blocked by mandatory capture)
        game.getBoard()[5][2] = new CheckersPiece(false, false);

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());

        // Try a simple move from the other piece: (5,2)->(4,3)
        GameMoveResult result = game.checkMove(currentPlayer, new CheckersGameMove(5, 2, 4, 3));
        assertEquals(GameMoveResult.WRONG_MOVE, result);
    }

    @Test
    @DisplayName("Given two pieces that can capture When each capture is attempted Then both are accepted as legal moves")
    void mandatoryCaptureAllowsCaptureFromAnyCapturingPiece() {
        BoardVerifier.resetBoard(game.getBoard());

        // Two white men can capture independently.
        game.getBoard()[2][2] = new CheckersPiece(false, false);
        game.getBoard()[3][3] = new CheckersPiece(true, false);

        game.getBoard()[4][2] = new CheckersPiece(false, false);
        game.getBoard()[5][3] = new CheckersPiece(true, false);

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());

        CheckersGame firstPath = new CheckersGame(game);
        GameMoveResult firstResult = firstPath.checkMove(currentPlayer, new CheckersGameMove(2, 2, 4, 4));

        CheckersGame secondPath = new CheckersGame(game);
        GameMoveResult secondResult = secondPath.checkMove(currentPlayer, new CheckersGameMove(4, 2, 6, 4));

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, firstResult),
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, secondResult)
        );
    }

    @Test
    @DisplayName("Given a board where a single capture is available When checkMove executes the capture Then the opponent piece is removed and the turn passes")
    void singleCaptureRemovesOpponentAndPassesTurn() {
        BoardVerifier.resetBoard(game.getBoard());

        // White man at (2,2) captures black man at (3,3) into (4,4)
        game.getBoard()[2][2] = new CheckersPiece(false, false);
        game.getBoard()[3][3] = new CheckersPiece(true, false);

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMoveResult result = game.checkMove(currentPlayer, new CheckersGameMove(2, 2, 4, 4));

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result),
                () -> assertNull(game.getBoard()[3][3], "Captured piece must be removed"),
                () -> assertNotNull(game.getBoard()[4][4], "Moving piece must land at the target"),
                () -> assertFalse(game.getBoard()[4][4].isKing(), "Piece should not be kinged yet"),
                () -> assertEquals(1, game.getPlayerAtMoveIndex(), "Turn should pass after a single capture")
        );
    }

    @Test
    @DisplayName("Given a board with a multi-capture sequence When the wrong piece is moved during continuation Then WRONG_MOVE is returned and the correct continuation is accepted")
    void multiCaptureContinuationRejectsWrongPieceAndAcceptsCorrectIntermediate() {
        BoardVerifier.resetBoard(game.getBoard());

        // First capture: (2,2)->(4,4) capturing (3,3)
        game.getBoard()[2][2] = new CheckersPiece(false, false);
        game.getBoard()[3][3] = new CheckersPiece(true, false);

        // Second capture available from (4,4): (4,4)->(6,6) capturing (5,5)
        game.getBoard()[5][5] = new CheckersPiece(true, false);

        // Another white piece that would be a legal simple move, but must be rejected due to continuation rule
        game.getBoard()[5][2] = new CheckersPiece(false, false);

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());

        // First capture accepted; continuation must be required
        GameMoveResult first = game.checkMove(currentPlayer, new CheckersGameMove(2, 2, 4, 4));
        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, first),
                () -> assertEquals(0, game.getPlayerAtMoveIndex(), "Turn must stay with the same player during multi-capture")
        );

        // Attempt to move the wrong piece during continuation -> WRONG_MOVE
        GameMoveResult wrongPiece = game.checkMove(currentPlayer, new CheckersGameMove(5, 2, 4, 3));
        assertEquals(GameMoveResult.WRONG_MOVE, wrongPiece);

        // Correct continuation: (4,4)->(6,6)
        GameMoveResult second = game.checkMove(currentPlayer, new CheckersGameMove(4, 4, 6, 6));
        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, second),
                () -> assertNull(game.getBoard()[5][5], "Second captured piece must be removed"),
                () -> assertNotNull(game.getBoard()[6][6], "Piece must land after second capture"),
                () -> assertEquals(1, game.getPlayerAtMoveIndex(), "Turn should pass after the multi-capture sequence")
        );
    }

    @Test
    @DisplayName("Given a piece positioned one move before the last rank When a move reaches the last rank Then the piece is kinged")
    void kingIsCreatedWhenPieceReachesLastRank() {
        BoardVerifier.resetBoard(game.getBoard());

        // Place a white man one step before the last line (y=7), so a simple move to y=7 should king it.
        game.getBoard()[2][6] = new CheckersPiece(false, false);

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMoveResult result = game.checkMove(currentPlayer, new CheckersGameMove(2, 6, 1, 7));

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result),
                () -> assertNotNull(game.getBoard()[1][7]),
                () -> assertTrue(game.getBoard()[1][7].isKing(), "Piece should be kinged upon reaching y=7")
        );
    }

    @Test
    @DisplayName("Given a capture move that lands on the last rank When checkMove is executed Then the moved piece is kinged")
    void kingIsCreatedWhenCaptureReachesLastRank() {
        BoardVerifier.resetBoard(game.getBoard());

        // White captures from (3,5) over (4,6) into (5,7), ending on last rank.
        game.getBoard()[3][5] = new CheckersPiece(false, false);
        game.getBoard()[4][6] = new CheckersPiece(true, false);

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMoveResult result = game.checkMove(currentPlayer, new CheckersGameMove(3, 5, 5, 7));

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result),
                () -> assertNull(game.getBoard()[4][6], "Captured piece must be removed"),
                () -> assertNotNull(game.getBoard()[5][7], "Moved piece should land on destination"),
                () -> assertTrue(game.getBoard()[5][7].isKing(), "Piece should be kinged after capture to last rank")
        );
    }

    @Test
    @DisplayName("Given a king piece When a backward simple move is attempted Then checkMove accepts it")
    void kingBackwardSimpleMoveIsAllowedByCheckersGame() {
        BoardVerifier.resetBoard(game.getBoard());

        // Minimal setup: only a king on the board.
        game.getBoard()[2][2] = new CheckersPiece(false, true);

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMoveResult result = game.checkMove(currentPlayer, new CheckersGameMove(2, 2, 1, 1));

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result),
                () -> assertTrue(game.getBoard()[1][1].isKing(), "King property must be preserved")
        );
    }

    @Test
    @DisplayName("Given a board where a capture removes the last opponent piece When checkMove executes it Then the game is finished and the winner is set")
    void gameIsWonWhenTheLastOpponentPieceIsCaptured() {
        BoardVerifier.resetBoard(game.getBoard());

        // White captures the only black piece.
        game.getBoard()[2][2] = new CheckersPiece(false, false);
        game.getBoard()[3][3] = new CheckersPiece(true, false);

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMoveResult result = game.checkMove(currentPlayer, new CheckersGameMove(2, 2, 4, 4));

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result),
                () -> assertTrue(game.isFinished(), "Game should be finished after capturing the last opponent piece"),
                () -> assertNotNull(game.getWinner(), "Winner must be set"),
                () -> assertEquals(game.getPlayer(0), game.getWinner(), "Player 0 should win by capturing the last opponent")
        );
    }

    @Test
    @DisplayName("Given a board where the opponent has no legal moves After the current player's move When checkMove is executed Then the game is finished and the current player wins")
    void gameIsWonWhenOpponentHasNoLegalMoves() {
        BoardVerifier.resetBoard(game.getBoard());

        // After player 0 moves, player 1 (black) will have no legal moves because its only man is on y=0.
        game.getBoard()[2][1] = new CheckersPiece(false, false); // white man
        game.getBoard()[2][0] = new CheckersPiece(true, false);  // black man on last rank for black forward direction

        Object currentPlayer = game.getPlayer(game.getPlayerAtMoveIndex());
        GameMoveResult result = game.checkMove(currentPlayer, new CheckersGameMove(2, 1, 3, 2));

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, result),
                () -> assertTrue(game.isFinished(), "Game should be finished when the opponent has no legal moves"),
                () -> assertEquals(game.getPlayer(0), game.getWinner(), "Player 0 should win when player 1 has no legal moves")
        );
    }

    private static CheckersPiece[][] copyBoard(CheckersPiece[][] original) {
        CheckersPiece[][] copy = new CheckersPiece[BoardVerifier.upperLimit][BoardVerifier.upperLimit];

        for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
            for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
                CheckersPiece piece = original[x][y];
                copy[x][y] = (piece == null) ? null : new CheckersPiece(piece);
            }
        }

        return copy;
    }

    private static boolean boardsDiffer(CheckersPiece[][] a, CheckersPiece[][] b) {
        for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
            for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
                CheckersPiece p1 = a[x][y];
                CheckersPiece p2 = b[x][y];

                if (p1 == null && p2 == null) {
                    continue;
                }
                if (p1 == null || p2 == null) {
                    return true;
                }
                if (p1.getColor() != p2.getColor() || p1.isKing() != p2.isKing()) {
                    return true;
                }
            }
        }
        return false;
    }
}

