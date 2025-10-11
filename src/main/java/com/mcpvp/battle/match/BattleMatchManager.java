package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameManager;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.map.manager.BattleMapManager;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j2
@AllArgsConstructor
public class BattleMatchManager {

    private final BattlePlugin plugin;
    private final Battle battle;
    private final BattleGameManager gameLoader;
    private final BattleMapManager mapManager;

    public BattleMatch create() {
        List<BattleGame> games = new ArrayList<>();
        List<BattleMapData> maps = this.selectMaps();
        for (int i = 0; i < maps.size(); i++) {
            games.add(this.gameLoader.create(maps.get(i), this.mapManager.getWorldData(maps.get(i)), i));
        }

        return new BattleMatch(this.plugin, this.battle, games);
    }

    private List<BattleMapData> selectMaps() {
        if (this.mapManager.getOverride() != null && !this.mapManager.getOverride().isEmpty()) {
            List<BattleMapData> maps = this.mapManager.getOverride().stream()
                .map(BattleMapData::getId)
                .peek(id -> {
                    if (!this.mapManager.isMap(id) || this.mapManager.loadMap(id) == null) {
                        log.warn("Map ID {} is not enabled and will be ignored", id);
                    }
                })
                .filter(this.mapManager::isMap)
                .map(this.mapManager::loadMap)
                .filter(Objects::nonNull)
                .toList();
            this.mapManager.clearOverride();
            return maps;
        }

        return this.mapManager.loadMaps(this.plugin.getBattle().getOptions().getMatch().getGames());
    }

}
