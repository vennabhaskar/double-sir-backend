package com.example.doublesir.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayedCard {
    private String playerId;
    private PlayerPosition position;
    private Card card;
    private boolean trumpPlay;
    private long timestamp;
}
