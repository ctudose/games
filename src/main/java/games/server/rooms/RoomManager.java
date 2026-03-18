package games.server.rooms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RoomManager {

    public static final String DEFAULT_ROOM_ID = "default";

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public RoomManager() {
        // create default room eagerly
        rooms.put(DEFAULT_ROOM_ID, new GameRoom(DEFAULT_ROOM_ID));
    }

    public GameRoom getOrCreateDefaultRoom() {
        return rooms.computeIfAbsent(DEFAULT_ROOM_ID, GameRoom::new);
    }

    public GameRoom createRoom() {
        String id = UUID.randomUUID().toString();
        GameRoom room = new GameRoom(id);
        rooms.put(id, room);
        return room;
    }

    public GameRoom get(String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            return getOrCreateDefaultRoom();
        }
        return rooms.get(roomId);
    }

    public List<RoomSummary> listRooms() {
        List<RoomSummary> list = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            list.add(new RoomSummary(
                    room.getRoomId(),
                    room.isPlayerSlotTaken(0),
                    room.isPlayerSlotTaken(1),
                    room.getSpectatorCount()
            ));
        }
        return list;
    }
}

