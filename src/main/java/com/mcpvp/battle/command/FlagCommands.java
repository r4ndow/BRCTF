package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.util.cmd.CmdUtil;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FlagCommands extends EasyCommandGroup {

    private final Battle battle;

    public FlagCommands(Battle battle) {
        super("flag");
        this.battle = battle;
        addCommand(new FlagJumpCommand());
    }

    public List<String> matchTeam(List<String> args) {
        if (args.isEmpty()) {
            return Collections.emptyList();
        }

        return CmdUtil.partialMatches(
                battle.getGame().getTeamManager().getTeams().stream().map(BattleTeam::getName).toList(),
                args.get(0)
        );
    }

    public Optional<BattleTeam> findTeam(String arg) {
        return battle.getGame().getTeamManager().getTeams().stream()
                .filter(bt -> bt.getName().toLowerCase().contains(arg))
                .findAny();
    }

    public class FlagJumpCommand extends EasyCommand {

        public FlagJumpCommand() {
            super("jump");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            findTeam(args.get(0)).ifPresent(bt -> {
                ((Player) sender).teleport(bt.getFlag().getLocation());
            });
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, List<String> args) {
            return matchTeam(args);
        }
    }

}
