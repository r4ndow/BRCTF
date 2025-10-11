package com.mcpvp.common.movement;

import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.util.EntityUtil;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

public class CancelNextFallTask implements EasyListener {

    @Getter
    private final Plugin plugin;
    private final Player player;

    public CancelNextFallTask(Plugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!this.player.isOnline()) {
            this.unregister();
            return;
        }

        if (EntityUtil.isOnGround(this.player)) {
            this.player.setFallDistance(0);
            this.unregister();
        } else {
            this.player.setFallDistance(-100);
        }
    }

}
