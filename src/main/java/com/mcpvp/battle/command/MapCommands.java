package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import org.bukkit.command.CommandSender;

import java.util.List;

public class MapCommands extends EasyCommandGroup {

    private final Battle battle;

    public MapCommands(Battle battle) {
        super("map");
        this.battle = battle;

        this.addCommand(new InfoCommand(), true);
        this.addCommand(new OverrideCommand());
        this.addCommand(new NextCommand());
    }

    public class InfoCommand extends EasyCommand {

        public InfoCommand() {
            super("info");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            BattleMapData map = MapCommands.this.battle.getGame().getMap();
            sender.sendMessage(C.cmdPass() + C.hl(map.getName()) + " by " + C.hl(map.getAuthor()) + " [" + map.getId() + "]");
            return true;
        }

    }

    public class OverrideCommand extends EasyCommand {

        public OverrideCommand() {
            super("override");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            for (String id : args) {
                if (!MapCommands.this.battle.getMapManager().isMap(Integer.parseInt(id))) {
                    sender.sendMessage(C.cmdFail() + C.hl(id) + " is not a valid map");
                    return false;
                }
            }

            MapCommands.this.battle.getMapManager().setOverride(args.stream().map(Integer::parseInt).toList());
            sender.sendMessage(C.cmdPass() + "The requested maps will play on reboot");
            return true;
        }

    }

    public class NextCommand extends EasyCommand {

        public NextCommand() {
            super("next");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            MapCommands.this.battle.getMatch().insertNextGame(Integer.parseInt(args.get(0)));
            sender.sendMessage(C.cmdPass() + "The requested map will play next");
            return true;
        }

    }

}
