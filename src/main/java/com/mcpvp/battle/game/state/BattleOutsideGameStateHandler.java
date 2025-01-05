package com.mcpvp.battle.game.state;

import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.game.BattleGame;

/**
 * A state handler for outside the game, i.e. before and after but not during.
 */
public class BattleOutsideGameStateHandler extends BattleGameStateHandler {

	public BattleOutsideGameStateHandler(BattlePlugin plugin, BattleGame game) {
		super(plugin, game);
	}

	@Override
	public void enterState() {
		super.enterState();

		plugin.getBattle().getMatch().getTimer().setSeconds(15);

		setupFlags();
	}

	private void setupFlags() {
		game.getTeamManager().getTeams().forEach(bt -> {
			bt.getFlag().reset();
			bt.getFlag().setLocked(true);
		});
	}

	@EventHandler
	public void onParticipate(PlayerParticipateEvent event) {
		event.getPlayer().teleport(game.getConfig().getSpawn());
		event.getPlayer().getInventory().clear();
		event.getPlayer().getInventory().setArmorContents(new ItemStack[4]);
		event.getPlayer().setHealth(event.getPlayer().getMaxHealth());
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		// For when players fall into the void
		event.getEntity().teleport(game.getConfig().getSpawn());
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		// For when players fall into the void
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
