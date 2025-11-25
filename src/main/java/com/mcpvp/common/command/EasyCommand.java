package com.mcpvp.common.command;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

@Getter
public abstract class EasyCommand implements CommandExecutor, TabCompleter {

    private final String name;

    protected EasyCommand(String name) {
        this.name = name;
    }

    @Override
    public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
        return this.onCommand(var1, var3, Arrays.asList(var4));
    }

    public abstract boolean onCommand(CommandSender sender, String label, List<String> args);

    @Override
    public List<String> onTabComplete(CommandSender var1, Command var2, String var3, String[] var4) {
        return CommandUtil.partialMatches(this.getTabCompletions(var1, var3, Arrays.asList(var4)), var4[var4.length - 1]);
    }

    public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    public void register(JavaPlugin plugin) {
        PluginCommand command = plugin.getCommand(this.name);
        if (command == null) {
            throw new IllegalStateException("No command registered for " + this.name);
        }

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    protected Player asPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new IllegalStateException("Only players can execute this command.");
        }

        return ((Player) sender);
    }

}
