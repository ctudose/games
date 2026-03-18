package games.server;

import games.server.rooms.RoomManager;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

public class RoomManagerTest {

    @Test
    void createRoomCreatesUniqueIds() {
        RoomManager manager = new RoomManager();

        Set<String> ids = new HashSet<>();

        for (int i = 0; i < 20; i++) {
            String id = manager.createRoom().getRoomId();
            assertNotNull(id);
            assertFalse(id.isEmpty());
            assertTrue(ids.add(id), "Duplicate roomId: " + id);
        }
    }

    @Test
    void defaultRoomAlwaysExists() {
        RoomManager manager = new RoomManager();

        assertAll(
                () -> assertNotNull(manager.getOrCreateDefaultRoom()),
                () -> assertEquals(RoomManager.DEFAULT_ROOM_ID, manager.getOrCreateDefaultRoom().getRoomId()),
                () -> assertNotNull(manager.get(RoomManager.DEFAULT_ROOM_ID))
        );
    }
}

