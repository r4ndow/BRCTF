package com.mcpvp.common;

import java.util.HashSet;
import java.util.Set;

import com.mcpvp.common.event.EasyListener;

/**
 * Allows different "live" objects to be attached, such as listeners. When {@link #shutdown()} is called,
 * they will automatically be unregistered. Calling {@link #attach(EasyListener)} also registers listeners.
 */
public class EasyLifecycle {

	private final Set<EasyListener> listeners = new HashSet<>();
	
	/**
	 * Registers the given listener. It will be unregistered on {@link #shutdown()}.
	 * 
	 * @param listener The listener to register.
	 */
	protected void attach(EasyListener listener) {
		this.listeners.add(listener);
		listener.register();
	}

	/**
	 * End this lifecycle, such as unregistering all listeners.
	 */
	public void shutdown() {
		listeners.forEach(EasyListener::unregister);
	}
	
}
