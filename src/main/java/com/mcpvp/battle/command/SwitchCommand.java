package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.team.BattleTeamManager;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.EasyCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SwitchCommand extends EasyCommand {

    private final Battle battle;

    public SwitchCommand(Battle battle) {
        super("switch");
        this.battle = battle;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        Player target = this.asPlayer(sender);

        // Allow switching other players
        if (!args.isEmpty()) {
            if (!sender.hasPermission("mcctf.switch.other")) {
                return false;
            }

            Player player = Bukkit.getPlayer(args.get(0));
            if (player == null || !this.battle.getGame().isParticipant(player)) {
                sender.sendMessage(C.cmdFail() + "This player is offline or not participating.");
                return false;
            }

            target = player;
        }

        // The message is sent by the PlayerJoinTeamEvent via BattleGameListener
        BattleTeamManager teamManager = this.battle.getGame().getTeamManager();
        BattleTeam current = teamManager.getTeam(target);
        BattleTeam next = teamManager.getNext(current);
        teamManager.setTeam(target, next);
        return true;
    }

}
