package games.net;

/**
 * Abstraction for a client connection to a checkers game server.
 *
 * Implementations are responsible for:
 * - establishing the connection asynchronously via connectAsync()
 * - sending moves to the server via sendMove(...)
 * - closing underlying resources via close()
 *
 * Server state and errors are delivered to the UI through callbacks
 * supplied to the concrete implementation's constructor.
 */
public interface CheckersConnection {

    void connectAsync();

    void sendMove(int xFrom, int yFrom, int xTo, int yTo);

    void close();
}

