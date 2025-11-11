package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.team.BattleTeamManager;
import com.mcpvp.common.command.EasyCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SwitchCommand extends EasyCommand {

    private final Battle battle;

    public SwitchCommand(Battle battle) {
        super("switch");
        this.battle = battle;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        BattleTeamManager teamManager = this.battle.getGame().getTeamManager();
        BattleTeam current = teamManager.getTeam(this.asPlayer(sender));
        BattleTeam next = teamManager.getNext(current);
        teamManager.setTeam(this.asPlayer(sender), next);
        return true;
    }

}
