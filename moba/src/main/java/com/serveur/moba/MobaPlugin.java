package com.serveur.moba;

import org.bukkit.plugin.java.JavaPlugin;

public final class MobaPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("Moba enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Moba disabled!");
    }
}
