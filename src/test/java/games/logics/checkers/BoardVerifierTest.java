package games.logics.checkers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardVerifierTest {

    @Test
    void validCoordinatesRespectsLimits() {
        assertAll(
                () -> assertTrue(BoardVerifier.validCoordinates(0, 0)),
                () -> assertTrue(BoardVerifier.validCoordinates(7, 7)),

                () -> assertFalse(BoardVerifier.validCoordinates(-1, 0)),
                () -> assertFalse(BoardVerifier.validCoordinates(0, -1)),
                () -> assertFalse(BoardVerifier.validCoordinates(8, 0)),
                () -> assertFalse(BoardVerifier.validCoordinates(0, 8))
        );
    }

    @Test
    void isLastLineDetectsFirstAndLastRanks() {
        assertAll(
                () -> assertTrue(BoardVerifier.isLastLine(BoardVerifier.lowerLimit)),
                () -> assertTrue(BoardVerifier.isLastLine(BoardVerifier.upperLimit - 1)),

                () -> assertFalse(BoardVerifier.isLastLine(1)),
                () -> assertFalse(BoardVerifier.isLastLine(BoardVerifier.upperLimit - 2))
        );
    }

    @Test
    void initBoardPlacesTwelvePiecesPerColor() {
        CheckersPiece[][] board = new CheckersPiece[BoardVerifier.upperLimit][BoardVerifier.upperLimit];

        BoardVerifier.initBoard(board);

        int countFalse = BoardVerifier.getNoOfPieces(board, false);
        int countTrue  = BoardVerifier.getNoOfPieces(board, true);

        assertAll(
                () -> assertEquals(12, countFalse, "Expected 12 pieces for color=false after initBoard"),
                () -> assertEquals(12, countTrue, "Expected 12 pieces for color=true after initBoard")
        );
    }

    @Test
    void resetBoardClearsAllSquares() {
        CheckersPiece[][] board = new CheckersPiece[BoardVerifier.upperLimit][BoardVerifier.upperLimit];
        BoardVerifier.initBoard(board);

        BoardVerifier.resetBoard(board);

        for (int x = BoardVerifier.lowerLimit; x < BoardVerifier.upperLimit; x++) {
            for (int y = BoardVerifier.lowerLimit; y < BoardVerifier.upperLimit; y++) {
                assertTrue(BoardVerifier.isEmptySquare(board, x, y),
                           "Expected empty square at (" + x + "," + y + ") after resetBoard");
            }
        }
    }
}

