package com.mcpvp.common.task;

import com.mcpvp.common.time.Duration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class DrainExpBarTask extends BukkitRunnable {

    private final Player player;
    private final float perTick;
    private final int runsRequired;
    private int runs = 0;

    public DrainExpBarTask(Player player, Duration duration) {
        this.player = player;
        this.perTick = 1f / duration.toTicks();
        this.runsRequired = duration.ticks();
    }

    @Override
    public void run() {
        player.setExp(Math.max(player.getExp() - perTick, 0f));

        if (player.getExp() == 1f || runs++ == runsRequired) {
            cancel();
        }
    }

    public BukkitTask schedule(Plugin plugin) {
        return this.runTaskTimer(plugin, 0, 1);
    }

}