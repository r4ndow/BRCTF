package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.common.event.EasyListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BattleMatchListener implements EasyListener {
	
	@Getter
	private final BattlePlugin plugin;
	private final Battle battle;
	
}
