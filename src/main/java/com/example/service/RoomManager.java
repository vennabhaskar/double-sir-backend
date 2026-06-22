package com.example.doublesir.service;

import com.example.doublesir.model.CardSuit;
import com.example.doublesir.model.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private final Map<String, GameEngine> rooms = new ConcurrentHashMap<>();

    public GameEngine getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, GameEngine::new);
    }

    public Map<String, Object> joinRoom(String roomId, String sessionId, String playerName) {
        GameEngine engine = getOrCreateRoom(roomId);
        Player p = engine.addPlayer(sessionId, playerName);
        return engine.buildClientView(p.getId());
    }

    public Map<String, Object> selectTrump(String roomId, String playerId, CardSuit suit) {
        GameEngine engine = getOrCreateRoom(roomId);
        engine.selectTrump(suit, playerId);
        return engine.buildClientView(playerId);
    }

    public Map<String, Object> playCard(String roomId, String playerId, String cardId) {
        GameEngine engine = getOrCreateRoom(roomId);
        engine.playCard(playerId, cardId);
        return engine.buildClientView(playerId);
    }

    public GameEngine engine(String roomId) {
        return rooms.get(roomId);
    }
}
