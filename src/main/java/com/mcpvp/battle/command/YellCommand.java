package com.mcpvp.battle.command;

import com.mcpvp.common.command.EasyCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class YellCommand extends EasyCommand {

    public YellCommand() {
        super("yell");
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        this.asPlayer(sender).chat("!" + String.join(" ", args));
        return true;
    }

}
