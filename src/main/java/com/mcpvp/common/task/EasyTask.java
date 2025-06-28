package com.mcpvp.common.task;

import org.bukkit.scheduler.BukkitRunnable;

public class EasyTask {

    public static BukkitRunnable of(EasyTaskRunner runner) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                runner.run(this::cancel);
            }
        };
    }

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
        void cancel();
    }

}
