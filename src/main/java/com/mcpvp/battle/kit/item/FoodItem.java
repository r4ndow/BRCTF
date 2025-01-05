package com.mcpvp.battle.kit.item;

import org.bukkit.inventory.ItemStack;

import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;

public class FoodItem extends KitItem {
    
    public FoodItem(Kit kit, ItemStack itemStack) {
        super(kit, itemStack);
        this.onInteract(ev -> {
            if (EventUtil.isRightClick(ev)) {
                consume();
            }
        });
    }

    protected void consume() {
        if (isPlaceholder()) {
            return;
        }

        if (kit.getPlayer().getHealth() == kit.getPlayer().getMaxHealth()) {
            return;
        }

        kit.getPlayer().setHealth(Math.min(kit.getPlayer().getHealth() + 8, kit.getPlayer().getMaxHealth()));
        decrement(true);
    }

}
