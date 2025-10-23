package com.serveur.moba.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.serveur.moba.game.GameManager;
import com.serveur.moba.game.enums.Lane;

public class CombatListener implements Listener {
        private final GameManager gm;

        public CombatListener(GameManager gm) {
                this.gm = gm;
        }

        @EventHandler(ignoreCancelled = true)
        public void onDamage(EntityDamageByEntityEvent e) {
                if (!(e.getEntity() instanceof Player victim))
                        return;
                Player damager = null;
                if (e.getDamager() instanceof Player p)
                        damager = p;
                else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p)
                        damager = p;
                if (damager == null)
                        return;

                // Déterminer la lane où ils sont (ex: via gm.zoneManager.nameOf(loc))
                Location loc = victim.getLocation();
                String zoneName = gm.zoneManager.nameOf(loc).orElse("");
                Lane lane = zoneName.startsWith("top") ? Lane.TOP
                                : zoneName.startsWith("mid") ? Lane.MID : zoneName.startsWith("bot") ? Lane.BOT : null;

                if (lane == null || !gm.pvpGate.isAllowed(lane)) {
                        e.setCancelled(true);
                        damager.sendMessage("§cLe PvP n'est pas encore autorisé ici.");
                }
        }
}
