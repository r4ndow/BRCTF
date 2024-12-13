package com.mcpvp.battle.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.mcpvp.battle.Battle;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;

public class TimerCommand extends EasyCommandGroup {
    
    private final Battle battle;

    public TimerCommand(Battle battle) {
        super("timer");
        this.battle = battle;

        addCommand(new TimerSetCommand());
        addCommand(new TimerLockCommand());
        addCommand(new TimerUnlockCommand());
    }

    public class TimerSetCommand extends EasyCommand {

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

    public class TimerLockCommand extends EasyCommand {

        public TimerLockCommand() {
            super("lock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            battle.getMatch().getTimer().setPaused(true);
            return true;
        }

    }

    public class TimerUnlockCommand extends EasyCommand {

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
