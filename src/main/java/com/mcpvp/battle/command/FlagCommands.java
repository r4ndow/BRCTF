package com.mcpvp.battle.command;

import com.google.common.collect.Sets;
import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePreferences;
import com.mcpvp.battle.flag.display.FlagDisplayChannel;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FlagCommands extends EasyCommandGroup {

    private final Battle battle;

    public FlagCommands(Battle battle) {
        super("flag");
        this.battle = battle;
        this.addCommand(new JumpCommand());
        this.addCommand(new LockCommand());
        this.addCommand(new UnlockCommand());
        this.addCommand(new ResetCommand());
    }

    public List<String> matchTeam() {
        return this.battle.getGame().getTeamManager().getTeams().stream().map(BattleTeam::getName).toList();
    }

    public Optional<BattleTeam> findTeam(String arg) {
        return this.battle.getGame().getTeamManager().getTeams().stream()
            .filter(bt -> bt.getName().toLowerCase().contains(arg))
            .findAny();
    }

    public class JumpCommand extends EasyCommand {

        public JumpCommand() {
            super("jump");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            FlagCommands.this.findTeam(args.get(0)).ifPresent(bt -> {
                ((Player) sender).teleport(bt.getFlag().getLocation());
            });
            return true;
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> arg) {
            return FlagCommands.this.matchTeam();
        }

    }

    public class LockCommand extends EasyCommand {

        public LockCommand() {
            super("lock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            FlagCommands.this.findTeam(args.get(0)).ifPresent(bt -> {
                bt.getFlag().setLocked(true);
            });
            return true;
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> arg) {
            return FlagCommands.this.matchTeam();
        }

    }

    public class UnlockCommand extends EasyCommand {

        public UnlockCommand() {
            super("unlock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            FlagCommands.this.findTeam(args.get(0)).ifPresent(bt -> {
                bt.getFlag().setLocked(false);
            });
            return true;
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> arg) {
            return FlagCommands.this.matchTeam();
        }

    }

    public class ResetCommand extends EasyCommand {

        public ResetCommand() {
            super("reset");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            FlagCommands.this.findTeam(args.get(0)).ifPresent(bt -> {
                bt.getFlag().reset();
            });
            return true;
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> arg) {
            return FlagCommands.this.matchTeam();
        }

    }

}
