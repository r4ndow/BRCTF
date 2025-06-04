package com.mcpvp.battle.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public class YellCommand extends BattleCommand {

    public YellCommand() {
        super("yell", List.of("all", "a"));
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        asPlayer(sender).chat("!" + String.join(" ", args));
        return true;
    }

}
