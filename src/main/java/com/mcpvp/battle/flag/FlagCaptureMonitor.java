package com.mcpvp.battle.flag;

import org.bukkit.event.EventHandler;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagCaptureEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.common.event.EasyListener;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FlagCaptureMonitor implements EasyListener {
    
    private final BattlePlugin plugin;
	private final Battle battle;
	private final BattleGame game;

    @EventHandler
    public void onCapture(FlagCaptureEvent event) {
        event.getCapturedFlag().capture();
        event.getPlayerTeam().onCapture();
    }

}
