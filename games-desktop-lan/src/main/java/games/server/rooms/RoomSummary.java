package games.server.rooms;

import games.contracts.ContractRoomSummary;

public final class RoomSummary {

    private final String roomId;
    private final boolean p1Taken;
    private final boolean p2Taken;
    private final int spectatorCount;

    public RoomSummary(String roomId, boolean p1Taken, boolean p2Taken, int spectatorCount) {
        this.roomId = roomId;
        this.p1Taken = p1Taken;
        this.p2Taken = p2Taken;
        this.spectatorCount = spectatorCount;
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

    public int getSpectatorCount() {
        return spectatorCount;
    }

    public ContractRoomSummary toContract() {
        return new ContractRoomSummary(roomId, p1Taken, p2Taken, spectatorCount);
    }
}

