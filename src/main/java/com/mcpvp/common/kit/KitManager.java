package com.mcpvp.common.kit;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Plugin plugin;
    private final Map<Player, KitDefinition> selected = new ConcurrentHashMap<>();
    private final Map<Player, Kit> active = new ConcurrentHashMap<>();

    public List<KitDefinition> getKitDefinitions() {
        return Collections.emptyList();
    }

    public KitDefinition getKitDefinition(String name) {
        return this.getKitDefinitions().stream().filter(k -> k.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public KitAttemptSelectEvent setSelected(
        Player player, KitDefinition definition, boolean force
    ) {
        return this.setSelected(player, definition, force, true);
    }

    public KitAttemptSelectEvent setSelected(
        Player player, KitDefinition definition, boolean force, boolean respawn
    ) {
        // Allow the kit selection event to be rejected to enforce limits and restrictions.
        KitAttemptSelectEvent kitAttemptSelectEvent = new KitAttemptSelectEvent(player, definition);
        if (!kitAttemptSelectEvent.callIsCancelled() || force) {
            this.selected.put(player, definition);
            new KitSelectedEvent(player, definition, respawn).call();
            return kitAttemptSelectEvent;
        }

        return kitAttemptSelectEvent;
    }

    public boolean isSelected(Player player, KitDefinition definition) {
        return this.selected.containsKey(player) && this.selected.get(player).equals(definition);
    }

    @Nullable
    public KitDefinition getSelected(Player player) {
        return this.selected.get(player);
    }

    public boolean createSelected(Player player) {
        KitDefinition selected = this.getSelected(player);
        if (selected == null) {
            return false;
        }

        log.info("Creating selected {} for {}", selected, player.getName());

        // Remove the existing kit
        if (this.active.containsKey(player)) {
            this.active.get(player).shutdown();
        }

        Kit created = selected.create(this.plugin, player);
        this.active.put(player, created);
        return true;
    }

    @Nullable
    public Kit get(Player player) {
        return this.active.get(player);
    }

    public boolean isPlaying(Player player, Class<? extends Kit> type) {
        return Optional.ofNullable(this.get(player)).map(k -> k.getClass().equals(type)).orElse(false);
    }

}
