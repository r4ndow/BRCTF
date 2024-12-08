package com.mcpvp.battle.event;

import com.mcpvp.battle.flag.IBattleFlag;
import com.mcpvp.common.event.EasyEvent;

import lombok.Data;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

@Data
public class FlagDropEvent extends EasyEvent {
	
	private final Player player;
	private final IBattleFlag flag;
	private final Item item;
	
}
