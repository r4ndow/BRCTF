package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.common.command.EasyCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class RespawnCommand extends EasyCommand {

    private final Battle battle;

    public RespawnCommand(Battle battle) {
        super("respawn");
        this.battle = battle;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        this.battle.getGame().respawn(this.asPlayer(sender), false);
        return true;
    }

}
