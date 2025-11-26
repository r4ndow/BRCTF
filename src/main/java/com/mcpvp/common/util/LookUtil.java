package com.mcpvp.common.util;

import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LookUtil {

    public static Location lookAt(Location loc, Location lookAt) {
        //Clone the loc to prevent applied changes to the input loc
        loc = loc.clone();

        // Values of change in distance (make it relative)
        double dx = lookAt.getX() - loc.getX();
        double dy = lookAt.getY() - loc.getY();
        double dz = lookAt.getZ() - loc.getZ();

        // Set yaw
        if (dx != 0) {
            // Set yaw start value based on dx
            if (dx < 0) {
                loc.setYaw((float) (1.5 * Math.PI));
            } else {
                loc.setYaw((float) (0.5 * Math.PI));
            }
            loc.setYaw(loc.getYaw() - (float) Math.atan(dz / dx));
        } else if (dz < 0) {
            loc.setYaw((float) Math.PI);
        }

        // Get the distance from dx/dz
        double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));

        // Set pitch
        loc.setPitch((float) -Math.atan(dy / dxz));

        // Set values, convert to degrees (invert the yaw since Bukkit uses a different yaw dimension format)
        loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI);
        loc.setPitch(loc.getPitch() * 180f / (float) Math.PI);

        return loc;
    }

    public static Optional<Player> getFirstPlayerInLineOfSight(Player viewer) {
        List<Player> candidates = viewer.getNearbyEntities(100, 100, 100).stream()
            .filter(Player.class::isInstance)
            .map(Player.class::cast)
            .filter(viewer::hasLineOfSight)
            .toList();

        return traverse(viewer.getEyeLocation().toVector(), viewer.getEyeLocation().getDirection(), 100, 0.35)
            .stream()
            .map(vector -> {
                return candidates.stream().filter(candidate -> {
                    return new BoundingBox(candidate, 0.25).isIntersectedBy(vector);
                }).findFirst();
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    public static List<Vector> traverse(Vector origin, Vector direction, double blocksAway, double accuracy) {
        List<Vector> positions = new ArrayList<>();
        for (double d = 0; d <= blocksAway; d += accuracy) {
            positions.add(direction.clone().normalize().multiply(d).add(origin));
        }
        return positions;
    }

    public static class BoundingBox {

        private final Vector max;
        private final Vector min;

        public BoundingBox(Entity entity, double buffer) {
            AxisAlignedBB bb = ((CraftEntity) entity).getHandle().getBoundingBox().grow(buffer, buffer, buffer);
            this.min = new Vector(bb.a, bb.b, bb.c);
            this.max = new Vector(bb.d, bb.e, bb.f);
        }

        public boolean isIntersectedBy(Vector position) {
            if (position.getX() < this.min.getX() || position.getX() > this.max.getX()) {
                return false;
            } else if (position.getY() < this.min.getY() || position.getY() > this.max.getY()) {
                return false;
            } else if (position.getZ() < this.min.getZ() || position.getZ() > this.max.getZ()) {
                return false;
            }
            return true;
        }

    }

}
