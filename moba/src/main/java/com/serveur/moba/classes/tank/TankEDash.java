package com.serveur.moba.classes.tank;

import com.serveur.moba.ability.*;
import com.serveur.moba.util.Dash;
import com.serveur.moba.util.Flags;
import org.bukkit.entity.Player;

public class TankEDash implements Ability {
    private final CooldownService cds;
    private final Flags flags;
    private final long ccImmuneMs;
    private final double distance;
    private final long cdMs;

    public TankEDash(CooldownService cds, Flags globalFlags, double distance, long ccImmuneMs, long cdMs) {
        this.cds = cds;
        this.flags = globalFlags;
        this.distance = distance;
        this.ccImmuneMs = ccImmuneMs;
        this.cdMs = cdMs;
    }

    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        if (!cds.ready(p, "tank.E", cdMs)) {
            p.sendMessage("§cE en CD.");
            return false;
        }
        flags.set(p, "CC_IMMUNE", true);
        Dash.smallForward(p, distance);
        p.getServer().getScheduler().runTaskLater(ctx.plugin(), () -> flags.set(p, "CC_IMMUNE", false),
                ccImmuneMs / 50L);
        p.sendMessage("§a[Tank] E — dash + immunité CC");
        return true;
    }
}
