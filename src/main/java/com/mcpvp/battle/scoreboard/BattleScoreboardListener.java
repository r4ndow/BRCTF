package com.mcpvp.battle.scoreboard;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerJoinTeamEvent;
import com.mcpvp.battle.util.ScoreboardUtil;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.DisplaySlot;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class BattleScoreboardListener implements EasyListener {
	
	private final BattlePlugin plugin;
	private final Battle battle;
	private final BattleScoreboardManager scoreboardManager;
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.getPlayer().setScoreboard(scoreboardManager.create());
	}

	@EventHandler
	public void onJoinTeam(PlayerJoinTeamEvent event) {
		scoreboardManager.setTeam(event.getPlayer(), event.getTeam());
	}
	
	@EventHandler
	public void onTick(TickEvent event) {
		Bukkit.getOnlinePlayers().forEach(p -> {
			List<String> scores = getScores(p);
			ScoreboardUtil.resetChanged(p.getScoreboard(), scores);
			ScoreboardUtil.addLargeScores(p.getScoreboard(), p.getScoreboard().getObjective(DisplaySlot.SIDEBAR), scores);
		});
	}
	
	private List<String> getScores(Player player) {
		List<String> scores = new ArrayList<>();
		
		scores.add(battle.getGame().getState().name());
		battle.getTeamManager().getTeams().forEach(bt -> {
			scores.add(bt.getName());
			if (bt.getFlag().isHome()) {
				scores.add(bt.getColor().CHAT_STRING + "- flag home");
			} else {
				scores.add(bt.getColor().CHAT_STRING + "- flag taken");
			}
			
			if (bt.getFlag().getCarrier() == null) {
				scores.add(bt.getColor().CHAT_STRING + "- no carrier");
			} else {
				scores.add(bt.getColor().CHAT_STRING + "- carrier: " + bt.getFlag().getCarrier().getName());
			}
		});
		
		return scores;
	}
	
}
