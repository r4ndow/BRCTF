package com.mcpvp.battle;

import com.mcpvp.battle.command.FlagCommands;
import com.mcpvp.battle.command.KitCommand;
import com.mcpvp.battle.command.NextCommand;
import com.mcpvp.battle.command.PlaceCommand;
import com.mcpvp.battle.command.StartCommand;
import com.mcpvp.common.event.TickEvent;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BattlePlugin extends JavaPlugin {
	
	@Getter
	private Battle battle;
	
	@SneakyThrows
	@Override
	public void onLoad() {
		super.onLoad();
		
		this.battle = new Battle(this);
		this.battle.load();

		registerCommands();
	}
	
	@Override
	public void onEnable() {
		super.onEnable();
		
		Bukkit.getScheduler().runTaskTimer(this, () -> {
			new TickEvent().call();
		}, 1, 1);

		this.battle.start();
	}
	
	private void registerCommands() {
		new StartCommand(battle).register();
		new NextCommand(battle).register();
		new PlaceCommand(battle).register();
		new FlagCommands(battle).register();
		new KitCommand(battle.getKitManager()).register();
	}
	
}
