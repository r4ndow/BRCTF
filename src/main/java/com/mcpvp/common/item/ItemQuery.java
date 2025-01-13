package com.mcpvp.common.item;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used for finding an ItemStack match based on specified parameters, like item
 * name and item type.
 *
 * @author NomNuggetNom
 */
public class ItemQuery {

    private final List<Predicate<ItemStack>> rules = new ArrayList<>();

    /**
     * Creates a new ItemQuery instance with no rules except that the ItemStack
     * is not null.
     */
    public ItemQuery() {
        rules.add(Objects::nonNull);
    }

    /**
     * Creates a new ItemQuery instance that mimics {@link ItemStack#isSimilar(ItemStack)}.
     *
     * @param item The ItemStack to use for checks.
     */
    public ItemQuery(ItemStack item) {
        this();
        this.type(item.getType()).durability(item.getDurability()).meta(item.getItemMeta());
    }

    /**
     * @param XMaterial The XMaterial to require.
     * @return The instance for chaining.
     */
    public ItemQuery type(Material XMaterial) {
        rules.add(i -> i.getType() == XMaterial);
        return this;
    }

    /**
     * @param data The XMaterialData to require.
     * @return The instance for chaining.
     */
    public ItemQuery data(MaterialData data) {
        rules.add(i -> i.getData() != null && i.getData().equals(data));
        return this;
    }

    /**
     * @param durability The durability to require.
     * @return The instance for chaining.
     */
    public ItemQuery durability(short durability) {
        rules.add(i -> i.getDurability() == durability);
        return this;
    }

    /**
     * @param amount The amount to require.
     * @return The instance for chaining.
     */
    public ItemQuery amount(int amount) {
        rules.add(i -> i.getAmount() == amount);
        return this;
    }

    /**
     * @param enchants The enchantments to require. All enchantments must be
     *            present for an ItemStack to pass.
     * @return The instance for chaining.
     */
    public ItemQuery enchants(Map<Enchantment, Integer> enchants) {
        rules.add(i -> i.getEnchantments().equals(enchants));
        return this;
    }

    /**
     * @param enchant An enchantment to require of any level. Other enchantments
     *            will not cause an ItemStack to fail.
     * @return The instance for chaining.
     */
    public ItemQuery enchant(Enchantment enchant) {
        rules.add(i -> i.getEnchantments().containsKey(enchant));
        return this;
    }

    /**
     * @param enchant An enchantment to require. Other enchantments will not
     *            cause an ItemStack to fail.
     * @param level The level of the Enchantment to have.
     * @return The instance for chaining.
     */
    public ItemQuery enchant(Enchantment enchant, int level) {
        rules.add(i -> i.getEnchantmentLevel(enchant) == level);
        return this;
    }

    /**
     * Requires ItemStacks to be enchanted.
     *
     * @return The instance for chaining.
     */
    public ItemQuery enchanted() {
        rules.add(i -> !i.getEnchantments().isEmpty());
        return this;
    }

    /**
     * @param flag The ItemFlag to require.
     * @return The instance for chaining.
     */
    public ItemQuery flag(ItemFlag flag) {
        rules.add(i -> i.hasItemMeta() && i.getItemMeta().hasItemFlag(flag));
        return this;
    }

    /**
     * @param key The NBT key to look for.
     * @param value The NBT value for the key to look for.
     * @return The instance for chaining.
     */
    public ItemQuery nbt(String key, String value) {
        rules.add(i -> {
            if(i == null)
                return false;
            return NBTUtil.hasString(i, key, value);
        });
        return this;
    }

    /**
     * @param meta The ItemMeta to require.
     * @return The instance for chaining.
     */
    public ItemQuery meta(ItemMeta meta) {
        rules.add(i -> (!i.hasItemMeta() && meta == null) || (i.getItemMeta() != null && i.getItemMeta().equals(meta)));
        return this;
    }

    /**
     * Adds a rule that the ItemStack will be tested for.
     *
     * @param rule The Predicate to test the ItemStack with.
     * @return The instance for chaining.
     */
    public ItemQuery rule(Predicate<ItemStack> rule) {
        this.rules.add(rule);
        return this;
    }

    /**
     * @param stack The ItemStack to check for a match.
     * @return True if all of the criteria of this query are fulfilled.
     */
    public boolean matches(ItemStack stack) {
        for (Predicate<ItemStack> predicate : rules)
            if (!predicate.test(stack))
                return false;
        return true;
    }

    /**
     * @param inventory The Inventory to find a match in.
     * @return The first item that matches this query, or null if none match.
     */
    public ItemStack first(Inventory inventory) {
        return Stream.of(inventory.getContents()).filter(this::matches).findFirst().orElse(null);
    }

    /**
     * @param inventory The Inventory to find matches in.
     * @return A list of all ItemStacks that match this query.
     */
    public List<ItemStack> all(Inventory inventory) {
        return Stream.of(inventory.getContents()).filter(this::matches).collect(Collectors.toList());
    }

    /**
     * Attempts to find the first occurrence of an ItemStack that matches this
     * query. The order of checking is: main inventory, crafting grid, cursor.
     *
     * @param player The Player to check the Inventory for.
     * @return The first ItemStack that matches, or else null.
     */
    public ItemStack first(Player player) {
        ArrayList<ItemStack> check = new ArrayList<>();
        check.addAll(Arrays.asList(player.getInventory().getContents()));

        // Check the Player's crafting grid.
        if (player.getOpenInventory().getTopInventory() != null && player.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING)
            check.addAll(Arrays.asList(player.getOpenInventory().getTopInventory().getContents()));

        // Check the Player's cursor.
        check.add(player.getItemOnCursor());

        return check.stream().filter(this::matches).findFirst().orElse(null);
    }

    /**
     *
     * Attempts to find all occurrences of an ItemStack that matches this query.
     * The order of checking is: main inventory, crafting grid, cursor.
     *
     * @param player The Player to check the Inventory for.
     * @return All ItemStacks that match.
     */
    public List<ItemStack> all(Player player) {
        ArrayList<ItemStack> check = new ArrayList<>();
        check.addAll(Arrays.asList(player.getInventory().getContents()));

        // Check the Player's crafting grid.
        if (player.getOpenInventory().getTopInventory() != null && player.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING)
            check.addAll(Arrays.asList(player.getOpenInventory().getTopInventory().getContents()));

        // Check the Player's cursor.
        check.add(player.getItemOnCursor());

        return check.stream().filter(this::matches).collect(Collectors.toList());
    }

}