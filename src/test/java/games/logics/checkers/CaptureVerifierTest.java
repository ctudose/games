package games.logics.checkers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureVerifierTest {

    @Test
    @DisplayName("Given a board where only the white side can capture When canMakeACapture is called Then playerIndex 0 is true and playerIndex 1 is false")
    void canMakeACaptureBoardPlayerIndexDetectsAnyCaptureForThatColor() {
        CheckersPiece[][] board = new CheckersPiece[BoardVerifier.upperLimit][BoardVerifier.upperLimit];

        // White man (color=false) at (2,2) can capture black man at (3,3) into (4,4).
        // Important: we block black's potential "capture back" landing square at (1,1),
        // otherwise canMakeACapture(board, 1) may become true.
        board[2][2] = new CheckersPiece(false, false);
        board[3][3] = new CheckersPiece(true, false);

        board[1][1] = new CheckersPiece(false, false); // blocks black from landing after capture-back

        assertAll(
                () -> assertTrue(CaptureVerifier.canMakeACapture(board, 0), "Expected capture available for playerIndex=0 (white)"),
                () -> assertFalse(CaptureVerifier.canMakeACapture(board, 1), "Expected no capture available for playerIndex=1 (black)")
        );
    }

    @Test
    @DisplayName("Given a board where no captures exist When canMakeACapture is called Then it returns false for both playerIndex values")
    void canMakeACaptureBoardPlayerIndexReturnsFalseWhenNoCaptureExists() {
        CheckersPiece[][] board = new CheckersPiece[BoardVerifier.upperLimit][BoardVerifier.upperLimit];

        // Two pieces, but no opponent placed to enable a capture landing square.
        board[2][2] = new CheckersPiece(false, false);
        board[5][5] = new CheckersPiece(true, false);

        assertAll(
                () -> assertFalse(CaptureVerifier.canMakeACapture(board, 0)),
                () -> assertFalse(CaptureVerifier.canMakeACapture(board, 1))
        );
    }

    @Test
    @DisplayName("Given an empty square When canMakeACapture(board,x,y) is called Then it returns false")
    void canMakeACaptureBoardXyReturnsFalseOnEmptySquare() {
        CheckersPiece[][] board = new CheckersPiece[BoardVerifier.upperLimit][BoardVerifier.upperLimit];
        assertFalse(CaptureVerifier.canMakeACapture(board, 2, 2));
    }

    @Test
    @DisplayName("Given a piece that has a legal capture When canMakeACapture(board,x,y) is called Then it returns true")
    void canMakeACaptureBoardXyDetectsCaptureForThatPiece() {
        CheckersPiece[][] board = new CheckersPiece[BoardVerifier.upperLimit][BoardVerifier.upperLimit];

        // White man (color=false) at (2,2) capture (3,3) -> landing (4,4)
        board[2][2] = new CheckersPiece(false, false);
        board[3][3] = new CheckersPiece(true, false);

        assertTrue(CaptureVerifier.canMakeACapture(board, 2, 2));
    }
}

