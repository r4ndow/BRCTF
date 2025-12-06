package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

public class CapturesCommand extends EasyCommandGroup {

    private final Battle battle;

    public CapturesCommand(Battle battle) {
        super(battle.getPlugin(), "captures");
        this.battle = battle;
        this.addCommand(new SetCommand());
        this.addCommand(new RequireCommand());
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

        public SetCommand() {
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

    public class RequireCommand extends EasyCommand {

        public RequireCommand() {
            super("require");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            if (args.isEmpty()) {
                return false;
            }

            CapturesCommand.this.battle.getGame().getConfig().setCaps(Integer.parseInt(args.get(0)));
            sender.sendMessage(C.cmdPass() + "Set captures required to " + C.hl(args.get(0)));

            return true;
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
            return CapturesCommand.this.matchTeam();
        }

    }

}
