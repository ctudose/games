package games.net;

import games.contracts.ContractRoomSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomInfoContractMappingTest {

    @Test
    @DisplayName("Given a room info object When converting to contract Then all fields are preserved")
    void roomInfoToContractPreservesFields() {
        RoomInfo room = new RoomInfo("abc", true, false, 4);
        ContractRoomSummary contract = room.toContract();

        assertAll(
                () -> assertNotNull(contract),
                () -> assertEquals("abc", contract.getRoomId()),
                () -> assertTrue(contract.isP1Taken()),
                () -> assertFalse(contract.isP2Taken()),
                () -> assertEquals(4, contract.getSpectators())
        );
    }

    @Test
    @DisplayName("Given a contract room summary When converting from contract Then all fields are preserved")
    void roomInfoFromContractPreservesFields() {
        ContractRoomSummary contract = new ContractRoomSummary("xyz", false, true, 1);
        RoomInfo room = RoomInfo.fromContract(contract);

        assertAll(
                () -> assertNotNull(room),
                () -> assertEquals("xyz", room.getRoomId()),
                () -> assertFalse(room.isP1Taken()),
                () -> assertTrue(room.isP2Taken()),
                () -> assertEquals(1, room.getSpectators())
        );
    }

    @Test
    @DisplayName("Given a null room contract When converting to room info Then null is returned")
    void roomInfoFromContractHandlesNull() {
        assertNull(RoomInfo.fromContract(null));
    }
}

