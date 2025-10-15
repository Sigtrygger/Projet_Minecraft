package com.serveur.moba.classes.adc;

import com.serveur.moba.state.PlayerStateService;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.*;

public class AdcPassiveListener implements Listener {
    private final Map<UUID, Integer> aaCount = new HashMap<>();
    private final PlayerStateService state;

    public AdcPassiveListener(PlayerStateService state) {
        this.state = state;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p))
            return;
        var s = state.get(p.getUniqueId());
        if (s.role != PlayerStateService.Role.ADC)
            return;

        int n = aaCount.merge(p.getUniqueId(), 1, Integer::sum);
        if (n >= 5) {
            e.setDamage(e.getDamage() * 3.0);
            aaCount.put(p.getUniqueId(), 0);
            p.sendActionBar(net.kyori.adventure.text.Component.text("§6[ADC] Tir renforcé !"));
        }
    }
}
