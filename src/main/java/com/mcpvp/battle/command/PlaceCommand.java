package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import org.bukkit.command.CommandSender;

import java.util.List;

public class PlaceCommand extends BattleCommand {

    private Battle battle;

    public PlaceCommand(Battle battle) {
        super("place");
        this.battle = battle;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        battle.getGame().getTeamManager().getTeams().forEach(bt -> {
            bt.getFlag().reset();
        });
        return true;
    }
}
