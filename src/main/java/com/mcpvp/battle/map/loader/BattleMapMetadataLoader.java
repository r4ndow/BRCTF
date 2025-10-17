package com.mcpvp.battle.map.loader;

import com.mcpvp.battle.config.BattleCallout;
import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.config.BattleTeamConfig;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.common.util.LookUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class BattleMapMetadataLoader implements BattleMapLoader {

    public BattleGameConfig parse(BattleMapData map, World world) {
        BattleGameConfig builder = new BattleGameConfig();
        BattleTeamConfig red = new BattleTeamConfig(1);
        BattleTeamConfig blue = new BattleTeamConfig(2);
        builder.getTeamConfigs().add(blue);
        builder.getTeamConfigs().add(red);

        for (String line : map.getMetadata().split("\n")) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }

            String key = line.split("=")[0];
            String value = line.split("=")[1];
            String domain = key.split(Pattern.quote("."))[0];
            String variable = key.split(Pattern.quote("."))[1];

            switch (domain) {
                case "Red", "Blue" -> {
                    BattleTeamConfig config = domain.equals("Red") ? red : blue;
                    switch (variable) {
                        case "Respawn" -> {
                            Location loc = this.findFirstSolidBlock(this.parseLocation(value, world))
                                .add(0.5, 1, 0.5);
                            config.setSpawn(loc);
                            builder.getCallouts().add(new BattleCallout(loc, config, "spawn"));
                        }
                        case "Chest" -> {
                            Location loc = this.parseLocation(value, world).add(0.5, 1, 0.5);
                            Block skull = this.parseLocation(value, world).getBlock().getRelative(BlockFace.UP);

                            if (skull.getState() instanceof Skull s) {
                                Location targ = skull.getRelative(s.getRotation()).getLocation();
                                Vector dir = LookUtil.lookAt(loc, targ).getDirection();
                                loc.setDirection(dir);
                                loc.getBlock().setType(Material.AIR);
                            }

                            config.setFlag(loc);
                            builder.getCallouts().add(new BattleCallout(loc, config, "flag"));
                        }
                        case "Face" -> {
                            BlockFace face = switch (value) {
                                case "n" -> BlockFace.NORTH;
                                case "e" -> BlockFace.EAST;
                                case "s" -> BlockFace.SOUTH;
                                case "w" -> BlockFace.WEST;
                                default -> null;
                            };
                            if (face != null) {
                                config.setSpawn(LookUtil.lookAt(config.getSpawn(), config.getSpawn().getBlock().getRelative(face).getLocation().add(0.5, 0, 0.5)));
                            }
                        }
                    }
                }
                case "CTF" -> {
                    switch (variable) {
                        case "CapturesToWin" -> builder.setCaps(Integer.parseInt(value));
                        case "Spawn" -> {
                            Location location = this.parseLocation(value, world);
                            int highestBlockYAt = world.getHighestBlockYAt(location.getBlockX(), location.getBlockZ());
                            location.setY(highestBlockYAt);
                            builder.setSpawn(location.add(0.5, 0, 0.5));
                        }
                        case "Time" -> builder.setTimeOfDay(Integer.parseInt(value));
                    }
                }
            }
        }

        return builder;
    }

    private Location findFirstSolidBlock(Location location) {
        if (location.getY() == 0) {
            throw new IllegalStateException(
                "Could not find a solid block at the spawn location"
            );
        }

        if (location.getBlock().getType().isSolid()) {
            return location;
        }

        return this.findFirstSolidBlock(location.subtract(0, 1, 0));
    }

    private Location parseLocation(String string, World world) {
        List<Integer> parts = Arrays.stream(string.trim().split(",")).mapToInt(Integer::parseInt).boxed().toList();
        return world.getBlockAt(parts.get(0), parts.get(1), parts.get(2)).getLocation();
    }

}
