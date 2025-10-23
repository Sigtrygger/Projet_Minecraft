package com.serveur.moba.game;

import java.util.EnumMap;

import com.serveur.moba.game.enums.Lane;

public class PvpGate {
    private final EnumMap<Lane, Boolean> allow = new EnumMap<>(Lane.class);

    public PvpGate() {
        for (Lane l : Lane.values())
            allow.put(l, false);
    }

    public void set(Lane lane, boolean on) {
        allow.put(lane, on);
    }

    public boolean isAllowed(Lane lane) {
        return allow.get(lane);
    }
}
