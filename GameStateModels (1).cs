using System;
using System.Collections.Generic;
using UnityEngine;

/// <summary>
/// These C# models must match the JSON shape broadcast by your Spring backend
/// on /topic/room/{roomId}/state.
///
/// Recommended JSON structure (camelCase):
/// {
///   "roomId": "ROOM1",
///   "phase": "PLAYING",
///   "trumpSuit": "HEARTS",
///   "trumpRevealed": true,
///   "currentTurn": "SOUTH",
///   "trickLeader": "NORTH",
///   "teamAHnds": 3,
///   "teamBHands": 2,
///   "consecutiveWins": 1,
///   "lastTrickWinner": "NORTH",
///   "winningTeam": null,
///   "players": [ { ... } ],
///   "currentTrick": [ { ... } ],
///   "tablePileCount": 12
/// }
/// </summary>
[Serializable]
public class GameStatePayload
{
    public string roomId;
    public string phase;
    public string trumpSuit;
    public bool trumpRevealed;
    public string currentTurn;
    public string trickLeader;
    public int teamAHnds;
    public int teamBHands;
    public int consecutiveWins;
    public string lastTrickWinner;
    public string winningTeam;

    public List<PlayerView> players;
    public List<PlayedCardView> currentTrick;
    public int tablePileCount;
}

[Serializable]
public class PlayerView
{
    public string id;
    public string name;
    public string position; // "NORTH", "EAST", etc.
    public string team;      // "A" or "B"
    public bool trumpKeeper;
    public bool connected;

    // For the local player, backend should fill "hand"; for others, only "handCount".
    public List<CardView> hand;
    public int handCount;
}

[Serializable]
public class CardView
{
    public string id;
    public string suit;   // "HEARTS", "DIAMONDS", ...
    public string rank;   // "ACE", "KING", etc.
    public bool faceUp;
}

[Serializable]
public class PlayedCardView
{
    public string playerId;
    public string position;
    public CardView card;
    public bool trumpPlay;
}

[Serializable]
public class JoinRoomMessage
{
    public string roomId;
    public string playerName;
}

[Serializable]
public class PlayCardMessage
{
    public string roomId;
    public string playerId;
    public string cardId;
}

[Serializable]
public class SelectTrumpMessage
{
    public string roomId;
    public string playerId;
    public string trumpSuit;
}

public enum CardSuit
{
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}
