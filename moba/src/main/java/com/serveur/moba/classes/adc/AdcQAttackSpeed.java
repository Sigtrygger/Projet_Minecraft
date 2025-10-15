package com.serveur.moba.classes.adc;

import com.serveur.moba.ability.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AdcQAttackSpeed implements Ability {
    private final CooldownService cds;
    private final long cdMs;
    private final long durationMs;
    private final double bonus; // +X sur GENERIC_ATTACK_SPEED
    private final UUID MOD_ID = UUID.fromString("8f7b8a8e-7a9f-4a8f-9b2a-1a1a1a1a1a1a");

    public AdcQAttackSpeed(CooldownService cds, long cdMs, long durationMs, double bonus) {
        this.cds = cds;
        this.cdMs = cdMs;
        this.durationMs = durationMs;
        this.bonus = bonus;
    }

    @SuppressWarnings("removal")
    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        if (!cds.ready(p, "adc.Q", cdMs)) {
            p.sendMessage("§cQ en CD.");
            return false;
        }
        AttributeInstance att = p.getAttribute(Attribute.ATTACK_SPEED);
        if (att == null) {
            p.sendMessage("§cAttaque speed non supportée.");
            return false;
        }

        // Ajout du mod
        AttributeModifier mod = new AttributeModifier(MOD_ID, "adc_q_as", bonus,
                AttributeModifier.Operation.ADD_NUMBER);
        att.removeModifier(MOD_ID); // cleanup si restait
        att.addModifier(mod);
        p.sendMessage("§a[ADC] Q — Vitesse d'attaque augmentée");

        // Retrait après durée
        ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), () -> {
            AttributeInstance at2 = p.getAttribute(Attribute.ATTACK_SPEED);
            if (at2 != null)
                at2.removeModifier(MOD_ID);
            p.sendMessage("§e[ADC] Q — expiré");
        }, durationMs / 50L);
        return true;
    }
}
