package games.contracts;

public final class ContractRoomSummary {

    private final String roomId;
    private final boolean p1Taken;
    private final boolean p2Taken;
    private final int spectators;

    public ContractRoomSummary(String roomId, boolean p1Taken, boolean p2Taken, int spectators) {
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
}
