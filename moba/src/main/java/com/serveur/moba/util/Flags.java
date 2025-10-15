package com.serveur.moba.util;

import org.bukkit.entity.Player;
import java.util.*;

public class Flags {
    private final Set<String> set = new HashSet<>();

    private static String k(Player p, String flag) {
        return p.getUniqueId() + ":" + flag;
    }

    public void set(Player p, String flag, boolean on) {
        if (on)
            set.add(k(p, flag));
        else
            set.remove(k(p, flag));
    }

    public boolean has(Player p, String flag) {
        return set.contains(k(p, flag));
    }
}
