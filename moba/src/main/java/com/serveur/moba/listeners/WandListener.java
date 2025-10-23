package com.serveur.moba.listeners;

import com.serveur.moba.MobaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class WandListener implements Listener {
    private final MobaPlugin plugin;

    public WandListener(MobaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;

        Action a = e.getAction();
        switch (a) {
            case LEFT_CLICK_BLOCK -> {
                plugin.setPos1(e.getPlayer().getUniqueId(), e.getClickedBlock().getLocation());
                e.getPlayer().sendMessage("§bpos1 enregistrée.");
            }
            case RIGHT_CLICK_BLOCK -> {
                plugin.setPos2(e.getPlayer().getUniqueId(), e.getClickedBlock().getLocation());
                e.getPlayer().sendMessage("§bpos2 enregistrée.");
            }
            default -> { /* rien */ }
        }
    }
}
