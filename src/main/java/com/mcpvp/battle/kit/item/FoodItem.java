package com.mcpvp.battle.kit.item;

import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import org.bukkit.inventory.ItemStack;

public class FoodItem extends KitItem {

    public FoodItem(Kit kit, ItemStack itemStack) {
        super(kit, itemStack, true);
        this.onInteract(ev -> {
            if (EventUtil.isRightClick(ev)) {
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

        this.kit.getPlayer().setHealth(Math.min(this.kit.getPlayer().getHealth() + 8, this.kit.getPlayer().getMaxHealth()));
        this.decrement(true);
    }

}
