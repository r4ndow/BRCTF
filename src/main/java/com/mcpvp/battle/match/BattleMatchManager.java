package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameManager;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.map.manager.MapManager;
import com.mcpvp.battle.map.repo.LocalMapRepo;
import com.mcpvp.battle.options.BattleOptions;
import com.mcpvp.battle.options.BattleOptionsInput;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class BattleMatchManager {
	
	private final BattlePlugin plugin;
	private final Battle battle;
	private final BattleOptions options;
	private final BattleGameManager gameLoader;
	private final MapManager mapManager;
	
	public BattleMatch create() {
		List<BattleGame> games = new ArrayList<>();
		List<BattleMapData> maps = selectMaps();
		for (int i = 0; i < maps.size(); i++) {
			games.add(gameLoader.create(maps.get(i), i));
		}
		
		return new BattleMatch(plugin, battle, games);
	}

	private List<BattleMapData> selectMaps() {
		// spawn = 0 138 0
		// 329797 good callout testing map
		return List.of(mapManager.loadMap(329797));
//		return mapManager.loadMaps(options.getMatch().getGames()).stream().filter(d -> d.getMetadata() == null).toList();
	}
	
}
