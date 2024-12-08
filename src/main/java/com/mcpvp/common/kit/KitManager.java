package com.mcpvp.common.kit;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * One-stop shop for managing KitTypes and Kits. There are two important concepts:
 * 
 * <ul>
 *  <li><b>Selected KitType</b>: A user can have a type of Kit selected without an instance existing.
 *  For example, before a game starts, a user has already decided which they want to play. But they have
 *  not been given armor, items, etc because no Kit exists yet.</li>
 *  <li><b>Active Kit</b>: A kit that is currently being used. This is created based on the selected KitType
 *  and it exists when a game is actually in progress.</li>
 * </ul>
 */
@Log4j2
@RequiredArgsConstructor
public class KitManager {

    private Map<Player, KitType<?>> selected = new ConcurrentHashMap<>();
    private Map<Player, Kit> active = new ConcurrentHashMap<>();
    
    public List<KitType<?>> getKitTypes() {
        return Collections.emptyList();
    }

    public KitType<?> getKitType(Class<? extends Kit> clazz) {
        return getKitTypes().stream().filter(k -> k.getKitType().equals(clazz)).findFirst().orElse(null);
    }

    public boolean setSelected(Player player, Class<? extends Kit> type, boolean force) {
        KitType<?> kitType = getKitType(type);

        // Allow the kit selection event to be rejected to enforce limits and resitrctions.
        if (!new KitAttemptSelectEvent(player, kitType).call() || force) {
            selected.put(player, kitType);
            new KitSelectedEvent(player, kitType).call();
            return true;
        }

        return false;
    }

    public boolean isSelected(Player player, KitType<?> type) {
        return selected.containsKey(player) && selected.get(player).getClass().equals(type.getClass());
    }

    @Nullable
    public KitType<?> getSelected(Player player) {
        return selected.get(player);
    }

    public boolean createSelected(Player player) {
        KitType<?> selected = getSelected(player);
        if (selected == null) {
            return false;
        }

        log.info("Creating selected " + selected + " for " + player.getName());

        // Remove the existing kit
        if (active.containsKey(player)) {
            active.get(player).shutdown();
        }

        Kit created = selected.create(player);
        active.put(player, created);
        return true;
    }

    @Nullable
    public Kit get(Player player) {
        return active.get(player);
    }

    public boolean isPlaying(Player player, Class<Kit> type) {
        return active.containsKey(player) && active.get(player).getClass().equals(type);
    }

}
