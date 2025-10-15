package com.serveur.moba.classes.bruiser;

import com.serveur.moba.ability.*;
import com.serveur.moba.util.Buffs;
import com.serveur.moba.util.Dash;
import org.bukkit.entity.Player;

public class BruiserEDashAbsorb implements Ability {
    private final CooldownService cds;
    private final double distance;
    private final long cdMs;

    public BruiserEDashAbsorb(CooldownService cds, double distance, long cdMs) {
        this.cds = cds;
        this.distance = distance;
        this.cdMs = cdMs;
    }

    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        if (!cds.ready(p, "bruiser.E", cdMs)) {
            p.sendMessage("§cE en CD.");
            return false;
        }
        Dash.smallForward(p, distance);
        Buffs.absorption(p, 4.0);
        p.sendMessage("§a[Bruiser] E — Dash + Absorption");
        return true;
    }
}
