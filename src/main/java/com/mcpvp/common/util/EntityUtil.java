package com.mcpvp.common.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;

public class EntityUtil {

    public static final double GRAVITY = -0.0784000015258789;

    public static <T extends Entity> List<T> getNearbyEntities(
            Location location, Class<T> type, double distance
    ) {
        return getNearbyEntities(location, type, distance, distance, distance);
    }

    public static <T extends Entity> List<T> getNearbyEntities(
            Location location, Class<T> type, double x, double y, double z
    ) {
        //noinspection unchecked
        return location.getWorld().getNearbyEntities(location, x, y, z).stream()
                .filter(e -> type.isAssignableFrom(e.getClass()))
                .map(e -> (T) e)
                .toList();
    }

    public static boolean isOnGround(Entity entity) {
        // https://www.spigotmc.org/threads/how-to-actually-detect-which-block-a-player-is-standing-on.492043/
        // https://www.spigotmc.org/threads/check-if-player-is-on-ground.606724/
        return entity.getVelocity().getY() == GRAVITY && entity.isOnGround();
    }

}
