package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import org.bukkit.command.CommandSender;

import java.util.List;

public class NextCommand extends BattleCommand {

    private Battle battle;

    public NextCommand(Battle battle) {
        super("next");
        this.battle = battle;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        battle.getMatch().advanceStateOrGame();
        return true;
    }
}
