package com.serveur.moba.game;

import com.serveur.moba.lane.LaneManager;
import com.serveur.moba.game.enums.Lane;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class GameManager {
    private final JavaPlugin plugin;
    private final LaneManager lane;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lane = new LaneManager(plugin);
    }

    // --- Façade vers les fonctionnalités actuelles ---
    public void start() {
        lane.startScheduler();
    }

    public void stop() {
        lane.stopScheduler();
    }

    public void reloadFromConfig(FileConfiguration cfg) {
        lane.reloadFromConfig(cfg);
    }

    public boolean forcePvp(String laneName, boolean on) {
        return lane.forcePvp(laneName, on);
    }

    // pour tes listeners (ex: PvpGuardListener)
    public LaneManager lane() {
        return lane;
    }

    // si tu veux enregistrer une zone depuis /moba setzone (facultatif)
    public void setZone(String name, com.serveur.moba.game.Cuboid c) {
        lane.setZone(name, c);
    }
}
