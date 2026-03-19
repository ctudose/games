# Checkers (JavaFX + Network)

This project supports two transport modes:

- **TCP sockets**: text or JSON line protocol
- **REST polling**: HTTP JSON endpoints with versioned state polling

When using TCP, you can still choose wire format per client:

- **Text protocol**: line-based commands like `JOIN`, `MOVE`, `STATE`, `ROLES`
- **JSON protocol**: one JSON object per line (newline-delimited JSON)

## Configuration

All configuration is loaded from `src/main/resources/games/config.properties` (see `games.config.Config`).

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

## Build and tests

- Run all tests during install:
  - `mvn clean install`

`pom.xml` uses `maven-surefire-plugin` so unit tests run in the Maven `test` phase.

