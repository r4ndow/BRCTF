package com.mcpvp.battle.map.parser;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.config.BattleTeamConfig;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.util.LookUtil;

public class BattleMapLoaderMetadataImpl implements BattleMapLoader {
    
	public BattleGameConfig parse(BattleMapData map, World world) {
        BattleGameConfig builder = new BattleGameConfig();
        BattleTeamConfig red = new BattleTeamConfig(1);
        BattleTeamConfig blue = new BattleTeamConfig(2);
        builder.getTeamConfigs().add(blue);
        builder.getTeamConfigs().add(red);

        for (String line : map.getMetadata().split("\n")) {
            if (line.isEmpty() || line.startsWith("#")) {
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
                        case "Respawn" -> config.setSpawn(parseLocation(value, world).add(0.5, 0, 0.5));
                        case "Chest" -> config.setFlag(parseLocation(value, world).add(0.5, 1, 0.5));
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
                        case "Spawn" -> builder.setSpawn(parseLocation(value, world).add(0.5, 1, 0.5));
                    }
                }
            }
        }

        return builder;
    }

    private Location parseLocation(String string, World world) {
        List<Integer> parts = Arrays.stream(string.trim().split(",")).mapToInt(Integer::parseInt).boxed().toList();
        return world.getBlockAt(parts.get(0), parts.get(1), parts.get(2)).getLocation();
    }

    // private void processOldMeta() {
    //     BattleGameConfig cfg = this.config;
        
    //     Block highestBlock = this.world.getHighestBlockAt(0, 0);
        
    //     if (highestBlock != null)
    //         cfg.setInitialSpawn(highestBlock.getLocation().add(0.5, 1, 0.5));
        
    //     try (BufferedReader input = new BufferedReader(
    //             new StringReader(this.map.getMetadata()))) {
    //         String line;
    //         while ((line = input.readLine()) != null) {
    //             line = line.trim();
                
    //             if (line.startsWith("#"))
    //                 continue;
                
    //             String[] parts = line.split("=");
    //             if (parts.length != 2)
    //                 continue;
                
    //             String[] keys = parts[0].split("\\.");
    //             String value = parts[1];
                
    //             if (keys.length < 2)
    //                 continue;
                
    //             String teamName = keys[0].toLowerCase().trim();
    //             String property = keys[1].toLowerCase().trim();
                
    //             if (teamName.equalsIgnoreCase("ctf")) {
    //                 switch (property) {
    //                     case "capturestowin" -> cfg.setCapsToWin(Integer.parseInt(value));
    //                     case "time" -> cfg.setTimeOfDay(Integer.parseInt(value));
    //                     case "timelimit" -> cfg.setTimeLimit(Duration.mins(Integer.parseInt(value)));
    //                     case "immunity" -> cfg.setSpawnInvincibility(Duration.seconds(Integer.parseInt(value)));
    //                     case "spawn" -> {
    //                         Location location = parseLocation(value);
    //                         highestBlock = this.world.getHighestBlockAt(location.getBlockX(), location.getBlockZ());
    //                         if (highestBlock != null)
    //                             location = highestBlock.getLocation().add(0.5, 1, 0.5);
    //                         cfg.setInitialSpawn(location);
    //                     }
    //                 }
    //             } else {
    //                 ChatColor cc = ChatColor.valueOf(teamName.toUpperCase());
    //                 final BattleTeam team = Battle.getTeamManager().getTeamByColor(cc);
    //                 final BattleGameConfig.BattleTeamConfig tcfg = cfg.team(team);
                    
    //                 Validate.notNull(team, "No team for color " + cc);
                    
    //                 switch (property) {
    //                     case "respawn" -> {
    //                         tcfg.setSpawn(parseLocation(value));
    //                         tcfg.setSpawnMaterial(parseLocation(value).getBlock().getRelative(BlockFace.DOWN).getType());
    //                     }
    //                     case "chest" -> {
    //                         Location flag = parseLocation(value);
    //                         Block block = this.world.getBlockAt(flag);
                            
    //                         // Most flag posts have a skull on top of them to
    //                         // prevent pyro from burning the flag.
    //                         //  game can be used to determine which way the banner
    //                         // faces.
    //                         Block skull = block.getRelative(BlockFace.UP);
    //                         if (skull.getState() instanceof Skull)
    //                             tcfg.setRotation(((Skull) skull.getState()).getRotation());
    //                         skull.setType(Material.AIR);
    //                         block.setType(Material.FENCE);
    //                         flag.add(0.5, 1, 0.5);
    //                         tcfg.setFlagHome(flag);
    //                     }
    //                     case "flag" -> {
    //                         try {
    //                             tcfg.setSpawnMaterial(Material.getMaterial(Integer.parseInt(value)));
    //                         } catch (NumberFormatException e) {
    //                             tcfg.setSpawnMaterial(Material.valueOf(value.toUpperCase()));
    //                         }
    //                     }
    //                     case "face" -> {
    //                         switch (value) {
    //                             case "n" -> tcfg.setSpawnRotation(BlockFace.NORTH);
    //                             case "e" -> tcfg.setSpawnRotation(BlockFace.EAST);
    //                             case "s" -> tcfg.setSpawnRotation(BlockFace.SOUTH);
    //                             case "w" -> tcfg.setSpawnRotation(BlockFace.WEST);
    //                         }
    //                     }
    //                 }
    //             }
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
        
    //     this.world.setGameRuleValue("doDaylightCycle", "false");
    //     this.world.setTime(this.config.getTimeOfDay());
    //     this.world.setSpawnLocation(this.config.getInitialSpawn().getBlockX(), this.config.getInitialSpawn().getBlockY(), this.config.getInitialSpawn().getBlockZ());
    // }

}
