package com.mcpvp.battle.config;

import com.mcpvp.battle.team.BattleTeam;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for a single game, most of which is parsed from map data.
 */
@Data
@NoArgsConstructor
public class BattleGameConfig {

    private Location spawn;
    private Set<BattleTeamConfig> teamConfigs = new HashSet<>();
    private Set<BattleCallout> callouts = new HashSet<>();
    private Set<Location> restricted = new HashSet<>();
    private Integer timeOfDay;
    private int caps = 3;
    /**
     * Time in minutes
     */
    private int time = 15;

    public BattleTeamConfig getTeamConfig(int id) {
        return teamConfigs.stream().filter(c -> c.getId() == id).findAny().orElseThrow();
    }

    public BattleTeamConfig getTeamConfig(BattleTeam team) {
        return getTeamConfig(team.getId());
    }

}
