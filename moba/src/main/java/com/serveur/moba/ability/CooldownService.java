package com.serveur.moba.ability;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownService {
    private final Map<String, Long> cds = new HashMap<>();

    private static String k(UUID id, String name) {
        return id + ":" + name;
    }

    public boolean ready(Player p, String name, long ms) {
        long now = System.currentTimeMillis();
        String key = k(p.getUniqueId(), name);
        Long last = cds.get(key);
        if (last == null || (now - last) >= ms) {
            cds.put(key, now);
            return true;
        }
        return false;
    }

    public long remaining(Player p, String name, long ms) {
        Long last = cds.get(k(p.getUniqueId(), name));
        if (last == null)
            return 0;
        long d = ms - (System.currentTimeMillis() - last);
        return Math.max(0, d);
    }
}
