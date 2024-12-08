package com.mcpvp.battle.event;

import com.mcpvp.battle.flag.IBattleFlag;
import org.bukkit.entity.Player;

public class FlagPickupEvent extends FlagTakeEvent {
	
	public FlagPickupEvent(Player player, IBattleFlag flag) {
		super(player, flag);
	}
}
