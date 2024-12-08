package com.mcpvp.common.kit;

import java.lang.reflect.Constructor;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import lombok.Getter;

/**
 * An enum-like representation of a Kit. This serves two purposes:
 * 
 * <ol>
 *  <li>It acts as a factory for Kits via the {@link #create(Player)} method.</li>
 *  <li>Kits (and their respective properties, such as icon, armor, etc) can be obtained without a player instance.</li>
 * </ol>
 */
public class KitType<K extends Kit> {

    @Getter
    private final Class<K> kitType;
    private final Plugin plugin;
    private final Constructor<K> constructor;
    /**
     * The instance of the Kit that has no player, which allows access to things such as inventory layout,
     * icon, and more.
     */
    private final K wrapper;

    public KitType(Class<K> type, Plugin plugin) {
        this.kitType = type;
        this.plugin = plugin;
        try {
            this.constructor = type.getConstructor(Plugin.class, Player.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Kit class must have a constructor that receives a Plugin and Player object, but %s did not".formatted(type));
        }
        this.wrapper = this.create(plugin, null);
    }

    /**
     * Create a Kit from this KitType for the player. The Kit will be initialized immediately (e.g. armor equipped,
     * listeners started, etc).
     * 
     * @param player The player to create the kit for.
     * @return The created Kit instance.
     */
    public K create(Plugin plugin, @Nullable Player player) {
        try {
            return constructor.newInstance(plugin, player);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create KitType %s for player %s".formatted(this, player), e);
        }
    }

    public K getBackingKit() {
        return wrapper;
    }

}
