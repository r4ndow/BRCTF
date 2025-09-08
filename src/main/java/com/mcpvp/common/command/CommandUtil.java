package com.mcpvp.common.command;

import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

public class CommandUtil {

    @SneakyThrows
    public static CommandMap getCommandMap() {
        Field mapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        mapField.setAccessible(true);

        return (CommandMap) mapField.get(Bukkit.getServer());
    }

    public static List<String> partialMatches(Collection<String> list, String query) {
        return list.stream()
            .map(String::toLowerCase)
            .filter(string -> string.startsWith(query.toLowerCase()))
            .toList();
    }
}
