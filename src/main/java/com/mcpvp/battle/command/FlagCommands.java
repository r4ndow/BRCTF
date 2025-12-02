package com.mcpvp.battle.command;

import com.google.common.collect.Sets;
import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePreferences;
import com.mcpvp.battle.flag.display.FlagDisplayChannel;
import com.mcpvp.battle.options.BattleOptionsInput;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FlagCommands extends EasyCommandGroup {

    private final Battle battle;

    public FlagCommands(Battle battle) {
        super(battle.getPlugin(), "flag");
        this.battle = battle;
        this.addCommand(new JumpCommand());
        this.addCommand(new LockCommand());
        this.addCommand(new UnlockCommand());
        this.addCommand(new ResetCommand());
        this.addCommand(new DisplayCommand());
        this.addCommand(new TypeCommand());
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

    public class DisplayCommand extends EasyCommand {

        public DisplayCommand() {
            super("display");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            if (args.size() != 2) {
                return false;
            }

            FlagDisplayChannel channel = FlagDisplayChannel.valueOf(args.get(0).toUpperCase());
            boolean active = args.get(1).equalsIgnoreCase("on");

            Set<FlagDisplayChannel> current = FlagCommands.this.battle.getPreferenceManager().find(
                this.asPlayer(sender), BattlePreferences.FLAG_DISPLAY
            ).orElse(Sets.newHashSet(FlagDisplayChannel.CHAT));

            if (active) {
                current.add(channel);
            } else {
                current.remove(channel);
            }

            FlagCommands.this.battle.getPreferenceManager().store(
                this.asPlayer(sender), BattlePreferences.FLAG_DISPLAY, current
            );

            if (active) {
                sender.sendMessage(C.cmdPass() + "Alerts " + C.hl("enabled") + " on the " + C.hl(channel.name().toLowerCase()) + " channel");
            } else {
                sender.sendMessage(C.cmdPass() + "Alerts " + C.hl("disabled") + " on the " + C.hl(channel.name().toLowerCase()) + " channel");
            }

            return true;
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
            if (args.size() == 1) {
                return Arrays.stream(FlagDisplayChannel.values()).map(Enum::name).toList();
            } else if (args.size() == 2) {
                return List.of("on", "off");
            }

            return super.getTabCompletions(sender, alias, args);
        }

    }

    public class TypeCommand extends EasyCommand {

        public TypeCommand() {
            super("type");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            if (args.isEmpty()) {
                return false;
            }

            BattleOptionsInput.FlagType flagType = BattleOptionsInput.FlagType.valueOf(args.get(0).toUpperCase());
            FlagCommands.this.battle.getGame().setupFlags(flagType);
            sender.sendMessage(C.cmdPass() + "Changed to flag type " + C.hl(flagType.name().toLowerCase()));

            try {
                FlagCommands.this.battle.getOptions().edit(opts -> {
                    opts.getGame().setFlagType(flagType);
                });
            } catch (IOException e) {
                sender.sendMessage(C.cmdFail() + "Failed to save changes");
                return false;
            }

            return true;
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
            return Arrays.stream(BattleOptionsInput.FlagType.values()).map(Enum::name).toList();
        }
    }

}
