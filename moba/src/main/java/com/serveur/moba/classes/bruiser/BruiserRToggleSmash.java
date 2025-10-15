package com.serveur.moba.classes.bruiser;

import com.serveur.moba.ability.*;
import com.serveur.moba.util.Buffs;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class BruiserRToggleSmash implements Ability {
    private final CooldownService cds;
    private final long durationMs;
    private final long cdMs;
    private final double radius;
    private final double coneDegrees;

    private final Set<UUID> active = new HashSet<>();

    public BruiserRToggleSmash(CooldownService cds, long durationMs, long cdMs, double radius, double coneDegrees) {
        this.cds = cds;
        this.durationMs = durationMs;
        this.cdMs = cdMs;
        this.radius = radius;
        this.coneDegrees = coneDegrees;
    }

    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        UUID id = p.getUniqueId();

        // Si actif -> relance pour SMASH
        if (active.contains(id)) {
            active.remove(id);
            p.removePotionEffect(PotionEffectType.STRENGTH);
            smash(ctx);
            p.sendMessage("§6[Bruiser] R — Smash déclenché !");
            return true;
        }

        // Activation du buff
        if (!cds.ready(p, "bruiser.R", cdMs)) {
            p.sendMessage("§cR en CD.");
            return false;
        }
        active.add(id);
        Buffs.give(p, PotionEffectType.STRENGTH, 2, (int) (durationMs / 1000));

        // Désactivation auto
        ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), () -> {
            if (active.remove(id)) {
                p.removePotionEffect(PotionEffectType.STRENGTH);
                p.sendMessage("§c[Bruiser] R — expiré.");
            }
        }, durationMs / 50L);
        p.sendMessage("§a[Bruiser] R — Strength II actif (appuie encore pour SMASH)");
        return true;
    }

    private void smash(AbilityContext ctx) {
        Player p = ctx.player();
        Vector fwd = p.getLocation().getDirection().normalize();
        double cos = Math.cos(Math.toRadians(coneDegrees / 2.0));

        p.getNearbyEntities(radius, radius, radius).stream()
                .filter(e -> e instanceof LivingEntity && !e.equals(p))
                .map(e -> (LivingEntity) e)
                .filter(le -> {
                    Vector to = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                    return fwd.dot(to) >= cos; // dans le cône devant
                })
                .forEach(le -> {
                    le.damage(8.0, p); // “gros dégâts” (à ajuster)
                    le.setVelocity(fwd.clone().multiply(0.7)); // petit knock
                });
    }
}
