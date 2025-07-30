package com.mcpvp.common.util.movement;

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
        if (!player.isOnline()) {
            unregister();
            return;
        }

        if (EntityUtil.isOnGround(player)) {
            player.setFallDistance(0);
            unregister();
        } else {
            player.setFallDistance(-100);
        }
    }

}
