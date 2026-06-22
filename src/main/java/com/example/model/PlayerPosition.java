package com.example.doublesir.model;

public enum PlayerPosition {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public PlayerPosition next() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    public PlayerPosition partner() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
}
