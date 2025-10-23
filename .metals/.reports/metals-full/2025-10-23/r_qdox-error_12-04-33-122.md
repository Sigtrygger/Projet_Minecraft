error id: file:///C:/Users/gwend/Documents/Projet_Minecraft/moba/src/main/java/com/serveur/moba/lane/LaneManager.java
file:///C:/Users/gwend/Documents/Projet_Minecraft/moba/src/main/java/com/serveur/moba/lane/LaneManager.java
### com.thoughtworks.qdox.parser.ParseException: syntax error @[130,1]

error in qdox parser
file content:
```java
offset: 5455
uri: file:///C:/Users/gwend/Documents/Projet_Minecraft/moba/src/main/java/com/serveur/moba/lane/LaneManager.java
text:
```scala
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

// com.serveur.moba.game.GameManager
p@@ackage com.serveur.moba.game
;

import com.serveur.moba.lane.LaneManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class GameManager {
    private final JavaPlugin plugin;
    private final LaneManager lane;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lane = new LaneManager(plugin); // ⬅️ instancié ici
    }

    // Expose le LM si besoin ponctuel
    public LaneManager lane() {
        return lane;
    }

    // Façade simple pour MobaCommand
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

    // plus tard: nexus, scoreboard, etc.
}

    public void reloadFromConfig(org.bukkit.configuration.file.FileConfiguration cfg) {
        // TODO: relire les fenêtres PvP et zones depuis cfg
        // puis éventuellement redémarrer le scheduler
    }

}

```

```



#### Error stacktrace:

```
com.thoughtworks.qdox.parser.impl.Parser.yyerror(Parser.java:2025)
	com.thoughtworks.qdox.parser.impl.Parser.yyparse(Parser.java:2147)
	com.thoughtworks.qdox.parser.impl.Parser.parse(Parser.java:2006)
	com.thoughtworks.qdox.library.SourceLibrary.parse(SourceLibrary.java:232)
	com.thoughtworks.qdox.library.SourceLibrary.parse(SourceLibrary.java:190)
	com.thoughtworks.qdox.library.SourceLibrary.addSource(SourceLibrary.java:94)
	com.thoughtworks.qdox.library.SourceLibrary.addSource(SourceLibrary.java:89)
	com.thoughtworks.qdox.library.SortedClassLibraryBuilder.addSource(SortedClassLibraryBuilder.java:162)
	com.thoughtworks.qdox.JavaProjectBuilder.addSource(JavaProjectBuilder.java:174)
	scala.meta.internal.mtags.JavaMtags.indexRoot(JavaMtags.scala:49)
	scala.meta.internal.metals.SemanticdbDefinition$.foreachWithReturnMtags(SemanticdbDefinition.scala:99)
	scala.meta.internal.metals.Indexer.indexSourceFile(Indexer.scala:546)
	scala.meta.internal.metals.Indexer.$anonfun$reindexWorkspaceSources$3(Indexer.scala:677)
	scala.meta.internal.metals.Indexer.$anonfun$reindexWorkspaceSources$3$adapted(Indexer.scala:674)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:630)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:628)
	scala.collection.AbstractIterator.foreach(Iterator.scala:1313)
	scala.meta.internal.metals.Indexer.reindexWorkspaceSources(Indexer.scala:674)
	scala.meta.internal.metals.MetalsLspService.$anonfun$onChange$2(MetalsLspService.scala:918)
	scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
	scala.concurrent.Future$.$anonfun$apply$1(Future.scala:691)
	scala.concurrent.impl.Promise$Transformation.run(Promise.scala:500)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	java.base/java.lang.Thread.run(Thread.java:1575)
```
#### Short summary: 

QDox parse error in file:///C:/Users/gwend/Documents/Projet_Minecraft/moba/src/main/java/com/serveur/moba/lane/LaneManager.java