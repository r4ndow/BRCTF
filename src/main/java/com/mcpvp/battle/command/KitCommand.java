package com.mcpvp.battle.command;

import com.mcpvp.battle.kit.BattleKitManager;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.kit.KitAttemptSelectEvent;
import com.mcpvp.common.kit.KitDefinition;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class KitCommand extends EasyCommand {

    private final BattleKitManager kitManager;

    public KitCommand(BattleKitManager kitManager) {
        // Aliases are handled in the plugin.yml
        super("kit");
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        Player player = this.asPlayer(sender);

        List<KitDefinition> eligible = this.kitManager.getKitDefinitions().stream()
            .filter(Predicate.not(this.kitManager::isDisabled))
            .toList();
        boolean direct = label.equals("kit") || label.equals("class");

        KitDefinition kit;
        if ((direct && args.contains("random")) || label.equals("random")) {
            kit = eligible.get(new Random().nextInt(eligible.size()));
        } else if (direct) {
            if (args.isEmpty()) {
                sender.sendMessage(C.B + "Available Classes");
                sender.sendMessage(C.info(C.PURPLE) + eligible.stream().map(KitDefinition::getName).collect(Collectors.joining(", ")));
                return true;
            } else {
                kit = this.kitManager.getKitDefinition(args.get(0));
            }
        } else {
            kit = this.kitManager.getKitDefinition(label);
        }

        if (kit != null) {
            KitAttemptSelectEvent kitAttemptSelectEvent = this.kitManager.setSelected(
                player, kit, false, !(args.contains("-f") && sender.isOp())
            );

            if (!kitAttemptSelectEvent.isCancelled()) {
                player.sendMessage(C.cmdPass() + "Selected " + C.hl(kit.getName()));
                return true;
            } else {
                player.sendMessage(C.cmdFail() + kitAttemptSelectEvent.getDenial());
                return false;
            }
        }

        return false;
    }


}
