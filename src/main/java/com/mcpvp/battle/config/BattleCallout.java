package com.mcpvp.battle.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;

@Data
@AllArgsConstructor
public class BattleCallout {

	private final Location location;
	private final BattleTeamConfig config;
	private final String text;
	
}
