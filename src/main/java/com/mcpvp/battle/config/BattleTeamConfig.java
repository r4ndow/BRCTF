package com.mcpvp.battle.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

/**
 * Configuration for a single team within one game.
 */
@Data
@RequiredArgsConstructor
public class BattleTeamConfig {

    private final int id;
    private Location spawn;
    private Location flag;

}
