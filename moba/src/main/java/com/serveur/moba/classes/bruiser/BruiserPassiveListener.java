package com.serveur.moba.classes.bruiser;

import com.serveur.moba.combat.CombatTagService;
import com.serveur.moba.util.Buffs;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class BruiserPassiveListener implements Listener {
    private final CombatTagService combat;

    public BruiserPassiveListener(CombatTagService combat) {
        this.combat = combat;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim))
            return;

        // Tag combat si dégâts par monstre ou joueur
        if (e.getDamager() instanceof Player damager) {
            combat.tag(victim);
            combat.tag(damager);
        } else if (CombatTagService.isMonster(e.getDamager())) {
            combat.tag(victim);
        }
    }

    @EventHandler
    public void tick(org.bukkit.event.player.PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (combat.inCombat(p))
            return; // en combat => pas de speed
        // hors combat => Speed 3 constant
        Buffs.give(p, org.bukkit.potion.PotionEffectType.SPEED, 3, 4); // ré-appliqué tant qu’on bouge
    }
}
