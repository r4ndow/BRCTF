package com.mcpvp.battle.team;

import com.mcpvp.battle.config.BattleTeamConfig;
import com.mcpvp.battle.event.PlayerJoinTeamEvent;
import com.mcpvp.battle.event.PlayerLeaveTeamEvent;
import com.mcpvp.common.chat.Colors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages a set of unique teams. This should be used per-game since teams are per-game.
 */
@RequiredArgsConstructor
public class BattleTeamManager {

    private static final Random RANDOM = new Random();

    @Getter
    private final List<BattleTeam> teams = new ArrayList<>();

    public void createDefaultTeams(Set<BattleTeamConfig> configs) {
        // The IDs are very important! They are used for map parsing
        // eg a sign `{{flag 1}}` specifies the red flag
        Map<Integer, BattleTeamConfig> configById = configs.stream().collect(
            Collectors.toMap(BattleTeamConfig::getId, e -> e)
        );

        BattleTeam red = new BattleTeam(1, "Red", Colors.RED, configById.get(1));
        BattleTeam blue = new BattleTeam(2, "Blue", Colors.BLUE, configById.get(2));
        this.teams.add(red);
        this.teams.add(blue);
    }

    public void setTeam(Player player, @Nullable BattleTeam team) {
        // Remove from any existing team
        for (BattleTeam battleTeam : getTeams()) {
            if (battleTeam.contains(player)) {
                battleTeam.remove(player);
                new PlayerLeaveTeamEvent(player, battleTeam).call();
            }
        }

        // Add to new team
        if (team != null) {
            team.add(player);
            new PlayerJoinTeamEvent(player, team).call();
        }
    }

    public BattleTeam getTeam(Player player) {
        return getTeams().stream().filter(bt -> bt.contains(player)).findFirst().orElse(null);
    }

    public BattleTeam getTeam(int id) {
        return getTeams().stream().filter(bt -> bt.getId() == id).findFirst().orElseThrow(() ->
            new IllegalStateException("No team found for id " + id)
        );
    }

    public BattleTeam selectAutoTeam() {
        if (getTeams().stream().map(t -> t.getPlayers().size()).distinct().count() == 1) {
            // All teams have the same size
            return getTeams().get(RANDOM.nextInt(getTeams().size()));
        } else {
            // Find the team with the fewest players
            return getTeams().stream()
                .min(Comparator.comparingInt(t -> t.getPlayers().size()))
                .orElse(getTeams().get(RANDOM.nextInt(getTeams().size())));
        }
    }

    public BattleTeam getNext(BattleTeam team) {
        return teams.get((teams.indexOf(team) + 1) % teams.size());
    }

    public boolean isSameTeam(Player first, Player second) {
        return getTeam(first) == getTeam(second);
    }

    public Map<BattleTeam, Set<Player>> getPlayerMap() {
        return teams.stream().collect(Collectors.toMap(e -> e, BattleTeam::getPlayers));
    }

}
