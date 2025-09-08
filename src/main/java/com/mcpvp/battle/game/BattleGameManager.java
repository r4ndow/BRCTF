package com.mcpvp.battle.game;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.map.BattleWorldManager;
import com.mcpvp.battle.map.parser.BattleMapLoader;
import com.mcpvp.battle.map.parser.BattleMapLoaderMetadataImpl;
import com.mcpvp.battle.map.parser.BattleMapLoaderSignImpl;
import com.mcpvp.battle.scoreboard.BattleScoreboardManager;
import com.mcpvp.battle.team.BattleTeamManager;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;

@Log4j2
@AllArgsConstructor
public class BattleGameManager {

    private final Battle battle;

    public BattleGame create(BattleMapData map, File mapDir, int index) {
        BattleMapLoader parser;
        if (map.getMetadata() != null) {
            parser = new BattleMapLoaderMetadataImpl();
        } else {
            parser = new BattleMapLoaderSignImpl();
        }

        try {
            // Extract map and create a world from it
            World world = BattleWorldManager.create(mapDir, index);
            // Parse the config. Teams must already exist at this point
            BattleGameConfig config = parser.parse(map, world);
            log.info("Parsed config: {}", config);

            if (config.getTimeOfDay() != null) {
                world.setTime(config.getTimeOfDay());
            }

            // Create default teams. This could be created from the parsed config.
            BattleTeamManager teamManager = new BattleTeamManager();
            teamManager.createDefaultTeams(config.getTeamConfigs());

            // Create scoreboard manager with the teams
            BattleScoreboardManager scoreboardManager = new BattleScoreboardManager(battle.getPlugin(), battle);

            // Create game instance
            // This game is inactive until `setup` is called
            return new BattleGame(
                battle.getPlugin(),
                battle,
                map,
                world,
                config,
                teamManager,
                scoreboardManager
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
