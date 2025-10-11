package com.mcpvp.common.task;

import com.mcpvp.common.time.Duration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class DrainExpBarTask extends BukkitRunnable implements ExpBarTask {

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
        this.player.setExp(Math.max(this.player.getExp() - this.perTick, 0f));

        if (this.player.getExp() == 1f || this.runs++ == this.runsRequired) {
            this.cancel();
        }
    }

    public BukkitTask schedule(Plugin plugin) {
        return this.runTaskTimer(plugin, 0, 1);
    }

}