package com.mcpvp.common.util.movement;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MovementUtil {

    /**
     * @return The force of gravity that acts on all entities every tick. If an
     * entity is on the ground, this is their Y velocity.
     */
    public static final double GRAVITY = -0.0784000015258789;

    /**
     * Sets the fall distance to -2 of the player until
     * {@link Util#isOnGround(Entity)} returns true. This way, their next fall
     * damage event is cancelled.
     *
     * @param player The player to cancel the fall damage for.
     */
    public static void cancelNextFall(Plugin plugin, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && !isOnGround(player.getPlayer()))
                    player.setFallDistance(-2);
                else
                    cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /**
     * @param entity The entity to check.
     * @return True if the player has the velocity of a player standing on the
     * ground, and there is a block other than air below them.
     */
    public static boolean isOnGround(Entity entity) {
        return (entity.getVelocity().getY() == GRAVITY || entity.getVelocity().getY() == -0.0)
                && entity.getLocation().add(0, -1, 0).getBlock().getType().isSolid();
    }

}
