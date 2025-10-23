package com.serveur.moba.game;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.serveur.moba.game.enums.Lane;

public class GameManager {
    private final JavaPlugin plugin;
    public final ZoneManager zoneManager = new ZoneManager();
    public final PvpGate pvpGate = new PvpGate();
    public Nexus nexusBlue, nexusRed;
    private BukkitTask timerTask;
    private int seconds;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadFromConfig(FileConfiguration cfg) {
        // lire zones: zones.top_blue.world, x1..., etc.
        // lire nexus: nexus.blue.{world,x,y,z,hp}, idem red
    }

    public void start() {
        stop(); // reset si besoin
        seconds = 0;
        // exemple: ouvrir PvPvE à T=600s
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            seconds++;
            if (seconds == 600) { // ouverture PvPvE
                for (Lane l : Lane.values())
                    pvpGate.set(l, true);
                Bukkit.broadcastMessage("§aLes zones PvPvE sont ouvertes !");
            }
            // mettre à jour un scoreboard simple si tu veux
        }, 20L, 20L);
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        // reset états, fermer PvP :
        for (Lane l : Lane.values())
            pvpGate.set(l, false);
    }
}
