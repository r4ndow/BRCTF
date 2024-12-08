package com.mcpvp.common.command;

import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

public class CommandUtil {
	
	@SneakyThrows
	public static CommandMap getCommandMap() {
		Field mapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
		mapField.setAccessible(true);
		
		return (CommandMap) mapField.get(Bukkit.getServer());
	}
	
}
