package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TimerCommand extends BattleCommandGroup {

    private final Battle battle;

    public TimerCommand(Battle battle) {
        super("timer");
        this.battle = battle;

        addCommand(new SetCommand());
        addCommand(new LockCommand());
        addCommand(new UnlockCommand());
        addCommand(new SkipCommand());
    }

    public class SetCommand extends BattleCommand {

        public SetCommand() {
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

    public class LockCommand extends BattleCommand {

        public LockCommand() {
            super("lock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            battle.getMatch().getTimer().setPaused(true);
            return true;
        }

    }

    public class UnlockCommand extends BattleCommand {

        public UnlockCommand() {
            super("unlock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            battle.getMatch().getTimer().setPaused(false);
            return true;
        }

    }


    public class SkipCommand extends BattleCommand {

        public SkipCommand() {
            super("skip");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            battle.getMatch().advanceStateOrGame();
            return true;
        }

    }

}
