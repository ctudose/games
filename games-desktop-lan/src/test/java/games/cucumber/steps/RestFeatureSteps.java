package games.cucumber.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import games.test.RestTestHttpUtil;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RestFeatureSteps {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RestBddContext ctx = new RestBddContext();

    @Before
    public void before() throws IOException {
        ctx.startServer();
    }

    @After
    public void after() {
        ctx.stopServer();
    }

    @Given("a fresh REST game room")
    public void aFreshRestGameRoom() throws IOException, InterruptedException {
        ctx.createRoom();
        assertNotNull(ctx.roomId);
    }

    @When("player one and player two join the room")
    public void playerOneAndPlayerTwoJoinTheRoom() throws IOException, InterruptedException {
        ctx.tokenP1 = RestTestHttpUtil.join(ctx.http, ctx.host, ctx.port, ctx.roomId, "P1", "P1");
        ctx.tokenP2 = RestTestHttpUtil.join(ctx.http, ctx.host, ctx.port, ctx.roomId, "P2", "P2");
    }

    @Then("both join tokens are issued")
    public void bothJoinTokensAreIssued() {
        assertAll(
                () -> assertNotNull(ctx.tokenP1),
                () -> assertNotNull(ctx.tokenP2)
        );
    }

    @When("player one performs a legal opening move")
    public void playerOnePerformsALegalOpeningMove() throws IOException, InterruptedException {
        RestTestHttpUtil.postMove(ctx.http, ctx.host, ctx.port, ctx.roomId, ctx.tokenP1, 1, 2, 0, 3);
        JsonNode state = RestTestHttpUtil.getJson(ctx.http, ctx.host, ctx.port,
                "/state?roomId=" + ctx.roomId + "&token=" + ctx.tokenP1);
        ctx.lastJson = state;
        ctx.lastStatus = 200;
    }

    @Then("the next turn belongs to player two")
    public void theNextTurnBelongsToPlayerTwo() {
        assertEquals(1, ctx.lastJson.get("playerAtMoveIndex").asInt());
    }

    @When("player one tries to move again immediately")
    public void playerOneTriesToMoveAgainImmediately() throws IOException, InterruptedException {
        JsonNode root = RestTestHttpUtil.postMoveExpectingStatus(
                ctx.http, ctx.host, ctx.port, ctx.roomId, ctx.tokenP1, 1, 2, 0, 3, 400
        );
        ctx.lastJson = root;
        ctx.lastStatus = 400;
    }

    @Then("the move is rejected as not allowed now")
    public void theMoveIsRejectedAsNotAllowedNow() {
        assertAll(
                () -> assertEquals(400, ctx.lastStatus),
                () -> assertEquals("error", ctx.lastJson.get("type").asText()),
                () -> assertTrue(ctx.lastJson.get("message").asText().contains("NOT_ALLOWED_TO_MOVE_NOW"))
        );
    }

    @When("a spectator joins the room")
    public void aSpectatorJoinsTheRoom() throws IOException, InterruptedException {
        ctx.tokenS = RestTestHttpUtil.join(ctx.http, ctx.host, ctx.port, ctx.roomId, "S", "Spec");
    }

    @When("the spectator attempts to move a piece")
    public void theSpectatorAttemptsToMoveAPiece() throws IOException, InterruptedException {
        JsonNode root = RestTestHttpUtil.postMoveExpectingStatus(
                ctx.http, ctx.host, ctx.port, ctx.roomId, ctx.tokenS, 0, 5, 1, 4, 403
        );
        ctx.lastJson = root;
        ctx.lastStatus = 403;
    }

    @Then("the server forbids the spectator move")
    public void theServerForbidsTheSpectatorMove() {
        assertAll(
                () -> assertEquals(403, ctx.lastStatus),
                () -> assertEquals("error", ctx.lastJson.get("type").asText()),
                () -> assertEquals("Spectators cannot move", ctx.lastJson.get("message").asText())
        );
    }

    @When("state is requested with an invalid token")
    public void stateIsRequestedWithAnInvalidToken() throws IOException, InterruptedException {
        HttpResponse<String> resp = ctx.http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://" + ctx.host + ":" + ctx.port + "/state?roomId=" + ctx.roomId + "&token=badtoken"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        ctx.lastStatus = resp.statusCode();
        ctx.lastJson = MAPPER.readTree(resp.body());
    }

    @Then("the server returns unauthorized")
    public void theServerReturnsUnauthorized() {
        assertAll(
                () -> assertEquals(401, ctx.lastStatus),
                () -> assertEquals("error", ctx.lastJson.get("type").asText()),
                () -> assertEquals("Invalid token", ctx.lastJson.get("message").asText())
        );
    }

    @Given("two fresh REST rooms each with two joined players")
    public void twoFreshRestRoomsEachWithTwoJoinedPlayers() throws IOException, InterruptedException {
        ctx.roomIdA = RestTestHttpUtil.createRoom(ctx.http, ctx.host, ctx.port);
        ctx.roomIdB = RestTestHttpUtil.createRoom(ctx.http, ctx.host, ctx.port);

        ctx.tokenAP1 = RestTestHttpUtil.join(ctx.http, ctx.host, ctx.port, ctx.roomIdA, "P1", "A1");
        ctx.tokenAP2 = RestTestHttpUtil.join(ctx.http, ctx.host, ctx.port, ctx.roomIdA, "P2", "A2");
        ctx.tokenBP1 = RestTestHttpUtil.join(ctx.http, ctx.host, ctx.port, ctx.roomIdB, "P1", "B1");
        ctx.tokenBP2 = RestTestHttpUtil.join(ctx.http, ctx.host, ctx.port, ctx.roomIdB, "P2", "B2");

        assertAll(
                () -> assertNotNull(ctx.tokenAP1),
                () -> assertNotNull(ctx.tokenAP2),
                () -> assertNotNull(ctx.tokenBP1),
                () -> assertNotNull(ctx.tokenBP2)
        );
    }

    @When("player one makes a legal move in room A")
    public void playerOneMakesALegalMoveInRoomA() throws IOException, InterruptedException {
        JsonNode beforeB = RestTestHttpUtil.getJson(
                ctx.http, ctx.host, ctx.port, "/state?roomId=" + ctx.roomIdB + "&token=" + ctx.tokenBP1
        );
        RestTestHttpUtil.postMove(ctx.http, ctx.host, ctx.port, ctx.roomIdA, ctx.tokenAP1, 1, 2, 0, 3);
        JsonNode afterB = RestTestHttpUtil.getJson(
                ctx.http, ctx.host, ctx.port, "/state?roomId=" + ctx.roomIdB + "&token=" + ctx.tokenBP1
        );

        ctx.lastJson = MAPPER.createObjectNode()
                .put("beforeTurn", beforeB.get("playerAtMoveIndex").asInt())
                .put("afterTurn", afterB.get("playerAtMoveIndex").asInt())
                .put("beforeSrc", String.valueOf(beforeB.get("board").get(2).asText().charAt(1)))
                .put("afterSrc", String.valueOf(afterB.get("board").get(2).asText().charAt(1)))
                .put("beforeDst", String.valueOf(beforeB.get("board").get(3).asText().charAt(0)))
                .put("afterDst", String.valueOf(afterB.get("board").get(3).asText().charAt(0)));
    }

    @Then("room B state remains unchanged")
    public void roomBStateRemainsUnchanged() {
        assertAll(
                () -> assertEquals(ctx.lastJson.get("beforeTurn").asInt(), ctx.lastJson.get("afterTurn").asInt()),
                () -> assertEquals(ctx.lastJson.get("beforeSrc").asText(), ctx.lastJson.get("afterSrc").asText()),
                () -> assertEquals(ctx.lastJson.get("beforeDst").asText(), ctx.lastJson.get("afterDst").asText())
        );
    }

    @When("the following moves are played:")
    public void theFollowingMovesArePlayed(DataTable table) throws IOException, InterruptedException {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            String player = row.get("player");
            int xFrom = Integer.parseInt(row.get("xFrom"));
            int yFrom = Integer.parseInt(row.get("yFrom"));
            int xTo = Integer.parseInt(row.get("xTo"));
            int yTo = Integer.parseInt(row.get("yTo"));

            String token = "P1".equalsIgnoreCase(player) ? ctx.tokenP1 : ctx.tokenP2;
            RestTestHttpUtil.postMove(ctx.http, ctx.host, ctx.port, ctx.roomId, token, xFrom, yFrom, xTo, yTo);
        }

        ctx.lastJson = RestTestHttpUtil.getJson(
                ctx.http, ctx.host, ctx.port, "/state?roomId=" + ctx.roomId + "&token=" + ctx.tokenP1
        );
        ctx.lastStatus = 200;
    }

    @Then("the board has {int} white pieces and {int} black pieces")
    public void theBoardHasWhiteAndBlackPieces(int expectedWhite, int expectedBlack) {
        JsonNode board = ctx.lastJson.get("board");
        int white = 0;
        int black = 0;

        for (JsonNode row : board) {
            String line = row.asText();
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == 'w' || c == 'W') {
                    white++;
                } else if (c == 'b' || c == 'B') {
                    black++;
                }
            }
        }
        final int whiteCount = white;
        final int blackCount = black;

        assertAll(
                () -> assertEquals(expectedWhite, whiteCount),
                () -> assertEquals(expectedBlack, blackCount)
        );
    }

    @Then("the next turn belongs to player one")
    public void theNextTurnBelongsToPlayerOne() {
        assertEquals(0, ctx.lastJson.get("playerAtMoveIndex").asInt());
    }
}

