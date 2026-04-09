package games.contracts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractsTest {

    @Test
    @DisplayName("Given a contract game state When getters are called Then all constructor values are preserved")
    void contractGameStateStoresAllFields() {
        String[] playerNames = {"P1", "P2"};
        char[][] board = new char[8][8];
        board[1][2] = 'w';

        ContractGameState state = new ContractGameState(0, playerNames, false, -1, board);

        assertAll(
                () -> assertEquals(0, state.getPlayerAtMoveIndex()),
                () -> assertArrayEquals(playerNames, state.getPlayerNames()),
                () -> assertFalse(state.isDraw()),
                () -> assertEquals(-1, state.getWinnerIndex()),
                () -> assertEquals('w', state.getBoardChars()[1][2])
        );
    }

    @Test
    @DisplayName("Given a contract move When getters are called Then source and destination coordinates are preserved")
    void contractMoveStoresCoordinates() {
        ContractMove move = new ContractMove(1, 2, 3, 4);

        assertAll(
                () -> assertEquals(1, move.getXFrom()),
                () -> assertEquals(2, move.getYFrom()),
                () -> assertEquals(3, move.getXTo()),
                () -> assertEquals(4, move.getYTo())
        );
    }

    @Test
    @DisplayName("Given a contract room summary When getters are called Then room occupancy fields are preserved")
    void contractRoomSummaryStoresFields() {
        ContractRoomSummary summary = new ContractRoomSummary("room-1", true, false, 3);

        assertAll(
                () -> assertEquals("room-1", summary.getRoomId()),
                () -> assertTrue(summary.isP1Taken()),
                () -> assertFalse(summary.isP2Taken()),
                () -> assertEquals(3, summary.getSpectators())
        );
    }

    @Test
    @DisplayName("Given a contract roles object When getters are called Then role flags are preserved")
    void contractRolesStoresFields() {
        ContractRoles roles = new ContractRoles(true, false);

        assertAll(
                () -> assertTrue(roles.isP0Taken()),
                () -> assertFalse(roles.isP1Taken())
        );
    }

    @Test
    @DisplayName("Given a contract error object When getters are called Then code and message are preserved")
    void contractErrorStoresFields() {
        ContractError error = new ContractError("E100", "Illegal move");

        assertAll(
                () -> assertEquals("E100", error.getCode()),
                () -> assertEquals("Illegal move", error.getMessage())
        );
    }
}

