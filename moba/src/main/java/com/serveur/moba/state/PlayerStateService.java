package com.serveur.moba.state;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.UUID;

/*
 * Service simple pour gérer l'état des joueurs (classe, niveau, golds, etc...)
 *  - Mémoire uniquement (aucune persistance)
 *  - Met à jour l'UI (level & barre d'xp simulée) périodiquement
 */

public class PlayerStateService {

    public enum Role {
        TANK, BRUISER, ADC
    }

    /** Etat minimal d'un joueur */
    public static class State {
        public Role role = Role.BRUISER;
        public int level = 1; // Niveau du joueur 1 -> 18
        public int xp = 0; // Expérience actuelle
        public int gold = 0; // Or gagné via kills et mobs
        public int aaCount = 0; // Nombre d'attaques basiques (Auto Attaque) effectuées
    }

    private final Map<UUID, State> states = new HashMap<>();

    public PlayerStateService(Plugin plugin) {
        // on fait une petite UI temporaire pour afficher le niveau en Level et l'or
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                State s = get(p.getUniqueId());
                p.setLevel(s.level);
                p.setExp(Math.min(0.99f, (s.gold % 100) / 100f));
            }
        }, 20L, 40L);
    }

    /** Définit la classe du joueur via une chaîne saisie en commande */
    public boolean setClass(UUID playerId, String role) {
        Role r = switch (role.toLowerCase()) {
            case "tank" -> Role.TANK;
            case "bruiser" -> Role.BRUISER;
            case "adc" -> Role.ADC;
            default -> null;
        };
        if (r != null) {
            get(playerId).role = r;
            return true;
        } else {
            return false;
        }
    }

    /** Récupère ou crée l'état du joueur */
    public State get(UUID playerId) {
        return states.computeIfAbsent(playerId, _ -> new State());
    }

}
