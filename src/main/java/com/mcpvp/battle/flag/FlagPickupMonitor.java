package com.mcpvp.battle.flag;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagPickupEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.common.event.EasyListener;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.event.EventHandler;

@Getter
@AllArgsConstructor
public class FlagPickupMonitor implements EasyListener {
	
	private final BattlePlugin plugin;
	private final Battle battle;
	private final BattleGame game;
	
	@EventHandler
	public void onPickup(FlagPickupEvent event) {
		event.getFlag().pickup(event.getPlayer());
	}
	
}
