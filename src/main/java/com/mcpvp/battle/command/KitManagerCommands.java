package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.kit.BattleKitManager;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;
import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.kit.KitInfo;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class KitManagerCommands extends EasyCommandGroup {

    private final Battle battle;
    private final BattleKitManager kitManager;

    public KitManagerCommands(Battle battle, BattleKitManager kitManager) {
        super(battle.getPlugin(), "kits");
        this.battle = battle;
        this.kitManager = kitManager;
        this.addCommand(new SummaryCommand(), true);
        this.addCommand(new DisableCommand());
        this.addCommand(new EnableCommand());
        this.addCommand(new LimitCommands());
    }

    private List<String> completeKits() {
        List<String> kits = new ArrayList<>();
        kits.add("all");
        kits.addAll(this.kitManager.getKitDefinitions().stream().map(KitInfo::getName).toList());
        return kits;
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

            // Archer * 1, players: new_instance (1)
            StringBuilder sb = new StringBuilder(C.info(C.PURPLE) + "Limited: ");
            if (!limited.isEmpty()) {
                sb.append(limited.stream().map(kit -> {
                    StringBuilder inner = new StringBuilder("\n    ");
                    inner.append(kit.getName());
                    inner.append(": ");
                    inner.append(C.R);
                    inner.append(KitManagerCommands.this.kitManager.getLimit(kit).map(Object::toString).orElse("none"));

                    Map<BattleTeam, List<Player>> playing = new HashMap<>();
                    KitManagerCommands.this.battle.getGame().getTeamManager().getTeams().forEach(team -> {
                        List<Player> onTeam = team.getPlayers().stream()
                            .filter(player -> KitManagerCommands.this.battle.getKitManager().isSelected(player, kit))
                            .toList();

                        if (!onTeam.isEmpty()) {
                            playing.put(team, onTeam);
                        }
                    });

                    if (!playing.isEmpty()) {
                        inner.append(C.GRAY);
                        inner.append(" [");
                        playing.forEach((battleTeam, players) -> {
                            inner.append(players.stream().map(player -> {
                                return battleTeam.getColor().getChat() + player.getName();
                            }).collect(Collectors.joining(C.GRAY + ", ")));

                            inner.append(" ");
                            inner.append(C.GRAY);
                            inner.append("(");
                            inner.append(C.hl(players.size()));
                            inner.append(")");
                        });
                        inner.append(C.GRAY);
                        inner.append("]");
                    }

                    return inner.toString();
                }).collect(Collectors.joining(C.GRAY + "\n")));
            } else {
                sb.append(C.R);
                sb.append("none");
            }

            sender.sendMessage(sb.toString());

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
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
            return KitManagerCommands.this.completeKits();
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
        public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
            return KitManagerCommands.this.completeKits();
        }

    }

    public class LimitCommands extends EasyCommandGroup {

        public LimitCommands() {
            super(KitManagerCommands.this.battle.getPlugin(), "limit");
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

                int limit = Integer.parseInt(args.get(1));
                kits.forEach(kit -> {
                    KitManagerCommands.this.kitManager.setLimit(kit, limit);
                });
                sender.sendMessage(C.cmdPass() + "Limited " + C.R + kits.stream()
                    .map(KitDefinition::getName)
                    .collect(Collectors.joining(C.GRAY + ", " + C.R)) + C.GRAY + " to " + C.hl(limit)
                );

                return true;
            }

            @Override
            public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
                return KitManagerCommands.this.completeKits();
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
            public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
                return KitManagerCommands.this.completeKits();
            }

        }

    }

}
