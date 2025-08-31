package com.mcpvp.battle.flag;

import com.mcpvp.battle.event.*;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Entrypoint for controlling flag mechanics and triggering flag events.
 * Called primarily from {@link FlagListener}.
 */
@RequiredArgsConstructor
public class FlagManager {

    private static final Duration FLAG_STEAL_TIMER = Duration.milliseconds(200);

    private final IBattleFlag flag;
    private final Map<Player, Expiration> stealTimers = new HashMap<>();

    /**
     * Attempts to steal the flag, but nothing will happen if the player hasn't been near
     * the flag for long enough.
     *
     * @param player The player attempting to steal.
     */
    public void attemptSteal(Player player) {
        // Ensure the player is allowed to even *try* to steal
        FlagStartStealEvent startStealEvent = new FlagStartStealEvent(player, flag, FLAG_STEAL_TIMER);
        if (startStealEvent.call()) {
            return;
        }

        if (!stealTimers.containsKey(player)) {
            // The player has just approached the flag. Set up the initial timer.
            stealTimers.put(player, new Expiration().expireIn(startStealEvent.getRequiredStealTime()));
        } else if (stealTimers.get(player).isExpired()) {
            steal(player);
        }
    }

    /**
     * Needs to be called when a player is no longer near the flag.
     *
     * @param player The player who can no longer steal.
     */
    public void stopStealAttempt(Player player) {
        stealTimers.remove(player);
    }

    /**
     * Called when a player can successfully steal the flag.
     *
     * @param player The player stealing.
     */
    private void steal(Player player) {
        if (!new FlagStealEvent(player, flag).call()) {
            flag.steal(player);
        }
    }

    /**
     * Restores the flag, such as when the flag has been sitting on the ground too long.
     */
    public void restore() {
        flag.reset();
        new FlagRestoreEvent(flag).call();
    }

    /**
     * Used when a player picks up their own team's flag from the ground.
     *
     * @param player The player who picked up the flag.
     */
    public void pickup(Player player) {
        flag.pickup(player);
        new FlagPickupEvent(player, flag).call();
    }

    /**
     * Used when a player picks up their own flag from the ground.
     *
     * @param player The player who recovered the flag.
     */
    public void recover(Player player) {
        flag.reset();
        new FlagRecoverEvent(player, flag).call();
    }

    /**
     * Captures the flag.
     *
     * @param carrier The player who is carrying the flag.
     * @param scored  The team that the player is on, which should be rewarded points.
     */
    public void capture(Player carrier, BattleTeam scored) {
        flag.capture();
        scored.onCapture();
        new FlagCaptureEvent(carrier, scored, flag).call();
    }

    /**
     * Used when a player drops a flag, either voluntarily or involuntarily.
     *
     * @param player The player who dropped the flag.
     * @param item   The item they dropped, optionally.
     */
    public void drop(Player player, @Nullable Item item) {
        flag.drop(player.getEyeLocation(), item);
        new FlagDropEvent(player, flag, item).call();
    }

}
