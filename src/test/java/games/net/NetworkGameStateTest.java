package games.net;

import games.logics.checkers.CheckersPiece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NetworkGameStateTest {

    @Test
    @DisplayName("Given a board snapshot and two player views When toPieceMatrix is called Then player 1 view is rotated relative to player 0 view")
    void toPieceMatrixRotatesBoardForPlayerIndex1() {
        char[][] board = new char[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                board[x][y] = '.';
            }
        }

        // Place a single white man at logical (2,3).
        board[2][3] = 'w';

        NetworkGameState state = new NetworkGameState(
                0,
                new String[]{"P1", "P2"},
                false,
                -1,
                board
        );

        CheckersPiece[][] normal = state.toPieceMatrix(0);
        CheckersPiece[][] rotated = state.toPieceMatrix(1);

        // For viewAsPlayerIndex=0, the piece stays at (2,3).
        assertAll(
                () -> assertNotNull(normal[2][3]),
                () -> assertEquals(false, normal[2][3].getColor()),
                () -> assertEquals(false, normal[2][3].isKing())
        );

        // For viewAsPlayerIndex=1, everything rotates 180 degrees.
        // (x,y) -> (7-x, 7-y), so (2,3) -> (5,4).
        assertAll(
                () -> assertNotNull(rotated[5][4]),
                () -> assertEquals(false, rotated[5][4].getColor()),
                () -> assertEquals(false, rotated[5][4].isKing()),
                () -> assertNull(rotated[2][3], "Rotated matrix should not keep the piece at original coordinates")
        );
    }
}

