package com.mcpvp.battle.event;

import com.mcpvp.battle.flag.IBattleFlag;
import com.mcpvp.common.event.EasyCancellableEvent;

import lombok.Data;
import org.bukkit.entity.Player;

@Data
public class FlagTakeEvent extends EasyCancellableEvent {

	private final Player player;
	private final IBattleFlag flag;
	
}
