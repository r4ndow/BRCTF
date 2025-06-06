package com.mcpvp.battle.command;

import com.mcpvp.battle.kit.BattleKitManager;
import com.mcpvp.battle.util.cmd.CmdUtil;
import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.kit.KitInfo;
import com.mcpvp.common.util.chat.C;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class KitManagerCommands extends BattleCommandGroup {

    private final BattleKitManager kitManager;

    public KitManagerCommands(BattleKitManager kitManager) {
        super("kits", List.of("classmanager", "classes"));
        this.kitManager = kitManager;
        addCommand(new SummaryCommand(), true);
        addCommand(new DisableCommand());
        addCommand(new EnableCommand());
        addCommand(new LimitCommands());
    }

    private List<String> completeKits(String arg) {
        List<String> kits = new ArrayList<>();
        kits.add("all");
        kits.addAll(kitManager.getKitDefinitions().stream().map(KitInfo::getName).toList());
        return CmdUtil.partialMatches(kits, arg);
    }

    private List<KitDefinition> findKit(String arg) {
        if (arg.equalsIgnoreCase("all")) {
            return kitManager.getKitDefinitions();
        }
        return List.of(kitManager.getKitDefinition(arg));
    }

    public class SummaryCommand extends BattleCommand {

        protected SummaryCommand() {
            super("summary");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            List<KitDefinition> disabled = kitManager.getKitDefinitions().stream().filter(kitManager::isDisabled).toList();
            List<KitDefinition> limited = kitManager.getKitDefinitions().stream()
                    .filter(kit -> kitManager.getLimit(kit).isPresent())
                    .sorted(Comparator.comparingInt(kit -> -kitManager.getLimit(kit).orElse(0)))
                    .toList();

            sender.sendMessage(C.BOLD + "Class Management Summary");

            String disabledText = disabled.stream().map(KitDefinition::getName).collect(Collectors.joining(C.GRAY + ", " + C.R));
            if (disabledText.isBlank()) {
                disabledText = C.R + "none";
            }
            sender.sendMessage(C.info(C.RED) + "Disabled: " + disabledText);

            String limitedText = limited.stream().map(kit ->
                    C.R + kit.getName() + C.GRAY + " × " + C.R + kitManager.getLimit(kit).map(Object::toString).orElse("none")
            ).collect(Collectors.joining(C.GRAY + ", "));
            if (limitedText.isBlank()) {
                limitedText = C.R + "none";
            }
            sender.sendMessage(C.info(C.PURPLE) + "Limited: " + limitedText);

            return false;
        }

    }

    public class DisableCommand extends BattleCommand {

        protected DisableCommand() {
            super("disable");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            List<KitDefinition> kits = findKit(args.get(0));
            if (kits.isEmpty()) {
                return false;
            }

            kits.forEach(kitManager::setDisabled);
            sender.sendMessage(C.cmdPass() + "Disabled " + kits.stream().map(KitDefinition::getName).collect(Collectors.joining(", ")));

            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return completeKits(arg);
        }

    }

    public class EnableCommand extends BattleCommand {

        protected EnableCommand() {
            super("enable");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            List<KitDefinition> kits = findKit(args.get(0));
            if (kits.isEmpty()) {
                return false;
            }

            kits.forEach(kitManager::setEnabled);
            sender.sendMessage(C.cmdPass() + "Enabled " + kits.stream().map(KitDefinition::getName).collect(Collectors.joining(", ")));

            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return completeKits(arg);
        }

    }

    public class LimitCommands extends BattleCommandGroup {

        public LimitCommands() {
            super("limit");
            addCommand(new SetCommand());
            addCommand(new RemoveCommand());
        }

        public class SetCommand extends BattleCommand {

            protected SetCommand() {
                super("set");
            }

            @Override
            public boolean onCommand(CommandSender sender, String label, List<String> args) {
                if (args.size() < 2) {
                    return false;
                }

                List<KitDefinition> kits = findKit(args.get(0));
                if (kits.isEmpty()) {
                    return false;
                }

                kits.forEach(kit -> {
                    kitManager.setLimit(kit, Integer.parseInt(args.get(1)));
                });
                sender.sendMessage(C.cmdPass() + "Limited " + C.R + kits.stream()
                        .map(KitDefinition::getName)
                        .collect(Collectors.joining(C.GRAY + ", " + C.R))
                );

                return true;
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String alias, List<String> args) {
                if (args.size() == 1) {
                    return completeKits(args.get(0));
                }
                return super.onTabComplete(sender, alias, args);
            }

        }

        public class RemoveCommand extends BattleCommand {

            protected RemoveCommand() {
                super("remove");
            }

            @Override
            public boolean onCommand(CommandSender sender, String label, List<String> args) {
                List<KitDefinition> kits = findKit(args.get(0));
                if (kits.isEmpty()) {
                    return false;
                }

                kits.forEach(kitManager::removeLimit);
                sender.sendMessage(C.cmdPass() + "Removed limit on " + C.R + kits.stream()
                        .map(KitDefinition::getName)
                        .collect(Collectors.joining(C.GRAY + ", " + C.R))
                );

                return true;
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
                return completeKits(arg);
            }

        }

    }

}
