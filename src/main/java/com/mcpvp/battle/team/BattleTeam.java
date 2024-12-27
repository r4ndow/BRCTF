package com.mcpvp.battle.team;

import com.mcpvp.battle.flag.IBattleFlag;
import com.mcpvp.battle.flag.WoolFlag;
import com.mcpvp.battle.util.Colors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
@ToString(of={"id", "name"})
public class BattleTeam {
	
	private final int id;
	private final Set<Player> players = new HashSet<>();
	private String name;
	private Colors color;
	private IBattleFlag flag;
	
	public BattleTeam(int id, String name, Colors color) {
		this.id = id;
		this.name = name;
		this.color = color;
		this.flag = new WoolFlag(this, null);
	}
	
	void add(Player player) {
		players.add(player);
	}
	
	void remove(Player player) {
		players.remove(player);
	}
	
	public boolean contains(Player player) {
		return players.contains(player);
	}
	
}
