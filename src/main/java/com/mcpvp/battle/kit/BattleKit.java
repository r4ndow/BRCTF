package com.mcpvp.battle.kit;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.item.InteractiveItem;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;

public abstract class BattleKit extends Kit {

    public BattleKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    protected Battle getBattle() {
        return ((BattlePlugin) plugin).getBattle();
    }

    protected void eatFood(KitItem food) {
        if (food.isPlaceholder()) {
            return;
        }

        if (getPlayer().getHealth() == getPlayer().getMaxHealth()) {
            return;
        }

        getPlayer().setHealth(getPlayer().getHealth() + 8);
        food.decrement(true);
    }

    public class KitInventoryBuilder {

        private static final int INVENTORY_SIZE = 9 * 4;
        private final KitItem[] items = new KitItem[INVENTORY_SIZE];

        private int currentSlot = 0;

        public KitInventoryBuilder add(Material material) {
            return add(ItemBuilder.of(material));
        }

        public KitInventoryBuilder add(ItemBuilder builder) {
            items[currentSlot++] = autoAdjust(builder);
            return this;
        }

        public KitInventoryBuilder add(KitItem item) {
            items[currentSlot++] = item;
            return this;
        }

        public KitInventoryBuilder addFood(int count) {
            KitItem ki = new KitItem(BattleKit.this, ItemBuilder.of(Material.COOKED_BEEF).name(getName() + " Food").amount(count).build());
            ki.onInteract(ev -> {
                if (EventUtil.isRightClick(ev)) {
                    eatFood(ki);
                }
            });
            items[currentSlot++] = ki;
            return this;
        }

        private KitItem autoAdjust(ItemBuilder itemBuilder) {
            String typeName = WordUtils.capitalize(itemBuilder.build().getType().name().replace("_", " ").toLowerCase());
            return new KitItem(BattleKit.this, itemBuilder.unbreakable().name(getName() + " " + typeName).build());
        }

        public Map<Integer, KitItem> build() {
            Map<Integer, KitItem> map = new HashMap<>();
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) {
                    map.put(i, items[i]);
                }
            }
            return map;
        }

    }

}
