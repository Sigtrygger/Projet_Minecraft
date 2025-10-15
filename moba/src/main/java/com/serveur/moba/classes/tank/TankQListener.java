package com.serveur.moba.classes.tank;

import com.serveur.moba.util.Buffs;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;

public class TankQListener implements Listener {
    private final TankQEmpowered q;

    public TankQListener(TankQEmpowered q) {
        this.q = q;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p))
            return;
        int s = q.getStacks(p);
        if (s <= 0)
            return;

        // Coups renforcés : +30% dégâts par exemple
        e.setDamage(e.getDamage() * 1.3);

        // Sur la 3e (quand s == 1), appliquer Slowness I 3s
        if (s == 1 && e.getEntity() instanceof Player target) {
            Buffs.give((Player) target, PotionEffectType.SLOWNESS, 1, 3);
        }
        q.consume(p);
    }
}
