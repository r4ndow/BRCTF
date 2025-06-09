package com.mcpvp.common.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockUtil {

    /**
     * @param block  The block to center the search around.
     * @param type   The type of Material to look for.
     * @param radius The radius to search in.
     * @return The closest Location of a block of the given type, or null if none is
     * found in the given radius.
     */
    public static Block getNearestType(Block block, Material type, int radius) {
        for (int i = 0; i < radius; i++) {
            Block closest = null;

            for (Block nearby : getBlocksInRadius(block, radius)) {
                if (nearby.getType() == type) {
                    if (closest == null || closest.getLocation().distanceSquared(block.getLocation()) > nearby
                            .getLocation().distanceSquared(block.getLocation()))
                        closest = nearby;
                }
            }

            if (closest != null)
                return closest;
        }
        return null;
    }

    /**
     * @param location The location to center the search around.
     * @param type     The type of Material to look for.
     * @param radius   The radius to search in.
     * @return The closest Location of a block of the given type, or null if none is
     * found in the given radius.
     */
    public static Block getNearestType(Location location, Material type, int radius) {
        for (int i = 0; i < radius; i++) {
            Block closest = null;

            for (Block nearby : getBlocksInRadius(location.getBlock(), radius)) {
                if (nearby.getType() == type) {
                    if (closest == null || closest.getLocation().add(0.5, 0.5, 0.5).distanceSquared(location) > nearby
                            .getLocation().add(0.5, 0.5, 0.5).distanceSquared(location))
                        closest = nearby;
                }
            }

            if (closest != null)
                return closest;
        }
        return null;
    }

    /**
     * @param center The center to get locations around.
     * @param radius The maximum distance from the center.
     * @return An Array of all the Locations within the given (spherical) distance
     * of the center.
     */
    public static List<Block> getBlocksInRadius(Block center, int radius) {
        List<Block> blocks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    Block block = center.getRelative(x, y, z);
                    if (center.getLocation().distance(block.getLocation()) <= radius) {
                        blocks.add(block);
                    }
                }
            }
        }
        return blocks;
    }

}
