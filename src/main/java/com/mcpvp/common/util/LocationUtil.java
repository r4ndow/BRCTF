package com.mcpvp.common.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LocationUtil {

    public static List<Location> trace(Location from, Location to, int count) {
        List<Location> locations = new ArrayList<>();
        Location progress = from.clone();
        Vector movement = to.toVector().subtract(from.toVector()).multiply(1d / count);
        for (int i = 0; i < count; i++) {
            progress.add(movement);
            locations.add(progress.clone());
        }
        return locations;
    }

}
