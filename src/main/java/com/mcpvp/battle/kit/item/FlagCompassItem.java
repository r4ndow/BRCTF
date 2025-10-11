package com.mcpvp.battle.kit.item;

import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class FlagCompassItem extends KitItem {

    private final BattleGame game;
    private final Player player;
    private BattleTeam target;

    public FlagCompassItem(Kit kit, BattleGame game) {
        super(kit, ItemBuilder.of(Material.COMPASS).name("Pointing to ??? flag").build());
        this.game = game;
        this.player = kit.getPlayer();
        this.target = game.getTeamManager().getTeam(kit.getPlayer());
        this.onInteract(ev -> this.toggle());
        this.toggle();
    }

    public void toggle() {
        if (this.target != null) {
            this.target = this.game.getTeamManager().getNext(this.target);
            if (this.target == this.game.getTeamManager().getTeam(this.player)) {
                this.modify(ib -> ib.name("Pointing to " + this.target.getColoredName() + " flag").dummyEnchant());
            } else {
                this.modify(ib -> ib.name("Pointing to " + this.target.getColoredName() + " flag").removeDummyEnchant());
            }

            this.update(this.kit.getPlayer().getInventory());
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (this.target != null && this.kit.getPlayer().getCompassTarget() != this.target.getFlag().getLocation()) {
            this.kit.getPlayer().setCompassTarget(this.target.getFlag().getLocation());
        }
    }

}
