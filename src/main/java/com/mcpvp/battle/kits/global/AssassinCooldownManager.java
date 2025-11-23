package com.mcpvp.battle.kits.global;

import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Handles strength restoration for Assassin, which needs to persist across Kit instances.
 */
public class AssassinCooldownManager {

    private final Map<Player, Expiration> cooldowns = new HashMap<>();

    /**
     * Store the given cooldown, which can be retrieved with {@link #getCooldownRemaining(Player)}.
     *
     * @param player The player to associate the cooldown with.
     * @param cooldown The time remaining on the cooldown.
     */
    public void storeCooldown(Player player, Duration cooldown) {
        this.cooldowns.put(player, Expiration.after(cooldown));
    }

    /**
     * Retrieves a stored cooldown for the given Player. The cooldown will only be returned if it exists
     * and is not already expired.
     *
     * @param player The player to receive the cooldown for.
     * @return The Duration of time remaining on the cooldown, or an empty optional when there was no cooldown stored or
     * the stored cooldown has expired.
     */
    public Optional<Duration> getCooldownRemaining(Player player) {
        return Optional.ofNullable(this.cooldowns.get(player))
            .filter(Predicate.not(Expiration::isExpired))
            .map(Expiration::getRemaining);
    }

}
