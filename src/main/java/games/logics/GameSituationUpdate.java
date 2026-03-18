package games.logics;

public class GameSituationUpdate {
    private GameMove move;

    public GameSituationUpdate(GameMove move) {
        this.move = move;
    }

    public GameMove getMove() {
        return move;
    }
}

