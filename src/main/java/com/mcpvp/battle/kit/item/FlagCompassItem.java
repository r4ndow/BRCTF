package com.mcpvp.battle.kit.item;

import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;

public class FlagCompassItem extends KitItem {

    private final BattleGame game;
    private BattleTeam target;

    public FlagCompassItem(BattleGame game, Kit kit) {
        super(kit, ItemBuilder.of(Material.COMPASS).name("Pointing to ??? flag").build());
        this.game = game;
        this.target = game.getTeamManager().getTeam(kit.getPlayer());
        this.onInteract(ev -> this.toggle());
        this.toggle();
    }

    public void toggle() {
        if (this.target != null) {
            target = game.getTeamManager().getNext(target);
            modify(ib -> ib.name("Pointing to " + target.getColoredName() + " flag"));
            update(kit.getPlayer().getInventory());
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (this.target != null && kit.getPlayer().getCompassTarget() != target.getFlag().getLocation()) {
            kit.getPlayer().setCompassTarget(target.getFlag().getLocation());
        }
    }

}
