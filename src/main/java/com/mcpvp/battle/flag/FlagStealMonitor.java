package com.mcpvp.battle.flag;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagStealEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class FlagStealMonitor implements EasyListener {
	
	private static final Duration FLAG_STEAL_TIMER = Duration.milliseconds(200);
	private static final Double FLAG_DIST = 1.5;
	
	private final BattlePlugin plugin;
	private final Battle battle;
	private final BattleGame game;
	private final Map<BattleTeam, Map<Player, Expiration>> stealTimers = new HashMap<>();
	
	@EventHandler
	public void onTick(TickEvent event) {
		for (Player player : game.getParticipants()) {
			BattleTeam team = game.getTeamManager().getTeam(player);
			if (team == null) {
				continue;
			}
			
			for (BattleTeam bt : game.getTeamManager().getTeams()) {
				if (team == bt || !bt.getFlag().isHome()) {
					continue;
				}
				
				Map<Player, Expiration> playersStealing = getPlayersStealing(bt);
				
				if (player.getLocation().distance(bt.getFlag().getLocation()) > FLAG_DIST) {
					// If they've strayed from the flag, make sure they're not considered to be stealing
					playersStealing.remove(player);
				}
			}
		}
	}
	
	// Triggered by the FlagListener when an item is picked up
	@EventHandler
	public void onSteal(FlagStealEvent event) {
		Player player = event.getPlayer();
		BattleTeam team = event.getFlag().getTeam();
		Map<Player, Expiration> playersStealing = getPlayersStealing(team);
		
		if (!playersStealing.containsKey(player)) {
			// The player has just approached the flag. Set up the initial timer.
			playersStealing.put(player, new Expiration().expireIn(FLAG_STEAL_TIMER));
			event.setCancelled(true);
		} else if (playersStealing.get(player).isExpired()) {
			// Time to steal
			team.getFlag().steal(player);
		} else {
			// Not enough time has passed yet
			event.setCancelled(true);
		}
	}
	
	private Map<Player, Expiration> getPlayersStealing(BattleTeam team) {
		return stealTimers.computeIfAbsent(team, t -> new HashMap<>());
	}
	
}
