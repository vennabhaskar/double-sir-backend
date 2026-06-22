package com.example.doublesir.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Player {
    private String id;          // internal id in room
    private String sessionId;   // WebSocket session id
    private String name;
    private PlayerPosition position;
    private String team;        // "A" or "B"
    private boolean trumpKeeper;
    private boolean connected = true;
    private final List<Card> hand = new ArrayList<>();
}
