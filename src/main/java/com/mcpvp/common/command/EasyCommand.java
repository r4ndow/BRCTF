package com.mcpvp.common.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class EasyCommand extends Command {

    protected EasyCommand(String name) {
        super(name);
    }

    protected EasyCommand(String name, String description, String usageMessage, List<String> aliases) {
        super(name, description, usageMessage, aliases);
    }

    /**
     * Actual invocation path for executing this command.
     */
    public abstract boolean onCommand(CommandSender sender, String label, List<String> args);

    /**
     * Actual invocation path for executing tab completion for this command. Can be triggered by {@link #onTabComplete(CommandSender, Command, String, String[])}.
     *
     * @param sender
     * @param alias
     * @param args
     * @return
     */
    public List<String> onTabComplete(CommandSender sender, String alias, List<String> args) {
        return Collections.emptyList();
    }

    /**
     * @return Prefix, eg `plugin` becomes `plugin:command`
     */
    protected String getFallbackPrefix() {
        return "";
    }

    public void register() {
        CommandUtil.getCommandMap().register(getFallbackPrefix(), this);
    }

    @Override // Overrides Command
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return onCommand(sender, commandLabel, Arrays.asList(args));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        return onTabComplete(sender, alias, Arrays.asList(args));
    }

    protected Player asPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new IllegalStateException("Only players can execute this command.");
        }

        return ((Player) sender);
    }

}
