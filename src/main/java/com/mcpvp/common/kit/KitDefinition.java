package com.mcpvp.common.kit;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface KitDefinition extends KitInfo {

    Kit create(Plugin plugin, @Nullable Player player);
    
}
