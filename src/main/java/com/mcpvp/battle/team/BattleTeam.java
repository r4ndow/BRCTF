package com.mcpvp.battle.team;

import com.mcpvp.battle.flag.IBattleFlag;
import com.mcpvp.battle.util.Colors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
@ToString(of={"id", "name"})
public class BattleTeam {
	
	private final Set<Player> players = new HashSet<>();
	private final int id;
	private String name;
	private Colors color;
	@Setter
	private IBattleFlag flag;
	
	public BattleTeam(int id, String name, Colors color) {
		this.id = id;
		this.name = name;
		this.color = color;
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
