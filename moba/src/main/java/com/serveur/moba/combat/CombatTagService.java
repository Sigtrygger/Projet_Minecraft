package com.serveur.moba.combat;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.*;

public class CombatTagService {
    private final Map<UUID, Long> lastHitMs = new HashMap<>();
    private final long timeoutMs;

    public CombatTagService(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public void tag(Player p) {
        lastHitMs.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public boolean inCombat(Player p) {
        Long t = lastHitMs.get(p.getUniqueId());
        return t != null && (System.currentTimeMillis() - t) < timeoutMs;
    }

    public static boolean isMonster(Entity e) {
        return switch (e.getType()) {
            case ZOMBIE, SKELETON, SPIDER, CREEPER, ENDERMAN, WITCH, SLIME, MAGMA_CUBE, PHANTOM, DROWNED -> true;
            default -> false;
        };
    }
}
