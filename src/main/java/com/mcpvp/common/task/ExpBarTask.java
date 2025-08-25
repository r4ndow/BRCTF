package com.mcpvp.common.task;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public sealed interface ExpBarTask extends Runnable permits DrainExpBarTask, FillExpBarTask {

    BukkitTask schedule(Plugin plugin);

}
