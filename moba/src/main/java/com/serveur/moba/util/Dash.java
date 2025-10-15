package com.serveur.moba.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class Dash {
    private Dash() {
    }

    public static void smallForward(Player p, double blocks) {
        Vector dir = p.getLocation().getDirection().normalize().multiply(blocks);
        Location dest = p.getLocation().clone().add(dir);
        dest.setY(p.getLocation().getY()); // dash “plat”, à modifier plus tard
        p.teleport(dest);
    }
}
