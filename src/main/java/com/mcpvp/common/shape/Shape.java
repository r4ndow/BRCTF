package com.mcpvp.common.shape;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

/**
 * A three dimensional area in Minecraft.
 */
public abstract class Shape {

    /**
     * @param location The location to check.
     * @return True if the given location is inside of the Shape.
     */
    public abstract boolean contains(Location location);

    /**
     * Shortcut method that checks if the shape contains the player's location.
     *
     * @param entity The entity to check.
     * @return True if the player is inside the shape.
     */
    public boolean contains(Entity entity) {
        return contains(entity.getLocation());
    }

    /**
     * Shortcut method that checks if the shape contains the block's location.
     *
     * @param block The block to check.
     * @return True if the block is inside the shape.
     */
    public boolean contains(Block block) {
        return contains(block.getLocation());
    }

    /**
     * @return All the "faces" of the Shape. If the shape were a glass tank of
     *         water, this would return the glass.
     */
    public abstract Iterable<Location> getFaces();

}