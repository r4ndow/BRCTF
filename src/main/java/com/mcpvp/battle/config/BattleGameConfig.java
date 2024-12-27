package com.mcpvp.battle.config;

import java.util.HashSet;
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
    private Set<BattleTeamConfig> teamConfigs = new HashSet<>() {
        {
            add(new BattleTeamConfig(1));
            add(new BattleTeamConfig(2));
        }
    };
    private Set<BattleCallout> callouts = new HashSet<>();
    private Set<Location> restricted = new HashSet<>();
    private int caps = 3;
    private int time = 15;

    public BattleTeamConfig getTeamConfig(int id) {
        return teamConfigs.stream().filter(c -> c.getId() == id).findAny().orElseThrow();
    }

    public BattleTeamConfig getTeamConfig(BattleTeam team) {
        return getTeamConfig(team.getId());
    }
    
}
