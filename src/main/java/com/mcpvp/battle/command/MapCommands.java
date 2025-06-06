package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.common.util.chat.C;
import org.bukkit.command.CommandSender;

import java.util.List;

public class MapCommands extends BattleCommandGroup {

    private final Battle battle;

    public MapCommands(Battle battle) {
        super("map");
        this.battle = battle;

        addCommand(new InfoCommand(), true);
        addCommand(new OverrideCommand());
    }

    public class InfoCommand extends BattleCommand {

        public InfoCommand() {
            super("info");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            BattleMapData map = battle.getGame().getMap();
            sender.sendMessage(C.cmdPass() + C.hl(map.getName()) + " by " + C.hl(map.getAuthor()) + " [" + map.getId() + "]");
            return true;
        }

    }

    public class OverrideCommand extends BattleCommand {

        public OverrideCommand() {
            super("override");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            battle.getMapManager().setOverride(args.stream().map(Integer::parseInt).toList());
            sender.sendMessage(C.cmdPass() + "The requested maps will play on reboot");
            return true;
        }

    }

}
