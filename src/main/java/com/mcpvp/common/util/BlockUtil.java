package com.mcpvp.common.util;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockUtil {

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

    /**
     * @param center The center to get locations around.
     * @param radius The maximum distance from the center.
     * @param height The maximum distance in the Y axis.
     * @return An Array of all the Locations within the given (spherical) distance
     * of the center.
     */
    public static List<Block> getBlocksInRadius(Block center, int radius, int height) {
        List<Block> blocks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -height; y <= height; y++) {
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
