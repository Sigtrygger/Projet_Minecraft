package com.serveur.moba.commands;

import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.serveur.moba.game.enums.Team;

import com.serveur.moba.game.GameManager;
import com.serveur.moba.game.enums.Lane;

import java.util.*;

public class MobaCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final GameManager gm;

    private final com.serveur.moba.state.PlayerStateService playerState;
    private final com.serveur.moba.ability.AbilityRegistry abilities;

    // pour /moba setzone (positions temporaires par joueur)
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public MobaCommand(JavaPlugin plugin, GameManager gm,
            com.serveur.moba.state.PlayerStateService playerState,
            com.serveur.moba.ability.AbilityRegistry abilities) {
        this.plugin = plugin;
        this.gm = gm;
        this.playerState = playerState;
        this.abilities = abilities;
    }

    // ---------- ROUTER PRINCIPAL ----------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        try {
            switch (cmd) {
                case "moba" -> handleMoba(sender, args);
                case "forcepvp" -> handleForcePvp(sender, args);
                case "class" -> handleClass(sender, args);
                case "q", "w", "e", "r" -> handleSpell(sender, cmd); // q/w/e/r
                default -> sender.sendMessage("§cCommande inconnue.");
            }
        } catch (Exception ex) {
            sender.sendMessage("§cErreur: " + ex.getMessage());
            ex.printStackTrace();
        }
        return true;
    }

    // ---------- /moba <subcommand> ----------
    private void handleMoba(CommandSender sender, String[] args) {
        if (!checkPlayer(sender))
            return;
        if (!sender.hasPermission("moba.admin")) {
            sender.sendMessage("§cPermission manquante.");
            return;
        }
        Player p = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /moba <start|stop|setzone|setnexus|reload>");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                gm.start();
                sender.sendMessage("§aPartie lancée.");
            }
            case "stop" -> {
                gm.stop();
                sender.sendMessage("§cPartie arrêtée.");
            }
            case "reload" -> {
                plugin.reloadConfig();
                gm.reloadFromConfig(plugin.getConfig());
                sender.sendMessage("§aConfig rechargée.");
            }
            case "setnexus" -> {
                if (args.length < 2) {
                    sender.sendMessage("§e/moba setnexus <blue|red>");
                    return;
                }
                Team t = args[1].equalsIgnoreCase("blue") ? Team.BLUE : Team.RED;
                Location l = p.getLocation().clone();
                int hpDefault = plugin.getConfig().getInt("nexus.default_hp", 5000);

                // on garde la position dans config.yml
                String base = "nexus." + (t == Team.BLUE ? "blue" : "red");
                plugin.getConfig().set(base + ".world", l.getWorld().getName());
                plugin.getConfig().set(base + ".x", l.getBlockX());
                plugin.getConfig().set(base + ".y", l.getBlockY());
                plugin.getConfig().set(base + ".z", l.getBlockZ());
                plugin.getConfig().set(base + ".hp", plugin.getConfig().getInt(base + ".hp", hpDefault));
                plugin.saveConfig();

                // plus tard, quand on implémentera le Nexus runtime, on ajoutera des setters
                // dans GameManager
                sender.sendMessage("§aNexus " + (t == Team.BLUE ? "blue" : "red") + " positionné en " + fmt(l));

                sender.sendMessage("§aNexus " + t + " placé en " + fmt(l));
            }

            case "setzone" -> {
                if (args.length < 2) {
                    sender.sendMessage("§e/moba setzone <name>  (clic gauche=définit pos1, clic droit=pos2)");
                    return;
                }
                // dans handleMoba -> "setzone"
                String name = args[1].toLowerCase();
                if (!List.of("top", "mid", "bot").contains(name)) {
                    sender.sendMessage("§cNom invalide. Utilise: top, mid, bot");
                    return;
                }

                Location a = pos1.get(p.getUniqueId());
                Location b = pos2.get(p.getUniqueId());
                if (a == null || b == null) {
                    sender.sendMessage("§eClique gauche = pos1, clique droit = pos2, puis /moba setzone " + name);
                    return;
                }
                var c = new com.serveur.moba.game.Cuboid(
                        a.getWorld().getName(),
                        a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                        b.getBlockX(), b.getBlockY(), b.getBlockZ());
                gm.setZone(name, c); // façade en mémoire

                // (optionnel) et si tu veux PERSISTer aussi :
                String base = "zones." + name;
                plugin.getConfig().set(base + ".world", a.getWorld().getName());
                plugin.getConfig().set(base + ".x1", a.getBlockX());
                plugin.getConfig().set(base + ".y1", a.getBlockY());
                plugin.getConfig().set(base + ".z1", a.getBlockZ());
                plugin.getConfig().set(base + ".x2", b.getBlockX());
                plugin.getConfig().set(base + ".y2", b.getBlockY());
                plugin.getConfig().set(base + ".z2", b.getBlockZ());
                plugin.saveConfig();
                // puis recharger les structures si besoin :
                gm.reloadFromConfig(plugin.getConfig());

                sender.sendMessage("§aZone '" + name + "' enregistrée.");
            }
            default -> sender.sendMessage("§eSous-commande inconnue.");
        }
    }

    // ---------- /forcepvp <top|mid|bot> <on|off> ----------
    private void handleForcePvp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("moba.admin")) {
            sender.sendMessage("§cPermission manquante.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§eUsage: /forcepvp <top|mid|bot> <on|off>");
            return;
        }

        String laneName = args[0].toLowerCase(Locale.ROOT);
        if (!List.of("top", "mid", "bot").contains(laneName)) {
            sender.sendMessage("§cLane invalide. Utilise: top, mid, bot");
            return;
        }
        boolean on = args[1].equalsIgnoreCase("on");
        boolean ok = gm.forcePvp(laneName, on);
        if (!ok) {
            sender.sendMessage("§cLane inconnue.");
            return;
        }
        sender.sendMessage("§aPvP " + (on ? "activé" : "désactivé") + " sur " + laneName + ".");

    }

    // ---------- /class <tank|bruiser|adc> ----------
    private void handleClass(CommandSender sender, String[] args) {
        if (!checkPlayer(sender))
            return;
        Player p = (Player) sender;

        if (args.length != 1) {
            p.sendMessage("§eUsage: /class <tank|bruiser|adc>");
            return;
        }
        String choice = args[0].toLowerCase(Locale.ROOT);

        boolean ok = playerState.setClass(p.getUniqueId(), choice);
        if (ok) {
            p.sendMessage("§aClasse définie à " + choice);
        } else {
            p.sendMessage("§cClasse inconnue (choix possibles: tank, bruiser, adc)");
        }
    }

    // ---------- /q /w /e /r ----------
    private void handleSpell(CommandSender sender, String key) {
        if (!checkPlayer(sender))
            return;
        Player p = (Player) sender;

        var state = playerState.get(p.getUniqueId());
        if (state == null) {
            p.sendMessage("§cAucune classe sélectionnée. Utilise §e/class <tank|bruiser|adc>");
            return;
        }

        com.serveur.moba.ability.AbilityKey abilityKey = switch (key.toLowerCase(Locale.ROOT)) {
            case "q" -> com.serveur.moba.ability.AbilityKey.Q;
            case "w" -> com.serveur.moba.ability.AbilityKey.W;
            case "e" -> com.serveur.moba.ability.AbilityKey.E;
            default -> com.serveur.moba.ability.AbilityKey.R;
        };

        var ability = abilities.get(state.role, abilityKey);
        if (ability == null) {
            p.sendMessage("§cAucune compétence assignée à cette touche pour ta classe.");
            return;
        }

        boolean ok = ability.cast(new com.serveur.moba.ability.AbilityContext(plugin, p));
        if (!ok)
            p.sendMessage("§cImpossible de lancer le sort.");
    }

    // ---------- Tab-complete ----------
    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        String cmd = c.getName().toLowerCase(Locale.ROOT);

        if (cmd.equals("moba")) {
            if (a.length == 1)
                return startsWith(a[0], List.of("start", "stop", "setzone", "setnexus", "reload"));
            if (a.length == 2) {
                if (a[0].equalsIgnoreCase("setnexus"))
                    return startsWith(a[1], List.of("blue", "red"));
                if (a[0].equalsIgnoreCase("setzone"))
                    return List.of("<name>");
            }
            return List.of();
        }
        if (cmd.equals("forcepvp")) {
            if (a.length == 1)
                return startsWith(a[0], List.of("top", "mid", "bot"));
            if (a.length == 2)
                return startsWith(a[1], List.of("on", "off"));
            return List.of();
        }
        if (cmd.equals("class")) {
            if (a.length == 1)
                return startsWith(a[0], List.of("tank", "bruiser", "adc"));
            return List.of();
        }
        // q/w/e/r : pas de complétion
        return List.of();
    }

    // ---------- Utilitaires ----------
    public void setPos1(UUID id, Location l) {
        pos1.put(id, l);
    }

    public void setPos2(UUID id, Location l) {
        pos2.put(id, l);
    }

    private boolean checkPlayer(CommandSender s) {
        if (!(s instanceof Player)) {
            s.sendMessage("§cCommande uniquement en jeu.");
            return false;
        }
        return true;
    }

    private static String fmt(Location l) {
        return String.format("%s %d %d %d", l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    private static List<String> startsWith(String prefix, List<String> opts) {
        if (prefix == null || prefix.isEmpty())
            return opts;
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : opts)
            if (o.toLowerCase(Locale.ROOT).startsWith(p))
                out.add(o);
        return out;
    }
}
