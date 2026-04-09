package games.net;

import games.contracts.ContractRoomSummary;

public final class RoomInfo {

    private final String roomId;
    private final boolean p1Taken;
    private final boolean p2Taken;
    private final int spectators;

    public RoomInfo(String roomId, boolean p1Taken, boolean p2Taken, int spectators) {
        this.roomId = roomId;
        this.p1Taken = p1Taken;
        this.p2Taken = p2Taken;
        this.spectators = spectators;
    }

    public String getRoomId() {
        return roomId;
    }

    public boolean isP1Taken() {
        return p1Taken;
    }

    public boolean isP2Taken() {
        return p2Taken;
    }

    public int getSpectators() {
        return spectators;
    }

    public ContractRoomSummary toContract() {
        return new ContractRoomSummary(roomId, p1Taken, p2Taken, spectators);
    }

    public static RoomInfo fromContract(ContractRoomSummary room) {
        if (room == null) {
            return null;
        }
        return new RoomInfo(room.getRoomId(), room.isP1Taken(), room.isP2Taken(), room.getSpectators());
    }
}

