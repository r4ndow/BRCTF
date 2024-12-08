package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.flag.FlagListener;
import com.mcpvp.battle.game.BattleGame;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BattleMatch {
	
	private final BattlePlugin plugin;
	private final Battle battle;
	@Getter
	private final List<BattleGame> games;
	private int currentGameIndex = 0;
	
	public BattleGame getCurrentGame() {
		return games.get(currentGameIndex);
	}
	
	public void start() {
		new BattleMatchListener(plugin, battle).register();
		new FlagListener(plugin, battle).register();
		
		getCurrentGame().setup();
	}

}
