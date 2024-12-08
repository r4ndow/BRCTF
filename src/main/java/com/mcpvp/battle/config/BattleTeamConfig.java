package com.mcpvp.battle.config;

import lombok.NoArgsConstructor;
import org.bukkit.Location;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for a single team within one game.
 */
@Data
@NoArgsConstructor
public class BattleTeamConfig {
 
    private Location spawn;
    private Location flag;

}
