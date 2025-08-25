package com.mcpvp.common.shape;

import lombok.Getter;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

/**
 * <code>
 * c1 - - - +<br>
 *  |&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br>
 *  |&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br>
 *  + - - - c2<br>
 *  </code>
 */
@Getter
public class Cuboid extends Shape {

    private final Location corner1;
    private final Location corner2;

    public Cuboid(Location corner1, Location corner2) {
        if (corner1.getWorld() != corner2.getWorld())
            throw new IllegalArgumentException("corners must be in the same world");
        this.corner1 = corner1;
        this.corner2 = corner2;
    }

    @Override
    public boolean contains(Location location) {
        if (location == null)
            throw new IllegalArgumentException("location can't be null");
        if (corner1 == null)
            throw new NullPointerException("corner1 can't be null");
        if (location.getWorld() != corner1.getWorld())
            return false;
        // compare using the first corner only
        if (corner2 == null) {
            if (corner1.getBlockX() != location.getBlockX())
                return false;
            if (corner1.getBlockY() != location.getBlockY())
                return false;
            return corner1.getBlockZ() == location.getBlockZ();
        }
        if (!isWithinCoords(corner1.getBlockX(), corner2.getBlockX(), location.getBlockX()))
            return false;
        if (!isWithinCoords(corner1.getBlockY(), corner2.getBlockY(), location.getBlockY()))
            return false;
        return isWithinCoords(corner1.getBlockZ(), corner2.getBlockZ(), location.getBlockZ());
    }

    private boolean isWithinCoords(double coord1, double coord2, double value) {
        if (coord1 > coord2)
            return (value <= coord1 && value >= coord2);
        return (value <= coord2 && value >= coord1);
    }

    /**
     * Get the the centre of the Cuboid
     *
     * @return Location at the centre of the Cuboid
     */
    public Location getCenter() {
        int x1 = corner1.getBlockX();
        int x2 = corner2.getBlockX();

        int y1 = corner1.getBlockY();
        int y2 = corner2.getBlockY();

        int z1 = corner1.getBlockZ();
        int z2 = corner2.getBlockZ();

        return new Location(corner1.getWorld(), (x1 + x2) / 2, (y1+y2) / 2, (z1+z2) / 2);
    }

    @Override
    public Set<Location> getFaces() {
        Set<Location> faceLocations = new HashSet<>();
        if (corner1 == null)
            throw new NullPointerException("corner1 can't be null");
        if (corner2 == null) {
            faceLocations.add(corner1);
            return faceLocations;
        }
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++) {
                Location minZFace = new Location(corner1.getWorld(), x, y, minZ);
                Location maxZFace = new Location(corner2.getWorld(), x, y, maxZ);
                faceLocations.add(minZFace);
                faceLocations.add(maxZFace);
            }
        for (int z = minZ; z <= maxZ; z++)
            for (int y = minY; y <= maxY; y++) {
                Location minXFace = new Location(corner1.getWorld(), minX, y, z);
                Location maxXFace = new Location(corner1.getWorld(), maxX, y, z);
                faceLocations.add(minXFace);
                faceLocations.add(maxXFace);
            }
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                Location minYFace = new Location(corner1.getWorld(), x, minY, z);
                Location maxYFace = new Location(corner1.getWorld(), x, maxY, z);
                faceLocations.add(minYFace);
                faceLocations.add(maxYFace);
            }
        return faceLocations;
    }

}