package com.serveur.moba.classes.tank;

import com.serveur.moba.combat.CombatTagService;
import com.serveur.moba.util.Buffs;
import com.serveur.moba.util.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TankPassiveListener implements Listener {
    private final long internalCdMs; // X secondes
    private final Flags flags = new Flags();
    private final CombatTagService combat;

    public TankPassiveListener(long internalCdMs, CombatTagService combat) {
        this.internalCdMs = internalCdMs;
        this.combat = combat;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !(e.getDamager() instanceof Player attacker))
            return;
        combat.tag(victim);
        combat.tag(attacker);

        String FLAG = "tank_passive_cd";
        if (flags.has(victim, FLAG))
            return;

        // Blindness 3 pendant X s
        Buffs.give(attacker, org.bukkit.potion.PotionEffectType.BLINDNESS, 3, 3);
        flags.set(victim, FLAG, true);
        victim.getServer().getScheduler().runTaskLater(victim.getServer().getPluginManager().getPlugin("Moba"),
                () -> flags.set(victim, FLAG, false), internalCdMs / 50L);
    }
}
