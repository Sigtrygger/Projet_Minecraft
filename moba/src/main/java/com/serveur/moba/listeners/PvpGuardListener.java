package com.serveur.moba.listeners;

import com.serveur.moba.lane.LaneManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

/*
 * Listener pour protéger les joueurs en dehors des lanes quand le PvP est fermé
 * - Utilise LaneManager pour savoir si un joueur est dans une lane
 * - Empêche les dégâts entre joueurs si PvP est fermé dans la lane courante
 * - Empêche les dégâts aux joueurs en dehors des lanes si PvP est fermé globalement
 */
public class PvpGuardListener implements Listener {

    private final LaneManager lanes;

    public PvpGuardListener(LaneManager lanes) {
        this.lanes = lanes;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim))
            return;
        if (!(e.getDamager() instanceof Player attacker))
            return;

        Optional<String> laneV = lanes.laneOf(victim);
        Optional<String> laneA = lanes.laneOf(attacker);
        if (laneV.isEmpty() || laneA.isEmpty())
            return; // hors de toute lane → on laisse passer le PvP donc à redéfinir plus tard

        boolean sameLane = laneV.get().equalsIgnoreCase(laneA.get());
        boolean pvpOpen = lanes.isPvpOpen(laneV.get());

        if (!sameLane || !pvpOpen) {
            e.setCancelled(true);
            attacker.sendActionBar(Component.text("§cPvP OFF ici (ou cible hors de ta lane)"));
        }
    }
}
