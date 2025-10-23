package com.serveur.moba;

import com.serveur.moba.ability.AbilityKey;
import com.serveur.moba.lane.LaneManager;
import com.serveur.moba.listeners.PvpGuardListener;
import com.serveur.moba.state.PlayerStateService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationType;
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

        private com.serveur.moba.ability.AbilityRegistry abilities;
        private com.serveur.moba.ability.CooldownService cooldowns;
        private com.serveur.moba.combat.CombatTagService combat;
        private com.serveur.moba.util.Flags globalFlags;

        private final java.util.Map<java.util.UUID, org.bukkit.Location> pos1 = new java.util.HashMap<>();
        private final java.util.Map<java.util.UUID, org.bukkit.Location> pos2 = new java.util.HashMap<>();

        public void setPos1(java.util.UUID id, org.bukkit.Location l) { pos1.put(id, l); }
        public void setPos2(java.util.UUID id, org.bukkit.Location l) { pos2.put(id, l); }
        public org.bukkit.Location getPos1(java.util.UUID id) { return pos1.get(id); }
        public org.bukkit.Location getPos2(java.util.UUID id) { return pos2.get(id); }


        @Override
        public void onEnable() {

                saveDefaultConfig();

                // On instancie les services
                this.playerState = new PlayerStateService(this);
                this.laneManager = new LaneManager(this);
                // champs
                this.cooldowns = new com.serveur.moba.ability.CooldownService();
                this.combat = new com.serveur.moba.combat.CombatTagService(3000L);
                this.globalFlags = new com.serveur.moba.util.Flags();
                this.abilities = new com.serveur.moba.ability.AbilityRegistry();

                // === Tank abilities ===
                getServer().getPluginManager()
                                .registerEvents(new com.serveur.moba.classes.tank.TankPassiveListener(4000L, combat),
                                                this);
                abilities.register(PlayerStateService.Role.TANK, com.serveur.moba.ability.AbilityKey.W,
                                new com.serveur.moba.classes.tank.TankWAbsorb(cooldowns, 4.0, 12000L));
                abilities.register(PlayerStateService.Role.TANK, com.serveur.moba.ability.AbilityKey.E,
                                new com.serveur.moba.classes.tank.TankEDash(cooldowns, globalFlags, 4.0, 500L, 8000L));
                abilities.register(PlayerStateService.Role.TANK, com.serveur.moba.ability.AbilityKey.R,
                                new com.serveur.moba.classes.tank.TankRSlowAoE(cooldowns, 2, 3, 6.0, 20000L));
                var tankQ = new com.serveur.moba.classes.tank.TankQEmpowered(cooldowns, 10000L, 6000L);
                abilities.register(PlayerStateService.Role.TANK, AbilityKey.Q, tankQ);
                getServer().getPluginManager().registerEvents(new com.serveur.moba.classes.tank.TankQListener(tankQ),
                                this);

                // === Fin Tank abilities ===

                // === Bruiser abilities ===
                getServer().getPluginManager()
                                .registerEvents(new com.serveur.moba.classes.bruiser.BruiserPassiveListener(combat),
                                                this);
                abilities.register(PlayerStateService.Role.BRUISER, AbilityKey.Q,
                                new com.serveur.moba.classes.bruiser.BruiserQTripleDash(cooldowns, 3.0, 6000L, 9000L));
                abilities.register(PlayerStateService.Role.BRUISER, AbilityKey.W,
                                new com.serveur.moba.classes.bruiser.BruiserWSlowAoE(cooldowns, 2, 3, 4.0, 10000L));
                abilities.register(PlayerStateService.Role.BRUISER, AbilityKey.E,
                                new com.serveur.moba.classes.bruiser.BruiserEDashAbsorb(cooldowns, 6.0, 8000L));
                abilities.register(PlayerStateService.Role.BRUISER, AbilityKey.R,
                                new com.serveur.moba.classes.bruiser.BruiserRToggleSmash(cooldowns, 8000L, 25000L, 6.0,
                                                70.0));

                // === Fin Bruiser abilities ===

                // === Adc abilities ===
                getServer().getPluginManager().registerEvents(
                                new com.serveur.moba.classes.adc.AdcPassiveListener(playerState),
                                this);
                abilities.register(PlayerStateService.Role.ADC, AbilityKey.Q,
                                new com.serveur.moba.classes.adc.AdcQAttackSpeed(cooldowns, 9000L, 6000L, 2.0));
                abilities.register(PlayerStateService.Role.ADC, AbilityKey.W,
                                new com.serveur.moba.classes.adc.AdcWShield(cooldowns, globalFlags, 12000L, 2000L));
                abilities.register(PlayerStateService.Role.ADC, AbilityKey.E,
                                new com.serveur.moba.classes.adc.AdcESlowZone(cooldowns, 2, 6.0, 6000L, 14000L));
                abilities.register(PlayerStateService.Role.ADC, AbilityKey.R,
                                new com.serveur.moba.classes.adc.AdcRAllSteroid(cooldowns, 25000L, 8000L, 3.0, 3.0));

                // === Fin Adc abilities ===

                getLogger().info("Abilities init OK.");

                // On enregistre le listener d'annulation des dégâts
                Bukkit.getPluginManager().registerEvents((@NotNull Listener) new PvpGuardListener(laneManager), this);
                getServer().getPluginManager()
                                .registerEvents(new com.serveur.moba.util.ProtectionListeners(globalFlags), this);

                getServer().getPluginManager().registerEvents(new com.serveur.moba.listeners.WandListener(this), this);


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
                        sender.sendMessage(
                                        ok ? "§aPvP " + (on ? "ON" : "OFF") + " pour " + args[0] : "§cLane inconnue.");
                        return true;
                }
                if (command.getName().equalsIgnoreCase("q")
                                || command.getName().equalsIgnoreCase("w")
                                || command.getName().equalsIgnoreCase("e")
                                || command.getName().equalsIgnoreCase("r")) {
                        if (!(sender instanceof Player p)) {
                                sender.sendMessage("Joueur requis.");
                                return true;
                        }
                        var s = playerState.get(p.getUniqueId());
                        com.serveur.moba.ability.AbilityKey key = switch (command.getName().toLowerCase()) {
                                case "q" -> com.serveur.moba.ability.AbilityKey.Q;
                                case "w" -> com.serveur.moba.ability.AbilityKey.W;
                                case "e" -> com.serveur.moba.ability.AbilityKey.E;
                                default -> com.serveur.moba.ability.AbilityKey.R;
                        };
                        var ab = abilities.get(s.role, key);
                        if (ab == null) {
                                p.sendMessage("§cAucune compétence assignée.");
                                return true;
                        }
                        boolean ok = ab.cast(new com.serveur.moba.ability.AbilityContext(this, p));
                        if (!ok)
                                p.sendMessage("§cImpossible de lancer le sort.");
                        return true;
                }

                if (command.getName().equalsIgnoreCase("moba")) {
                if (!sender.hasPermission("moba.admin")) {
                        sender.sendMessage("§cPermission manquante: moba.admin");
                        return true;
                }
                if (!(sender instanceof Player p)) {
                        sender.sendMessage("§cCommande en jeu uniquement.");
                        return true;
                }
                if (args.length == 0) {
                        sender.sendMessage("§eUsage: /moba <start|stop|setzone|setnexus|reload>");
                        return true;
                }

                switch (args[0].toLowerCase()) {
                        case "start" -> {
                        laneManager.startScheduler();
                        sender.sendMessage("§aPartie lancée.");
                        }
                        case "stop" -> {
                        laneManager.stopScheduler(); // ajoute cette méthode si elle n'existe pas
                        sender.sendMessage("§cPartie arrêtée.");
                        }
                        case "reload" -> {
                        this.reloadConfig();
                        laneManager.reloadFromConfig(getConfig()); // ajoute cette méthode côté LaneManager
                        sender.sendMessage("§aConfig rechargée.");
                        }
                        case "setnexus" -> {
                        if (args.length < 2) {
                                sender.sendMessage("§e/moba setnexus <blue|red>");
                                return true;
                        }
                        var teamArg = args[1].toLowerCase();
                        var l = p.getLocation().clone();
                        // Écrit dans la config (simple et persistant)
                        String base = "nexus." + (teamArg.equals("blue") ? "blue" : "red");
                        getConfig().set(base + ".world", l.getWorld().getName());
                        getConfig().set(base + ".x", l.getBlockX());
                        getConfig().set(base + ".y", l.getBlockY());
                        getConfig().set(base + ".z", l.getBlockZ());
                        if (!getConfig().isSet(base + ".hp")) getConfig().set(base + ".hp", 5000);
                        saveConfig();
                        sender.sendMessage("§aNexus " + teamArg + " positionné en " +
                                l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ());
                        }
                        case "setzone" -> {
                        if (args.length < 2) {
                                sender.sendMessage("§e/moba setzone <name>  (clic gauche=pos1, clic droit=pos2)");
                                return true;
                        }
                        String name = args[1];
                        var a = getPos1(p.getUniqueId());
                        var b = getPos2(p.getUniqueId());
                        if (a == null || b == null) {
                                sender.sendMessage("§eDéfinis d'abord pos1 et pos2 (clic gauche/droit sur un bloc).");
                                return true;
                        }
                        String base = "zones." + name;
                        getConfig().set(base + ".world", a.getWorld().getName());
                        getConfig().set(base + ".x1", a.getBlockX());
                        getConfig().set(base + ".y1", a.getBlockY());
                        getConfig().set(base + ".z1", a.getBlockZ());
                        getConfig().set(base + ".x2", b.getBlockX());
                        getConfig().set(base + ".y2", b.getBlockY());
                        getConfig().set(base + ".z2", b.getBlockZ());
                        saveConfig();

                        // Si ton LaneManager sait consommer les zones à chaud, recharge-le :
                        laneManager.reloadFromConfig(getConfig());

                        sender.sendMessage("§aZone '" + name + "' enregistrée.");
                        }
                        default -> sender.sendMessage("§eSous-commande inconnue.");
                }
                return true;
                }


                return false;
        }
}
