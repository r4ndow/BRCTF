package com.mcpvp.common.task;

import org.bukkit.scheduler.BukkitRunnable;

public class EasyTask {

    /**
     * Creates a task which receives a reference that can be cancelled. This allows tasks to cancel
     * themselves.
     *
     * @param runner The runner which will receive a cancellable reference.
     * @return The runnable, which still needs to be scheduled.
     */
    public static BukkitRunnable of(EasyTaskRunner runner) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                runner.run(this::cancel);
            }
        };
    }

    /**
     * Creates a task which can be easily scheduled.
     *
     * @param runnable The task to run.
     * @return The runnable, which still needs to be scheduled.
     */
    public static BukkitRunnable of(Runnable runnable) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        };
    }

    public interface EasyTaskRunner {
        void run(EasyTaskReference reference);
    }

    public interface EasyTaskReference {
        /**
         * Cancels the enclosing Bukkit task. This task will not run again.
         */
        void cancel();
    }

}
