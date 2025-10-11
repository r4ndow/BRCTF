package com.mcpvp.battle.kit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.common.item.ItemUtil;
import com.mcpvp.common.kit.KitItem;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Log4j2
public class BattleInventoryManager {

    private final File dataFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<UUID, InventoryLayoutData> liveData = new HashMap<>();

    public BattleInventoryManager(Plugin plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "inventory_layouts.json");
    }

    public void loadAll() {
        if (!this.dataFile.exists()) {
            try {
                log.debug("Created new data file");

                this.dataFile.createNewFile();
                // Initialize the file with valid JSON by writing
                this.saveAll();
            } catch (IOException e) {
                throw new RuntimeException("Unable to create inventory_layouts.json file at " + this.dataFile.getAbsolutePath(), e);
            }
        } else {
            try {
                Map<UUID, InventoryLayoutData> saved = this.objectMapper.readValue(this.dataFile, new TypeReference<Map<UUID, InventoryLayoutData>>() {
                });
                this.liveData.putAll(saved);

                log.debug("Loaded existing data: {}", saved);
            } catch (IOException e) {
                throw new RuntimeException("Existing inventory_layouts.json file could not be read. Try deleting it and restarting. Location: " + this.dataFile.getAbsolutePath(), e);
            }
        }
    }

    public void saveAll() {
        try {
            this.objectMapper.writeValue(this.dataFile, this.liveData);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save inventory_layouts.json file at " + this.dataFile.getAbsolutePath(), e);
        }
    }

    public void save(BattleKit kit) {
        InventoryLayoutData data = this.load(kit.getPlayer()).orElse(new InventoryLayoutData());
        log.debug("While saving, loaded data for {}", data);

        Map<String, Integer> keyToSlot = getKeyToSlot(kit);
        if (keyToSlot.isEmpty()) {
            // When a player dies, it triggers a CloseInventoryEvent, but they have no items in their inventory
            // So avoid saving the data when there are no items found
            return;
        }

        data.keyToSlot.put(kit.getName(), keyToSlot);
        this.save(kit.getPlayer(), data);

        log.debug("Saved {} data for {}: {}", kit.getPlayer().getName(), kit.getName(), data);
    }

    public Optional<InventoryLayoutData> load(Player player) {
        return Optional.ofNullable(this.liveData.getOrDefault(player.getUniqueId(), null));
    }

    private void save(Player player, InventoryLayoutData data) {
        this.liveData.put(player.getUniqueId(), data);
    }

    /**
     * Applies any saved layouts for the given Kit to the given map, returning a new
     * re-ordered map with correctly associated slots.
     *
     * @param kit   The kit to load data for.
     * @param items A list of the default kit item arrangement.
     * @return A reordered map of inventory slot to kit item.
     */
    public Map<Integer, KitItem> applyLayout(BattleKit kit, Map<Integer, KitItem> items) {
        Optional<BattleInventoryManager.InventoryLayoutData> load = this.load(kit.getPlayer());

        // Ensure that there is data present for the specific kit
        if (load.isPresent() && load.get().getKeyToSlot().containsKey(kit.getName())) {
            BattleInventoryManager.InventoryLayoutData inventoryLayoutData = load.get();
            Map<String, Integer> savedItemMap = inventoryLayoutData.getKeyToSlot().get(kit.getName());
            Map<Integer, KitItem> reordered = new HashMap<>();

            log.debug("Existing data: {}", savedItemMap);

            // For all the items in that were saved, find the corresponding KitItem instance
            // Associate the proper slot
            savedItemMap.forEach((itemName, slot) -> {
                items.values().stream().filter(kitItem -> {
                    return getKey(kitItem.getOriginal()).equals(itemName);
                }).findFirst().ifPresent(kitItem -> {
                    reordered.put(slot, kitItem);
                });
            });

            // There might be items that weren't saved, but the player still needs them
            // Ensure they are filled in
            int maxSlot = savedItemMap.values().stream().mapToInt(i -> i).max().orElse(0);
            for (Map.Entry<Integer, KitItem> entry : items.entrySet()) {
                KitItem item = entry.getValue();
                if (!savedItemMap.containsKey(ItemUtil.getName(item.getOriginal()))) {
                    reordered.put(++maxSlot, item);
                }
            }

            log.debug("Reordered: {}", reordered);

            return reordered;
        }

        return items;
    }

    /**
     * Generates a mapping of unique kit item storage keys to the respective slot
     * as they currently are in the kit player's inventory.
     *
     * @param kit The kit to save the current inventory for.
     * @return A unique mapping of item storage key to slot.
     */
    private static Map<String, Integer> getKeyToSlot(BattleKit kit) {
        Map<String, Integer> keyToSlot = new HashMap<>();

        kit.getItems().values().forEach(ki -> {
            for (int i = 0; i < 9; i++) {
                if (ki.isItem(kit.getPlayer().getInventory().getItem(i))) {
                    keyToSlot.put(getKey(ki), i);
                }
            }
        });

        return keyToSlot;
    }

    /**
     * @param kitItem The kit item to get a unique key for.
     * @return A unique key that this item can be identified by.
     */
    private static String getKey(KitItem kitItem) {
        return getKey(kitItem.getOriginal());
    }

    /**
     * @param itemStack The kit item to get a unique key for.
     * @return A unique key that this item can be identified by.
     */
    private static String getKey(ItemStack itemStack) {
        return ItemUtil.getName(itemStack);
    }

    @Data
    @NoArgsConstructor
    public static class InventoryLayoutData {

        // kit -> [item_key -> slot]
        private final Map<String, Map<String, Integer>> keyToSlot = new HashMap<>();

    }

}
