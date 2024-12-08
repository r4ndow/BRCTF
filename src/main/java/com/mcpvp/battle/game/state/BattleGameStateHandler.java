package com.mcpvp.battle.game.state;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.event.EasyListener;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class BattleGameStateHandler extends EasyLifecycle implements EasyListener {
	
	protected final BattlePlugin plugin;
	protected final BattleGame game;
	
	public void enter() {
		attach(this);
	}
	
	public void leave() {
		shutdown();
	}

}
