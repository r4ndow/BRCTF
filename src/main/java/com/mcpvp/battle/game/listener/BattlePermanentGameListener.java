package com.mcpvp.battle.game.listener;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.event.PlayerResignEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.kits.HeavyKit;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.kit.KitType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Log4j2
@Getter
@AllArgsConstructor
public class BattlePermanentGameListener implements EasyListener {

	private final BattlePlugin plugin;
	private final BattleGame game;

	// ===============================
	//  EVENT CREATION AND FORWARDING
	// ===============================

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		// Ensure that every join results in either a participate or resign event
		if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
			new PlayerParticipateEvent(event.getPlayer(), game).call();
		} else {
			new PlayerResignEvent(event.getPlayer(), game).call();
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		new PlayerResignEvent(event.getPlayer(), game).call();
	}

	@EventHandler
	public void onKick(PlayerKickEvent event) {
		new PlayerResignEvent(event.getPlayer(), game).call();
	}

	@EventHandler
	public void onGamemodeChange(PlayerGameModeChangeEvent event) {
		GameMode prev = event.getPlayer().getGameMode();
		GameMode next = event.getNewGameMode();

		if (prev == GameMode.SURVIVAL) {
			// Switching out of survival, probably to spectate
			new PlayerResignEvent(event.getPlayer(), game).call();
		} else if (next == GameMode.SURVIVAL) {
			// Switching into survival, probably to play
			new PlayerParticipateEvent(event.getPlayer(), game).call();
		}
	}

	// ==============
	// EVENT HANDLING
	// ==============

	@EventHandler
	public void selectDefaultKit(PlayerParticipateEvent event) {
		// For players who join without a kit selected, make sure they have one before they are respawned
		// The kit creation/equipping will be handled by the game
		KitType<?> selected = game.getBattle().getKitManager().getSelected(event.getPlayer());
		if (selected == null) {
			game.getBattle().getKitManager().setSelected(event.getPlayer(), HeavyKit.class, true);
		}
	}

	@EventHandler
	public void onHunger(FoodLevelChangeEvent event) {
		event.setCancelled(true);
		if (event.getEntity() instanceof Player player) {
			player.setFoodLevel(50);
		}
	}

	@EventHandler
	public void onResign(PlayerResignEvent event) {
		game.remove(event.getPlayer());
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (game.getBattle().getTeamManager().getTeams().stream().noneMatch(bt ->
			bt.getFlag().isItem(event.getItemDrop().getItemStack())
		)) {
			event.setCancelled(true);
		}
	}

}
