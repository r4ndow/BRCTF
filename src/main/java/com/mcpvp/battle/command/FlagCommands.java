package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.command.CommandUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class FlagCommands extends BattleCommandGroup {

    private final Battle battle;

    public FlagCommands(Battle battle) {
        super("flag");
        this.battle = battle;
        this.addCommand(new JumpCommand());
        this.addCommand(new LockCommand());
        this.addCommand(new UnlockCommand());
        this.addCommand(new ResetCommand());
    }

    public List<String> matchTeam(String arg) {
        return CommandUtil.partialMatches(
            this.battle.getGame().getTeamManager().getTeams().stream().map(BattleTeam::getName).toList(),
            arg
        );
    }

    public Optional<BattleTeam> findTeam(String arg) {
        return this.battle.getGame().getTeamManager().getTeams().stream()
            .filter(bt -> bt.getName().toLowerCase().contains(arg))
            .findAny();
    }

    public class JumpCommand extends BattleCommand {

        public JumpCommand() {
            super("jump");
            this.setPermission("mcctf.flag.jump");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            FlagCommands.this.findTeam(args.get(0)).ifPresent(bt -> {
                ((Player) sender).teleport(bt.getFlag().getLocation());
            });
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return FlagCommands.this.matchTeam(arg);
        }

    }

    public class LockCommand extends BattleCommand {

        public LockCommand() {
            super("lock");
            this.setPermission("mcctf.flag.lock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            FlagCommands.this.findTeam(args.get(0)).ifPresent(bt -> {
                bt.getFlag().setLocked(true);
            });
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return FlagCommands.this.matchTeam(arg);
        }

    }

    public class UnlockCommand extends BattleCommand {

        public UnlockCommand() {
            super("unlock");
            this.setPermission("mcctf.flag.unlock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            FlagCommands.this.findTeam(args.get(0)).ifPresent(bt -> {
                bt.getFlag().setLocked(false);
            });
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return FlagCommands.this.matchTeam(arg);
        }

    }

    public class ResetCommand extends BattleCommand {

        public ResetCommand() {
            super("reset");
            this.setPermission("mcctf.flag.reset");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            FlagCommands.this.findTeam(args.get(0)).ifPresent(bt -> {
                bt.getFlag().reset();
            });
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return FlagCommands.this.matchTeam(arg);
        }

    }

}
