package com.serveur.moba.lane;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/*
 * Gestion des lanes (top, mid, bot)
 * - Chargement des régions depuis le fichier config.yml
 * - Ouverture/Fermeture des fenêtres PvP par scheduler
 * - Méthodes utilitaires pour savoir si un joueur est dans une lane, etc...
 */

public class LaneManager {

    /**
     * Représentation d'un cuboïde (simplement) avec des bornes inclusives dans un
     * monde
     */
    public record Cuboid(World w, int x1, int y1, int z1, int x2, int y2, int z2) {
        public boolean contains(Location loc) {
            if (!loc.getWorld().equals(w))
                return false;
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            return x >= Math.min(x1, x2) && x <= Math.max(x1, x2)
                    && y >= Math.min(y1, y2) && y <= Math.max(y1, y2)
                    && z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
        }
    }

    private final Plugin plugin;

    // Nom de la lane
    private final Map<String, Cuboid> lanesRegions = new HashMap<>();
    // Etat PvP par lane (true = ouvert, false = fermé)
    private final Map<String, Boolean> lanesPvpState = new ConcurrentHashMap<>();
    // Tâches de planification pour chaque lane pour les annuler si besoin
    private final List<BukkitTask> scheduledTasks = new ArrayList<>();

    public LaneManager(Plugin plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    /** Charge les régions des lanes depuis le fichier config.yml (section lanes) */
    private void loadFromConfig() {
        ConfigurationSection lanes = plugin.getConfig().getConfigurationSection("lanes");
        for (String lane : lanes.getKeys(false)) {
            String worldName = lanes.getString(lane + ".world");
            World w = Bukkit.getWorld(worldName);
            List<Integer> min = lanes.getIntegerList(lane + ".min");
            List<Integer> max = lanes.getIntegerList(lane + ".max");
            lanesRegions.put(lane.toLowerCase(),
                    new Cuboid(w, min.get(0), min.get(1), min.get(2), max.get(0), max.get(1), max.get(2)));
            lanesPvpState.put(lane.toLowerCase(), false); // PvP fermé par défaut
        }
    }

    /** Démarre le scheduler pour ouvrir/fermer les lanes périodiquement */
    public void startScheduler() {
        int startDelay = plugin.getConfig().getInt("timers.game_start_delay_sec", 10);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ConfigurationSection lanes = plugin.getConfig().getConfigurationSection("lanes");
            for (String lane : lanes.getKeys(false)) {
                var sec = lanes.getConfigurationSection(lane);
                var windows = sec.getMapList("pvp_windows");
                for (Map<?, ?> w : windows) {
                    int start = ((Number) w.get("start_sec")).intValue();
                    int duration = ((Number) w.get("duration_sec")).intValue();
                    // Ouvre la fenêtre PvP
                    scheduledTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        setPvp(lane, true);
                        plugin.getLogger().info("Lane " + lane + " PvP OPEN");
                    }, startDelay + start * 20L));

                    // Ferme la fenêtre PvP
                    scheduledTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        setPvp(lane, false);
                        plugin.getLogger().info("Lane " + lane + " PvP CLOSE");
                    }, startDelay + (start + duration) * 20L));

                }
            }
        }, startDelay * 20L);
    }

    /** Change l'état de PvP et fait une annonce globale */
    @SuppressWarnings("deprecation")
    private void setPvp(String lane, boolean open) {
        lane = lane.toLowerCase();
        if (lanesPvpState.containsKey(lane)) {
            lanesPvpState.put(lane.toLowerCase(), open);
            String msg = open ? "ouverte" : "fermée";
            Bukkit.broadcastMessage(
                    "§6[Annonce] §eLa fenêtre PvP de la lane §a" + lane + " §eest maintenant §a" + msg + "§e !");
        }
    }

    /** Commande admin pour forcer l'état du pvp d'une lane */
    public boolean forcePvp(String lane, boolean open) {
        if (!lanesRegions.containsKey(lane.toLowerCase())) {
            return false;
        }
        setPvp(lane, open);
        return true;
    }

    /** Retourne la lane dans laquelle se trouve un joueur, si trouvé. */
    public Optional<String> laneOf(org.bukkit.entity.Player p) {
        return lanesRegions.entrySet().stream()
                .filter(e -> e.getValue().contains(p.getLocation()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /** Indique si le PvP est ouvert dans la lane donnée */
    public boolean isPvpOpen(String lane) {
        return lanesPvpState.getOrDefault(lane.toLowerCase(), false);
    }

    public void stopScheduler() {
    // TODO: annuler le/les BukkitTask(s) lancés dans startScheduler()
}

public void reloadFromConfig(org.bukkit.configuration.file.FileConfiguration cfg) {
    // TODO: relire les fenêtres PvP et zones depuis cfg
    // puis éventuellement redémarrer le scheduler
}

}
