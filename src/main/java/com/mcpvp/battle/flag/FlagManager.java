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
        FlagStartStealEvent startStealEvent = new FlagStartStealEvent(player, this.flag, FLAG_STEAL_TIMER);
        if (startStealEvent.callIsCancelled()) {
            return;
        }

        if (!this.stealTimers.containsKey(player)) {
            // The player has just approached the flag. Set up the initial timer.
            this.stealTimers.put(player, Expiration.after(startStealEvent.getRequiredStealTime()));
        } else if (this.stealTimers.get(player).isExpired()) {
            this.steal(player);
        }
    }

    /**
     * Needs to be called when a player is no longer near the flag, or should otherwise
     * have their steal timer reset.
     *
     * @param player The player who needs to restart stealing.
     */
    public void resetStealTimer(Player player) {
        this.stealTimers.remove(player);
    }

    /**
     * Called when a player can successfully steal the flag.
     *
     * @param player The player stealing.
     */
    private void steal(Player player) {
        if (!new FlagStealEvent(player, this.flag).callIsCancelled()) {
            this.flag.steal(player);
        }
    }

    /**
     * Used when a player picks up their own team's flag from the ground.
     *
     * @param player The player who picked up the flag.
     */
    public void pickup(Player player) {
        if (!new FlagPickupEvent(player, this.flag).callIsCancelled()) {
            this.flag.pickup(player);
        }
    }

    /**
     * Restores the flag, such as when the flag has been sitting on the ground too long.
     */
    public void restore() {
        this.flag.reset();
        new FlagRestoreEvent(this.flag).call();
    }

    /**
     * Used when a player picks up their own flag from the ground.
     *
     * @param player The player who recovered the flag.
     */
    public void recover(Player player) {
        this.flag.reset();
        new FlagRecoverEvent(player, this.flag).call();
    }

    /**
     * Captures the flag.
     *
     * @param carrier The player who is carrying the flag.
     * @param scored  The team that the player is on, which should be rewarded points.
     */
    public void capture(Player carrier, BattleTeam scored) {
        this.flag.capture();
        scored.onCapture();
        new FlagCaptureEvent(carrier, scored, this.flag).call();
    }

    /**
     * Used when a player drops a flag, either voluntarily or involuntarily.
     *
     * @param player The player who dropped the flag.
     * @param item   The item they dropped, optionally.
     */
    public void drop(Player player, @Nullable Item item) {
        this.flag.drop(player.getEyeLocation(), item);
        new FlagDropEvent(player, this.flag, item).call();
    }

}
