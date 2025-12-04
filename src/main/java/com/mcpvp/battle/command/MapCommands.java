package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

public class MapCommands extends EasyCommandGroup {

    private final Battle battle;

    public MapCommands(Battle battle) {
        super(battle.getPlugin(), "map");
        this.battle = battle;

        this.addCommand(new InfoCommand(), true);
        this.addCommand(new SearchCommand());
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
            sender.sendMessage(
                    C.cmdPass()
                            + C.hl(map.getName())
                            + " by "
                            + C.hl(map.getAuthor())
                            + " ["
                            + map.getId()
                            + "]"
            );
            return true;
        }
    }

    public class SearchCommand extends EasyCommand {

        private static final int MAX_RESULTS = 35;

        public SearchCommand() {
            super("search");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            if (args.isEmpty()) {
                return false;
            }

            String query = String.join(" ", args).toLowerCase(Locale.ENGLISH);

            @SuppressWarnings("unchecked")
            List<BattleMapData> matches = ((List<Object>) MapCommands.this.battle
                    .getMapManager()
                    .getFunctional())
                    .stream()
                    .map(obj -> (BattleMapData) obj)
                    .filter(map -> {
                        String name = map.getName();
                        return name != null
                                && name.toLowerCase(Locale.ENGLISH).contains(query);
                    })
                    .limit(MAX_RESULTS)
                    .toList();

            if (matches.isEmpty()) {
                sender.sendMessage(
                        C.cmdFail()
                                + "No maps found matching "
                                + C.hl(query)
                );
                return true;
            }

            sender.sendMessage(
                    C.cmdPass()
                            + "Found "
                            + C.hl(matches.size())
                            + " map(s):"
            );

            for (BattleMapData map : matches) {
                String author = map.getAuthor() != null ? map.getAuthor() : "unknown";
                sender.sendMessage(
                        C.GRAY
                                + "- "
                                + C.hl(map.getName())
                                + " by "
                                + C.hl(author)
                                + " ["
                                + map.getId()
                                + "]"
                );
            }

            sender.sendMessage(
                    C.info(C.PURPLE)
                            + "Use "
                            + C.hl("/map next <id>")
                            + " to queue a map."
            );

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

            MapCommands.this.battle
                    .getMapManager()
                    .setOverride(args.stream().map(Integer::parseInt).toList());
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
            MapCommands.this.battle
                    .getMatch()
                    .insertNextGame(Integer.parseInt(args.get(0)));
            sender.sendMessage(C.cmdPass() + "The requested map will play next");
            return true;
        }
    }
}
