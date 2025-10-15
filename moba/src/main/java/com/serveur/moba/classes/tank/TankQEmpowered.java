package com.serveur.moba.classes.tank;

import com.serveur.moba.ability.*;
import org.bukkit.entity.Player;

import java.util.*;

public class TankQEmpowered implements Ability {
    private final Map<java.util.UUID, Integer> stacks = new HashMap<>();
    private final Map<java.util.UUID, Long> window = new HashMap<>();

    private final CooldownService cds;
    private final long cdMs; // CD global Q
    private final long windowMs; // fenêtre pour consommer les 3 AA

    public TankQEmpowered(CooldownService cds, long cdMs, long windowMs) {
        this.cds = cds;
        this.cdMs = cdMs;
        this.windowMs = windowMs;
    }

    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        if (!cds.ready(p, "tank.Q", cdMs)) {
            p.sendMessage("§cQ en CD.");
            return false;
        }
        stacks.put(p.getUniqueId(), 3);
        window.put(p.getUniqueId(), System.currentTimeMillis() + windowMs);
        p.sendActionBar(net.kyori.adventure.text.Component.text("§6[Tank] Q — 3 coups renforcés prêts"));
        return true;
    }

    public int getStacks(Player p) {
        Long until = window.get(p.getUniqueId());
        if (until == null || System.currentTimeMillis() > until) {
            stacks.remove(p.getUniqueId());
            window.remove(p.getUniqueId());
            return 0;
        }
        return stacks.getOrDefault(p.getUniqueId(), 0);
    }

    public void consume(Player p) {
        int s = getStacks(p);
        if (s <= 1) {
            stacks.remove(p.getUniqueId());
            window.remove(p.getUniqueId());
        } else
            stacks.put(p.getUniqueId(), s - 1);
    }
}
