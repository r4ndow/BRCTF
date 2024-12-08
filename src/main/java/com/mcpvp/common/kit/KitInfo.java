package com.mcpvp.common.kit;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

public interface KitInfo {
    
    String getName();

    ItemStack[] getArmor();

    Map<Integer, ItemStack> getItems();

}
