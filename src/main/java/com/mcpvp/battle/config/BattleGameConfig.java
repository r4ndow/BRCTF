package com.mcpvp.battle.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Location;

import com.mcpvp.battle.team.BattleTeam;

import lombok.Data;

/**
 * Configuration for a single game, most of which is parsed from map data.
 */
@Data
@NoArgsConstructor
public class BattleGameConfig {
    
    private Location spawn;
    @Getter(AccessLevel.PRIVATE)
    private Map<BattleTeam, BattleTeamConfig> teamConfigs = new HashMap<>();
    private Set<BattleCallout> callouts = new HashSet<>();
    private Set<Location> restricted = new HashSet<>();
    private int caps = 3;
    private int time = 15;
    
    public BattleTeamConfig getTeamConfig(BattleTeam team) {
        return this.teamConfigs.computeIfAbsent(team, k -> new BattleTeamConfig());
    }
    
    public Set<BattleTeam> getTeams() {
        return this.teamConfigs.keySet();
    }
    
}
