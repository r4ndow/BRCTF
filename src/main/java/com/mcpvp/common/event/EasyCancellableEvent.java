package com.mcpvp.common.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EasyCancellableEvent extends Event implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();
	
	private boolean cancelled = false;
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	// Required by Bukkit
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public boolean call() {
		Bukkit.getPluginManager().callEvent(this);
		return isCancelled();
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean b) {
		this.cancelled = b;
	}
	
	public void cancel() {
		setCancelled(true);
	}
	
}
