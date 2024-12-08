package com.mcpvp.battle.scoreboard;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.player.BattlePlayer;
import com.mcpvp.battle.player.BattlePlayerManager;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.team.BattleTeamManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

@RequiredArgsConstructor
public class BattleScoreboardManager {
	
	private final BattlePlugin plugin;
	private final Battle battle;
	private final BattleTeamManager teamManager;
	
	public void init() {
		new BattleScoreboardListener(plugin, battle, this).register();
	}
	
	public Scoreboard create() {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective objective = scoreboard.registerNewObjective("score", "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		for (BattleTeam bt : teamManager.getTeams()) {
			createTeam(scoreboard, bt);
		}
		
		return scoreboard;
	}
	
	public void setTeam(Player player, BattleTeam bt) {
		for (Scoreboard sb : getAllScoreboards()) {
			getScoreboardTeam(sb, bt).addEntry(player.getName());
		}
	}
	
	private List<Scoreboard> getAllScoreboards() {
		return Bukkit.getOnlinePlayers().stream().map(Player::getScoreboard).toList();
	}
	
	private void createTeam(Scoreboard scoreboard, BattleTeam bt) {
		// Register the team.
		Team team = scoreboard.registerNewTeam(getTeamName(bt));
		team.setAllowFriendlyFire(true);
		team.setPrefix(bt.getColor().toString());
		
		// Add all players to the team. From here the join and quit
		// events are responsible
		// for updating team members.
		for (Player player : Bukkit.getOnlinePlayers())
			if (bt.contains(player))
				team.addEntry(player.getName());
	}
	
	private Team getScoreboardTeam(Scoreboard scoreboard, BattleTeam bt) {
		return scoreboard.getTeam(getTeamName(bt));
	}
	
	private String getTeamName(BattleTeam bt) {
		return "team_" + bt.getId();
	}
	
}
