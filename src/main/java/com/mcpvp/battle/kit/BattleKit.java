package com.mcpvp.battle.kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;

public abstract class BattleKit extends Kit {
    
    public BattleKit(BattlePlugin plugin, @Nullable Player player) {
        super(plugin, player);
    }

    protected Battle getBattle() {
        return ((BattlePlugin) plugin).getBattle();
    }

    public class KitInventoryBuilder {

        private static final int INVENTORY_SIZE = 27 + 9;
        private final List<ItemStack> items = new ArrayList<>(INVENTORY_SIZE);

        private int currentSlot = 0;

        public KitInventoryBuilder add(Material material) {
            items.set(currentSlot++, autoAdjust(ItemBuilder.of(material)).build());
            return this;
        }

        public KitInventoryBuilder addFood(int count) {
            items.set(currentSlot++, ItemBuilder.of(Material.COOKED_BEEF).name(getName() + " Food").build());
            return this;
        }

        private ItemBuilder autoAdjust(ItemBuilder itemBuilder) {
            return itemBuilder.unbreakable().name(
                getName() + " " + StringUtils.capitalize(itemBuilder.build().getType().name().toLowerCase())
            );
        }

        public Map<Integer, ItemStack> build() {
            Map<Integer, ItemStack> map = new HashMap<>();
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                if (items.get(i) != null) {
                    map.put(i, items.get(i));
                }
            }
            return map;
        }

    }

}
