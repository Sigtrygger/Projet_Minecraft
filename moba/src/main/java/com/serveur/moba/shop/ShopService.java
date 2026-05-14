package com.serveur.moba.shop;

import com.serveur.moba.MobaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class ShopService {

    private final NamespacedKey KEY;
    private final Map<UUID, Set<String>> active = new HashMap<>();

    // Titre de la GUI
    private static final String TITLE = "§aBoutique";

    private static final int COL_BRUISER = 1; // deuxième case
    private static final int COL_TANK = 4; // au centre
    private static final int COL_ADC = 7; // presque à droite

    public ShopService(MobaPlugin plugin) {
        this.KEY = new NamespacedKey(plugin, "moba_shop_item");
    }

    /**
     * Ouvre la boutique pour un joueur
     */
    public void openShop(Player p) {
        Inventory inv = Bukkit.createInventory(p, 36, TITLE); // 4 lignes

        decorate(inv);
        placeItems(inv);

        p.openInventory(inv);
    }

    private void decorate(Inventory inv) {
        // fond gris
        ItemStack filler = createSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // headers
        inv.setItem(COL_BRUISER, createHeader(
                Material.RED_STAINED_GLASS_PANE,
                "§c§lBruiser",
                "§7Objets mêlant dégâts et résistance"));

        inv.setItem(COL_TANK, createHeader(
                Material.BLUE_STAINED_GLASS_PANE,
                "§9§lTank",
                "§7Objets défensifs"));

        inv.setItem(COL_ADC, createHeader(
                Material.LIME_STAINED_GLASS_PANE,
                "§e§lADC",
                "§7Objets de dégâts"));
    }

    private void placeItems(Inventory inv) {
        // ligne de départ pour les items (1 = deuxième ligne, 2 = troisième, etc.)
        Map<ShopItem.Role, Integer> nextRow = new EnumMap<>(ShopItem.Role.class);
        nextRow.put(ShopItem.Role.BRUISER, 1);
        nextRow.put(ShopItem.Role.TANK, 1);
        nextRow.put(ShopItem.Role.ADC, 1);

        for (ShopItem si : ShopItem.values()) {
            int col = switch (si.role) {
                case BRUISER -> COL_BRUISER;
                case TANK -> COL_TANK;
                case ADC -> COL_ADC;
            };

            int row = nextRow.get(si.role); // 1 ou 2
            int slot = row * 9 + col; // row=1 → 9+col ; row=2 → 18+col

            inv.setItem(slot, createItemStack(si));
            nextRow.put(si.role, row + 1);
        }
    }

    // item simple sans meta de shop (fond / header)
    private ItemStack createSimpleItem(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack createHeader(Material mat, String name, String loreLine) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            m.setLore(new ArrayList<>(List.of(loreLine)));
            it.setItemMeta(m);
        }
        return it;
    }

    /**
     * Savoir si un inventaire est la GUI de shop
     */
    public boolean isShopInventory(InventoryView view) {
        return view != null && TITLE.equals(view.getTitle());
    }

    /**
     * Retrouver le ShopItem correspondant à un ItemStack de la GUI
     */
    public ShopItem fromItemStack(ItemStack it) {
        if (it == null)
            return null;
        ItemMeta m = it.getItemMeta();
        if (m == null)
            return null;
        String id = m.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        if (id == null)
            return null;

        for (ShopItem si : ShopItem.values()) {
            if (si.id.equals(id)) {
                return si;
            }
        }
        return null;
    }

    // ======================= EXISTANT =======================

    // Create an ItemStack representing the shop item (price=0 for now)
    public ItemStack createItemStack(ShopItem si) {
        ItemStack it = new ItemStack(si.icon);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName("§e" + si.display);

            m.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, si.id);

            List<String> lore = new ArrayList<>();
            lore.add("§7" + si.description); // description de l'effet
            lore.add(""); // ligne vide
            lore.add("§aPrix: §e0"); // pour l'instant
            lore.add("§8Clic gauche: acheter");
            lore.add("§8Clic droit: vendre");

            m.setLore(lore);

            it.setItemMeta(m);
        }
        return it;
    }

    public void giveBoughtItem(Player p, ShopItem item) {
        active.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>()).add(item.id);

        ItemStack stack = createItemStack(item);
        PlayerInventory inv = p.getInventory();

        boolean placed = false;
        for (int slot = 9; slot <= 35; slot++) {
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                inv.setItem(slot, stack);
                placed = true;
                break;
            }
        }

        if (!placed) {
            // inventaire plein => fallback, on laisse Bukkit gérer
            inv.addItem(stack);
        }
    }

    // remove item effects and unmark
    public void removeBoughtItem(Player p, ShopItem item) {
        Set<String> s = active.get(p.getUniqueId());
        if (s != null) {
            s.remove(item.id);
            if (s.isEmpty())
                active.remove(p.getUniqueId());
        }
        // remove item stacks from inventory
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null)
                continue;
            ItemMeta m = it.getItemMeta();
            if (m == null)
                continue;
            String id = m.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
            if (item.id.equals(id)) {
                p.getInventory().remove(it);
            }
        }
    }

    public void clearBoughtItems(Player p) {
        active.remove(p.getUniqueId());

        for (ItemStack it : p.getInventory().getContents()) {
            if (isShopItem(it)) {
                p.getInventory().remove(it);
            }
        }
    }

    public boolean playerHas(Player p, ShopItem item) {
        Set<String> s = active.get(p.getUniqueId());
        return s != null && s.contains(item.id);
    }

    // helper used by listeners to check persistent id on ItemStack if needed
    public boolean isShopItem(ItemStack it, ShopItem item) {
        if (it == null)
            return false;
        ItemMeta m = it.getItemMeta();
        if (m == null)
            return false;
        String id = m.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        return item.id.equals(id);
    }

    // true si l'item porte la clé de la boutique (peu importe lequel)
    public boolean isShopItem(ItemStack it) {
        if (it == null)
            return false;
        var m = it.getItemMeta();
        if (m == null)
            return false;
        return m.getPersistentDataContainer().has(KEY, PersistentDataType.STRING);
    }

    public Set<String> getActiveIds(UUID playerId) {
        return active.getOrDefault(playerId, Collections.emptySet());
    }
}
