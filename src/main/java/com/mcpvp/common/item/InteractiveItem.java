package com.mcpvp.common.item;

import com.mcpvp.common.event.EasyListener;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A simple class for handling interact events on one item. Items are identified by an NBT tag,
 * which allows mutation of all other item properties, even the material.
 *
 * @author NomNuggetNom
 */
@EqualsAndHashCode(of = "id")
public class InteractiveItem implements EasyListener {

    private static final String NBT_KEY = "interactive_item_id";
    private static final AtomicInteger GLOBAL_ID = new AtomicInteger(1);

    @Getter
    private final Plugin plugin;
    @Getter
    private final int id;
    @Getter
    private ItemStack item;
    @Setter
    private boolean ignoreCancelled;
    private final List<Consumer<BlockPlaceEvent>> blockPlaceHandlers = new ArrayList<>();
    private final List<Consumer<PlayerInteractEvent>> interactHandlers = new ArrayList<>();
    private final List<Consumer<EntityDamageByEntityEvent>> edbeeHandlers = new ArrayList<>();
    private final List<Consumer<PlayerInteractEntityEvent>> interactEntityHandlers = new ArrayList<>();
    private final List<Consumer<PlayerDropItemEvent>> dropHandlers = new ArrayList<>();
    private Consumer<InventoryClickEvent> clickHandler = e -> {
    };
    private Consumer<Player> anyHandler = e -> {
    };

    /**
     * @param item The Item to catch and handle events for.
     */
    public InteractiveItem(Plugin plugin, ItemStack item) {
        this.plugin = plugin;
        this.id = GLOBAL_ID.getAndIncrement();
        this.item = NBTUtil.saveString(item, NBT_KEY, "" + this.id);
    }

    /**
     * @param builder Item builder instance to create item.
     */
    public InteractiveItem(Plugin plugin, ItemBuilder builder) {
        this(plugin, builder.build());
    }

    /**
     * Assigns a consumer that will receive any PlayerInteractEvent that involves
     * place on this item.
     *
     * @param consumer The consumer that will receive the event. This event has
     *                 already been cancelled when it is passed.
     * @return The instance for chaining.
     */
    public InteractiveItem onInteract(Consumer<PlayerInteractEvent> consumer) {
        this.interactHandlers.add(consumer);
        return this;
    }

    /*
     * Assigns a consumer that will receive any BlockPlaceEvent that involves
     * place on this item.
     *
     * @param consumer The consumer that will receive the event. This event has
     *        already been cancelled when it is passed.
     * @return The instance for chaining.
     */
    public InteractiveItem onBlockPlace(Consumer<BlockPlaceEvent> consumer) {
        this.blockPlaceHandlers.add(consumer);
        return this;
    }

    /**
     * Assigns a consumer that will receive any InventoryClickEvent that takes
     * place on this item.
     *
     * @param consumer The consumer that will receive the event. This event has
     *                 already been cancelled when it is passed.
     * @return The instance for chaining.
     */
    public InteractiveItem onClick(Consumer<InventoryClickEvent> consumer) {
        this.clickHandler = consumer;
        return this;
    }

    /**
     * Assigns a consumer that will receive any EntityDamageByEntityEvent where the
     * damager in the event is a player holding this item.
     *
     * @param consumer The consumer that will receive the event.
     * @return The instance for chaining.
     */
    public InteractiveItem onDamage(Consumer<EntityDamageByEntityEvent> consumer) {
        this.edbeeHandlers.add(consumer);
        return this;
    }

    /**
     * Assigns a consumer that will receive any PlayerInteractEntityEvent where the
     * player interacting with an entity is a player holding this item.
     *
     * @param consumer The consumer that will receive the event.
     * @return The instance for chaining.
     */
    public InteractiveItem onInteractEntity(Consumer<PlayerInteractEntityEvent> consumer) {
        this.interactEntityHandlers.add(consumer);
        return this;
    }

    /**
     * Assigns a consumer that will receive any PlayerDropItemEvent where the
     * item is involved in.
     *
     * @param consumer The consumer that will receive the event.
     * @return The instance for chaining.
     */
    public InteractiveItem onDrop(Consumer<PlayerDropItemEvent> consumer) {
        this.dropHandlers.add(consumer);
        return this;
    }

    /**
     * Assigns a consumer that will receive any Player that interacted with this
     * item, whether through a PlayerInteractEvent or an InventoryClickEvent.
     * Either event will be cancelled automatically.
     *
     * @param consumer The consumer that will receive the Player object.
     * @return The instance for chaining.
     */
    public InteractiveItem onAny(Consumer<Player> consumer) {
        this.anyHandler = consumer;
        return this;
    }

    protected void setItem(ItemStack item) {
        if (!this.isItem(item)) {
            throw new IllegalArgumentException("Given ItemStack did not have correct tag");
        }
        this.item = item;
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEvent event) {
        if (event.isCancelled() && ignoreCancelled) {
            return;
        }

        if (!event.hasItem()) {
            return;
        }

        if (isItem(event.getItem())) {
            // Re-assign the ItemStack instance, which improves syncing with the client
            // Without doing this, a call to `update()` would be required
            this.setItem(event.getItem());
            interactHandlers.forEach(ih -> ih.accept(event));
            anyHandler.accept(event.getPlayer());
        }
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEntityEvent event) {
        if (event.isCancelled() && ignoreCancelled) {
            return;
        }

        if (event.getPlayer().getItemInHand() == null) {
            return;
        }

        if (isItem(event.getPlayer().getItemInHand())) {
            anyHandler.accept(event.getPlayer());
        }
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        if (event.isCancelled() && ignoreCancelled) {
            return;
        }

        if (isItem(event.getItemInHand())) {
            blockPlaceHandlers.forEach(ih -> ih.accept(event));
            anyHandler.accept(event.getPlayer());
        }
    }

    @EventHandler
    public void onClickEvent(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        // Allow movement in the player's inventory.
        if (isItem(event.getCurrentItem())) {
            clickHandler.accept(event);
            anyHandler.accept((Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        if (damager.getItemInHand() == null) {
            return;
        }

        if (isItem(damager.getItemInHand())) {
            edbeeHandlers.forEach(h -> h.accept(event));
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (player.getItemInHand() == null) {
            return;
        }

        if (isItem(player.getItemInHand())) {
            interactEntityHandlers.forEach(h -> h.accept(event));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isItem(event.getItemDrop())) {
            dropHandlers.forEach(h -> h.accept(event));
        }
    }

    /**
     * @param item The item to compare.
     * @return True if the given item is this InteractiveItem's item.
     */
    public boolean isItem(ItemStack item) {
        return NBTUtil.hasString(item, NBT_KEY, "" + id);
    }

    /**
     * @param item The item to compare.
     * @return True if the given item is this InteractiveItem's item.
     */
    public boolean isItem(Item item) {
        return isItem(item.getItemStack());
    }

    /**
     * @return An ItemQuery that will match this item.
     */
    public ItemQuery query() {
        return new ItemQuery().nbt(NBT_KEY, String.valueOf(this.id));
    }

    /**
     * Updates this ItemStack in the given inventory. This should be called whenever a change to the ItemStack is made.
     *
     * @param inv The Inventory to update the item in.
     */
    public void update(Inventory inv) {
        for (int i = 0; i < inv.getContents().length; i++) {
            if (isItem(inv.getItem(i))) {
                inv.setItem(i, getItem());
            }
        }
    }

}