package com.mcpvp.common.movement;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Manages velocity from sponge launches via {@link SpongeUtil}.
 */
public class VelocityManager implements Runnable {

    private static final boolean smooth = false;

    public static final double X_INTENSITY = 3;
    public static final double Y_INTENSITY = 1;
    private final Vector vector;
    private final Entity player;
    private final BukkitTask task;
    private final Runnable afterLaunch;

    public VelocityManager(Plugin plugin, Entity player, Vector vector, Runnable afterLaunch) {
        this.player = player;
        this.vector = vector;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 1);
        this.afterLaunch = afterLaunch;
        new CancelNextFallTask(plugin, (Player) player).register();
    }

    @Override
    public void run() {
        if (((this.player instanceof Player p) && !p.isOnline()) || this.player.isDead()) {
            this.task.cancel();
            return;
        }
        int x = this.vector.getBlockX();
        int y = this.vector.getBlockY();
        int z = this.vector.getBlockZ();
        if (x == 0 && y == 0 && z == 0) {
            if (!smooth) {
                this.player.setVelocity(new Vector(0, 0, 0));
            }

            this.task.cancel();

            if (this.afterLaunch != null) {
                this.afterLaunch.run();
            }

            return;
        }
        Vector result = new Vector();
        if (x > 0) {
            this.vector.setX(x - 1);
            result.setX(result.getX() + X_INTENSITY);
        } else if (x < 0) {
            this.vector.setX(x + 1);
            result.setX(result.getX() - X_INTENSITY);
        }
        if (y > 0) {
            this.vector.setY(y - 1);
            result.setY(result.getY() + Y_INTENSITY);
        } else if (y < 0) {
            this.vector.setY(y + 1);
            result.setY(result.getY() - Y_INTENSITY);
        }
        if (z > 0) {
            this.vector.setZ(z - 1);
            result.setZ(result.getZ() + X_INTENSITY);
        } else if (z < 0) {
            this.vector.setZ(z + 1);
            result.setZ(result.getZ() - X_INTENSITY);
        }

        this.player.setVelocity(result);
    }

}
