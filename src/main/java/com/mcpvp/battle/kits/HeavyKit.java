package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.kit.KitItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class HeavyKit extends BattleKit {

    public HeavyKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Heavy";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[] {
                new ItemStack(Material.DIAMOND_BOOTS),
                new ItemStack(Material.DIAMOND_LEGGINGS),
                new ItemStack(Material.DIAMOND_CHESTPLATE),
                new ItemStack(Material.DIAMOND_HELMET)
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        return new KitInventoryBuilder()
                .add(Material.DIAMOND_SWORD)
                .addFood(3)
                .addCompass(8)
                .build();
    }

}
