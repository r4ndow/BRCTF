package com.mcpvp.battle.map.parser;

import com.mcpvp.battle.config.BattleCallout;
import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.config.BattleTeamConfig;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.util.LookUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.material.Directional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Log4j2
public class BattleMapLoaderSignImpl implements BattleMapLoader {

    private static final String RE_SIGN = Pattern.quote("{{") + "(.*)" + Pattern.quote("}}");
    private static final String RE_VAL_SIGN = Pattern.quote("{{") + "(.*)=(.*)" + Pattern.quote("}}");
    private static final String RE_TEAM_SIGN = Pattern.quote("{{") + "(.*) (.*)" + Pattern.quote("}}");
    private static final String RE_CALLOUT_SIGN = Pattern.quote("{{") + "callout\\s?(\\d)?" + Pattern.quote("}}");

    public sealed interface MapSign permits SimpleMapSign, ValueMapSign, TeamMapSign, CalloutSign {
        Block getBlock();
    }
    
    @Data
    @Getter
    @AllArgsConstructor
    public static final class SimpleMapSign implements MapSign {
        private final Block block;
        private final String text;
    }
    
    @Data
    @Getter
    @AllArgsConstructor
    public static final class ValueMapSign implements MapSign {
        private final Block block;
        private final String key;
        private final String value;
    }
    
    @Data
    @Getter
    @AllArgsConstructor
    public static final class TeamMapSign implements MapSign {
        private final Block block;
        private final Integer team;
        private final String text;
    }
    
    /**
     * General:
     * ```
     * {{callout}}
     * Center
     * ```
     *
     * Team specific:
     * ```
     * {{callout 2}}
     * Spawn
     * ```
     */
    @Data
    @Getter
    @AllArgsConstructor
    public static final class CalloutSign implements MapSign {
        private final Block block;
        private final Integer team;
        private final String text;
    }
    
    @Override
    public BattleGameConfig parse(BattleMapData map, World world) {
        BattleGameConfig builder = new BattleGameConfig();
        // TODO
        builder.getTeamConfigs().add(new BattleTeamConfig(1));
        builder.getTeamConfigs().add(new BattleTeamConfig(2));
        log.info("Parsing map " + map);
        
        // Step 1: find center of the map
        // By default, we assume it's getSpawnLocation
        // But a `spawn_box` sign should override this
        
        // Assume spawn, load chunks
        // Check for new center, detect if changed
        // If changed: reload new chunks, process
        // Otherwise: keep old chunk list, process
        Location assumedCenter = world.getSpawnLocation();
        List<ChunkSnapshot> chunks = getChunkSnapshotsAround(assumedCenter, 16);
        chunks.forEach(cs -> this.highlightChunk(cs, world));
        List<MapSign> signs = this.getAllMapSigns(chunks);
        Optional<MapSign> spawnBox = signs
            .stream()
            .filter(ms -> ms instanceof SimpleMapSign sms && sms.getText().equals("spawn_box"))
            .findAny();
        Location foundCenter = spawnBox.map(ms -> ms.getBlock().getLocation()).orElse(assumedCenter);
        boolean spawnChanged = foundCenter.getBlockX() != assumedCenter.getBlockX() || foundCenter.getBlockZ() != assumedCenter.getBlockZ();

        builder.setSpawn(center(foundCenter));
        
        if (spawnChanged) {
            log.info("New spawn location found, re-loading chunks");
            chunks = getChunkSnapshotsAround(foundCenter, 16);
            signs = this.getAllMapSigns(chunks);
        }
        
        loadSignsIntoConfig(builder, signs);

        return builder;
    }
    
    private void loadSignsIntoConfig(
        BattleGameConfig builder, List<MapSign> signs
    ) {
        for (MapSign sign : signs) {
            if (sign instanceof TeamMapSign teamSign) {
                loadTeamSign(builder, teamSign);
            } else if (sign instanceof ValueMapSign valueSign) {
                loadValueSign(builder, valueSign);
            } else if (sign instanceof SimpleMapSign simpleSign) {
                loadSimpleSign(builder, simpleSign);
            } else if (sign instanceof CalloutSign calloutSign) {
                loadCalloutSign(builder, calloutSign);
            }
        }
    }
    
    private void loadSimpleSign(BattleGameConfig builder, SimpleMapSign sign) {
        switch (sign.getText()) {
            case "restrict" -> builder.getRestricted().add(sign.getBlock().getLocation());
            case "spawn_box" -> builder.setSpawn(sign.getBlock().getLocation());
            default -> log.warn("Unknown simple config given: " + sign);
        }
    }
    
    private void loadValueSign(BattleGameConfig builder, ValueMapSign sign) {
        switch (sign.getKey()) {
            case "caps" -> builder.setCaps(Integer.parseInt(sign.getValue()));
            case "time" -> builder.setTime(Integer.parseInt(sign.getValue()));
            default -> log.warn("Unknown value config given: " + sign);
        }
    }

    private void loadTeamSign(BattleGameConfig builder, TeamMapSign sign) {
        BattleTeamConfig teamConfig = builder.getTeamConfig(sign.getTeam());
        Location loc = sign.getBlock().getLocation();
		
		switch (sign.getText()) {
            case "spawn" -> teamConfig.setSpawn(center(loc));
            case "flag" -> teamConfig.setFlag(center(loc));
            default -> log.warn("Unknown team config given: " + sign);
        }
    }
    
    private void loadCalloutSign(BattleGameConfig builder, CalloutSign sign) {
        if (sign.getTeam() == null) {
            builder.getCallouts().add(new BattleCallout(sign.getBlock().getLocation(), null, sign.getText()));
        } else {
            BattleTeamConfig config = builder.getTeamConfig(sign.getTeam());
            builder.getCallouts().add(new BattleCallout(sign.getBlock().getLocation(), config, sign.getText()));
        }
    }

    private List<MapSign> getAllMapSigns(List<ChunkSnapshot> chunks) {
        return chunks.stream().flatMap(this::findSignBlocks).distinct().flatMap(b -> {
            if (!(b.getState() instanceof Sign sign)) {
                return Stream.empty();
            }
            log.info("Found sign to parse at " + b.getLocation() + " with text: " + Arrays.toString(sign.getLines()));
            return parseMapSign(b, sign).stream();
        }).toList();
    }

    private List<MapSign> parseMapSign(Block block, Sign sign) {
        List<MapSign> found = new ArrayList<>();

        for (String line : sign.getLines()) {
            if (line.isBlank()) {
                continue;
            }
            
            if (line.matches(RE_VAL_SIGN)) {
                found.add(new ValueMapSign(block, line.replaceAll(RE_VAL_SIGN, "$1"), line.replaceAll(RE_VAL_SIGN, "$2")));
            } else if (line.matches(RE_CALLOUT_SIGN)) {
                String teamId = line.replaceAll(RE_CALLOUT_SIGN, "$1");
                if (teamId.isBlank()) {
                    found.add(new CalloutSign(block, null, sign.getLine(1)));
                } else {
                    found.add(new CalloutSign(block, Integer.parseInt(teamId), sign.getLine(1)));
                }
            } else if (line.matches(RE_TEAM_SIGN)) {
                found.add(new TeamMapSign(block, Integer.parseInt(line.replaceAll(RE_TEAM_SIGN, "$2")), line.replaceAll(RE_TEAM_SIGN, "$1")));
            } else if (line.matches(RE_SIGN)) {
                found.add(new SimpleMapSign(block, line.replaceAll(RE_SIGN, "$1")));
            }
        }

        return found;
    }

    private Stream<Block> findSignBlocks(ChunkSnapshot snapshot) {
        // TODO might need Material.WALL_SIGN here
        return findBlocks(snapshot, m -> m == Material.SIGN_POST).stream().distinct();
    }

    @SuppressWarnings("deprecation")
    private List<Block> findBlocks(ChunkSnapshot chunk, Predicate<Material> filter) {
        List<Block> result = new ArrayList<>();

        for (int x = 0; x != 16; x++)
            for (int z = 0; z != 16; z++)
                for (int y = 0; y != 256; y++) {
                    int absX = chunk.getX() * 16 + x;
                    int absZ = chunk.getZ() * 16 + z;
                    Material m = Material.getMaterial(chunk.getBlockTypeId(x, y, z));
                    if (filter.test(m)) {
                        result.add(new Location(Bukkit.getWorld(chunk.getWorldName()), absX, y, absZ).getBlock());
                    }
                }

        return result;
    }

    private List<ChunkSnapshot> getChunkSnapshotsAround(Location center, int radius) {
        World world = center.getWorld();
        int cX = ((int) center.getX());
        int cZ = ((int) center.getZ());
        List<ChunkSnapshot> chunkSnapshots = new ArrayList<>();
        for (int x = 0; x < radius; x++) {
            for (int z = 0; z < radius; z++) {
                chunkSnapshots.add(world.getChunkAt(cX + x, cZ + z).getChunkSnapshot());
                chunkSnapshots.add(world.getChunkAt(cX - x, cZ + z).getChunkSnapshot());
                chunkSnapshots.add(world.getChunkAt(cX + x, cZ - z).getChunkSnapshot());
                chunkSnapshots.add(world.getChunkAt(cX - x, cZ - z).getChunkSnapshot());
            }
        }
        return chunkSnapshots;
    }
    
    private void highlightChunk(ChunkSnapshot snapshot, World world) {
        world.getChunkAt(snapshot.getX(), snapshot.getZ()).getBlock(10, 128, 10).setType(Material.DIAMOND_BLOCK);
    }
    
    private Location center(Location loc) {
		if (loc.getBlock().getState().getData() instanceof Directional) {
			loc = getSignLocation(loc);
		}
        return loc.clone().add(0.5, 0, 0.5);
    }
	
	private Location getSignLocation(Location sign) {
		Directional dir = (Directional) sign.getBlock().getState().getData();
		Location targ = sign.getBlock().getRelative(dir.getFacing()).getLocation();
		return LookUtil.lookAt(sign, targ);
	}
    
}
