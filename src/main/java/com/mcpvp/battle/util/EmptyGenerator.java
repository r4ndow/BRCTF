package com.mcpvp.battle.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class EmptyGenerator extends ChunkGenerator {
	
	@Override
	public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomes) {
		for (int i = 0; i < 16; i++) {
			for (int k = 0; k < 16; k++) {
				biomes.setBiome(i, k, Biome.PLAINS);
			}
		}
		return new byte[world.getMaxHeight() >> 4][];
	}
	
	@Override
	public boolean canSpawn(World world, int x, int z) {
		return true;
	}
	
	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		return new Location(world, 0, 64, 0);
	}
	
}