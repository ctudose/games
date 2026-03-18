package games.logics.checkers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static games.logics.checkers.BoardVerifier.upperLimit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveVerifierTest {

    private CheckersPiece[][] board;

    @BeforeEach
    void setUp() {
        board = new CheckersPiece[upperLimit][upperLimit];
    }

    @Test
    void moveOutsideTableRejectsOutOfBounds() {
        assertAll(
                () -> assertTrue(MoveVerifier.moveOutsideTable(-1, 0, 0, 1)),
                () -> assertTrue(MoveVerifier.moveOutsideTable(0, 0, 8, 1)),
                () -> assertFalse(MoveVerifier.moveOutsideTable(0, 0, 1, 1))
        );
    }

    @Test
    void isLegalDiagonalMoveAcceptsSingleAndDoubleSteps() {
        assertAll(
                () -> assertTrue(MoveVerifier.isLegalDiagonalMove(2, 2, 3, 3)), // single step
                () -> assertTrue(MoveVerifier.isLegalDiagonalMove(2, 2, 4, 4)), // capture step

                () -> assertFalse(MoveVerifier.isLegalDiagonalMove(2, 2, 2, 3)), // vertical
                () -> assertFalse(MoveVerifier.isLegalDiagonalMove(2, 2, 3, 4))  // non 45-degree
        );
    }

    @Test
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
}

