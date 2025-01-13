package com.mcpvp.battle.util;

import com.mcpvp.common.item.ItemBuilder;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BattleUtil {

    public static Item spawnWool(Location location, ItemStack item) {
        Location l = location.clone().add(0, .5, 0);
        Item i = location.getWorld().dropItem(l, item);
        i.setVelocity(new Vector(0, 0.15, 0));
        return i;
    }

    public static ItemStack getColoredWool(DyeColor color) {
        return ItemBuilder.of(Material.WOOL).color(color).build();
    }

}
