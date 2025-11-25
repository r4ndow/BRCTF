package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.CommandUtil;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

public class CapturesCommand extends EasyCommandGroup {

    private final Battle battle;

    public CapturesCommand(Battle battle) {
        super("captures");
        this.battle = battle;
        this.addCommand(new SetCommand());
    }

    public List<String> matchTeam() {
        return this.battle.getGame().getTeamManager().getTeams().stream().map(BattleTeam::getName).toList();
    }

    public Optional<BattleTeam> findTeam(String arg) {
        return this.battle.getGame().getTeamManager().getTeams().stream()
            .filter(bt -> bt.getName().toLowerCase().contains(arg))
            .findAny();
    }

    public class SetCommand extends EasyCommand {

        protected SetCommand() {
            super("set");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            CapturesCommand.this.findTeam(args.get(0)).ifPresent(bt -> {
                bt.setCaptures(Integer.parseInt(args.get(1)));
                sender.sendMessage(C.cmdPass() + "Set captures for " + bt.getColoredName() + C.GRAY + " to " + C.hl(args.get(1)));
            });
            return true;
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
            return CapturesCommand.this.matchTeam();
        }

    }

}
