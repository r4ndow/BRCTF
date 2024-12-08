package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class BattleMatchListener implements EasyListener {
	
	@Getter
	private final BattlePlugin plugin;
	private final Battle battle;
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		BattleTeam toJoin = battle.getTeamManager().selectAutoTeam();
		battle.getTeamManager().setTeam(event.getPlayer(), toJoin);
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		battle.getTeamManager().setTeam(event.getPlayer(), null);
	}
	
	@EventHandler
	public void onKicked(PlayerKickEvent event) {
		battle.getTeamManager().setTeam(event.getPlayer(), null);
	}
	
}
