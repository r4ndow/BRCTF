package com.mcpvp.battle.map.parser;

import org.bukkit.World;

import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.map.BattleMapData;

public interface BattleMapLoader {
 
	BattleGameConfig parse(BattleMapData map, World world);
	
}
