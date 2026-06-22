package com.example.doublesir.websocket.dto;

import com.example.doublesir.model.CardSuit;
import lombok.Data;

@Data
public class SelectTrumpMessage {
    private String roomId;
    private String playerId; // trump keeper id
    private CardSuit trumpSuit;
}
