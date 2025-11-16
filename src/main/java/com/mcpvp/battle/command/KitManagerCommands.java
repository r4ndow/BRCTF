package com.mcpvp.battle.command;

import com.mcpvp.battle.kit.BattleKitManager;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.CommandUtil;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.kit.KitInfo;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class KitManagerCommands extends EasyCommandGroup {

    private final BattleKitManager kitManager;

    public KitManagerCommands(BattleKitManager kitManager) {
        super("kits");
        this.kitManager = kitManager;
        this.addCommand(new SummaryCommand(), true);
        this.addCommand(new DisableCommand());
        this.addCommand(new EnableCommand());
        this.addCommand(new LimitCommands());
    }

    private List<String> completeKits(String arg) {
        List<String> kits = new ArrayList<>();
        kits.add("all");
        kits.addAll(this.kitManager.getKitDefinitions().stream().map(KitInfo::getName).toList());
        return CommandUtil.partialMatches(kits, arg);
    }

    private List<KitDefinition> findKit(String arg) {
        if (arg.equalsIgnoreCase("all")) {
            return this.kitManager.getKitDefinitions();
        }
        return List.of(this.kitManager.getKitDefinition(arg));
    }

    public class SummaryCommand extends EasyCommand {

        protected SummaryCommand() {
            super("summary");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            List<KitDefinition> disabled = KitManagerCommands.this.kitManager.getKitDefinitions().stream().filter(KitManagerCommands.this.kitManager::isDisabled).toList();
            List<KitDefinition> limited = KitManagerCommands.this.kitManager.getKitDefinitions().stream()
                .filter(kit -> KitManagerCommands.this.kitManager.getLimit(kit).isPresent())
                .sorted(Comparator.comparingInt(kit -> -KitManagerCommands.this.kitManager.getLimit(kit).orElse(0)))
                .toList();

            sender.sendMessage(C.BOLD + "Class Management Summary");

            String disabledText = disabled.stream().map(KitDefinition::getName).collect(Collectors.joining(C.GRAY + ", " + C.R));
            if (disabledText.isBlank()) {
                disabledText = C.R + "none";
            }
            sender.sendMessage(C.info(C.RED) + "Disabled: " + disabledText);

            String limitedText = limited.stream().map(kit ->
                C.R + kit.getName() + C.GRAY + " × " + C.R + KitManagerCommands.this.kitManager.getLimit(kit).map(Object::toString).orElse("none")
            ).collect(Collectors.joining(C.GRAY + ", "));
            if (limitedText.isBlank()) {
                limitedText = C.R + "none";
            }
            sender.sendMessage(C.info(C.PURPLE) + "Limited: " + limitedText);

            return false;
        }

    }

    public class DisableCommand extends EasyCommand {

        protected DisableCommand() {
            super("disable");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            List<KitDefinition> kits = KitManagerCommands.this.findKit(args.get(0));
            if (kits.isEmpty()) {
                return false;
            }

            kits.forEach(KitManagerCommands.this.kitManager::setDisabled);
            sender.sendMessage(C.cmdPass() + "Disabled " + kits.stream().map(KitDefinition::getName).collect(Collectors.joining(", ")));

            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return KitManagerCommands.this.completeKits(arg);
        }

    }

    public class EnableCommand extends EasyCommand {

        protected EnableCommand() {
            super("enable");
        }

        @Override
        public boolean onCommand(CommandSender sender, String label, List<String> args) {
            List<KitDefinition> kits = KitManagerCommands.this.findKit(args.get(0));
            if (kits.isEmpty()) {
                return false;
            }

            kits.forEach(KitManagerCommands.this.kitManager::setEnabled);
            sender.sendMessage(C.cmdPass() + "Enabled " + kits.stream().map(KitDefinition::getName).collect(Collectors.joining(", ")));

            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
            return KitManagerCommands.this.completeKits(arg);
        }

    }

    public class LimitCommands extends EasyCommandGroup {

        public LimitCommands() {
            super("limit");
            this.addCommand(new SetCommand());
            this.addCommand(new RemoveCommand());
        }

        public class SetCommand extends EasyCommand {

            protected SetCommand() {
                super("set");
            }

            @Override
            public boolean onCommand(CommandSender sender, String label, List<String> args) {
                if (args.size() < 2) {
                    return false;
                }

                List<KitDefinition> kits = KitManagerCommands.this.findKit(args.get(0));
                if (kits.isEmpty()) {
                    return false;
                }

                kits.forEach(kit -> {
                    KitManagerCommands.this.kitManager.setLimit(kit, Integer.parseInt(args.get(1)));
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
                    return KitManagerCommands.this.completeKits(args.get(0));
                }
                return super.onTabComplete(sender, alias, args);
            }

        }

        public class RemoveCommand extends EasyCommand {

            protected RemoveCommand() {
                super("remove");
            }

            @Override
            public boolean onCommand(CommandSender sender, String label, List<String> args) {
                List<KitDefinition> kits = KitManagerCommands.this.findKit(args.get(0));
                if (kits.isEmpty()) {
                    return false;
                }

                kits.forEach(KitManagerCommands.this.kitManager::removeLimit);
                sender.sendMessage(C.cmdPass() + "Removed limit on " + C.R + kits.stream()
                    .map(KitDefinition::getName)
                    .collect(Collectors.joining(C.GRAY + ", " + C.R))
                );

                return true;
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String alias, String arg) {
                return KitManagerCommands.this.completeKits(arg);
            }

        }

    }

}
