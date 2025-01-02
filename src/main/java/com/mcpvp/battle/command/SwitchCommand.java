package com.mcpvp.battle.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.team.BattleTeamManager;

public class SwitchCommand extends BattleCommand {

    private final Battle battle;

    public SwitchCommand(Battle battle) {
        super("switch");
        this.battle = battle;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        BattleTeamManager teamManager = battle.getGame().getTeamManager();
        BattleTeam current = teamManager.getTeam(asPlayer(sender));
        BattleTeam next = teamManager.getNext(current);
        teamManager.setTeam(asPlayer(sender), next);
        return true;
    }
    
}
