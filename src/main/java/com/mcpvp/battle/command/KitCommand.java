package com.mcpvp.battle.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mcpvp.battle.kit.BattleKitManager;

public class KitCommand extends BattleCommand {

    private final BattleKitManager kitManager;

    public KitCommand(BattleKitManager kitManager) {
        super("kit");
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        Player player = asPlayer(sender);
        
        this.kitManager.getKitTypes().stream().filter(type -> type.getBackingKit().getName().equalsIgnoreCase(args.get(0))).findAny().ifPresent(kt -> {
            player.sendMessage("Selected " + kt.getBackingKit().getName());
            kitManager.setSelected(player, kt.getKitType(), false);
        });

        return true;
    }
    


}
