package com.mcpvp.common.event;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.mcpvp.common.PluginContainer;

public interface EasyListener extends Listener, PluginContainer {
	
	default void register() {
		Bukkit.getPluginManager().registerEvents(this, getPlugin());
	}
	
	default void unregister() {
		HandlerList.unregisterAll(this);
	}
	
}
