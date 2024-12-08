package com.mcpvp.battle.player;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import com.mcpvp.battle.team.BattleTeam;

@RequiredArgsConstructor
public class BattlePlayer {
	
	private final Player player;
	private BattleTeam team;
	
}
