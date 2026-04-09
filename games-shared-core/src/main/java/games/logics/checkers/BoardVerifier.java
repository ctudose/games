package games.logics.checkers;

public class BoardVerifier {
    public static final int lowerLimit = 0;
    public static final int upperLimit = 8;

    public static void resetBoard(CheckersPiece board[][]) {
        for (int i = lowerLimit; i < upperLimit; i++) {
            for (int j = lowerLimit; j < upperLimit; j++) {
                board[i][j] = null;
            }
        }
    }

    public static void initBoard(CheckersPiece board[][]) {
        resetBoard(board);
        board[1][0] = new CheckersPiece(false, false);
        board[3][0] = new CheckersPiece(false, false);
        board[5][0] = new CheckersPiece(false, false);
        board[7][0] = new CheckersPiece(false, false);
        board[0][1] = new CheckersPiece(false, false);
        board[2][1] = new CheckersPiece(false, false);
        board[4][1] = new CheckersPiece(false, false);
        board[6][1] = new CheckersPiece(false, false);
        board[1][2] = new CheckersPiece(false, false);
        board[3][2] = new CheckersPiece(false, false);
        board[5][2] = new CheckersPiece(false, false);
        board[7][2] = new CheckersPiece(false, false);
        board[0][7] = new CheckersPiece(true, false);
        board[2][7] = new CheckersPiece(true, false);
        board[4][7] = new CheckersPiece(true, false);
        board[6][7] = new CheckersPiece(true, false);
        board[1][6] = new CheckersPiece(true, false);
        board[3][6] = new CheckersPiece(true, false);
        board[5][6] = new CheckersPiece(true, false);
        board[7][6] = new CheckersPiece(true, false);
        board[0][5] = new CheckersPiece(true, false);
        board[2][5] = new CheckersPiece(true, false);
        board[4][5] = new CheckersPiece(true, false);
        board[6][5] = new CheckersPiece(true, false);
    }

    public static boolean validCoordinates(int x, int y) {
        return (x >= lowerLimit) && (x < upperLimit) && (y >= lowerLimit) && (y < upperLimit);
    }

    public static boolean isLastLine(int yTo) {
        return (yTo == lowerLimit) || (yTo == upperLimit - 1);
    }

    public static boolean isEmptySquare(CheckersPiece board[][], int x, int y) {
        return board[x][y] == null;
    }

    public static int getNoOfPieces(CheckersPiece board[][], boolean color) {
        int count = 0;

        for (int x = lowerLimit; x < upperLimit; x++) {
            for (int y = lowerLimit; y < upperLimit; y++) {
                if (!isEmptySquare(board, x, y) && (board[x][y].getColor() == color)) {
                    count++;
                }
            }
        }

        return count;
    }
}

