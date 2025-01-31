package com.mcpvp.battle.kit;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.item.FlagCompassItem;
import com.mcpvp.battle.kit.item.FoodItem;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureViolation;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BattleKit extends Kit {

    public BattleKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    public Battle getBattle() {
        return ((BattlePlugin) plugin).getBattle();
    }

    protected void placeStructure(Structure structure, Block center) {
        List<StructureViolation> violations = structure.place(center);
        if (!violations.isEmpty()) {
            violations.forEach(v -> {
                getPlayer().sendMessage("! " + v.getMessage());
            });
        } else {
            // Structure will be removed on kit destruction
            attach((EasyLifecycle) structure);
        }
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
            items[currentSlot++] = new FoodItem(BattleKit.this, ItemBuilder.of(Material.COOKED_BEEF).name("Food").amount(count).build());
            return this;
        }

        public KitInventoryBuilder addCompass(int slot) {
            items[slot] = new FlagCompassItem(getBattle().getGame(), BattleKit.this);
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
