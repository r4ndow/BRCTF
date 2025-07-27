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
        return (entity.getVelocity().getY() == GRAVITY || entity.getVelocity().getY() == -0.0)
            && entity.getLocation().add(0, -1, 0).getBlock().getType().isSolid();
    }

}
