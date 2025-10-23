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

public class LaneManager {

    /** Cuboïde avec bornes inclusives */
    public record Cuboid(World w, int x1, int y1, int z1, int x2, int y2, int z2) {
        public boolean contains(Location loc) {
            if (loc == null || loc.getWorld() == null || !loc.getWorld().equals(w))
                return false;
            int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
            return x >= Math.min(x1, x2) && x <= Math.max(x1, x2)
                    && y >= Math.min(y1, y2) && y <= Math.max(y1, y2)
                    && z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
        }
    }

    private final Plugin plugin;

    // lanes connues (ex: "top", "mid", "bot") -> région
    private final Map<String, Cuboid> lanesRegions = new HashMap<>();
    // état PvP par lane
    private final Map<String, Boolean> lanesPvpState = new ConcurrentHashMap<>();
    // toutes les tâches planifiées (on pourra TOUT annuler)
    private final List<BukkitTask> scheduledTasks = new ArrayList<>();

    public LaneManager(Plugin plugin) {
        this.plugin = plugin;
        reloadFromConfig(plugin.getConfig()); // chargement initial
    }

    /**
     * Démarre la programmation des fenêtres PvP, en se basant sur la conf
     * "lanes.*.pvp_windows"
     */
    public void startScheduler() {
        stopScheduler(); // annule tout ce qui aurait pu être programmé avant
        int startDelay = plugin.getConfig().getInt("timers.game_start_delay_sec", 10);

        BukkitTask startAll = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ConfigurationSection lanesSec = plugin.getConfig().getConfigurationSection("lanes");
            if (lanesSec == null)
                return;

            for (String lane : lanesSec.getKeys(false)) {
                ConfigurationSection sec = lanesSec.getConfigurationSection(lane);
                if (sec == null)
                    continue;

                List<Map<?, ?>> windows = sec.getMapList("pvp_windows");
                if (windows == null)
                    continue; // par sécurité (getMapList renvoie souvent [] plutôt que null)

                for (Map<?, ?> w : windows) {
                    int start = mapInt(w, "start_sec", 0);
                    int duration = mapInt(w, "duration_sec", 0);

                    // OUVERTURE
                    scheduledTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        setPvp(lane, true);
                        plugin.getLogger().info("[MOBA] Lane " + lane + " PvP OPEN");
                    }, start * 20L));

                    // FERMETURE
                    scheduledTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        setPvp(lane, false);
                        plugin.getLogger().info("[MOBA] Lane " + lane + " PvP CLOSE");
                    }, (start + duration) * 20L));
                }
            }
        }, startDelay * 20L);

        scheduledTasks.add(startAll);
    }

    /** Annule TOUTES les tâches */
    public void stopScheduler() {
        for (BukkitTask t : scheduledTasks) {
            try {
                t.cancel();
            } catch (Exception ignored) {
            }
        }
        scheduledTasks.clear();
    }

    /** Forcer PvP ON/OFF via commande */
    public boolean forcePvp(String lane, boolean open) {
        String key = safeKey(lane);
        if (!lanesRegions.containsKey(key))
            return false;
        setPvp(key, open);
        return true;
    }

    /** Lane du joueur si dans une région */
    public Optional<String> laneOf(org.bukkit.entity.Player p) {
        if (p == null)
            return Optional.empty();
        Location L = p.getLocation();
        for (var e : lanesRegions.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(L))
                return Optional.of(e.getKey());
        }
        return Optional.empty();
    }

    public boolean isPvpOpen(String lane) {
        return lanesPvpState.getOrDefault(safeKey(lane), false);
    }

    /** Recharger TOUT (lanes + zones admin si présentes) depuis la conf */
    public void reloadFromConfig(FileConfiguration cfg) {
        lanesRegions.clear();
        lanesPvpState.clear();

        loadLanesFromConfig(cfg); // section "lanes" (ton format d'origine)
        loadZonesFromConfig(cfg); // section "zones" (posée par /moba setzone)

        // par défaut PvP fermé
        for (String k : lanesRegions.keySet()) {
            lanesPvpState.putIfAbsent(k, false);
        }
    }

    /** Lecture format d'origine : lanes.<lane>.{world,min[3],max[3]} */
    private void loadLanesFromConfig(FileConfiguration cfg) {
        ConfigurationSection lanes = cfg.getConfigurationSection("lanes");
        if (lanes == null)
            return;

        for (String lane : lanes.getKeys(false)) {
            String base = "lanes." + lane;
            String worldName = cfg.getString(base + ".world");
            List<Integer> min = cfg.getIntegerList(base + ".min");
            List<Integer> max = cfg.getIntegerList(base + ".max");

            World w = worldOrNull(worldName);
            if (w == null || min.size() < 3 || max.size() < 3) {
                plugin.getLogger()
                        .warning("[MOBA] Lane " + lane + " ignorée (monde introuvable ou min/max invalides).");
                continue;
            }
            Cuboid c = new Cuboid(w, min.get(0), min.get(1), min.get(2), max.get(0), max.get(1), max.get(2));
            lanesRegions.put(safeKey(lane), c);
        }
        plugin.getLogger().info("[MOBA] Lanes chargées: " + lanesRegions.keySet());
    }

    /**
     * Lecture "zones" posées par /moba setzone (écrase si le nom correspond à une
     * lane)
     */
    private void loadZonesFromConfig(FileConfiguration cfg) {
        ConfigurationSection root = cfg.getConfigurationSection("zones");
        if (root == null)
            return;

        for (String name : root.getKeys(false)) {
            String base = "zones." + name;
            String worldName = cfg.getString(base + ".world");
            World w = worldOrNull(worldName);
            int x1 = cfg.getInt(base + ".x1");
            int y1 = cfg.getInt(base + ".y1");
            int z1 = cfg.getInt(base + ".z1");
            int x2 = cfg.getInt(base + ".x2");
            int y2 = cfg.getInt(base + ".y2");
            int z2 = cfg.getInt(base + ".z2");
            if (w == null) {
                plugin.getLogger().warning("[MOBA] Zone " + name + " ignorée (monde introuvable).");
                continue;
            }
            // Si le nom correspond à une lane ("top","mid","bot"), on l'utilise directement
            lanesRegions.put(safeKey(name), new Cuboid(w, x1, y1, z1, x2, y2, z2));
        }
        plugin.getLogger().info("[MOBA] Zones chargées/écrasées: " + root.getKeys(false));
    }

    /** Injection à chaud depuis la commande (optionnel) */
    public void setZone(String name, com.serveur.moba.game.Cuboid c) {
        World w = worldOrNull(c.world());
        if (w == null) {
            plugin.getLogger().warning("[MOBA] setZone ignoré: monde " + c.world() + " introuvable.");
            return;
        }
        lanesRegions.put(safeKey(name), new Cuboid(w, c.x1(), c.y1(), c.z1(), c.x2(), c.y2(), c.z2()));
    }

    private static String safeKey(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static World worldOrNull(String name) {
        if (name == null)
            return null;
        return Bukkit.getWorld(name);
    }

    /** Annonce + MAJ d'état */
    private void setPvp(String lane, boolean open) {
        String key = safeKey(lane);
        if (!lanesRegions.containsKey(key))
            return;
        lanesPvpState.put(key, open);
        String msg = open ? "ouverte" : "fermée";
        Bukkit.broadcastMessage(
                "§6[Annonce] §eLa fenêtre PvP de la lane §a" + key + " §eest maintenant §a" + msg + "§e !");
    }

    private static int mapInt(Map<?, ?> m, String key, int def) {
        Object v = m.get(key);
        return (v instanceof Number) ? ((Number) v).intValue() : def;
    }

}
