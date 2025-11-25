package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TimerCommand extends EasyCommandGroup {

    private final Battle battle;

    public TimerCommand(Battle battle) {
        super("timer");
        this.battle = battle;

        this.addCommand(new SetCommand());
        this.addCommand(new LockCommand());
        this.addCommand(new UnlockCommand());
        this.addCommand(new SkipCommand());
    }

    public class SetCommand extends EasyCommand {

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
                TimerCommand.this.battle.getMatch().getTimer().setSeconds(set);
                sender.sendMessage(C.cmdPass() + "Timer set to " + C.hl(set) + " seconds");
                return true;
            } catch (Exception e) {
                return false;
            }
        }

    }

    public class LockCommand extends EasyCommand {

        public LockCommand() {
            super("lock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            TimerCommand.this.battle.getMatch().getTimer().setPaused(true);
            sender.sendMessage(C.cmdPass() + "Timer locked!");
            return true;
        }

    }

    public class UnlockCommand extends EasyCommand {

        public UnlockCommand() {
            super("unlock");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            TimerCommand.this.battle.getMatch().getTimer().setPaused(false);
            sender.sendMessage(C.cmdPass() + "Timer unlocked!");
            return true;
        }

    }

    public class SkipCommand extends EasyCommand {

        public SkipCommand() {
            super("skip");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            TimerCommand.this.battle.getMatch().advanceStateOrGame();
            sender.sendMessage(C.cmdPass() + "Skipping...");
            return true;
        }

    }

}
