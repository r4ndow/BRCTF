package com.mcpvp.common.event;

import com.mcpvp.common.PluginContainer;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public interface EasyListener extends Listener, PluginContainer {

    default EasyListener register() {
        Bukkit.getPluginManager().registerEvents(this, this.getPlugin());
        return this;
    }

    default void unregister() {
        HandlerList.unregisterAll(this);
    }

}
