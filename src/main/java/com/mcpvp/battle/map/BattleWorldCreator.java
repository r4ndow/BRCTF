package com.mcpvp.battle.map;

import com.mcpvp.battle.util.EmptyGenerator;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.io.IOException;

/**
 * Creates a Minecraft world given map data and an index (used for the name).
 * This step comes before map parsing.
 */
public class BattleWorldCreator {
	
	public static World create(
		BattleMapData map, File mapsDir, int index
	) throws IOException {
		String worldName = "game_" + (index + 1) + "_world";
		
		// Ensure no weird issues when a new map is extracted
		File worldDir = new File(Bukkit.getWorldContainer(), worldName);
		if (worldDir.exists()) {
			FileUtils.deleteDirectory(worldDir);
		}
		
		// Find the map directory that needs to be copied
		File mapDir = new File(mapsDir, map.getFile());
		if (!mapDir.exists()) {
			throw new IllegalStateException("Map file doesn't exist: " + mapDir);
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
	
	private static void initializeWorld(World world) {
		world.setAutoSave(false);
		world.setGameRuleValue("naturalRegeneration", "false");
		world.setGameRuleValue("doDaylightCycle", "false");
	}
	
}
