package com.mcpvp.common.kit;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface KitDefinition extends KitInfo {

    Kit create(Plugin plugin, Player player);

}
