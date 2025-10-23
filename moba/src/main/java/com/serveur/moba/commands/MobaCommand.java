package com.serveur.moba.commands;

import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import com.serveur.moba.game.GameManager;
import com.serveur.moba.game.enums.Lane;

import java.util.*;

public class MobaCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final GameManager gm;

    // pour /moba setzone (positions temporaires par joueur)
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public MobaCommand(JavaPlugin plugin, GameManager gm) {
        this.plugin = plugin;
        this.gm = gm;
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
                gm.loadFromConfig(plugin.getConfig());
                sender.sendMessage("§aConfig rechargée.");
            }
            case "setnexus" -> {
                if (args.length < 2) {
                    sender.sendMessage("§e/moba setnexus <blue|red>");
                    return;
                }
                com.serveur.moba.game.enums.Team t = args[1].equalsIgnoreCase("blue") ? com.serveur.moba.game.enums.Team.BLUE : com.serveur.moba.game.enums.Team.RED;
                Location l = p.getLocation().clone();
                int hp = plugin.getConfig().getInt("nexus.default_hp", 5000);
                if (t.equals("BLUE"))
                    gm.nexusBlue = new com.serveur.moba.game.Nexus(t, l, hp);
                else
                    gm.nexusRed = new com.serveur.moba.game.Nexus(t, l, hp);
                sender.sendMessage("§aNexus " + t + " placé en " + fmt(l));
            }
            case "setzone" -> {
                if (args.length < 2) {
                    sender.sendMessage("§e/moba setzone <name>  (clic gauche=définit pos1, clic droit=pos2)");
                    return;
                }
                String name = args[1];
                Location a = pos1.get(p.getUniqueId());
                Location b = pos2.get(p.getUniqueId());
                if (a == null || b == null) {
                    sender.sendMessage("§eClique gauche = pos1, clique droit = pos2, puis /moba setzone " + name);
                    return;
                }
                var c = new com.serveur.moba.game.Cuboid(a.getWorld().getName(),
                        a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                        b.getBlockX(), b.getBlockY(), b.getBlockZ());
                gm.zoneManager.setZone(name, c);
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

        Lane lane = switch (args[0].toLowerCase(Locale.ROOT)) {
            case "top" -> Lane.TOP;
            case "mid" -> Lane.MID;
            case "bot" -> Lane.BOT;
            default -> null;
        };
        if (lane == null) {
            sender.sendMessage("§cLane invalide.");
            return;
        }

        boolean on = args[1].equalsIgnoreCase("on");
        gm.pvpGate.set(lane, on);
        sender.sendMessage(
                "§aPvP " + (on ? "activé" : "désactivé") + " sur " + lane.name().toLowerCase(Locale.ROOT) + ".");
    }

    // ---------- /class <tank|bruiser|adc> ----------
    private void handleClass(CommandSender sender, String[] args) {
        if (!checkPlayer(sender))
            return;
        Player p = (Player) sender;
        if (args.length < 1) {
            p.sendMessage("§eUsage: /class <tank|bruiser|adc>");
            return;
        }
        String cls = args[0].toLowerCase(Locale.ROOT);
        // TODO: branche ton système de classes ici
        p.sendMessage("§aClasse sélectionnée: " + cls);
    }

    // ---------- /q /w /e /r ----------
    private void handleSpell(CommandSender sender, String key) {
        if (!checkPlayer(sender))
            return;
        Player p = (Player) sender;
        // TODO: vérifie la classe du joueur et résous le sort associé à key
        p.sendMessage("§bLancement du sort " + key.toUpperCase(Locale.ROOT) + " !");
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
