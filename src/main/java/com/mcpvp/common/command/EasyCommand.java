package com.mcpvp.common.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class EasyCommand extends Command implements CommandExecutor, TabCompleter {

    protected EasyCommand(String name) {
        super(name);
    }

    protected EasyCommand(String name, List<String> aliases) {
        super(name, "", "", aliases);
    }

    protected EasyCommand(String name, String description, String usageMessage, List<String> aliases) {
        super(name, description, usageMessage, aliases);
    }

    /**
     * Actual invocation path (as opposed to {@link #execute(CommandSender, String, String[])}) for executing this command.
     */
    public abstract boolean onCommand(CommandSender sender, String label, List<String> args);

    public boolean onCommand(CommandSender var1, Command var2, String var3, String[] var4) {
        return this.onCommand(var1, var3, Arrays.asList(var4));
    }

    /**
     * Actual invocation path (as opposed to {@link #tabComplete(CommandSender, String, String[])}) for executing tab
     * completion for this command.
     */
    public List<String> onTabComplete(CommandSender sender, String alias, List<String> args) {
        if (!args.isEmpty()) {
            return this.onTabComplete(sender, alias, args.get(args.size() - 1));
        }
        return Collections.emptyList();
    }

    /**
     * Actual invocation path (as opposed to {@link #tabComplete(CommandSender, String, String[])}) for executing tab
     * completion for this command.
     */
    public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
        return Collections.emptyList();
    }

    public List<String> onTabComplete(CommandSender var1, Command var2, String var3, String[] var4) {
        return this.onTabComplete(var1, var3, Arrays.asList(var4));
    }

    /**
     * @return Prefix, eg `plugin` becomes `plugin:command`
     */
    protected String getFallbackPrefix() {
        return "";
    }

    public void register() {
        CommandUtil.getCommandMap().register(this.getFallbackPrefix(), this);
    }

    @Override // Overrides Command
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.isOp()) {
            super.testPermission(sender);
        }
        return this.onCommand(sender, commandLabel, Arrays.asList(args));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        return this.onTabComplete(sender, alias, Arrays.asList(args));
    }

    protected Player asPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new IllegalStateException("Only players can execute this command.");
        }

        return ((Player) sender);
    }

}
