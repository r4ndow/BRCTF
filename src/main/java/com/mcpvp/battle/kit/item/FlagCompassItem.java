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

import java.util.HashMap;
import java.util.Map;

public class FlagCompassItem extends KitItem {

    private static final Map<Player, BattleTeam> TARGETS = new HashMap<>();

    private final BattleGame game;
    private final Player player;

    public FlagCompassItem(Kit kit, BattleGame game) {
        super(kit, ItemBuilder.of(Material.COMPASS).name("Pointing to ??? flag").build());
        this.game = game;
        this.player = kit.getPlayer();
        this.onInteract(ev -> this.toggle());

        if (this.getTarget() == null) {
            this.setTarget(this.game.getTeamManager().getTeam(this.player));
        }

        this.rename();
    }

    public void toggle() {
        if (this.getTarget() != null) {
            this.setTarget(this.game.getTeamManager().getNext(this.getTarget()));
            this.rename();
        }
    }

    private void rename() {
        if (this.getTarget() == this.game.getTeamManager().getTeam(this.player)) {
            this.modify(ib -> ib.name("Pointing to " + this.getTarget().getColoredName() + " flag").dummyEnchant());
        } else {
            this.modify(ib -> ib.name("Pointing to " + this.getTarget().getColoredName() + " flag").removeDummyEnchant());
        }
        this.update(this.kit.getPlayer().getInventory());
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (this.getTarget() != null && this.kit.getPlayer().getCompassTarget() != this.getTarget().getFlag().getLocation()) {
            this.kit.getPlayer().setCompassTarget(this.getTarget().getFlag().getLocation());
        }
    }

    private BattleTeam getTarget() {
        return TARGETS.get(this.player);
    }

    private void setTarget(BattleTeam target) {
        TARGETS.put(this.player, target);
    }

}
