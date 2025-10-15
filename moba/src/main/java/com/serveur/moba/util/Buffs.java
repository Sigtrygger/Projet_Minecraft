package com.serveur.moba.util;

import org.bukkit.entity.Player;
import org.bukkit.potion.*;

public final class Buffs {
    private Buffs() {
    }

    public static void give(Player p, PotionEffectType type, int amplifier, int seconds) {
        p.addPotionEffect(new PotionEffect(type, seconds * 20, Math.max(0, amplifier - 1), false, false, true));
    }

    public static void absorption(Player p, double hearts) {
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), hearts * 2.0)); // 1 coeur = 2 hp
    }
}
