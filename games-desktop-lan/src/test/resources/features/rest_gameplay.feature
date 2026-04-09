Feature: REST room gameplay and permissions

  Scenario: Players can join and the turn advances after a legal move
    Given a fresh REST game room
    When player one and player two join the room
    Then both join tokens are issued
    When player one performs a legal opening move
    Then the next turn belongs to player two

  Scenario: Wrong turn is rejected
    Given a fresh REST game room
    When player one and player two join the room
    And player one performs a legal opening move
    And player one tries to move again immediately
    Then the move is rejected as not allowed now

  Scenario: Spectator move is forbidden
    Given a fresh REST game room
    When player one and player two join the room
    And a spectator joins the room
    And the spectator attempts to move a piece
    Then the server forbids the spectator move

  Scenario: Invalid token on state request is unauthorized
    Given a fresh REST game room
    When state is requested with an invalid token
    Then the server returns unauthorized

  Scenario: Room isolation across REST rooms
    Given two fresh REST rooms each with two joined players
    When player one makes a legal move in room A
    Then room B state remains unchanged

  Scenario: Simulate a longer legal game sequence
    Given a fresh REST game room
    When player one and player two join the room
    And the following moves are played:
      | player | xFrom | yFrom | xTo | yTo |
      | P1     | 1     | 2     | 0   | 3   |
      | P2     | 2     | 5     | 1   | 4   |
      | P1     | 0     | 3     | 2   | 5   |
      | P2     | 3     | 6     | 1   | 4   |
    Then the board has 11 white pieces and 11 black pieces
    And the next turn belongs to player one

  Scenario: Simulate an opening with alternating legal moves
    Given a fresh REST game room
    When player one and player two join the room
    And the following moves are played:
      | player | xFrom | yFrom | xTo | yTo |
      | P1     | 1     | 2     | 0   | 3   |
      | P2     | 2     | 5     | 1   | 4   |
      | P1     | 0     | 3     | 2   | 5   |
    Then the board has 12 white pieces and 11 black pieces
    And the next turn belongs to player two

