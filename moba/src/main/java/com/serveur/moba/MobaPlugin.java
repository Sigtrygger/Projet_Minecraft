package com.serveur.moba;

import com.serveur.moba.lane.LaneManager;
import com.serveur.moba.pvp.PvpGuardListener;
import com.serveur.moba.state.PlayerStateService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Point d'entrée du plugin :
 * - charge la config
 * - instancie les services
 * - enregistre le listener PvP intra-lane
 * - expose 2 commandes : /class et /forcepvp pour l'instant
 */

public final class MobaPlugin extends JavaPlugin {

    private LaneManager laneManager;
    private PlayerStateService playerState;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        // On instancie les services
        this.playerState = new PlayerStateService(this);
        this.laneManager = new LaneManager(this);

        // On enregistre le listener d'annulation des dégâts
        Bukkit.getPluginManager().registerEvents((@NotNull Listener) new PvpGuardListener(laneManager), this);

        // Programme les fenêtres PvP depuis la config
        laneManager.startScheduler();
        getLogger().info("Moba enabled!");

    }

    @Override
    public void onDisable() {
        getLogger().info("Moba disabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        // commande /class <tank|bruiser|adc>
        if (command.getName().equalsIgnoreCase("class")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cSeulement les joueurs in-game peuvent exécuter cette commande");
                return true;
            }
            if (args.length != 1) {
                p.sendMessage("§cUsage: /class <tank|bruiser|adc>");
                return true;
            }
            boolean ok = playerState.setClass(p.getUniqueId(), args[0]);
            p.sendMessage(ok ? "§aClasse définie à " + args[0]
                    : "§cClasse inconnue (choix possibles: tank, bruiser, adc)");
            return true;
        }

        // commande /forcepvp <lane> <on|off>
        if (command.getName().equalsIgnoreCase("forcepvp")) {
            if (!sender.hasPermission("moba.admin")) {
                sender.sendMessage("§cPermission manquante: moba.admin");
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /forcepvp <lane> <on|off>");
                return true;
            }
            boolean on = args[1].equalsIgnoreCase("on");
            boolean ok = laneManager.forcePvp(args[0], on);
            sender.sendMessage(ok ? "§aPvP " + (on ? "ON" : "OFF") + " pour " + args[0] : "§cLane inconnue.");
            return true;
        }
        return false;
    }
}
