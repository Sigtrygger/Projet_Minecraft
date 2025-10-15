package com.serveur.moba.classes.tank;

import com.serveur.moba.ability.*;
import com.serveur.moba.util.Buffs;
import org.bukkit.entity.Player;

public class TankRSlowAoE implements Ability {
    private final CooldownService cds;
    private final int amp; // Slowness X++
    private final int seconds; // durée
    private final double radius;
    private final long cdMs;

    public TankRSlowAoE(CooldownService cds, int amp, int seconds, double radius, long cdMs) {
        this.cds = cds;
        this.amp = amp;
        this.seconds = seconds;
        this.radius = radius;
        this.cdMs = cdMs;
    }

    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        if (!cds.ready(p, "tank.R", cdMs)) {
            p.sendMessage("§cR en CD.");
            return false;
        }
        p.getNearbyEntities(radius, radius, radius).stream()
                .filter(e -> e instanceof Player && !((Player) e).equals(p))
                .forEach(e -> Buffs.give((Player) e, org.bukkit.potion.PotionEffectType.SLOWNESS, amp, seconds));
        p.sendMessage("§a[Tank] R — AOE Slowness");
        return true;
    }
}
