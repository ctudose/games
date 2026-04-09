# Checkers (JavaFX + Network)

This project supports two transport modes:

- **TCP sockets**: text or JSON line protocol
- **REST polling**: HTTP JSON endpoints with versioned state polling

When using TCP, you can still choose wire format per client:

- **Text protocol**: line-based commands like `JOIN`, `MOVE`, `STATE`, `ROLES`
- **JSON protocol**: one JSON object per line (newline-delimited JSON)

## Configuration

All configuration is loaded from `games-desktop-lan/src/main/resources/games/config.properties` (see `games.config.Config` in `games-desktop-lan`).

### Server (per server process)

- `network.host`: host/interface to bind to (default `localhost`)
- `network.transport`: `tcp` or `rest` (default `tcp`)
- `network.port.text`: TCP text listener port (default `5000`)
- `network.port.json`: TCP JSON listener port (default `5001`)
- `network.rest.port`: REST server port (default `5002`)

### Client (per client process)

- `client.transport`: `tcp` or `rest` (default `tcp`)
- `client.protocol`: `text` or `json` (used only when `client.transport=tcp`)
- `client.port`: optional override; if missing:
  - REST client uses `network.rest.port`
  - TCP client uses `network.port.text` or `network.port.json` based on `client.protocol`

### Important

For a working setup, **server and client transports must match**:

- `network.transport=tcp` with `client.transport=tcp`
- `network.transport=rest` with `client.transport=rest`

## TCP mode (dual listeners)

When `network.transport=tcp`, `CheckersServer` starts both listeners:

- text on `network.port.text`
- JSON on `network.port.json`

This allows mixed clients in one room (one text, one JSON) against the same game state.

## REST mode

When `network.transport=rest`, `CheckersServer` starts `games.server.rest.CheckersRestServer`.

REST endpoints:

- `GET /rooms`
- `POST /rooms/create`
- `GET /roles?roomId=<id>`
- `POST /join`
- `POST /move`
- `GET /state?roomId=<id>&token=<sessionToken>`

`/state` includes a `version` field. REST clients poll `/state` and redraw only when `version` changes.

## JSON protocol (newline-delimited)

Client → Server:

- Join:
  - `{"type":"join","role":"P1","name":"Alice"}`
  - `{"type":"join","role":"P2","name":"Bob"}`
- Move:
  - `{"type":"move","from":{"x":2,"y":5},"to":{"x":3,"y":4}}`
- Roles query:
  - `{"type":"roles"}`

Server → Client:

- Error:
  - `{"type":"error","message":"Not your turn"}`
- Roles:
  - `{"type":"roles","p0Taken":true,"p1Taken":false}`
- State:
  - `{"type":"state","roomId":"default","version":12,"playerAtMoveIndex":0,"players":[...],"draw":false,"winnerIndex":-1,"board":["........", "..."]}`

## Test matrix

| Area | Scenario | Covered by | Test file |
|---|---|---|---|
| Engine (`games-shared-core`) | Forced-capture precedence with multiple candidate pieces | `CheckersGameTest` | [`games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java`](games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java) |
| Engine (`games-shared-core`) | Multi-capture continuation and turn ownership | `CheckersGameTest` | [`games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java`](games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java) |
| Engine (`games-shared-core`) | Promotion on last rank, including capture-to-last-rank | `CheckersGameTest` | [`games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java`](games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java) |
| Engine (`games-shared-core`) | End-state transitions (last piece captured / no legal moves) | `CheckersGameTest` | [`games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java`](games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java) |
| Engine (`games-shared-core`) | Invalid input robustness (`INVALID_DATA_FORMAT`, unknown/null player) | `CheckersGameTest` | [`games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java`](games-shared-core/src/test/java/games/logics/checkers/CheckersGameTest.java) |
| Engine (`games-shared-core`) | Robot legality under forced-capture and no-legal-move behavior | `RobotMoveTest` | [`games-shared-core/src/test/java/games/logics/checkers/RobotMoveTest.java`](games-shared-core/src/test/java/games/logics/checkers/RobotMoveTest.java) |
| Integration TCP (`games-desktop-lan`) | Turn accept/reject sequence (legal move then wrong turn) | `TcpJsonIntegrationTest` | [`games-desktop-lan/src/test/java/games/net/TcpJsonIntegrationTest.java`](games-desktop-lan/src/test/java/games/net/TcpJsonIntegrationTest.java) |
| Integration TCP (`games-desktop-lan`) | Room isolation (room A move does not affect room B) | `TcpJsonIntegrationTest` | [`games-desktop-lan/src/test/java/games/net/TcpJsonIntegrationTest.java`](games-desktop-lan/src/test/java/games/net/TcpJsonIntegrationTest.java) |
| Integration REST (`games-desktop-lan`) | Versioned state updates and board update after move | `RestTransportIntegrationTest` | [`games-desktop-lan/src/test/java/games/server/rest/RestTransportIntegrationTest.java`](games-desktop-lan/src/test/java/games/server/rest/RestTransportIntegrationTest.java) |
| Integration REST (`games-desktop-lan`) | Wrong-turn rejection parity and spectator update consistency | `RestTransportIntegrationTest` | [`games-desktop-lan/src/test/java/games/server/rest/RestTransportIntegrationTest.java`](games-desktop-lan/src/test/java/games/server/rest/RestTransportIntegrationTest.java) |
| Integration REST (`games-desktop-lan`) | Error paths (invalid token, spectator move forbidden) | `RestErrorPathsIntegrationTest` | [`games-desktop-lan/src/test/java/games/server/rest/RestErrorPathsIntegrationTest.java`](games-desktop-lan/src/test/java/games/server/rest/RestErrorPathsIntegrationTest.java) |

## Build and tests

- Run all modules:
  - `mvn clean install`

- Run only shared core:
  - `mvn -pl games-shared-core -am test`

- Run only desktop/LAN app module:
  - `mvn -pl games-desktop-lan -am test`

- Compile desktop/LAN app with dependencies:
  - `mvn -pl games-desktop-lan -am compile`

### CI test lanes

- Fast lane (default):
  - `mvn -pl games-shared-core,games-desktop-lan -am test`

- Behavior lane (Cucumber BDD):
  - `mvn -pl games-desktop-lan -Pbehavior-tests test`

- Quality lane (mutation testing with PiTest):
  - `mvn -pl games-shared-core -Pquality-tests verify`
  - `mvn -pl games-desktop-lan -Pquality-tests verify`

- UI lane (TestFX + Monocle, non-default):
  - `mvn -pl games-desktop-lan -Pui-tests verify`

### Coverage (JaCoCo)

- Run all tests with coverage and generate reports:
  - `mvn clean verify`
- Module reports:
  - `games-shared-core/target/site/jacoco/index.html`
  - `games-desktop-lan/target/site/jacoco/index.html`
- Aggregated multi-module report:
  - `target/site/jacoco-aggregate/index.html`

`pom.xml` uses `maven-surefire-plugin` so unit tests run in the Maven `test` phase.

