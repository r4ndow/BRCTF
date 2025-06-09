package com.mcpvp.battle.command;

import com.mcpvp.battle.kit.BattleKitManager;
import com.mcpvp.common.kit.KitAttemptSelectEvent;
import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.kit.KitInfo;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.util.chat.C;
import com.mcpvp.common.util.nms.ActionbarUtil;
import com.mcpvp.common.util.nms.TitleUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class KitCommand extends BattleCommand {

    private final BattleKitManager kitManager;

    public KitCommand(BattleKitManager kitManager) {
        super("kit", kitManager.getKitDefinitions().stream()
                .map(KitInfo::getName)
                .map(String::toLowerCase)
                .toList()
        );
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        Player player = asPlayer(sender);

        KitDefinition kit;
        if (label.equals("kit")) {
            kit = this.kitManager.getKitDefinition(args.get(0));
        } else {
            kit = this.kitManager.getKitDefinition(label);
        }

        if (kit != null) {
            KitAttemptSelectEvent kitAttemptSelectEvent = kitManager.setSelected(player, kit, false);
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
