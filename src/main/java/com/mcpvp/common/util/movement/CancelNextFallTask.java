package com.mcpvp.common.util.movement;

import com.mcpvp.common.util.EntityUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class CancelNextFallTask implements Runnable {

    private final Player player;
    @Getter
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

        if (EntityUtil.isOnGround(player)) {
            player.setFallDistance(0);
            task.cancel();
        }
    }

}
