package com.mcpvp.common.kit;

import com.mcpvp.common.item.InteractiveItem;
import com.mcpvp.common.item.ItemBuilder;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.UnaryOperator;

/**
 * An item tied to an instance of a kit, with extra utilities.
 */
@ToString
public class KitItem extends InteractiveItem {

    protected final Kit kit;
    @Getter
    private final ItemStack original;
    @Getter
    private final boolean restorable;

    public KitItem(Kit kit, ItemStack itemStack, boolean restorable) {
        super(kit.getPlugin(), itemStack);
        this.kit = kit;
        this.original = getItem().clone();
        this.restorable = restorable;
    }

    public KitItem(Kit kit, ItemStack itemStack) {
        this(kit, itemStack, false);
    }

    /**
     * Decrease the number of items in the stack by one, or swap the item to a placeholder
     * if there will be no items left.
     *
     * @param placeholder If the item should be swapped to a placeholder.
     */
    public void decrement(boolean placeholder) {
        if (getItem().getAmount() == 1 && placeholder) {
            setPlaceholder();
        } else {
            modify(builder -> builder.amount(getItem().getAmount() - 1));
        }
    }

    /**
     * Makes this item look like a placeholder. Calls to {@link #isPlaceholder()} will be true.
     */
    public void setPlaceholder() {
        modify(ib ->
            ib.type(Material.STAINED_GLASS_PANE).durability(0)
        );
    }

    /**
     * Check if this item has been made into a placeholder by {@link #setPlaceholder()}.
     *
     * @return If this item is a placeholder.
     */
    public boolean isPlaceholder() {
        return getItem().getType() == Material.STAINED_GLASS_PANE;
    }

    /**
     * Increases the number of items in the item stack, unless the number of items is already
     * the given max.
     *
     * @param max The maximum amount.
     */
    public void increment(int max) {
        if (isPlaceholder()) {
            modify(builder -> builder.type(original.getType()));
        } else if (getItem().getAmount() < max) {
            modify(builder -> builder.amount(getItem().getAmount() + 1));
        }
    }

    /**
     * Restore this item to its original type and quantity.
     */
    public void restore() {
        setItem(getOriginal().clone());
        update(kit.getPlayer().getInventory());
    }

    @Override
    public void update(Inventory inv) {
        super.update(inv);

        // Only force update the item if the player is holding the item
        // Otherwise, we might interrupt something else, e.g. a bow being drawn back
        if (isItem(kit.getPlayer().getItemInHand())) {
            kit.getPlayer().updateInventory();
        }
    }

    public void modify(UnaryOperator<ItemBuilder> editor) {
        editor.apply(new ItemBuilder(getItem(), false));
        update(kit.getPlayer().getInventory());
    }

}
