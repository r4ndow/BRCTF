package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.common.command.EasyCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RoleCommand extends EasyCommand {

    private final Battle battle;

    public RoleCommand(Battle battle) {
        super("role");
        this.battle = battle;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        Player player = this.asPlayer(sender);
        this.battle.getRolePreferenceGui().open(player);
        return true;
    }
}
