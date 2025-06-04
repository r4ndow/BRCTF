package com.mcpvp.common.util.movement;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class CancelNextFallTask implements Runnable {

    private static final double GRAVITY = -0.0784000015258789;

    private final Player player;
    private final BukkitTask task;

    public CancelNextFallTask(Plugin plugin, Player player) {
        this.player = player;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 1);
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            task.cancel();
        }

        player.setFallDistance(-100);

        if (isOnGround(player)) {
            player.setFallDistance(0);
            task.cancel();
        }
    }

    protected boolean isOnGround(Entity entity) {
        return (entity.getVelocity().getY() == GRAVITY || entity.getVelocity().getY() == -0.0)
                && entity.getLocation().add(0, -1, 0).getBlock().getType().isSolid();
    }

}
