package com.serveur.moba.classes.tank;

import com.serveur.moba.ability.*;
import com.serveur.moba.util.Buffs;
import org.bukkit.entity.Player;

public class TankWAbsorb implements Ability {
    private final CooldownService cds;
    private final double hearts;
    private final long cdMs;

    public TankWAbsorb(CooldownService cds, double hearts, long cdMs) {
        this.cds = cds;
        this.hearts = hearts;
        this.cdMs = cdMs;
    }

    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        if (!cds.ready(p, "tank.W", cdMs)) {
            long r = cds.remaining(p, "tank.W", cdMs) / 1000;
            p.sendMessage("§cW en CD (" + r + "s).");
            return false;
        }
        Buffs.absorption(p, hearts);
        p.sendMessage("§a[Tank] W — Absorption +" + hearts + "❤");
        return true;
    }
}
