package com.mcpvp.battle.map;

import com.mcpvp.battle.util.EmptyGenerator;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Creates a Minecraft world given map data and an index (used for the name).
 * This step comes before map parsing.
 */
@Log4j2
public class BattleWorldManager {

    private static final String WORLD_PREFIX = "ctf_game_world_";

    public static World create(
        BattleMapData map, File mapsDir, int index
    ) throws IOException {
        String worldName = WORLD_PREFIX + index;

        // Ensure no weird issues when a new map is extracted
        File worldDir = new File(Bukkit.getWorldContainer(), worldName);
        if (worldDir.exists()) {
            FileUtils.deleteDirectory(worldDir);
        }

        // Find the map directory that needs to be copied
        File mapDir = new File(mapsDir, map.getFile());
        if (!mapDir.exists()) {
            throw new FileNotFoundException("Map file doesn't exist: " + mapDir);
        }

        // Copy the map directory
        FileUtils.copyDirectory(mapDir, worldDir);

        // Create the world
        World world = new WorldCreator(worldName)
            .generateStructures(false)
            .generator(new EmptyGenerator())
            .environment(World.Environment.NORMAL)
            .type(WorldType.FLAT)
            .createWorld();

        // Apply some rules
        initializeWorld(world);

        return world;
    }

    public static void cleanUpWorlds() throws IOException {
        try (Stream<Path> paths = Files.list(Bukkit.getWorldContainer().toPath())) {
            paths
                .map(Path::toFile)
                .filter(File::isDirectory)
                .filter(file -> file.getName().startsWith(WORLD_PREFIX))
                .forEach(f -> {
                    try {
                        FileUtils.deleteDirectory(f);
                    } catch (IOException e) {
                        log.warn("Failed to delete old map file at {}", f.getAbsolutePath(), e);
                    }
                });
        }

    }

    private static void initializeWorld(World world) {
        world.setAutoSave(false);
        world.setGameRuleValue("naturalRegeneration", "false");
        world.setGameRuleValue("doDaylightCycle", "false");
    }

}
