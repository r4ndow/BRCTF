package com.mcpvp.battle.match;

import com.mcpvp.common.structure.StructureManager;
import com.mcpvp.common.structure.StructureViolation;
import com.mcpvp.common.util.BlockUtil;
import org.bukkit.Material;

import java.util.List;
import java.util.Optional;

public class BattleMatchStructureRestrictions {

    public static final String IN_SPAWN = "IN_SPAWN";
    public static final String NEAR_SPAWN = "NEAR_SPAWN";
    public static final String NEAR_RESTRICTED = "NEAR_RESTRICTED";
    public static final String NEAR_FLAG = "NEAR_FLAG";
    public static final String IN_FLAG = "IN_FLAG";
    public static final String NEAR_PLAYER = "NEAR_PLAYER";

    private static final List<Material> RESTRICT_NEARBY = List.of(
        Material.PISTON_MOVING_PIECE, Material.MONSTER_EGGS, Material.BARRIER
    );
    private static final int SPAWN_RANGE = 10;
    private static final int FLAG_RANGE = 10;
    private static final int NEARBY_RANGE = 3;
    private static final double PLAYER_RANGE = 1.31;

    public static void register(BattleMatch match, StructureManager structureManager) {
        structureManager.registerChecker((block) -> {
            if (match.getCurrentGame().getTeamManager().getTeams().stream().anyMatch(bt ->
                bt.isInSpawn(block.getLocation()))
            ) {
                return Optional.of(new StructureViolation(IN_SPAWN, "You can't place this in spawn"));
            }
            return Optional.empty();
        });

        structureManager.registerChecker((block) -> {
            if (match.getCurrentGame().getTeamManager().getTeams().stream().anyMatch(bt ->
                block.getLocation().distance(bt.getConfig().getSpawn()) <= SPAWN_RANGE)
            ) {
                return Optional.of(new StructureViolation(NEAR_SPAWN, "You can't place this near spawn"));
            }
            return Optional.empty();
        });

        structureManager.registerChecker((block) -> {
            if (match.getCurrentGame().getTeamManager().getTeams().stream().anyMatch(bt ->
                block.getLocation().equals(bt.getFlag().getHome())
            )) {
                return Optional.of(new StructureViolation(IN_FLAG, "You can't place this on the flag"));
            }
            return Optional.empty();
        });

        structureManager.registerChecker((block) -> {
            if (match.getCurrentGame().getTeamManager().getTeams().stream().anyMatch(bt ->
                block.getLocation().distance(bt.getFlag().getHome()) <= FLAG_RANGE)
            ) {
                return Optional.of(new StructureViolation(NEAR_FLAG, "You can't place this near the flag"));
            }
            return Optional.empty();
        });

        structureManager.registerChecker((block) -> {
            if (BlockUtil.getBlocksInRadius(block, NEARBY_RANGE).stream().anyMatch(nearby ->
                match.getCurrentGame().getConfig().getRestricted().contains(nearby.getLocation())
                    || RESTRICT_NEARBY.contains(nearby.getType())
            )) {
                return Optional.of(new StructureViolation(NEAR_RESTRICTED, "Building is prohibited in this area"));
            }
            return Optional.empty();
        });

        structureManager.registerChecker((block) -> {
            if (match.getCurrentGame().getParticipants().stream().anyMatch(player ->
                player.getLocation().distance(block.getLocation()) <= PLAYER_RANGE
            )) {
                return Optional.of(new StructureViolation(NEAR_PLAYER, "Someone is in the way"));
            }
            return Optional.empty();
        });
    }

}
