package com.mcpvp.common.kit;

import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.event.EasyListener;

import lombok.Getter;
import lombok.NonNull;

/**
 * An instance of this class represents a "live" instance of a KitType. Every Kit corresponds
 * to one player, although the player might be null (supported for KitType). Instances are recreated
 * often. For example when a player dies, a new Kit will be created and re-initialized. This makes
 * state management significantly easier.
 */
public abstract class Kit extends EasyLifecycle implements KitInfo, EasyListener {
    
    @Getter
    protected final Plugin plugin;
    @Nullable
    private final Player player;

    public Kit(Plugin plugin, @Nullable Player player) {
        this.plugin = plugin;
        this.player = player;

        if (player != null) {
            setup(player);
        }
    }

    /**
     * Performs required setup when this kit is created for a player.
     * Things like registering listeners should be called here.
     * 
     * @param player The player this Kit exists for.
     */
    protected void setup(@NonNull Player player) {
        attach(this);

        player.getInventory().clear();
        player.getInventory().setArmorContents(getArmor());
        getItems().forEach(player.getInventory()::setItem);
    }

    public abstract String getName();

    public abstract ItemStack[] getArmor();

    public abstract Map<Integer, ItemStack> getItems();

}
