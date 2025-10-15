package com.serveur.moba.ability;

public interface Ability {
    /**
     * Retourne false si l’ability ne peut pas partir (CD, état…), true si cast OK
     */
    boolean cast(AbilityContext ctx);
}
