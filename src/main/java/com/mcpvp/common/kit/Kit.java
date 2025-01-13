package com.mcpvp.common.kit;

import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.event.EasyListener;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Collection;
import java.util.Map;

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
    private final Map<Integer, KitItem> items;

    public Kit(Plugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.items = createItems();
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
        player.getInventory().setArmorContents(createArmor());

        this.items.entrySet().stream()
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

    /**
     * Create the armor to equip the player with.
     */
    public abstract ItemStack[] createArmor();

    /**
     * Create all the items for this Kit. This should only be called once per
     * kit instance to avoid duplicating items.
     */
    public abstract Map<Integer, KitItem> createItems();

    /**
     * @return A list of all KitItems associated with this Kit. These items should
     * be created when the Kit is initialized.
     */
    public Collection<KitItem> getAllItems() {
        return this.items.values();
    }

}
