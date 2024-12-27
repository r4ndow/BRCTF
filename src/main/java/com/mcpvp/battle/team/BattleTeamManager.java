package com.mcpvp.battle.team;

import com.mcpvp.battle.event.PlayerJoinTeamEvent;
import com.mcpvp.battle.event.PlayerLeaveTeamEvent;
import com.mcpvp.battle.util.Colors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

@RequiredArgsConstructor
public class BattleTeamManager {

	private final Random random = new Random();
	@Getter
	private final List<BattleTeam> teams = new ArrayList<>();
	private BattleTeam red;
	private BattleTeam blue;

	public void createDefaultTeams() {
		// The IDs are very important! They are used for map parsing
		// eg a sign `{{flag 1}}` specifies the red flag
		this.red = new BattleTeam(1, "Red", Colors.RED);
		this.blue = new BattleTeam(2, "Blue", Colors.BLUE);
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
	
	public BattleTeam selectAutoTeam() {
		if (getTeams().stream().map(t -> t.getPlayers().size()).distinct().count() == 1) {
			// All teams have the same size
			return getTeams().get(random.nextInt(getTeams().size()));
		} else {
			// Find the team with the fewest players
			return getTeams().stream()
				.min(Comparator.comparingInt(t -> t.getPlayers().size()))
				.orElse(getTeams().get(random.nextInt(getTeams().size())));
		}
	}
	
	public BattleTeam getNext(BattleTeam team) {
		return teams.get(teams.size() % teams.indexOf(team) + 1);
	}
	
}
