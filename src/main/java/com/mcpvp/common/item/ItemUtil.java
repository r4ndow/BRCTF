package com.mcpvp.common.item;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {

    public static String getName(ItemStack stack) {
        if (stack == null) {
            return "";
        }

        if (stack.getItemMeta() == null) {
            return "";
        }

        String displayed = stack.getItemMeta().getDisplayName();
        if (displayed == null) {
            return "";
        }

        return displayed;
    }

    public static float getItemDurability(ItemStack item) {
        if (item != null) {
            return (float) (item.getType().getMaxDurability() - item.getDurability()) / item.getType().getMaxDurability();
        }
        return -1;
    }

    public static void setDescription(ItemStack stack, List<String> description) {
        if (stack == null) {
            return;
        }
        if (description == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        ArrayList<String> lore = new ArrayList<>();
        for (String line : description) {
            if (line != null) {
                lore.add(ChatColor.RESET + line);
            }
        }

        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }

        stack.setItemMeta(meta);
    }


}
