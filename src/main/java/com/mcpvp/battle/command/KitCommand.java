package com.mcpvp.battle.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mcpvp.battle.kit.BattleKitManager;
import com.mcpvp.common.kit.KitDefinition;

public class KitCommand extends BattleCommand {

    private final BattleKitManager kitManager;

    public KitCommand(BattleKitManager kitManager) {
        super("kit");
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        Player player = asPlayer(sender);
        
        KitDefinition kit = this.kitManager.getKitDefinition(args.get(0));
        if (kit != null) {
            if (kitManager.setSelected(player, kit, false)) {
                player.sendMessage("Selected " + kit.getName());
                return true;
            } else {
                player.sendMessage("Not allowed :(");
                return false;
            }
        }

        return false;
    }
    


}
