package com.serveur.moba.classes.bruiser;

import com.serveur.moba.ability.*;
import com.serveur.moba.util.Buffs;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class BruiserWSlowAoE implements Ability {
    private final CooldownService cds;
    private final int amp;
    private final int seconds;
    private final double radius;
    private final long cdMs;

    public BruiserWSlowAoE(CooldownService cds, int amp, int seconds, double radius, long cdMs) {
        this.cds = cds;
        this.amp = amp;
        this.seconds = seconds;
        this.radius = radius;
        this.cdMs = cdMs;
    }

    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        if (!cds.ready(p, "bruiser.W", cdMs)) {
            p.sendMessage("§cW en CD.");
            return false;
        }
        p.getNearbyEntities(radius, radius, radius).stream()
                .filter(e -> e instanceof Player && !((Player) e).equals(p))
                .forEach(e -> Buffs.give((Player) e, PotionEffectType.SLOWNESS, amp, seconds));
        p.sendMessage("§a[Bruiser] W — Slowness AOE");
        return true;
    }
}
