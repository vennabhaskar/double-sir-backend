package com.example.doublesir.websocket.dto;

import lombok.Data;

@Data
public class JoinRoomMessage {
    private String roomId;
    private String playerName;
}
