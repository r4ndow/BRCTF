package com.mcpvp.common.kit;

import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.event.EasyListener;

import lombok.Getter;
import lombok.NonNull;

/**
 * An instance of this class represents a "live" instance of a KitType. Every
 * Kit corresponds
 * to one player, and instances are recreated often. For example, when a player
 * dies,
 * a new Kit will be created and re-initialized. This makess tate management
 * significantly easier.
 */
public abstract class Kit extends EasyLifecycle implements KitInfo, EasyListener {

    @Getter
    protected final Plugin plugin;
    @Getter
    private final Player player;

    public Kit(Plugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        setup(player);
    }

    /**
     * Performs required setup when this kit is created for a player.
     * Things like registering listeners should be called here.
     * 
     * @param player The player this Kit exists for.
     */
    protected void setup(@NonNull Player player) {
        attach((EasyListener) this);

        player.getInventory().clear();
        player.getInventory().setArmorContents(getArmor());
        getItems().entrySet().stream()
                .filter(e -> e.getValue() != null)
                .forEach(e -> {
                    attach(e.getValue());
                    player.getInventory().setItem(e.getKey(), e.getValue().getItem());
                });
    }

    protected boolean isPlayer(Player player) {
        return player.equals(this.player);
    }

    protected boolean isPlayer(ProjectileSource source) {
        return source.equals(this.player);
    }

    public abstract String getName();

    public abstract ItemStack[] getArmor();

    public abstract Map<Integer, KitItem> getItems();

}
