package com.mcpvp.battle.map.loader;

import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.map.BattleMapData;
import org.bukkit.World;

public interface BattleMapLoader {

    BattleGameConfig parse(BattleMapData map, World world);

}
