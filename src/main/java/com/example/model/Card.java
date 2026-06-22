package com.example.doublesir.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    private String id;
    private CardSuit suit;
    private CardRank rank;
    private boolean faceUp = true;
}
