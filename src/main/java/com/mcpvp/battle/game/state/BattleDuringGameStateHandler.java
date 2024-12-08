package com.mcpvp.battle.game.state;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerJoinTeamEvent;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.event.PlayerResignEvent;
import com.mcpvp.battle.flag.FlagDropMonitor;
import com.mcpvp.battle.flag.FlagPickupMonitor;
import com.mcpvp.battle.flag.FlagStealMonitor;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.team.BattleTeam;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

public class BattleDuringGameStateHandler extends BattleGameStateHandler {
	
	public BattleDuringGameStateHandler(BattlePlugin plugin, BattleGame game) {
		super(plugin, game);
	}
	
	@Override
	public void enter() {
		super.enter();
		
		attach(new FlagStealMonitor(plugin, game.getBattle(), game));
		attach(new FlagDropMonitor(plugin, game.getBattle(), game));
		attach(new FlagPickupMonitor(plugin, game.getBattle(), game));
		
		game.getBattle().getTeamManager().getTeams().forEach(bt -> {
			bt.getFlag().setLocked(false);
		});
	}
	
	@EventHandler
	public void onJoinTeam(PlayerJoinTeamEvent event) {
		game.respawn(event.getPlayer());
	}
	
	@EventHandler
	public void onParticipate(PlayerParticipateEvent event) {
		game.respawn(event.getPlayer());
	}
	
	@EventHandler
	public void onDeath(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player player && player.getHealth() - event.getDamage() <= 0) {
			game.respawn(player);
		}
	}
	
	@EventHandler
	public void onDamageWhileInSpawn(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		
		BattleTeam team = game.getBattle().getTeamManager().getTeam(player);
		Block spawnBlock = game.getConfig().getTeamConfig(team).getSpawn().getBlock().getRelative(BlockFace.DOWN);
		Block onBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		
		if (spawnBlock.getType() != Material.AIR && spawnBlock.getType() == onBlock.getType()) {
			event.setCancelled(true);
		}
	}
	
}
