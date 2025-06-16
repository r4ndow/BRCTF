package com.mcpvp.common.kit;

import com.mcpvp.common.item.InteractiveItem;
import com.mcpvp.common.item.ItemBuilder;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.Material;
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
        this.original = itemStack.clone();
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
            getItem().setAmount(getItem().getAmount() - 1);
            update(kit.getPlayer().getInventory());
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
            update(kit.getPlayer().getInventory());
        } else if (getItem().getAmount() < max) {
            getItem().setAmount(getItem().getAmount() + 1);
            update(kit.getPlayer().getInventory());
        }
    }

    public void restore() {
        if (isPlaceholder()) {
            getItem().setType(original.getType());
        }

        if (getItem().getAmount() != original.getAmount()) {
            getItem().setAmount(original.getAmount());
        }

        update(kit.getPlayer().getInventory());
    }

    public void modify(UnaryOperator<ItemBuilder> editor) {
        editor.apply(new ItemBuilder(getItem(), false));
        update(kit.getPlayer().getInventory());
    }

}
