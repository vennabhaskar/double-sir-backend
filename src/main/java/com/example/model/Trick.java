package com.example.doublesir.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Trick {
    private final List<PlayedCard> cards = new ArrayList<>();
    private CardSuit leadingSuit;
    private PlayerPosition winner;
    private boolean complete;
}
