package com.mcpvp.battle.player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class BattlePlayerManager {

	private final Map<Player, BattlePlayer> players = new HashMap<>();

	public BattlePlayer get(Player player) {
		return players.computeIfAbsent(player, this::create);
	}

	private BattlePlayer create(Player player) {
		return new BattlePlayer(player);
	}

	public void destroy(Player player) {
		players.remove(player);
	}

	public Collection<BattlePlayer> getAll() {
		return players.values();
	}
	
}
