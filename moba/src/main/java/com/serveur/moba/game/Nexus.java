package com.serveur.moba.game;

import org.bukkit.Location;

import com.serveur.moba.game.enums.Team;

public class Nexus {
    private final Team team;
    private final Location loc;
    private int hp;

    public Nexus(Team t, Location loc, int hp) {
        this.team = t;
        this.loc = loc;
        this.hp = hp;
    }

    public Team team() {
        return team;
    }

    public Location loc() {
        return loc;
    }

    public int hp() {
        return hp;
    }

    public void damage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    public boolean isDestroyed() {
        return hp <= 0;
    }
}
