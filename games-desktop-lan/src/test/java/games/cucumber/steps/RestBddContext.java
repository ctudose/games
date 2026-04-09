package games.cucumber.steps;

import com.fasterxml.jackson.databind.JsonNode;
import games.server.rest.CheckersRestServer;
import games.test.JfxTestUtil;
import games.test.RestTestHttpUtil;

import java.io.IOException;
import java.net.http.HttpClient;

public class RestBddContext {
    String host;
    int port;
    CheckersRestServer server;
    HttpClient http;
    String roomId;
    String roomIdA;
    String roomIdB;
    String tokenP1;
    String tokenP2;
    String tokenAP1;
    String tokenAP2;
    String tokenBP1;
    String tokenBP2;
    String tokenS;
    JsonNode lastJson;
    int lastStatus;

    void startServer() throws IOException {
        JfxTestUtil.initJfx();
        host = "127.0.0.1";
        port = JfxTestUtil.freePort();
        server = new CheckersRestServer(host, port);
        server.start();
        http = HttpClient.newHttpClient();
    }

    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    void createRoom() throws IOException, InterruptedException {
        roomId = RestTestHttpUtil.createRoom(http, host, port);
    }
}

