package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.team.BattleTeamManager;
import org.bukkit.command.CommandSender;

import java.util.List;

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
