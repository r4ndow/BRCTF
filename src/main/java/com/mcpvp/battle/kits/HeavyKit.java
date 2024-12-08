package com.mcpvp.battle.kits;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;

public class HeavyKit extends BattleKit {

    public HeavyKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Heavy";
    }

    @Override
    public ItemStack[] getArmor() {
        return new ItemStack[]{
            new ItemStack(Material.DIAMOND_HELMET),
            new ItemStack(Material.DIAMOND_CHESTPLATE),
            new ItemStack(Material.DIAMOND_LEGGINGS),
            new ItemStack(Material.DIAMOND_BOOTS)
        };
    }

    @Override
    public Map<Integer, ItemStack> getItems() {
        return new KitInventoryBuilder()
            .add(Material.DIAMOND_SWORD)
            .addFood(3)
            .build();
    }

    @Override
    public Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Battle");
    }
    
}
