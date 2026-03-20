package games.logics.checkers;

import games.logics.GameMoveResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static games.logics.checkers.BoardVerifier.upperLimit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveVerifierTest {

    private CheckersPiece[][] board;

    @BeforeEach
    void setUp() {
        board = new CheckersPiece[upperLimit][upperLimit];
    }

    @Test
    @DisplayName("Given move coordinates When moveOutsideTable is called Then it returns true only for out-of-bounds coordinates")
    void moveOutsideTableRejectsOutOfBounds() {
        assertAll(
                () -> assertTrue(MoveVerifier.moveOutsideTable(-1, 0, 0, 1)),
                () -> assertTrue(MoveVerifier.moveOutsideTable(0, 0, 8, 1)),
                () -> assertFalse(MoveVerifier.moveOutsideTable(0, 0, 1, 1))
        );
    }

    @Test
    @DisplayName("Given diagonal endpoints When isLegalDiagonalMove is called Then it accepts both simple moves and capture moves")
    void isLegalDiagonalMoveAcceptsSingleAndDoubleSteps() {
        assertAll(
                () -> assertTrue(MoveVerifier.isLegalDiagonalMove(2, 2, 3, 3)), // single step
                () -> assertTrue(MoveVerifier.isLegalDiagonalMove(2, 2, 4, 4)), // capture step

                () -> assertFalse(MoveVerifier.isLegalDiagonalMove(2, 2, 2, 3)), // vertical
                () -> assertFalse(MoveVerifier.isLegalDiagonalMove(2, 2, 3, 4))  // non 45-degree
        );
    }

    @Test
    @DisplayName("Given a non-king man and a move direction When manBackwardMove is called Then it returns true only for forbidden backward motion")
    void manBackwardMovePreventsNonKingMovingBackwards() {
        CheckersPiece whiteMan = new CheckersPiece(false, false); // moves +y
        CheckersPiece blackMan = new CheckersPiece(true, false);  // moves -y

        assertAll(
                // For white man (color=false): dy<0 is forward, dy>0 backward
                () -> assertTrue(MoveVerifier.manBackwardMove(whiteMan, +1)),
                () -> assertFalse(MoveVerifier.manBackwardMove(whiteMan, -1)),

                // For black man (color=true): dy>0 is forward, dy<0 backward
                () -> assertTrue(MoveVerifier.manBackwardMove(blackMan, -1)),
                () -> assertFalse(MoveVerifier.manBackwardMove(blackMan, +1))
        );
    }

    @Test
    @DisplayName("Given a piece and target squares When allowedSimpleMove is called Then it requires empty target and valid forward direction")
    void allowedSimpleMoveHonorsEmptyTargetAndForwardDirection() {
        // Place a white man (color=false) at (2,2), moving forward +y
        board[2][2] = new CheckersPiece(false, false);

        assertAll(
                // Target empty and forward
                () -> assertTrue(MoveVerifier.allowedSimpleMove(board, 2, 2, +1, +1)),

                // Target not empty
                () -> {
                    board[3][3] = new CheckersPiece(true, false);
                    assertFalse(MoveVerifier.allowedSimpleMove(board, 2, 2, +1, +1));
                }
        );
    }

    @Test
    @DisplayName("Given a capture scenario When allowedCapture is called Then it requires an opponent in between and an empty landing square")
    void allowedCaptureRequiresOpponentPieceAndEmptyLandingSquare() {
        // White man at (2,2), black man at (3,3), landing at (4,4)
        board[2][2] = new CheckersPiece(false, false);
        board[3][3] = new CheckersPiece(true, false);

        assertAll(
                // Landing square empty and opponent in between
                () -> assertTrue(MoveVerifier.allowedCapture(board, 2, 2, +2, +2)),

                // If landing square not empty, capture not allowed
                () -> {
                    board[4][4] = new CheckersPiece(false, false);
                    assertFalse(MoveVerifier.allowedCapture(board, 2, 2, +2, +2));
                }
        );
    }

    @Test
    @DisplayName("Given a board When canMakeAMove is called Then it reports whether any legal move exists for the player")
    void canMakeAMoveDetectsAnyLegalMoveForPlayer() {
        assertAll(
                // Empty board -> no moves
                () -> assertFalse(MoveVerifier.canMakeAMove(board, 0)),

                // Put a white man (player 0) that can move forward
                () -> {
                    board[2][2] = new CheckersPiece(false, false);
                    assertTrue(MoveVerifier.canMakeAMove(board, 0));
                }
        );
    }

    @Test
    @DisplayName("Given a king piece When CheckersGame.checkMove is called for a backward move Then the move is accepted")
    void kingAllowsBackwardSimpleMoveInCheckersGame() {
        CheckersGame game = new CheckersGame();
        game.setPlayer(0, "P0");
        game.setPlayer(1, "P1");
        game.start();

        // Build a minimal board: only a white king at (2,2)
        BoardVerifier.resetBoard(game.getBoard());
        game.getBoard()[2][2] = new CheckersPiece(false, true);

        // Try a backward simple move for a king: (2,2) -> (1,1)
        // A non-king man would be rejected, but the king should be accepted.
        CheckersGameMove move = new CheckersGameMove(2, 2, 1, 1);
        Object player = game.getPlayer(0);

        assertAll(
                () -> assertEquals(GameMoveResult.CORRECT_MOVE, game.checkMove(player, move)),
                () -> assertTrue(game.getBoard()[1][1].isKing(), "Expected moved piece to remain a king")
        );
    }
}

