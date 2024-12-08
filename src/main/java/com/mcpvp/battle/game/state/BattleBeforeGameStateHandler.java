package com.mcpvp.battle.game.state;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.game.BattleGame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class BattleBeforeGameStateHandler extends BattleGameStateHandler {
	
	public BattleBeforeGameStateHandler(BattlePlugin plugin, BattleGame game) {
		super(plugin, game);
	}
	
	@Override
	public void enter() {
		super.enter();
		
		setupFlags();
	}
	
	private void setupFlags() {
		game.getBattle().getTeamManager().getTeams().forEach(bt -> {
			bt.getFlag().reset();
			bt.getFlag().setLocked(true);
		});
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		event.getPlayer().teleport(game.getConfig().getSpawn());
	}
	
	@EventHandler
	public void onParticipate(PlayerParticipateEvent event) {
		event.getPlayer().teleport(game.getConfig().getSpawn());
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		event.getEntity().teleport(game.getConfig().getSpawn());
	}
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		event.setRespawnLocation(game.getConfig().getSpawn());
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		if (event.getTo().getY() < 0) {
			event.getPlayer().teleport(game.getConfig().getSpawn());
		}
	}
	
}
