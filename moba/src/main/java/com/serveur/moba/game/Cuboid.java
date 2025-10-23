package com.serveur.moba.game;

import org.bukkit.Location;

public record Cuboid(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(world))
            return false;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX
                && loc.getBlockY() >= minY && loc.getBlockY() <= maxY
                && loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }
}
