package com.mcpvp.common.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class EffectUtil {

    public static void fakeLightning(Location location) {
        World world = location.getWorld();
        Block block = world.getHighestBlockAt(location.getBlockX(), location.getBlockZ());
        if (block != null) {
            location = location.clone();
            location.setY(Math.min(world.getMaxHeight(), block.getY() ));
            world.strikeLightningEffect(location);
        }
    }

}
