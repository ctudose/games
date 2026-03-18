# Checkers (JavaFX + TCP)

This project supports two wire protocols between the network client and `games.server.CheckersServer`:

- **Text protocol** (legacy, default): line-based commands like `JOIN`, `MOVE`, `STATE`, `ROLES`
- **JSON protocol** (optional): one JSON object per line (newline-delimited JSON)

## Configuration

All configuration is loaded from `src/main/resources/games/config.properties` (see `games.config.Config`).

### Server (per server process)

- `network.port`: TCP port this server instance listens on
- `server.protocol`: `text` or `json` (defaults to `text`)

### Client (per client process)

- `client.protocol`: `text` or `json` (defaults to `text`)
- `client.port`: optional; if missing, the client falls back to `network.port`

## Run multiple servers (different ports/protocols)

You can start multiple server JVMs at once, each reading a different `.properties` file (one per instance), e.g.:

- Server A: `server.protocol=text`, `network.port=5000`
- Server B: `server.protocol=json`, `network.port=5001`

Clients must connect to the matching port and use the matching `client.protocol`.

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
  - `{"type":"state","playerAtMoveIndex":0,"players":[...],"draw":false,"winnerIndex":-1,"board":["........", "..."]}`

