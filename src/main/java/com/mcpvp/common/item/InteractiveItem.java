package com.mcpvp.common.item;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.mcpvp.common.event.EasyListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A simple class for handling an interact event on one item.
 *
 * @author NomNuggetNom
 */
@EqualsAndHashCode(of = "id")
public class InteractiveItem implements EasyListener {

    private static final String NBT_KEY = "interactive_item_id";
    private static final List<Material> DONT_UPDATE = List.of(Material.BOW, Material.WOOD_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLD_SWORD, Material.DIAMOND_SWORD);
    private static AtomicInteger globalId = new AtomicInteger(1);

    @Getter
    private final Plugin plugin;
    private ItemStack item;
    private int id;
    private boolean ignoreCancelled;
    private List<Consumer<BlockPlaceEvent>> blockPlaceHandlers = new ArrayList<>();
    private List<Consumer<PlayerInteractEvent>> interactHandlers = new ArrayList<>();
    private List<Consumer<EntityDamageByEntityEvent>> edbeeHandlers = new ArrayList<>();
    private List<Consumer<PlayerInteractEntityEvent>> interactEntityHandlers = new ArrayList<>();
    private Consumer<InventoryClickEvent> clickHandler = e -> {};
    private Consumer<Player> anyHandler = e -> {};
    private List<Consumer<PlayerDropItemEvent>> dropHandlers = new ArrayList<>();

    /**
     * @param item The Item to catch and handle events for.
     */
    public InteractiveItem(Plugin plugin, ItemStack item) {
        this.plugin = plugin;
        this.id = globalId.getAndIncrement();
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
     *        already been cancelled when it is passed.
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
     *        already been cancelled when it is passed.
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

    /**
     * Returns the ID of this {@code InteractiveItem}. Each
     * instance is guaranteed to return a different value.
     *
     * @return the ID of this {@code InteractiveItem}.
     */
    public int getID() {
        return id;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setIgnoreCancelled(boolean ignoreCancelled) {
        this.ignoreCancelled = ignoreCancelled;
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEvent event) {
        if (event.isCancelled() && ignoreCancelled)
            return;

        if (!event.hasItem())
            return;

        if (isItem(event.getItem())) {
            interactHandlers.forEach(ih -> ih.accept(event));
            anyHandler.accept(event.getPlayer());
        }
    }

    protected void onInteractEvent(PlayerInteractEntityEvent event) {
        if (event.isCancelled() && ignoreCancelled)
            return;

        if (event.getPlayer().getItemInHand() == null)
            return;

        if (isItem(event.getPlayer().getItemInHand())) {
            anyHandler.accept(event.getPlayer());
        }
    }

    protected void onBlockPlaceEvent(BlockPlaceEvent event) {
        if (event.isCancelled() && ignoreCancelled)
            return;

        if (isItem(event.getItemInHand())) {
            blockPlaceHandlers.forEach(ih -> ih.accept(event));
            anyHandler.accept(event.getPlayer());
        }
    }

    protected void onClickEvent(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        if (event.getCurrentItem() == null)
            return;

        // Allow movement in the player's inventory.
        if (isItem(event.getCurrentItem())) {
            clickHandler.accept(event);
            anyHandler.accept((Player) event.getWhoClicked());
        }
    }

    protected void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;

        Player damager = (Player) event.getDamager();

        if (damager.getItemInHand() == null)
            return;

        if (isItem(damager.getItemInHand()))
            edbeeHandlers.forEach(h -> h.accept(event));
    }

    protected void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (player.getItemInHand() == null)
            return;

        if (isItem(player.getItemInHand()))
            interactEntityHandlers.forEach(h -> h.accept(event));
    }

    protected void onDrop(PlayerDropItemEvent event) {
        if (isItem(event.getItemDrop())) {
            dropHandlers.forEach(h -> h.accept(event));
        }
    }

    // /**
    //  * @param inv The inventory to check.
    //  * @return True if the item is in the inventory.
    //  */
    // public boolean inInventory(Inventory inv) {
    //     return new ItemQuery().nbt(NBT_KEY, "" + this.id).all(inv).size() > 0;
    // }

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

    // /**
    //  * @return An ItemQuery that will match this item.
    //  */
    // public ItemQuery query() {
    //     return new ItemQuery().nbt(NBT_KEY, String.valueOf(this.id));
    // }
    
}