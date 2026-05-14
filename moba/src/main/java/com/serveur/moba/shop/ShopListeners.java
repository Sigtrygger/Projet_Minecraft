package com.serveur.moba.shop;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;

import com.serveur.moba.ability.AbilityKey;
import com.serveur.moba.ability.CooldownIds;
import com.serveur.moba.ability.CooldownService;
import com.serveur.moba.state.PlayerStateService;
import com.serveur.moba.util.CooldownBase;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import com.serveur.moba.util.Buffs;

import java.util.*;

public class ShopListeners implements Listener {

    private final Plugin plugin;
    private final ShopService shop;
    private final NamespacedKey heartsteelStacksKey;
    private static final double DEFAULT_MAX_HEALTH = 20.0;

    // cooldowns / counters
    private final Map<UUID, Long> lastDamageTaken = new HashMap<>(); // for Rookern
    private final Set<UUID> rookernApplied = new HashSet<>();
    private final Map<UUID, Long> divineSundererLast = new HashMap<>();
    private final Map<String, Integer> thornCounters = new HashMap<>();
    private final Map<UUID, Integer> heartsteelCounters = new HashMap<>();
    private final Map<UUID, Integer> heartsteelStacks = new HashMap<>();
    private final Map<UUID, Long> sterakCooldown = new HashMap<>();
    private CooldownService cds;
    private CooldownBase base;
    private PlayerStateService states;

    public ShopListeners(Plugin plugin, ShopService shop) {
        this.plugin = plugin;
        this.shop = shop;
        this.heartsteelStacksKey = new NamespacedKey(plugin, "heartsteel_stacks");
        // periodic task to check Rookern (10s no damage => grant absorption)
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkRookern, 20L, 20L);
    }

    // setter to inject cooldown-related services (keeps old constructor intact)
    public void setCooldownDeps(CooldownService cds, CooldownBase base, PlayerStateService states) {
        this.cds = cds;
        this.base = base;
        this.states = states;
    }

    public void clearItemEffects(Player p) {
        UUID id = p.getUniqueId();

        removeHeartsteelStacks(p);

        Buffs.removeSource(p, "item_rookern");
        Buffs.removeSource(p, "item_sterak");

        lastDamageTaken.remove(id);
        rookernApplied.remove(id);
        divineSundererLast.remove(id);
        sterakCooldown.remove(id);
        thornCounters.keySet().removeIf(key -> key.contains(id.toString()));
    }

    private void removeHeartsteelStacks(Player p) {
        UUID id = p.getUniqueId();
        int stacks = heartsteelStackCount(p);

        heartsteelCounters.remove(id);
        heartsteelStacks.remove(id);
        p.getPersistentDataContainer().remove(heartsteelStacksKey);

        var maxHealth = p.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null)
            return;

        maxHealth.setBaseValue(DEFAULT_MAX_HEALTH);
        p.setHealth(Math.min(p.getHealth(), maxHealth.getValue()));
    }

    private int heartsteelStackCount(Player p) {
        int tracked = heartsteelStacks.getOrDefault(p.getUniqueId(), 0);
        Integer stored = p.getPersistentDataContainer().get(heartsteelStacksKey, PersistentDataType.INTEGER);
        return Math.max(tracked, stored == null ? 0 : stored);
    }

    private void setHeartsteelStackCount(Player p, int stacks) {
        UUID id = p.getUniqueId();
        if (stacks <= 0) {
            heartsteelStacks.remove(id);
            p.getPersistentDataContainer().remove(heartsteelStacksKey);
            return;
        }

        heartsteelStacks.put(id, stacks);
        p.getPersistentDataContainer().set(heartsteelStacksKey, PersistentDataType.INTEGER, stacks);
    }

    private void checkRookern() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!shop.playerHas(p, ShopItem.TANK_ROOKERN))
                continue;

            UUID id = p.getUniqueId();
            long last = lastDamageTaken.getOrDefault(id, 0L);
            long diff = now - last;

            if (diff >= 10_000L) {
                // Absorption actuelle en HP (2 HP = 1 cœur)
                double curHp = p.getAbsorptionAmount();

                // Rookern garantit "jusqu'à 8 cœurs" au total
                double targetHp = 8.0 * 2.0;
                if (curHp < targetHp) {
                    p.setAbsorptionAmount(targetHp);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent ev) {
        if (!(ev.getDamager() instanceof Player))
            return;
        Player attacker = (Player) ev.getDamager();
        if (!(ev.getEntity() instanceof LivingEntity))
            return;

        // BRUISER - Divine Sunderer: every 4s next autoattack that hits returns 3
        // hearts.
        if (shop.playerHas(attacker, ShopItem.BRUISER_DIVINE_SUNDERER)) {
            long last = divineSundererLast.getOrDefault(attacker.getUniqueId(), 0L);
            if (System.currentTimeMillis() - last >= 4_000L) {
                // heal attacker by 3 hearts = 6 health
                double max = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
                attacker.setHealth(Math.min(max, attacker.getHealth() + 6.0));
                divineSundererLast.put(attacker.getUniqueId(), System.currentTimeMillis());
            }
        }

        // ADC - Soif de sang: each auto attack regen 0.5 heart = 1 health
        if (shop.playerHas(attacker, ShopItem.ADC_BLOODTHIRST)) {
            double max = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
            attacker.setHealth(Math.min(max, attacker.getHealth() + 1.0));
        }

        // ADC - Navori: placeholder - reduce cooldowns by 1% (requires your cooldown
        // system)
        if (shop.playerHas(attacker, ShopItem.ADC_NAVORI) && cds != null && base != null && states != null) {
            var st = states.get(attacker.getUniqueId());
            if (st != null) {
                var role = st.role;
                for (AbilityKey key : AbilityKey.values()) {
                    String id = cooldownId(role, key);
                    long ms = base.get(role, key);
                    if (id != null && ms > 0) {
                        cds.reduceRemainingPercent(attacker, id, ms, 1.0);
                    }
                }
            }
        }

        // TANK - Heartsteel: every 20 autoattacks on an adversary -> gain 0.5 red heart
        // (max hp up)
        if (shop.playerHas(attacker, ShopItem.TANK_HEARTSTEEL)) {
            int c = heartsteelCounters.getOrDefault(attacker.getUniqueId(), 0) + 1;
            if (c >= 20) {
                heartsteelCounters.put(attacker.getUniqueId(), 0);
                // add 0.5 heart = +1 max health
                double cur = attacker.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                attacker.getAttribute(Attribute.MAX_HEALTH).setBaseValue(cur + 1.0);
                // optionally heal a bit to show the increase:
                attacker.setHealth(Math.min(attacker.getHealth() + 1.0, cur + 1.0));
                setHeartsteelStackCount(attacker, heartsteelStackCount(attacker) + 1);
            } else {
                heartsteelCounters.put(attacker.getUniqueId(), c);
            }
        }

        // TANK - Thornmail: when a player is hit 3 times by same attacker, the attacker
        // loses 0.5 heart
        if (ev.getEntity() instanceof Player) {
            Player target = (Player) ev.getEntity();
            if (shop.playerHas(target, ShopItem.TANK_THORNMAIL)) {
                String key = attacker.getUniqueId().toString() + "_" + target.getUniqueId().toString();
                int cnt = thornCounters.getOrDefault(key, 0) + 1;
                if (cnt >= 3) {
                    // damage the attacker by 1 (0.5 heart)
                    attacker.damage(1.0, target);
                    thornCounters.put(key, 0);
                } else {
                    thornCounters.put(key, cnt);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent ev) {
        // update last damage time for rookern and apply Sterak behavior
        if (ev.getEntity() instanceof Player) {
            Player p = (Player) ev.getEntity();
            UUID id = p.getUniqueId();
            lastDamageTaken.put(id, System.currentTimeMillis());

            // BRUISER - Sterak's Gage: when player loses half their life => grant
            // absorption + strength 1 for 3s (30s CD)
            if (shop.playerHas(p, ShopItem.BRUISER_STERAKS_GAGE)) {
                double after = p.getHealth() - ev.getFinalDamage();
                double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
                if (after <= max / 2.0) {
                    long now = System.currentTimeMillis();
                    long cd = sterakCooldown.getOrDefault(id, 0L);
                    if (now - cd >= 30_000L) {

                        // On enlève uniquement l'ancien Sterak s'il y en avait un
                        Buffs.removeSource(p, "item_sterak");

                        // On ajoute Sterak comme source à part
                        Buffs.absorption(p, "item_sterak", 4.0, 5); // 4 cœurs pendant 5s

                        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 5 * 20, 0));
                        sterakCooldown.put(id, now);
                    }
                }
            }

        }
    }

    private static String cooldownId(PlayerStateService.Role role, AbilityKey key) {
        switch (role) {
            case TANK -> {
                return switch (key) {
                    case Q -> CooldownIds.TANK_Q;
                    case W -> CooldownIds.TANK_W;
                    case E -> CooldownIds.TANK_E;
                    case R -> CooldownIds.TANK_R;
                    default -> null;
                };
            }
            case BRUISER -> {
                return switch (key) {
                    case Q -> CooldownIds.BRUISER_Q;
                    case W -> CooldownIds.BRUISER_W;
                    case E -> CooldownIds.BRUISER_E;
                    case R -> CooldownIds.BRUISER_R;
                    default -> null;
                };
            }
            case ADC -> {
                return switch (key) {
                    case Q -> CooldownIds.ADC_Q;
                    case W -> CooldownIds.ADC_W;
                    case E -> CooldownIds.ADC_E;
                    case R -> CooldownIds.ADC_R;
                    default -> null;
                };
            }
            default -> {
                return null;
            }
        }
    }

    // === GUI de boutique : clic gauche = acheter, clic droit = vendre ===
    @EventHandler
    public void onShopClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;

        if (!shop.isShopInventory(e.getView()))
            return;

        if (e.getRawSlot() >= e.getView().getTopInventory().getSize())
            return;

        // On empêche de déplacer quoi que ce soit dans la boutique
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        ShopItem item = shop.fromItemStack(clicked);
        if (item == null)
            return;

        // Clic gauche -> achat
        if (e.isLeftClick()) {

            // --- restriction par classe ---
            if (states == null) {
                p.sendMessage("§cErreur interne : état de classe indisponible.");
                return;
            }

            var st = states.get(p.getUniqueId());
            if (st == null) {
                p.sendMessage("§cTu dois choisir une classe avant d'acheter des objets.");
                return;
            }

            if (!canBuyItem(st.role, item)) {
                p.sendMessage("§cTu ne peux pas acheter d'objets " +
                        switch (item.role) {
                            case TANK -> "§9Tank§c.";
                            case BRUISER -> "§cBruiser§c.";
                            case ADC -> "§eADC§c.";
                        });
                return;
            }
            // --- fin restriction par classe ---

            if (shop.playerHas(p, item)) {
                p.sendMessage("§cTu possèdes déjà cet objet.");
                return;
            }

            shop.giveBoughtItem(p, item);
            p.sendMessage("§aTu achètes §e" + item.display + "§a.");

            if (item == ShopItem.TANK_ROOKERN) {
                UUID id = p.getUniqueId();
                Buffs.absorption(p, "item_rookern", 8.0, 0);
                lastDamageTaken.put(id, System.currentTimeMillis());
            }

        }
        // Clic droit -> vente
        else if (e.isRightClick()) {
            if (!shop.playerHas(p, item)) {
                p.sendMessage("§cTu ne possèdes pas cet objet.");
                return;
            }
            if (item == ShopItem.TANK_ROOKERN) {
                UUID id = p.getUniqueId();
                Buffs.removeSource(p, "item_rookern");
                lastDamageTaken.remove(id);
            }
            if (item == ShopItem.TANK_HEARTSTEEL) {
                removeHeartsteelStacks(p);
            }
            shop.removeBoughtItem(p, item);
            p.sendMessage("§eTu revends §e" + item.display + "§e.");
        }
    }

    @EventHandler
    public void onShopDrag(InventoryDragEvent e) {
        if (shop.isShopInventory(e.getView())) {
            e.setCancelled(true);
        }
    }

    private boolean canBuyItem(PlayerStateService.Role playerRole, ShopItem item) {
        if (playerRole == null || item == null)
            return false;

        return switch (playerRole) {
            case TANK -> item.role == ShopItem.Role.TANK;
            case BRUISER -> item.role == ShopItem.Role.BRUISER;
            case ADC -> item.role == ShopItem.Role.ADC;
            default -> false;
        };
    }

}
