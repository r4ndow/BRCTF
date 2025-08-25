package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.util.cmd.CmdUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class FlagCommands extends BattleCommandGroup {

    private final Battle battle;

    public FlagCommands(Battle battle) {
        super("flag");
        this.battle = battle;
        addCommand(new JumpCommand());
        addCommand(new LockCommand());
        addCommand(new UnlockCommand());
        addCommand(new ResetCommand());
    }

    public List<String> matchTeam(String arg) {
        return CmdUtil.partialMatches(
                battle.getGame().getTeamManager().getTeams().stream().map(BattleTeam::getName).toList(),
                arg
        );
    }

    public Optional<BattleTeam> findTeam(String arg) {
        return battle.getGame().getTeamManager().getTeams().stream()
                .filter(bt -> bt.getName().toLowerCase().contains(arg))
                .findAny();
    }

    public class JumpCommand extends BattleCommand {

        public JumpCommand() {
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
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return matchTeam(arg);
        }

    }

    public class LockCommand extends BattleCommand {

        public LockCommand() {
            super("lock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            findTeam(args.get(0)).ifPresent(bt -> {
                bt.getFlag().setLocked(true);
            });
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return matchTeam(arg);
        }

    }

    public class UnlockCommand extends BattleCommand {

        public UnlockCommand() {
            super("unlock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            findTeam(args.get(0)).ifPresent(bt -> {
                bt.getFlag().setLocked(false);
            });
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return matchTeam(arg);
        }

    }

    public class ResetCommand extends BattleCommand {

        public ResetCommand() {
            super("reset");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            findTeam(args.get(0)).ifPresent(bt -> {
                bt.getFlag().reset();
            });
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return matchTeam(arg);
        }

    }

}
