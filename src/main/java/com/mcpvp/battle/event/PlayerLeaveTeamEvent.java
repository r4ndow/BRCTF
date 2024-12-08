package com.mcpvp.battle.event;

import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyEvent;

import lombok.Data;
import org.bukkit.entity.Player;

@Data
public class PlayerLeaveTeamEvent extends EasyEvent {

	private final Player player;
	private final BattleTeam team;
	
}
