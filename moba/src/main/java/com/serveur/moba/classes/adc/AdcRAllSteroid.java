package com.serveur.moba.classes.adc;

import com.serveur.moba.ability.*;
import com.serveur.moba.util.Buffs;
import org.bukkit.attribute.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class AdcRAllSteroid implements Ability {
    private final CooldownService cds;
    private final long cdMs;
    private final long durationMs;
    private final double bonusMaxHearts; // +X coeurs (max health)
    private final double absorbHearts; // +X coeurs d’absorption

    private final UUID MAXHP_MOD = UUID.fromString("2c3d98e6-4c47-4d7f-9f0a-aa2e0e11a111");

    public AdcRAllSteroid(CooldownService cds, long cdMs, long durationMs, double bonusMaxHearts, double absorbHearts) {
        this.cds = cds;
        this.cdMs = cdMs;
        this.durationMs = durationMs;
        this.bonusMaxHearts = bonusMaxHearts;
        this.absorbHearts = absorbHearts;
    }

    @SuppressWarnings("removal")
    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        if (!cds.ready(p, "adc.R", cdMs)) {
            p.sendMessage("§cR en CD.");
            return false;
        }

        // Max health+
        AttributeInstance maxHp = p.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) {
            double plus = bonusMaxHearts * 2.0;
            AttributeModifier mod = new AttributeModifier(MAXHP_MOD, "adc_r_maxhp", plus,
                    AttributeModifier.Operation.ADD_NUMBER);
            maxHp.removeModifier(MAXHP_MOD);
            maxHp.addModifier(mod);
            p.setHealth(Math.min(p.getHealth() + plus, maxHp.getValue())); // heal pour profiter
        }

        // Absorption
        Buffs.absorption(p, absorbHearts);

        // Strength I, Speed I, Fire Resistance
        Buffs.give(p, PotionEffectType.STRENGTH, 1, (int) (durationMs / 1000));
        Buffs.give(p, PotionEffectType.SPEED, 1, (int) (durationMs / 1000));
        Buffs.give(p, PotionEffectType.FIRE_RESISTANCE, 1, (int) (durationMs / 1000));

        // Cleanup
        ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), () -> {
            AttributeInstance mh = p.getAttribute(Attribute.MAX_HEALTH);
            if (mh != null)
                mh.removeModifier(MAXHP_MOD);
            p.sendMessage("§e[ADC] R — expiré");
        }, durationMs / 50L);

        p.sendMessage("§a[ADC] R — Stéroïde activé");
        return true;
    }
}
