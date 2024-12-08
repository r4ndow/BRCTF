package com.mcpvp.common.kit;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * An enum-like representation of a Kit. This serves two purposes:
 * 
 * <ol>
 * <li>It acts as a factory for Kits via the {@link #create(Player)}
 * method.</li>
 * <li>Kits (and their respective properties, such as icon, armor, etc) can be
 * obtained without a player instance.</li>
 * </ol>
 */
@Log4j2
public class KitType<K extends Kit> {

    @Getter
    private final Class<K> kitType;
    private final Plugin plugin;
    private final Constructor<K> constructor;
    /**
     * The instance of the Kit that has no player, which allows access to things
     * such as inventory layout,
     * icon, and more.
     */
    private final K wrapper;

    @SuppressWarnings("unchecked")
    public KitType(Class<K> type, Plugin plugin) {
        this.kitType = type;
        this.plugin = plugin;

        try {
            this.constructor = (Constructor<K>) Arrays.stream(type.getDeclaredConstructors()).filter(c -> {
                return c.getParameterTypes().length == 2 &&
                        Plugin.class.isAssignableFrom(c.getParameterTypes()[0]) &&
                        Player.class.isAssignableFrom(c.getParameterTypes()[1]);
            }).findFirst().orElseThrow();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Kit class must have a constructor that receives a Plugin and Player object, but %s did not"
                            .formatted(type));
        }
        this.wrapper = this.create(plugin, null);
    }

    /**
     * Create a Kit from this KitType for the player. The Kit will be initialized
     * immediately (e.g. armor equipped,
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

    @Override
    public String toString() {
        return "KitType[backing=%s]".formatted(getBackingKit().getName());
    }

}
