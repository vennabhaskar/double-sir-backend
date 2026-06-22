package com.example.doublesir.websocket.dto;

import lombok.Data;

@Data
public class PlayCardMessage {
    private String roomId;
    private String playerId;
    private String cardId;
}
