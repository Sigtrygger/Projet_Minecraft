package com.serveur.moba.game;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;

public class ZoneManager {
    private final Map<String, Cuboid> zones = new HashMap<>(); // ex: top_blue, mid_red...

    public void setZone(String name, Cuboid c) {
        zones.put(name, c);
    }

    public Cuboid get(String name) {
        return zones.get(name);
    }

    public Optional<String> nameOf(Location loc) {
        return zones.entrySet().stream().filter(e -> e.getValue().contains(loc)).map(Map.Entry::getKey).findFirst();
    }
}
