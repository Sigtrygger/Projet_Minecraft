package com.serveur.moba.ability;

import com.serveur.moba.state.PlayerStateService;
import java.util.EnumMap;
import java.util.Map;

public class AbilityRegistry {
    private final Map<PlayerStateService.Role, Map<AbilityKey, Ability>> map = new EnumMap<>(
            PlayerStateService.Role.class);

    public void register(PlayerStateService.Role role, AbilityKey key, Ability ability) {
        map.computeIfAbsent(role, _ -> new EnumMap<>(AbilityKey.class)).put(key, ability);
    }

    public Ability get(PlayerStateService.Role role, AbilityKey key) {
        var inner = map.get(role);
        return inner == null ? null : inner.get(key);
    }
}
