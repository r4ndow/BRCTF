package com.mcpvp.battle.kit;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.common.item.InteractiveItem;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;

public abstract class BattleKit extends Kit {

    public BattleKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    protected Battle getBattle() {
        return ((BattlePlugin) plugin).getBattle();
    }

    public class BattleKitItem extends InteractiveItem {

        public BattleKitItem(ItemStack item) {
            super(plugin, item);
        }

        public BattleKitItem(ItemBuilder builder) {
            super(plugin, builder);
        }

    }

    public class KitInventoryBuilder {

        private static final int INVENTORY_SIZE = 9 * 4;
        private final ItemStack[] items = new ItemStack[INVENTORY_SIZE];

        private int currentSlot = 0;

        public KitInventoryBuilder add(Material material) {
            items[currentSlot++] = autoAdjust(ItemBuilder.of(material)).build();
            return this;
        }

        public KitInventoryBuilder add(ItemBuilder builder) {
            items[currentSlot++] = autoAdjust(builder).build();
            return this;
        }

        public KitInventoryBuilder add(InteractiveItem item) {
            items[currentSlot++] = autoAdjust(ItemBuilder.of(item.getItem())).build();
            if (getPlayer() != null) {
                attach(item);
            }
            return this;
        }

        public KitInventoryBuilder add(BattleKitItem item) {
            items[currentSlot++] = autoAdjust(ItemBuilder.of(item.getItem())).build();
            if (getPlayer() != null) {
                attach(item);
            }
            return this;
        }

        public KitInventoryBuilder addFood(int count) {
            items[currentSlot++] = ItemBuilder.of(Material.COOKED_BEEF).name(getName() + " Food").amount(count).build();
            return this;
        }

        private ItemBuilder autoAdjust(ItemBuilder itemBuilder) {
            return itemBuilder.unbreakable().name(
                    getName() + " " + StringUtils.capitalize(itemBuilder.build().getType().name().toLowerCase()));
        }

        public Map<Integer, ItemStack> build() {
            Map<Integer, ItemStack> map = new HashMap<>();
            for (int i = 0; i < items.length; i++) {
                map.put(i, items[i]);
            }
            return map;
        }

    }

}
