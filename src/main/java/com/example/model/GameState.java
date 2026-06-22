package com.example.doublesir.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GameState {
    private String roomId;
    private GamePhase phase = GamePhase.WAITING;
    private final List<Player> players = new ArrayList<>();
    private final List<Card> deck = new ArrayList<>();

    private CardSuit trumpSuit;
    private Card trumpCard;          // card kept aside
    private boolean trumpRevealed = false;

    private final Trick currentTrick = new Trick();
    private final List<Trick> completedTricks = new ArrayList<>();
    private final List<PlayedCard> tablePile = new ArrayList<>();

    private PlayerPosition currentTurn = PlayerPosition.NORTH;
    private PlayerPosition trickLeader = PlayerPosition.NORTH;
    private int consecutiveWins = 0;
    private PlayerPosition lastTrickWinner;

    private int teamAHnds = 0;
    private int teamBHands = 0;

    private boolean lastWinWasAce = false;
    private String winningTeam; // "A" or "B"
}
