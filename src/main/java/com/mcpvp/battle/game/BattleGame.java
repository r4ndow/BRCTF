package com.mcpvp.battle.game;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.game.listener.BattlePermanentGameListener;
import com.mcpvp.battle.game.state.BattleDuringGameStateHandler;
import com.mcpvp.battle.game.state.BattleGameStateHandler;
import com.mcpvp.battle.game.state.BattleOutsideGameStateHandler;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.scoreboard.BattleScoreboardManager;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.team.BattleTeamManager;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.kit.Kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.mcpvp.battle.flag.FlagListener;

@Log4j2
@Getter
@RequiredArgsConstructor
public class BattleGame extends EasyLifecycle {

	private final BattlePlugin plugin;
	private final Battle battle;
	private final BattleMapData map;
	private final World world;
	private final BattleGameConfig config;
	private final BattleTeamManager teamManager;
	private final BattleScoreboardManager scoreboardManager;
	private final Map<BattleTeam, BattleGameTeamData> teamData = new HashMap<>();

	@Nullable
	private BattleGameState state = null;
	private BattleGameStateHandler stateHandler;

	public void setup() {
		log.info("Setup game on map " + map);

		attach(new BattlePermanentGameListener(plugin, this));
		attach(new FlagListener(plugin, this));
		attach(scoreboardManager);

		scoreboardManager.init();

		world.setGameRuleValue("doDaylightCycle", "false");
		world.setGameRuleValue("naturalGeneration", "false");

		setState(BattleGameState.BEFORE);
	}

	public void stop() {
		setState(null);
		super.shutdown();
	}

	/**
	 * Leaves the current state (if present) and enters the given state (if not
	 * null).
	 * 
	 * @param state The state to enter.
	 */
	public void setState(BattleGameState state) {
		if (this.state != state) {
			if (this.state != null) {
				leaveState(this.state);
			}

			this.state = state;

			if (state != null) {
				enterState(state);
			}
		}
	}

	private void enterState(BattleGameState state) {
		this.stateHandler = switch (state) {
			case BEFORE, AFTER -> new BattleOutsideGameStateHandler(plugin, this);
			case DURING -> new BattleDuringGameStateHandler(plugin, this);
		};
		this.stateHandler.enterState();

		fireParticipateEvents();
	}

	private void leaveState(BattleGameState state) {
		if (this.stateHandler != null) {
			this.stateHandler.leaveState();
		}
		this.stateHandler = null;
	}

	/**
	 * Respawns the player **during the game**. Not before or after.
	 * 
	 * @param player The player to respawn.
	 */
	public void respawn(Player player) {
		// Reset negative statues
		player.setHealth(player.getMaxHealth());
		player.setFireTicks(0);

		// Clear inventory
		player.getInventory().clear();
		player.getInventory().setArmorContents(new ItemStack[4]);

		// Teleport to spawn
		BattleTeam team = getTeamManager().getTeam(player);
		Location spawn = getConfig().getTeamConfig(team).getSpawn();
		player.teleport(spawn);

		// Equip kit
		battle.getKitManager().createSelected(player);
	}

	public void remove(Player player) {
		// Remove kit
		Kit kit = battle.getKitManager().get(player);
		if (kit != null) {
			kit.shutdown();
		}

		// Remove player from team
		getTeamManager().setTeam(player, null);
	}

	private void fireParticipateEvents() {
		for (Player player : getParticipants()) {
			new PlayerParticipateEvent(player, this).call();
		}
	}

	public boolean isParticipant(Player player) {
		return player.getGameMode() == GameMode.SURVIVAL;
	}

	public Collection<? extends Player> getParticipants() {
		return Bukkit.getOnlinePlayers().stream()
				.filter(this::isParticipant)
				.toList();
	}

	public BattleGameTeamData getTeamData(BattleTeam team) {
		return this.getTeamData().computeIfAbsent(team, k -> new BattleGameTeamData());
	}

}
