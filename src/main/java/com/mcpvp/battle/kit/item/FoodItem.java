package com.mcpvp.battle.kit.item;

import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

public class FoodItem extends KitItem {

    public FoodItem(Kit kit, ItemStack itemStack) {
        super(kit, itemStack, true);
        this.onInteract(ev -> {
            if (EventUtil.isRightClick(ev)) {
                // Canceling the interaction event is very important, otherwise the items get out of sync
                ev.setCancelled(true);
                this.consume();
            }
        });
    }

    protected void consume() {
        if (this.isPlaceholder()) {
            return;
        }

        if (this.kit.getPlayer().getHealth() == this.kit.getPlayer().getMaxHealth()) {
            return;
        }

        this.kit.getPlayer().playSound(
                this.kit.getPlayer().getLocation(),
                Sound.EAT,
                0.6f,
                1.0f
        );

        this.kit.getPlayer().setHealth(Math.min(this.kit.getPlayer().getHealth() + 8, this.kit.getPlayer().getMaxHealth()));
        this.decrement(true);
    }

}
