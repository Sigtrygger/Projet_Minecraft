package com.serveur.moba.classes;

import com.serveur.moba.classes.adc.AdcPassiveListener;
import com.serveur.moba.kit.KitService;
import com.serveur.moba.shop.ShopListeners;
import com.serveur.moba.shop.ShopService;
import com.serveur.moba.state.PlayerStateService;

import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.Locale;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class ClassService {
    private final PlayerStateService state;
    private final AdcPassiveListener adcListener; // pour reset compteur
    private final KitService kitService;
    private ShopService shopService;
    private ShopListeners shopListeners;

    public ClassService(PlayerStateService state, AdcPassiveListener adcListener, KitService kitService) {
        this.state = state;
        this.adcListener = adcListener;
        this.kitService = kitService;
    }

    public void setShopDeps(ShopService shopService, ShopListeners shopListeners) {
        this.shopService = shopService;
        this.shopListeners = shopListeners;
    }

    public Optional<PlayerStateService.Role> parseRole(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "tank" -> Optional.of(PlayerStateService.Role.TANK);
            case "bruiser" -> Optional.of(PlayerStateService.Role.BRUISER);
            case "adc" -> Optional.of(PlayerStateService.Role.ADC);
            case "none" -> Optional.of(PlayerStateService.Role.NONE);
            default -> Optional.empty();
        };
    }

    public boolean setClass(Player p, PlayerStateService.Role newRole) {
        var st = state.get(p.getUniqueId());
        var old = st.role;
        if (old == newRole)
            return true;

        // --- CLEANUP de l'ancienne classe ---
        switch (old) {
            case BRUISER -> p.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
            case ADC -> adcListener.reset(p.getUniqueId());
            case TANK, NONE -> {
            }
        }

        // set l'état
        clearShopLoadout(p);
        state.setRole(p, newRole);

        // --- INIT de la nouvelle classe (si tu veux appliquer quelque chose
        // immédiatement) ---
        kitService.applyKit(p, newRole);

        if (newRole == PlayerStateService.Role.ADC) {
            adcListener.reset(p.getUniqueId()); // démarre propre
        }

        p.setGameMode(GameMode.ADVENTURE);
        p.setFoodLevel(20);
        p.setSaturation(0f);
        p.setExhaustion(0f);

        return true;
    }

    public void disableCurrent(Player p) {
        // Appelé sur quit/death: enlever effets volatiles
        switch (state.get(p.getUniqueId()).role) {
            case BRUISER -> p.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
            case ADC -> adcListener.reset(p.getUniqueId());
            case TANK, NONE -> {
            }
        }
    }

    public void clearClass(Player p) {

        disableCurrent(p);
        clearShopLoadout(p);

        kitService.clearKit(p);

        state.clear(p);

        state.setRole(p, PlayerStateService.Role.NONE);

        p.sendActionBar(Component.empty());

        p.setGameMode(GameMode.SURVIVAL);
        p.setFoodLevel(20);
        p.setSaturation(5f);
        p.setExhaustion(0f);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setAllowFlight(false);
    }

    public void clearShopLoadout(Player p) {
        if (shopListeners != null) {
            shopListeners.clearItemEffects(p);
        }
        if (shopService != null) {
            shopService.clearBoughtItems(p);
        }
    }

}
