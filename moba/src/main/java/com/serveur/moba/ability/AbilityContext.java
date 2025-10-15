package com.serveur.moba.ability;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public record AbilityContext(Plugin plugin, Player player) {
}
