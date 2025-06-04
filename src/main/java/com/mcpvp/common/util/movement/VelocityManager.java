package com.mcpvp.common.util.movement;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
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

    public static boolean smooth = false;

    public static final double X_INTENSITY = 3;
    public static final double Y_INTENSITY = 1;
    private final Vector vector;
    private final Entity player;
    private final BukkitTask task;
    private final Runnable afterLaunch;

    public VelocityManager(Plugin plugin, Entity player, Vector vector, Runnable afterLaunch) {
        this.player = player;
        this.vector = vector;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 1);
        this.afterLaunch = afterLaunch;
        new CancelNextFallTask(plugin, (Player) player);
    }

    @Override
    public void run() {
        if (((player instanceof Player p) && !p.isOnline()) || player.isDead()) {
            task.cancel();
            return;
        }
        int x = vector.getBlockX();
        int y = vector.getBlockY();
        int z = vector.getBlockZ();
        if (x == 0 && y == 0 && z == 0) {
            if (!smooth)
                player.setVelocity(new Vector(0, 0, 0));

            task.cancel();

            if (afterLaunch != null)
                afterLaunch.run();

            return;
        }
        Vector result = new Vector();
        if (x > 0) {
            vector.setX(x - 1);
            result.setX(result.getX() + X_INTENSITY);
        } else if (x < 0) {
            vector.setX(x + 1);
            result.setX(result.getX() - X_INTENSITY);
        }
        if (y > 0) {
            vector.setY(y - 1);
            result.setY(result.getY() + Y_INTENSITY);
        } else if (y < 0) {
            vector.setY(y + 1);
            result.setY(result.getY() - Y_INTENSITY);
        }
        if (z > 0) {
            vector.setZ(z - 1);
            result.setZ(result.getZ() + X_INTENSITY);
        } else if (z < 0) {
            vector.setZ(z + 1);
            result.setZ(result.getZ() - X_INTENSITY);
        }

        player.setVelocity(result);
    }

}
