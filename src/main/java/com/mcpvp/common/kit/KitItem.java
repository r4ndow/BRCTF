package com.mcpvp.common.kit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import com.mcpvp.common.item.InteractiveItem;

/**
 * An item tied to an instance of a kit, with extra utilities.
 */
public class KitItem extends InteractiveItem {
    
    protected final Kit kit;
    private final ItemStack original;

    public KitItem(Kit kit, ItemStack itemStack) {
        super(kit.getPlugin(), itemStack);
        this.kit = kit;
        this.original = itemStack.clone();
    }

    /**
     * Decrease the number of items in the stack by one, or swap the item to a placeholder
     * if there will be no items left.
     * 
     * @param placeholder If the item should be swapped to a placeholder.
     */
    public void decrement(boolean placeholder) {
        if (getItem().getAmount() == 1) {
            setPlaceholder();
        } else {
            getItem().setAmount(getItem().getAmount() - 1);
        }
    }

    /**
     * Makes this item look like a placeholder. Calls to {@link #isPlaceholder()} will be true.
     */
    public void setPlaceholder() {
        getItem().setType(Material.STAINED_GLASS_PANE);
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
            getItem().setType(original.getType());
        } else if (getItem().getAmount() < max) {
            getItem().setAmount(getItem().getAmount() + 1);
        }
    }

}
