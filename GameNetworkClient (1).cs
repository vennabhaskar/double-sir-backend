using System;
using System.Collections.Generic;
using System.Text;
using UnityEngine;
using WebSocketSharp;

/// <summary>
/// Unity client for the Double Sir Spring Boot WebSocket/STOMP backend.
/// - Connects to ws://HOST:PORT/ws-game (Spring endpoint)
/// - Sends STOMP CONNECT / SUBSCRIBE / SEND frames
/// - Deserializes JSON game state into GameStatePayload
///
/// Drop this script on a GameObject in your first scene,
/// set serverUrl, roomId, playerName in the Inspector,
/// and call JoinRoom() from UI when ready.
/// </summary>
public class GameNetworkClient : MonoBehaviour
{
    [Header("Server connection")]
    public string serverUrl = "ws://localhost:8080/ws-game"; // Spring Boot WebSocket endpoint
    public string roomId = "ROOM1";
    public string playerName = "Player1";

    [Header("Runtime state (read-only)")]
    public bool connected;
    public GameStatePayload latestState;

    private WebSocket _ws;
    private bool _stompConnected;
    private int _subscriptionId = 0;

    private const string StompVersion = "1.2";
    private const string StompHost = "localhost"; // logical host for Spring STOMP

    void OnDisable()
    {
        Disconnect();
    }

    public void JoinRoom()
    {
        if (_ws != null && _ws.IsAlive)
        {
            Debug.LogWarning("Already connected.");
            return;
        }

        Debug.Log($"[GameNetworkClient] Connecting to {serverUrl}...");
        _ws = new WebSocket(serverUrl);
        _ws.OnOpen += OnWebSocketOpen;
        _ws.OnMessage += OnWebSocketMessage;
        _ws.OnError += (s, e) => Debug.LogError($"[WS ERROR] {e.Message}");
        _ws.OnClose += (s, e) =>
        {
            Debug.LogWarning($"[WS CLOSED] Code={e.Code} Reason={e.Reason}");
            connected = false;
            _stompConnected = false;
        };

        _ws.ConnectAsync();
    }

    public void Disconnect()
    {
        try
        {
            if (_ws != null)
            {
                _ws.CloseAsync();
                _ws = null;
            }
        }
        catch (Exception ex)
        {
            Debug.LogError($"Error closing WebSocket: {ex.Message}");
        }
    }

    private void OnWebSocketOpen(object sender, EventArgs e)
    {
        Debug.Log("[GameNetworkClient] WebSocket open, sending STOMP CONNECT...");
        SendStompConnect();
    }

    private void OnWebSocketMessage(object sender, MessageEventArgs e)
    {
        if (!e.IsText)
        {
            return;
        }

        string data = e.Data;
        // STOMP frames may be concatenated; split on null terminator
        string[] frames = data.Split('\0');
        foreach (var raw in frames)
        {
            var frame = raw.Trim();
            if (string.IsNullOrEmpty(frame)) continue;
            HandleStompFrame(frame);
        }
    }

    private void SendStompConnect()
    {
        var sb = new StringBuilder();
        sb.Append("CONNECT\n");
        sb.Append("accept-version:" + StompVersion + "\n");
        sb.Append("host:" + StompHost + "\n");
        sb.Append("heart-beat:0,0\n\n");
        sb.Append('\0');

        _ws.Send(sb.ToString());
    }

    private void SubscribeGameState()
    {
        _subscriptionId++;
        string destination = $"/topic/room/{roomId}/state";
        var sb = new StringBuilder();
        sb.Append("SUBSCRIBE\n");
        sb.Append($"id:sub-{_subscriptionId}\n");
        sb.Append($"destination:{destination}\n");
        sb.Append("ack:auto\n\n");
        sb.Append('\0');

        _ws.Send(sb.ToString());
        Debug.Log($"[STOMP] SUBSCRIBE {destination}");
    }

    private void SendJoin()
    {
        var join = new JoinRoomMessage
        {
            roomId = roomId,
            playerName = playerName
        };
        string json = JsonUtility.ToJson(join);
        SendStompSend($"/app/room/{roomId}/join", json);
        Debug.Log($"[STOMP] SEND join room={roomId} name={playerName}");
    }

    public void SendPlayCard(string playerId, string cardId)
    {
        if (!_stompConnected)
        {
            Debug.LogWarning("Cannot play card, STOMP not connected yet.");
            return;
        }
        var msg = new PlayCardMessage
        {
            roomId = roomId,
            playerId = playerId,
            cardId = cardId
        };
        string json = JsonUtility.ToJson(msg);
        SendStompSend($"/app/room/{roomId}/play-card", json);
    }

    public void SendSelectTrump(string playerId, CardSuit trumpSuit)
    {
        if (!_stompConnected)
        {
            Debug.LogWarning("Cannot select trump, STOMP not connected yet.");
            return;
        }
        var msg = new SelectTrumpMessage
        {
            roomId = roomId,
            playerId = playerId,
            trumpSuit = trumpSuit.ToString()
        };
        string json = JsonUtility.ToJson(msg);
        SendStompSend($"/app/room/{roomId}/select-trump", json);
    }

    private void SendStompSend(string destination, string jsonBody)
    {
        var sb = new StringBuilder();
        sb.Append("SEND\n");
        sb.Append($"destination:{destination}\n");
        sb.Append("content-type:application/json\n\n");
        sb.Append(jsonBody);
        sb.Append('\0');

        _ws.Send(sb.ToString());
    }

    private void HandleStompFrame(string frame)
    {
        // Very small STOMP parser: split headers and body by first blank line
        var parts = frame.Split(new[] { "\n\n" }, 2, StringSplitOptions.None);
        string headerPart = parts[0];
        string bodyPart = parts.Length > 1 ? parts[1] : string.Empty;

        string[] headerLines = headerPart.Split('\n');
        string command = headerLines[0].Trim();

        if (command == "CONNECTED")
        {
            Debug.Log("[STOMP] CONNECTED");
            _stompConnected = true;
            connected = true;
            SubscribeGameState();
            SendJoin();
            return;
        }

        if (command == "MESSAGE")
        {
            // bodyPart may still contain null terminator trimmed earlier
            string body = bodyPart.Trim('\0', '\n', '\r');
            if (!string.IsNullOrEmpty(body))
            {
                try
                {
                    var state = JsonUtility.FromJson<GameStatePayload>(body);
                    latestState = state;
                    // Here you can trigger UI updates, events, etc.
                }
                catch (Exception ex)
                {
                    Debug.LogError($"Failed to parse GameStatePayload: {ex.Message}\nBody: {body}");
                }
            }
            return;
        }

        if (command == "ERROR")
        {
            Debug.LogError("[STOMP ERROR] " + frame);
            return;
        }

        // Other STOMP frames (RECEIPT, etc.) can be handled here if needed
    }
}
