package com.mcpvp.common;

import com.mcpvp.common.event.EasyListener;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

/**
 * Allows different "live" objects to be attached, such as listeners. When {@link #shutdown()} is called,
 * they will automatically be unregistered. Calling {@link #attach(EasyListener)} also registers listeners.
 */
public class EasyLifecycle {

    private final Set<EasyListener> listeners = new HashSet<>();
    private final Set<BukkitTask> tasks = new HashSet<>();
    private final Set<EasyLifecycle> lifecycles = new HashSet<>();
    private final Set<Entity> entities = new HashSet<>();

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
     * Attaches the given task. It will be cancelled on {@link #shutdown()}.
     *
     * @param task The task to attach.
     */
    protected void attach(BukkitTask task) {
        this.tasks.add(task);
    }

    /**
     * Attaches the given lifecycle. It will be shutdown on {@link #shutdown()}.
     *
     * @param lifecycle The lifecycle to attach.
     */
    protected void attach(EasyLifecycle lifecycle) {
        this.lifecycles.add(lifecycle);
    }

    /**
     * Attaches the given entity. It will be removed on {@link #shutdown()}.
     *
     * @param entity The entity to attach.
     */
    protected void attach(Entity entity) {
        this.entities.add(entity);
    }

    /**
     * End this lifecycle, such as unregistering all listeners.
     */
    public void shutdown() {
        listeners.forEach(EasyListener::unregister);
        tasks.forEach(BukkitTask::cancel);
        lifecycles.forEach(EasyLifecycle::shutdown);
        entities.forEach(Entity::remove);
    }

}
