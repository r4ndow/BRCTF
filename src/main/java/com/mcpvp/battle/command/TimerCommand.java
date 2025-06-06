package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TimerCommand extends BattleCommandGroup {

    private final Battle battle;

    public TimerCommand(Battle battle) {
        super("timer");
        this.battle = battle;

        addCommand(new TimerSetCommand());
        addCommand(new TimerLockCommand());
        addCommand(new TimerUnlockCommand());
    }

    public class TimerSetCommand extends BattleCommand {

        public TimerSetCommand() {
            super("set");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            if (args.isEmpty()) {
                return false;
            }

            try {
                int set = Integer.parseInt(args.get(0));
                battle.getMatch().getTimer().setSeconds(set);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

    }

    public class TimerLockCommand extends BattleCommand {

        public TimerLockCommand() {
            super("lock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            battle.getMatch().getTimer().setPaused(true);
            return true;
        }

    }

    public class TimerUnlockCommand extends BattleCommand {

        public TimerUnlockCommand() {
            super("unlock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            battle.getMatch().getTimer().setPaused(false);
            return true;
        }

    }

}
