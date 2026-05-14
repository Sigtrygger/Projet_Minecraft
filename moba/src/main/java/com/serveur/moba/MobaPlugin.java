package com.serveur.moba;

import com.serveur.moba.ability.AbilityContext;
import com.serveur.moba.ability.AbilityKey;
import com.serveur.moba.ability.AbilityRegistry;
import com.serveur.moba.ability.CooldownIds;
import com.serveur.moba.ability.CooldownService;
import com.serveur.moba.classes.ClassService;
import com.serveur.moba.classes.adc.AdcPassiveListener;
import com.serveur.moba.classes.bruiser.BruiserPassiveListener;
import com.serveur.moba.classes.tank.TankPassiveListener;
import com.serveur.moba.combat.CombatTagService;
import com.serveur.moba.game.GameManager;
import com.serveur.moba.kit.KitService;
import com.serveur.moba.kit.SpellHotbar;
import com.serveur.moba.kit.SpellHotbar.SpellTag;
import com.serveur.moba.listeners.ClassItemLockListener;
import com.serveur.moba.listeners.HotbarSpellListener;
import com.serveur.moba.listeners.HungerGuardListener;
import com.serveur.moba.listeners.PvpGuardListener;
import com.serveur.moba.shop.ShopItemLockListener;
import com.serveur.moba.shop.ShopListeners;
import com.serveur.moba.shop.ShopService;
import com.serveur.moba.state.PlayerStateService;
import com.serveur.moba.state.PlayerStateService.Role;
import com.serveur.moba.team.ChatTeamListener;
import com.serveur.moba.team.TeamCommand;
import com.serveur.moba.team.TeamPvpListener;
import com.serveur.moba.team.TeamQuitListener;
import com.serveur.moba.team.TeamService;
import com.serveur.moba.util.Buffs;
import com.serveur.moba.util.CooldownBase;
import com.serveur.moba.util.Flags;
import com.serveur.moba.util.ProtectionListeners;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import com.serveur.moba.ui.ActionBarBus;
import com.serveur.moba.ui.CooldownHudService;
import com.serveur.moba.shop.ShopOpenListener;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Point d'entrée du plugin :
 * - charge la config
 * - instancie les services
 * - enregistre les listeners globaux
 * - expose les commandes /moba /forcepvp /class et les sorts (q/w/e/r)
 */
public final class MobaPlugin extends JavaPlugin implements Listener {

        // === Services & états centraux ===
        private PlayerStateService state; // état pur (classe/level/gold…)
        private GameManager gameManager;
        private AbilityRegistry abilities;
        private CooldownService cooldowns;
        private CombatTagService combat;
        private Flags globalFlags;
        private ClassService classService;
        private ActionBarBus actionBarBus;
        private KitService kitService;
        private NamespacedKey spellKey;
        private CooldownBase cooldownBase;
        private TeamService teamService;
        private ShopService shopService;

        // Zones (wand)
        private final Map<UUID, Location> pos1 = new HashMap<>();
        private final Map<UUID, Location> pos2 = new HashMap<>();

        public void setPos1(UUID id, Location l) {
                pos1.put(id, l);
        }

        public void setPos2(UUID id, Location l) {
                pos2.put(id, l);
        }

        public Location getPos1(UUID id) {
                return pos1.get(id);
        }

        public Location getPos2(UUID id) {
                return pos2.get(id);
        }

        @Override
        public void onEnable() {
                saveDefaultConfig();

                // === Instanciation des services (UNE SEULE FOIS) ===
                this.state = new PlayerStateService(); // service "pur état"
                this.combat = new CombatTagService(3000L); // tagging combat
                this.cooldowns = new CooldownService();
                this.globalFlags = new Flags();
                this.abilities = new AbilityRegistry();
                this.gameManager = new GameManager(this);
                this.actionBarBus = new ActionBarBus();
                this.kitService = new KitService(this);
                this.spellKey = new NamespacedKey(this, "spell_key");
                this.teamService = new TeamService();

                // === Listeners passifs globaux (role-guarded en interne) ===
                var adcListener = new AdcPassiveListener(state);
                var tankListener = new TankPassiveListener(6_000L, globalFlags, combat, this, state, teamService);
                var bruiserListener = new BruiserPassiveListener(combat, state, actionBarBus);

                var pm = getServer().getPluginManager();
                pm.registerEvents(adcListener, this);
                pm.registerEvents(tankListener, this);
                pm.registerEvents(bruiserListener, this);

                // === Service de changement de classe (disable old → enable new) ===
                this.classService = new ClassService(state, adcListener, kitService);

                // === Abilities (par rôle) ===
                // Tank
                var tankQ = new com.serveur.moba.classes.tank.TankQEmpowered(cooldowns, 10000L, 6000L);
                abilities.register(PlayerStateService.Role.TANK, AbilityKey.Q, tankQ);
                abilities.register(PlayerStateService.Role.TANK, AbilityKey.W,
                                new com.serveur.moba.classes.tank.TankWAbsorb(cooldowns, 8.0, 12000L));
                abilities.register(PlayerStateService.Role.TANK, AbilityKey.E,
                                new com.serveur.moba.classes.tank.TankEDash(cooldowns, globalFlags, 4.0, 500L, 8000L));
                abilities.register(
                                PlayerStateService.Role.TANK,
                                AbilityKey.R,
                                new com.serveur.moba.classes.tank.TankRSectorSlowAoE(
                                                cooldowns,
                                                teamService, globalFlags, 20, // slowness amp
                                                5, // durée en s
                                                10.5, // rayon
                                                110, // angle total du secteur
                                                20_000L, // CD
                                                16 // wind-up en ticks
                                ));

                pm.registerEvents(new com.serveur.moba.classes.tank.TankQListener(tankQ), this);

                // Bruiser
                abilities.register(PlayerStateService.Role.BRUISER, AbilityKey.Q,
                                new com.serveur.moba.classes.bruiser.BruiserQTripleDash(cooldowns, 3.0, 6000L, 9000L));
                abilities.register(PlayerStateService.Role.BRUISER, AbilityKey.W,
                                new com.serveur.moba.classes.bruiser.BruiserWSlowAoE(cooldowns, teamService,
                                                globalFlags, 2, 3, 4.0,
                                                10000L));
                abilities.register(PlayerStateService.Role.BRUISER, AbilityKey.E,
                                new com.serveur.moba.classes.bruiser.BruiserEDashAbsorb(cooldowns, 6.0, 8000L));
                abilities.register(PlayerStateService.Role.BRUISER, AbilityKey.R,
                                new com.serveur.moba.classes.bruiser.BruiserRToggleSmash(cooldowns, 8000L, 25000L, 6.0,
                                                70.0));

                // ADC
                abilities.register(PlayerStateService.Role.ADC, AbilityKey.Q,
                                new com.serveur.moba.classes.adc.AdcQAttackSpeed(cooldowns, 9000L, 6000L, 2.0));
                abilities.register(PlayerStateService.Role.ADC, AbilityKey.W,
                                new com.serveur.moba.classes.adc.AdcWShield(cooldowns, globalFlags, 12000L, 2000L));
                abilities.register(PlayerStateService.Role.ADC, AbilityKey.E,
                                new com.serveur.moba.classes.adc.AdcESlowZone(cooldowns, teamService, globalFlags, 2,
                                                6.0, 6000L,
                                                14000L));
                abilities.register(PlayerStateService.Role.ADC, AbilityKey.R,
                                new com.serveur.moba.classes.adc.AdcRAllSteroid(cooldowns, 25000L, 8000L, 6.0, 3.0));

                var hud = new CooldownHudService(this, state, cooldowns, actionBarBus);
                var cdBase = new CooldownBase();
                // === Tank ===
                hud.map(Role.TANK, AbilityKey.Q, CooldownIds.TANK_Q, 10_000L);
                cdBase.set(Role.TANK, AbilityKey.Q, 10_000L);
                hud.map(Role.TANK, AbilityKey.W, CooldownIds.TANK_W, 12_000L);
                cdBase.set(Role.TANK, AbilityKey.W, 12_000L);
                hud.map(Role.TANK, AbilityKey.E, CooldownIds.TANK_E, 8_000L);
                cdBase.set(Role.TANK, AbilityKey.E, 8_000L);
                hud.map(Role.TANK, AbilityKey.R, CooldownIds.TANK_R, 20_000L);
                cdBase.set(Role.TANK, AbilityKey.R, 20_000L);

                // === Bruiser ===
                hud.map(Role.BRUISER, AbilityKey.Q, CooldownIds.BRUISER_Q, 9_000L);
                cdBase.set(Role.BRUISER, AbilityKey.Q, 9_000L);
                hud.map(Role.BRUISER, AbilityKey.W, CooldownIds.BRUISER_W, 10_000L);
                cdBase.set(Role.BRUISER, AbilityKey.W, 10_000L);
                hud.map(Role.BRUISER, AbilityKey.E, CooldownIds.BRUISER_E, 8_000L);
                cdBase.set(Role.BRUISER, AbilityKey.E, 8_000L);
                hud.map(Role.BRUISER, AbilityKey.R, CooldownIds.BRUISER_R, 25_000L);
                cdBase.set(Role.BRUISER, AbilityKey.R, 25_000L);

                // === ADC ===
                hud.map(Role.ADC, AbilityKey.Q, CooldownIds.ADC_Q, 6_000L);
                cdBase.set(Role.ADC, AbilityKey.Q, 6_000L);
                hud.map(Role.ADC, AbilityKey.W, CooldownIds.ADC_W, 12_000L);
                cdBase.set(Role.ADC, AbilityKey.W, 12_000L);
                hud.map(Role.ADC, AbilityKey.E, CooldownIds.ADC_E, 14_000L);
                cdBase.set(Role.ADC, AbilityKey.E, 14_000L);
                hud.map(Role.ADC, AbilityKey.R, CooldownIds.ADC_R, 25_000L);
                cdBase.set(Role.ADC, AbilityKey.R, 25_000L);
                this.cooldownBase = cdBase;

                // Démarrer l’affichage périodique
                hud.start();

                var listener = new HotbarSpellListener(this, abilities, state,
                                this.spellKey);
                getServer().getPluginManager().registerEvents(listener, this);

                getLogger().info("Abilities init OK.");

                Buffs.init(this);

                // === Listeners “système” ===
                pm.registerEvents(new PvpGuardListener(gameManager.lane()), this);
                pm.registerEvents(new ProtectionListeners(globalFlags), this);
                pm.registerEvents(new com.serveur.moba.listeners.WandListener(this), this);
                pm.registerEvents(new ClassItemLockListener(state, kitService), this);
                getServer().getPluginManager().registerEvents(new HungerGuardListener(state), this);

                // Hooks de nettoyage (désactiver passifs + nettoyer état)
                pm.registerEvents(new Listener() {
                        @EventHandler
                        public void onJoin(PlayerJoinEvent e) {
                                getServer().getScheduler().runTaskLater(MobaPlugin.this,
                                                () -> classService.clearShopLoadout(e.getPlayer()), 1L);
                        }

                        @EventHandler
                        public void onQuit(PlayerQuitEvent e) {
                                classService.disableCurrent(e.getPlayer());
                                classService.clearShopLoadout(e.getPlayer());
                                state.clear(e.getPlayer());
                        }

                        @EventHandler
                        public void onDeath(PlayerDeathEvent e) {
                                classService.disableCurrent(e.getEntity());
                        }

                        @EventHandler
                        public void onRespawn(PlayerRespawnEvent e) {
                                var p = e.getPlayer();
                                var r = state.get(p.getUniqueId()).role;
                                getServer().getScheduler().runTaskLater(MobaPlugin.this,
                                                () -> kitService.applyKit(p, r), 1L);
                        }
                }, this);

                // TEAM
                getCommand("team").setExecutor(new TeamCommand(teamService));
                getCommand("team").setTabCompleter(new TeamCommand(teamService));
                getServer().getPluginManager().registerEvents(new TeamPvpListener(teamService), this);
                getServer().getPluginManager().registerEvents(new TeamQuitListener(teamService), this);
                getServer().getPluginManager().registerEvents(new ChatTeamListener(teamService), this);

                // SHOP
                this.shopService = new ShopService(this);
                var shopListeners = new ShopListeners(this, shopService);
                // On injecte CooldownService, CooldownBase et PlayerStateService pour Navori
                shopListeners.setCooldownDeps(this.cooldowns, this.cooldownBase, this.state);
                classService.setShopDeps(shopService, shopListeners);
                pm.registerEvents(shopListeners, this);

                // Emeraude -> boutique
                pm.registerEvents(new ShopOpenListener(shopService, kitService), this);

                // On lock les items du shop dans l'inventaire
                pm.registerEvents(new ShopItemLockListener(shopService), this);

                getCommand("boutique").setExecutor(this);
                getCommand("boutique").setTabCompleter(this);

                // === Commandes ===
                var mobaCmd = new com.serveur.moba.commands.MobaCommand(
                                this, gameManager, state, abilities, classService);
                getCommand("moba").setExecutor(mobaCmd);
                getCommand("moba").setTabCompleter(mobaCmd);
                getCommand("forcepvp").setExecutor(mobaCmd);
                getCommand("forcepvp").setTabCompleter(mobaCmd);
                getCommand("class").setExecutor(mobaCmd);

                getCommand("q").setExecutor(this);
                getCommand("w").setExecutor(this);
                getCommand("e").setExecutor(this);
                getCommand("r").setExecutor(this);

                // === Lancement de la boucle de jeu ===
                gameManager.start();
                getLogger().info("Moba enabled!");
        }

        @Override
        public void onDisable() {
                // Désactiver proprement les passifs encore actifs
                for (Player p : getServer().getOnlinePlayers()) {
                        classService.disableCurrent(p);
                        classService.clearShopLoadout(p);
                }
                getLogger().info("Moba disabled!");
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                        @NotNull String label, @NotNull String[] args) {

                if (command.getName().equalsIgnoreCase("boutique")) {
                        if (!(sender instanceof Player p)) {
                                sender.sendMessage("§cCette commande est réservée aux joueurs in-game.");
                                return true;
                        }
                        shopService.openShop(p);
                        return true;
                }
                if (command.getName().equalsIgnoreCase("class")) {
                        if (!(sender instanceof Player p)) {
                                sender.sendMessage("§cSeulement les joueurs in-game peuvent exécuter cette commande");
                                return true;
                        }
                        if (args.length != 1) {
                                p.sendMessage("§cUsage: /class <tank|bruiser|adc>");
                                return true;
                        }
                        var opt = classService.parseRole(args[0]);
                        if (opt.isEmpty()) {
                                p.sendMessage("§cClasse inconnue (choix: tank, bruiser, adc)");
                                return true;
                        }
                        classService.setClass(p, opt.get());
                        p.sendMessage("§aClasse définie à §b" + opt.get().name());
                        return true;
                }

                // Sorts (Q/W/E/R) déclenchés ici
                if (command.getName().equalsIgnoreCase("q")
                                || command.getName().equalsIgnoreCase("w")
                                || command.getName().equalsIgnoreCase("e")
                                || command.getName().equalsIgnoreCase("r")) {
                        if (!(sender instanceof Player p)) {
                                sender.sendMessage("Joueur requis.");
                                return true;
                        }
                        var s = state.get(p.getUniqueId());
                        AbilityKey key = switch (command.getName().toLowerCase()) {
                                case "q" -> AbilityKey.Q;
                                case "w" -> AbilityKey.W;
                                case "e" -> AbilityKey.E;
                                default -> AbilityKey.R;
                        };
                        var ab = abilities.get(s.role, key);
                        if (ab == null) {
                                p.sendMessage("§cAucune compétence assignée.");
                                return true;
                        }
                        boolean ok = ab.cast(new AbilityContext(this, p));
                        if (!ok)
                                p.sendMessage("§cImpossible de lancer le sort.");
                        return true;
                }

                return false;
        }

        private static String cdText(long ms) {
                return ("CD: " + (ms / 1000) + "s");
        }

        public void giveClassKit(Player p, Role role) {
                var hb = new SpellHotbar(spellKey, kitService);

                // Descriptions par classe
                Map<SpellTag, String> baseDesc = switch (role) {
                        case TANK -> Map.of(
                                        SpellTag.Q, "Renforce les 3 prochaines attaques",
                                        SpellTag.W, "Obtient " + "coeurs d'absorption",
                                        SpellTag.E, "Petit dash qui rend insensible aux contrôles de foules",
                                        SpellTag.R, "Détermine une zone de slow massif à une certaine distance de lui",
                                        SpellTag.PASSIVE,
                                        "Si le tank reçoit une attaque, il applique blindness à son attaquant mais a un gros CD sur chaque ennemi");
                        case BRUISER -> Map.of(
                                        SpellTag.Q, "Triple dash",
                                        SpellTag.W, "Zone de ralentissement autour du joueur",
                                        SpellTag.E, "Dash + absorption",
                                        SpellTag.R,
                                        "Stéroïde de dégâts puis Smash enragé si on rappuit. En cas de réutilisation, le stéroïde disparaît",
                                        SpellTag.PASSIVE, "Speed 3 hors combat");
                        case ADC -> Map.of(
                                        SpellTag.Q, "Améliore l'attack speed pendant un court lapse de temps",
                                        SpellTag.W, "Bouclier qui supprime les dégâts de la prochaine source de dégâts",
                                        SpellTag.E,
                                        "Grosse zone de slow autour du joueur qui reste sur le sol pendant un court lapse de temps",
                                        SpellTag.R, "Gros stéroïde de stats",
                                        SpellTag.PASSIVE,
                                        "Toutes les 5 attaques sur un joueur, la prochaine attaque applique des dégâts supplémentaires");
                        default -> Map.of();
                };

                // Ajoute “CD: …” en lisant CooldownBase sauf pour le passif
                var tagToKey = Map.of(
                                SpellTag.Q, AbilityKey.Q,
                                SpellTag.W, AbilityKey.W,
                                SpellTag.E, AbilityKey.E,
                                SpellTag.R, AbilityKey.R,
                                SpellTag.PASSIVE, AbilityKey.PASSIVE);

                Map<SpellTag, String> withCd = new java.util.EnumMap<>(SpellTag.class);
                for (var e : baseDesc.entrySet()) {
                        var tag = e.getKey();
                        var text = e.getValue();

                        var key = tagToKey.get(tag);
                        long ms = (key == null) ? 0L : cooldownBase.get(role, key);
                        // Si c’est le passif, on affiche juste la description sans CD
                        String full = (tag == SpellTag.PASSIVE)
                                        ? text
                                        : text + " — " + cdText(ms);

                        withCd.put(tag, full);
                }

                hb.applyTo(p, withCd);
        }

        public TeamService getTeamService() {
                return teamService;
        }

}
