Unity client integration for Double Sir Spring Boot WebSocket/STOMP game
================================================================

This folder contains C# scripts you can drop into a Unity project to
connect to your existing Spring Boot + WebSocket (STOMP) backend.

Files
-----
- Assets/Scripts/GameNetworkClient.cs
  Minimal STOMP-over-WebSocket client built on top of websocket-sharp.
  It:
    * Connects to ws://HOST:PORT/ws-game (Spring endpoint)
    * Sends STOMP CONNECT, SUBSCRIBE, SEND frames
    * Subscribes to /topic/room/{roomId}/state
    * Sends join / select-trump / play-card messages to /app/room/{roomId}/...
    * Exposes latest GameStatePayload as a public field for your UI/logic.

- Assets/Scripts/GameStateModels.cs
  Serializable C# models that match the JSON structure broadcast by
  your Spring GameEngine (roomId, phase, players, cards, etc.).

How to use in Unity
-------------------
1. Create a new 3D/2D Unity project.
2. Copy the `Assets` folder from this package into your project so that
   `Assets/Scripts/GameNetworkClient.cs` and `GameStateModels.cs` exist.
3. Add the `websocket-sharp` DLL:
   - Download websocket-sharp (MIT-licensed) for Unity.
   - Drop the DLL into `Assets/Plugins/` or `Assets/Plugins/WebSocketSharp/`.

4. In your first scene:
   - Create an empty GameObject, name it `NetworkClient`.
   - Attach the `GameNetworkClient` component.
   - Set `serverUrl` = ws://localhost:8080/ws-game (or your deployed server URL).
   - Set `roomId` and `playerName` in the Inspector.
   - From a UI button or Start(), call `GameNetworkClient.JoinRoom()`.

5. Backend side:
   - Ensure Spring Boot broadcasts a unified game state as JSON to
     `/topic/room/{roomId}/state`.
   - The JSON should match the fields defined in GameStatePayload and
     nested models (camelCase).

6. In your Unity game logic:
   - Read `GameNetworkClient.latestState` each frame or via events and
     update your card table, player labels, and animations accordingly.
   - To play a card from the local hand, call
       `networkClient.SendPlayCard(myPlayerId, cardIdFromLatestState)`.

Note on Unity licensing
-----------------------
Unity Personal remains free for individuals and small teams whose
revenue or funding is under USD $200,000 in the last 12 months.
You can build and deploy games commercially on PC, mobile, and WebGL
with Unity Personal under that revenue cap, and there is no runtime
per-install fee on Personal games.
Closed consoles (PlayStation, Xbox, Switch) require Pro + platform
holder approval.

Check https://unity.com/products/unity-personal for the latest terms.
