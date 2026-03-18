---
name: networked-checkers-javafx
overview: Add a network-capable two-player mode to the JavaFX checkers game, with Player 1/Player 2 role selection and rotated board for Player 2, starting with two windows on one machine but designed to work over LAN later.
todos:
  - id: setup-server
    content: Create a simple TCP-based CheckersServer that hosts a single CheckersGame and validates/broadcasts moves.
    status: pending
  - id: client-connection-layer
    content: Implement a CheckersClientConnection class to connect to the server, send JOIN/MOVE commands, and receive STATE/UPDATE messages.
    status: pending
  - id: role-selection-ui
    content: Update the JavaFX app to show a role selection screen (Player 1 / Player 2) and then connect to the server with the chosen role.
    status: pending
  - id: client-game-scene
    content: Refactor the JavaFX game scene to use server-driven state (no local authoritative CheckersGame) and send moves over the network.
    status: pending
  - id: turn-and-ownership-enforcement
    content: Enforce turn order and per-player piece ownership on both client and server sides.
    status: pending
  - id: board-orientation-player2
    content: Implement coordinate-based board orientation so Player 2 sees the board rotated 180 degrees and interacts with flipped coordinates.
    status: pending
  - id: basic-error-handling
    content: Handle disconnects and invalid moves gracefully, and disable interaction when game is over or connection is lost.
    status: pending
isProject: false
---

### Networked JavaFX Checkers Plan

#### 1. High-level architecture

- **Authoritative server**: A small Java process that owns a single `CheckersGame` instance, validates all moves, and broadcasts state/move updates.
- **JavaFX client**: Existing UI evolved into a client that:
  - Shows an initial **role selection screen** with two buttons: `Player 1`, `Player 2`.
  - Connects to the server (initially `localhost:PORT`) and registers as the chosen player.
  - Shows the board and status, oriented according to role (Player 2 sees a 180° rotated view).
  - Sends moves to the server and applies updates pushed from the server.
- **Future LAN support**: Because both windows talk to the server via TCP sockets, switching from `localhost` to a LAN IP is enough to move from same-machine testing to two machines.

#### 2. Server-side design

- **Module / main class**
  - Create a `CheckersServer` main class (e.g. `[src/com/luxoft/games/server/CheckersServer.java](src/com/luxoft/games/server/CheckersServer.java)`).
  - Responsibilities:
    - Listen on a TCP port (configurable, default like `5000`).
    - Accept up to two client connections.
    - Maintain mapping: connection → player index (0 or 1).
    - Hold a single `CheckersGame` instance and the `Game`-level player names.
    - Apply moves, validate via `checkMove`, and broadcast `GameSituation` or incremental updates to both clients.
- **Protocol choices (simple and extensible)**
  - Use a **line-based text protocol** or trivial JSON lines, for example:
    - Client → server:
      - `JOIN playerName role` (role = `P1` or `P2`).
      - `MOVE xFrom yFrom xTo yTo`.
    - Server → client:
      - `ACCEPTED role` / `REJECTED reason`.
      - `STATE <serializedBoardAndMeta>` (send full game snapshot) **or** `UPDATE xFrom yFrom xTo yTo flags`.
      - `ERROR reason`.
  - Start with a minimal text format that serializes:
    - Player names and whose turn it is.
    - Board as 8×8 array of `pieceColor:kingFlag` (or `.` for empty).
- **Game loop on server**
  - For each client connection, run a thread or use non-blocking I/O:
    - Read commands, parse, and pass moves to `CheckersGame.checkMove(...)`.
    - If move is valid:
      - Update game state.
      - Broadcast updated `STATE` / `UPDATE` to both clients.
    - If invalid:
      - Send `ERROR` or `MOVE_INVALID` back to that client (do not change game state).

#### 3. Client-side networking layer

- **Client connection class**
  - Introduce a small networking helper, e.g. `[src/com/luxoft/games/net/CheckersClientConnection.java](src/com/luxoft/games/net/CheckersClientConnection.java)`:
    - Handles TCP socket to server.
    - Provides async callbacks or listeners for:
      - `onConnected`, `onAssignedRole`, `onStateUpdate`, `onError`.
    - Provides methods:
      - `connect(host, port)`.
      - `joinAsPlayer(role, name)`.
      - `sendMove(CheckersGameMove move)`.
  - Parse server messages and convert them into either:
    - A **pure UI-side model** (mirroring `CheckersGame` state but without logic), or
    - Calls to local `CheckersGame` used only as a *mirror* (without running rules locally; rules live on server).
- **Synchronization model**
  - Treat server as the single source of truth:
    - After every accepted move, server sends updated state.
    - The client updates its local board representation and requests a UI redraw.
  - For simplicity, initially only handle **"latest state wins"** and avoid reconnection/replay logic.

#### 4. Adapting the JavaFX UI to client/server

- **Startup flow**
  - Replace or wrap the existing `CheckersApp.start(...)` to:
    - First show a **role selection scene**:
      - `Button "Player 1"` → choose role P1.
      - `Button "Player 2"` → choose role P2.
    - Optional text field for player name (default `Player 1` / `Player 2`).
    - On button press:
      - Create a `CheckersClientConnection`.
      - Connect to `localhost:5000` (for now).
      - Send `JOIN` with selected role and name.
      - On server confirmation and initial `STATE`, switch to the game scene.
- **Game scene changes**
  - The game scene reuses `BoardView` and `CheckersController`, but:
    - **Does not own a local `CheckersGame` instance as the source of truth**.
    - Instead consumes a client-side model updated from the server messages.
    - On cell click / drag-drop, the controller:
      - Builds a `CheckersGameMove` conceptually (xFrom, yFrom, xTo, yTo), but
      - Sends it to the server via `CheckersClientConnection.sendMove(...)` instead of calling `game.checkMove(...)` directly.
    - UI only applies moves when server sends back a valid `STATE/UPDATE`.

#### 5. Enforcing per-player turn and piece ownership

- **On the client**
  - The client keeps track of:
    - Its assigned role: `Player 1` → index 0, `Player 2` → index 1.
    - The current `playerAtMoveIndex` from the last `STATE` message.
  - The controller must:
    - Allow move attempts *only* if `myIndex == playerAtMoveIndex`.
    - Only allow selecting/moving pieces that match **my color** based on the board representation (mirroring `CheckersGame.wrongPlayer`).
    - If the user tries to move when it’s not their turn, simply ignore the click/drag or show a small status message.
- **On the server**
  - The server double-checks:
    - That the sender corresponds to the player whose turn it is.
    - That the move is valid by passing through `CheckersGame.checkMove`.
  - This ensures a misbehaving client can’t cheat.

#### 6. Board orientation for Player 2

- **Coordinate mapping approach**
  - Keep the server’s board coordinates as today (`0..7` indices, bottom side = Player 1).
  - On the client, for Player 2, **flip coordinates and/or board drawing**:
    - In `BoardView` or a small adapter, invert indices for P2:
      - Logical `(x, y)` from server becomes
        - displayX = `7 - x`
        - displayY = `7 - y`.
    - Alternatively, use a `Rotate` transform on the `BoardView` node by 180° and also invert the hit-testing mapping in the controller from mouse events back to logical `(x,y)`.
  - Choose the **coordinate mapping** approach, as it is clearer for hit-testing and drag/drop.
- **Implementation steps**
  - Add a `viewAsPlayerIndex` flag to the JavaFX side (0 or 1).
  - When updating board cells from state:
    - If `viewAsPlayerIndex == 0` → draw as now.
    - If `viewAsPlayerIndex == 1` → use flipped indices when placing pieces and interpreting clicks.
  - Reuse the earlier applet idea of `viewAsWhichPlayer`, but confined to the JavaFX client.

#### 7. Handling game lifecycle and reconnection (basic)

- **Game start**
  - Server waits until both roles are joined before allowing moves.
  - Until then, clients show “Waiting for opponent to join…” in the status bar.
- **Game over**
  - When server’s `CheckersGame` detects a winner or draw:
    - Send a `STATE` that contains winner/draw info.
    - Clients show “Player X won” or “Draw” and disable move attempts.
- **Minimal error handling**
  - If connection drops, client shows a message and disables moves.
  - For now, no automatic reconnection; can be added later.

#### 8. Testing strategy (single machine)

- **Manual tests**
  - Start the `CheckersServer` in one JVM.
  - Run **two instances** of the JavaFX client app (or start the client twice from IDE/command line).
  - In the first client:
    - Click `Player 1`, enter name (optional), verify you see the board from Player 1’s side.
  - In the second client:
    - Click `Player 2`, verify board appears rotated (via coordinate mapping) and you see your pieces at the bottom.
  - Verify:
    - Only the active player (from server’s point of view) can move.
    - Moves are reflected on both clients after the server accepts them.
    - Illegal moves are rejected and the board remains unchanged.
- **Future LAN test**
  - Run `CheckersServer` on one machine.
  - Start clients on two different machines, using the server machine’s IP instead of `localhost`.

#### 9. Possible future enhancements (out of initial scope)

- Replace raw TCP with a higher-level library (e.g., WebSocket or RMI) for cleaner protocol handling.
- Add a simple lobby UI to select host/join and configure server IP/port.
- Add chat, move history, undo (if rules allow), and timer/clock support.
- Persist game results or enable replays by logging moves on the server.

