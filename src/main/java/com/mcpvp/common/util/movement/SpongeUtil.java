package com.mcpvp.common.util.movement;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class SpongeUtil {

    private SpongeUtil() {
    }

    public static void launch(Plugin plugin, Player player, Block block) {
        launch(plugin, player, block, null);
    }

    public static void launch(Plugin plugin, Player player, Block block, Runnable task) {
        Vector velocity = new Vector(0, getSpongeDepth(block), 0);
        velocity = velocity.add(new Vector(getSpongeDepth(block.getRelative(-1, -1, 0)), 0, 0));
        velocity = velocity.add(new Vector(-getSpongeDepth(block.getRelative(1, -1, 0)), 0, 0));
        velocity = velocity.add(new Vector(0, 0, getSpongeDepth(block.getRelative(0, -1, -1))));
        velocity = velocity.add(new Vector(0, 0, -getSpongeDepth(block.getRelative(0, -1, 1))));
        velocity = velocity.multiply(10);
        new VelocityManager(plugin, player, velocity, task);
    }

    private static int getSpongeDepth(Block block) {
        int depth = 0;
        while (block.getType() == Material.SPONGE) {
            depth++;
            if (block.getY() == 0 || depth > 10)
                break;
            block = block.getRelative(BlockFace.DOWN);
        }
        return depth;
    }
}
