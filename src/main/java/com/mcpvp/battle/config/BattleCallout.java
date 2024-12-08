package com.mcpvp.battle.config;

import com.mcpvp.battle.team.BattleTeam;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;

import javax.annotation.Nullable;

@Data
@AllArgsConstructor
public class BattleCallout {

	private final Location location;
	@Nullable
	private final BattleTeam team;
	private final String text;
	
}
