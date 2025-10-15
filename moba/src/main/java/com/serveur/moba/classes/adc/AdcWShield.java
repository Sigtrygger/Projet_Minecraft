package com.serveur.moba.classes.adc;

import com.serveur.moba.ability.*;
import com.serveur.moba.util.Flags;
import org.bukkit.entity.Player;

public class AdcWShield implements Ability {
    private final CooldownService cds;
    private final Flags flags;
    private final long cdMs;
    private final long shieldMs;

    public AdcWShield(CooldownService cds, Flags flags, long cdMs, long shieldMs) {
        this.cds = cds;
        this.flags = flags;
        this.cdMs = cdMs;
        this.shieldMs = shieldMs;
    }

    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        if (!cds.ready(p, "adc.W", cdMs)) {
            p.sendMessage("§cW en CD.");
            return false;
        }
        flags.set(p, "GOD_SHIELD", true);
        ctx.plugin().getServer().getScheduler().runTaskLater(ctx.plugin(), () -> flags.set(p, "GOD_SHIELD", false),
                shieldMs / 50L);
        p.sendMessage("§a[ADC] W — Bouclier actif");
        return true;
    }
}
