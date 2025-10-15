package com.serveur.moba.classes.bruiser;

import com.serveur.moba.ability.*;
import com.serveur.moba.util.Dash;
import org.bukkit.entity.Player;

import java.util.*;

public class BruiserQTripleDash implements Ability {
    private final CooldownService cds;
    private final double dashBlocks;
    private final long chargesWindowMs; // fenêtre pour faire les 3 dashs
    private final long cdMs; // CD total après la 3e (ou fin fenêtre)

    private final Map<UUID, Integer> used = new HashMap<>();
    private final Map<UUID, Long> until = new HashMap<>();

    public BruiserQTripleDash(CooldownService cds, double dashBlocks, long chargesWindowMs, long cdMs) {
        this.cds = cds;
        this.dashBlocks = dashBlocks;
        this.chargesWindowMs = chargesWindowMs;
        this.cdMs = cdMs;
    }

    @Override
    public boolean cast(AbilityContext ctx) {
        Player p = ctx.player();
        UUID id = p.getUniqueId();

        // Première activation — check CD
        if (!until.containsKey(id)) {
            if (!cds.ready(p, "bruiser.Q", cdMs)) {
                p.sendMessage("§cQ en CD.");
                return false;
            }
            used.put(id, 0);
            until.put(id, System.currentTimeMillis() + chargesWindowMs);
            p.sendActionBar(net.kyori.adventure.text.Component.text("§e[Bruiser] Q — Dashs prêts (x3)"));
        }

        // Fenêtre expirée ?
        if (System.currentTimeMillis() > until.get(id)) {
            until.remove(id);
            used.remove(id);
            p.sendMessage("§cFenêtre de dash expirée.");
            return false;
        }

        // Exécuter un dash
        Dash.smallForward(p, dashBlocks);
        int u = used.merge(id, 1, Integer::sum);

        if (u >= 3) { // fin des charges
            until.remove(id);
            used.remove(id);
            p.sendActionBar(net.kyori.adventure.text.Component.text("§a[Bruiser] Q — Charges consommées"));
            // le CD est déjà “pris” au premier appui : on laisse le service gérer le temps
            // restant
        }
        return true;
    }
}
