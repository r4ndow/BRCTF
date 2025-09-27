package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameManager;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.map.manager.BattleMapManager;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class BattleMatchManager {

    private final BattlePlugin plugin;
    private final Battle battle;
    private final BattleGameManager gameLoader;
    private final BattleMapManager mapManager;

    public BattleMatch create() {
        List<BattleGame> games = new ArrayList<>();
        List<BattleMapData> maps = selectMaps();
        for (int i = 0; i < maps.size(); i++) {
            games.add(gameLoader.create(maps.get(i), mapManager.getWorldData(maps.get(i)), i));
        }

        return new BattleMatch(plugin, battle, games);
    }

    private List<BattleMapData> selectMaps() {
        if (mapManager.getOverride() != null && !mapManager.getOverride().isEmpty()) {
            List<BattleMapData> maps = mapManager.getOverride().stream()
                .map(BattleMapData::getId)
                .map(mapManager::loadMap)
                .toList();
            mapManager.clearOverride();
            return maps;
        }

        return mapManager.loadMaps(plugin.getBattle().getOptions().getMatch().getGames());
    }

}
