package com.example.doublesir.websocket;

import com.example.doublesir.model.CardSuit;
import com.example.doublesir.service.GameEngine;
import com.example.doublesir.service.RoomManager;
import com.example.doublesir.websocket.dto.JoinRoomMessage;
import com.example.doublesir.websocket.dto.PlayCardMessage;
import com.example.doublesir.websocket.dto.SelectTrumpMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final RoomManager roomManager;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, JoinRoomMessage message, Principal principal) {
        String playerName = message.getPlayerName() != null ? message.getPlayerName() : principal.getName();
        Map<String, Object> view = roomManager.joinRoom(roomId, principal.getName(), playerName);
        // broadcast updated state to all clients in room
        GameEngine engine = roomManager.engine(roomId);
        if (engine != null) {
            for (var p : engine.getState().getPlayers()) {
                Map<String, Object> pv = engine.buildClientView(p.getId());
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/" + p.getId(), pv);
            }
        }
    }

    @MessageMapping("/room/{roomId}/select-trump")
    public void selectTrump(@DestinationVariable String roomId, SelectTrumpMessage message) {
        roomManager.selectTrump(roomId, message.getPlayerId(), message.getTrumpSuit());
        GameEngine engine = roomManager.engine(roomId);
        if (engine != null) {
            for (var p : engine.getState().getPlayers()) {
                Map<String, Object> pv = engine.buildClientView(p.getId());
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/" + p.getId(), pv);
            }
        }
    }

    @MessageMapping("/room/{roomId}/play-card")
    public void playCard(@DestinationVariable String roomId, PlayCardMessage message) {
        roomManager.playCard(roomId, message.getPlayerId(), message.getCardId());
        GameEngine engine = roomManager.engine(roomId);
        if (engine != null) {
            for (var p : engine.getState().getPlayers()) {
                Map<String, Object> pv = engine.buildClientView(p.getId());
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/" + p.getId(), pv);
            }
        }
    }
}
