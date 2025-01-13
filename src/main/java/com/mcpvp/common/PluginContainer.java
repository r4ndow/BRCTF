package com.mcpvp.common;

import org.bukkit.plugin.Plugin;

/**
 * Any object that has a reference to a plugin.
 */
public interface PluginContainer {

    /**
     * @return The relevant plugin.
     */
    Plugin getPlugin();

}
