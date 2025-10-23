package com.serveur.moba.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.serveur.moba.game.GameManager;

import java.util.Optional;

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
                if (e.getDamager() instanceof Player p) {
                        damager = p;
                } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                        damager = p;
                }
                if (damager == null)
                        return;

                // On prend la lane du VICTIME (emplacement du combat)
                Optional<String> laneOpt = gm.lane().laneOf(victim);
                if (laneOpt.isEmpty()) {
                        // en dehors des lanes connues -> pas de PvP
                        e.setCancelled(true);
                        damager.sendMessage("§cLe PvP n'est pas autorisé ici.");
                        return;
                }

                String laneName = laneOpt.get(); // "top" / "mid" / "bot"
                if (!gm.lane().isPvpOpen(laneName)) {
                        e.setCancelled(true);
                        damager.sendMessage("§cLe PvP n'est pas encore autorisé sur la lane §e" + laneName + "§c.");
                }
        }
}
