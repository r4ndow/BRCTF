package com.mcpvp.battle.scoreboard;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerJoinTeamEvent;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

@Getter
@AllArgsConstructor
public class BattleScoreboardListener implements EasyListener {
	
	private final BattlePlugin plugin;
	private final Battle battle;
	private final BattleScoreboardManager scoreboardManager;
	
	// Need to have a scoreboard for other join events
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.getPlayer().setScoreboard(scoreboardManager.create());
	}

	@EventHandler
	public void onJoinTeam(PlayerJoinTeamEvent event) {
		scoreboardManager.setTeam(event.getPlayer(), event.getTeam());
	}
	
	@EventHandler
	public void onTick(TickEvent event) {
		Bukkit.getOnlinePlayers().forEach(scoreboardManager::refresh);
	}
	
}
