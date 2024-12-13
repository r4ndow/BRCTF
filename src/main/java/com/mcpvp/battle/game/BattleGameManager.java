package com.mcpvp.battle.game;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.flag.IBattleFlag;
import com.mcpvp.battle.flag.WoolFlag;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.map.BattleWorldCreator;
import com.mcpvp.battle.map.parser.BattleMapLoader;
import com.mcpvp.battle.match.BattleMatchTimer;
import com.mcpvp.battle.team.BattleTeam;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;

@Log4j2
@AllArgsConstructor
public class BattleGameManager {
	
	private final Battle battle;
	private final BattleMapLoader parser;
	
	public BattleGame create(BattleMapData map, int index) {
		try {
			// Extract map and create a world from it
			World world = BattleWorldCreator.create(map, new File(battle.getOptions().getMaps().getDir()), index);
			// Parse the config. Teams must already exist at this point
			BattleGameConfig config = parser.parse(map, world, battle.getTeamManager());
			log.info("Parsed config: " + config);
			
			// Create game instance. Just creating this doesn't do anything
			BattleGame game = new BattleGame(battle.getPlugin(), battle, map, world, config);
			
			// Kind of weird, but we need to go back after and set up the flags
			// The flag needs a reference to the game, which doesn't exist when the map is parsed
			config.getTeams().forEach(bt -> {
				bt.setFlag(getFlag(game, bt));
			});
			
			return game;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private IBattleFlag getFlag(BattleGame game, BattleTeam team) {
		return new WoolFlag(game, team);
	}
	
}
