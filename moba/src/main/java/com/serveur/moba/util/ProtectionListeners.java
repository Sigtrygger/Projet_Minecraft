package com.serveur.moba.util;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

public class ProtectionListeners implements Listener {
    private final Flags flags;

    private boolean isNegativeEffect(PotionEffectType type) {
        return type == PotionEffectType.POISON ||
                type == PotionEffectType.WITHER ||
                type == PotionEffectType.BLINDNESS ||
                type == PotionEffectType.INSTANT_DAMAGE ||
                type == PotionEffectType.HUNGER ||
                type == PotionEffectType.SLOWNESS ||
                type == PotionEffectType.MINING_FATIGUE ||
                type == PotionEffectType.WEAKNESS ||
                type == PotionEffectType.BAD_OMEN ||
                type == PotionEffectType.UNLUCK ||
                type == PotionEffectType.LEVITATION ||
                type == PotionEffectType.DARKNESS;
    }

    public ProtectionListeners(Flags flags) {
        this.flags = flags;

    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;
        if (flags.has(p, "GOD_SHIELD"))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPotion(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;

        // GOD_SHIELD : bloque tous les effets négatifs
        if (flags.has(p, "GOD_SHIELD")) {
            if (e.getAction() == EntityPotionEffectEvent.Action.ADDED
                    || e.getAction() == EntityPotionEffectEvent.Action.CHANGED) {
                if (e.getModifiedType() != null && isNegativeEffect(e.getModifiedType()))
                    e.setCancelled(true);
            }
        }

        // CC_IMMUNE : bloque principaux CC
        if (flags.has(p, "CC_IMMUNE")) {
            PotionEffectType t = e.getModifiedType();
            if (isNegativeEffect(t)) {
                e.setCancelled(true);
            }
        }
    }
}
