package com.mcpvp.common.kit;

import com.mcpvp.common.item.InteractiveItem;
import com.mcpvp.common.item.ItemBuilder;
import lombok.Getter;
import lombok.ToString;
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
        this.original = this.getItem().clone();
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
        if (this.getItem().getAmount() == 1 && placeholder) {
            this.setPlaceholder();
        } else {
            this.modify(builder -> builder.amount(this.getItem().getAmount() - 1));
        }
    }

    /**
     * Makes this item look like a placeholder. Calls to {@link #isPlaceholder()} will be true.
     */
    public void setPlaceholder() {
        this.modify(ib ->
            ib.type(Material.STAINED_GLASS_PANE).durability(0)
        );
    }

    /**
     * Check if this item has been made into a placeholder by {@link #setPlaceholder()}.
     *
     * @return If this item is a placeholder.
     */
    public boolean isPlaceholder() {
        return this.getItem().getType() == Material.STAINED_GLASS_PANE;
    }

    /**
     * Increases the number of items in the item stack, unless the number of items is already
     * the given max.
     *
     * @param max The maximum amount.
     */
    public void increment(int max) {
        if (this.isPlaceholder()) {
            this.restore();
            this.modify(builder -> builder.amount(1));
        } else if (this.getItem().getAmount() < max) {
            this.modify(builder -> builder.amount(this.getItem().getAmount() + 1));
        }
    }

    /**
     * Restore this item to its original type and quantity.
     */
    public void restore() {
        this.setItem(this.getOriginal().clone());
        this.update(this.kit.getPlayer().getInventory());
    }

    @Override
    public void update(Inventory inv) {
        super.update(inv);

        // Only force update the item if the player is holding the item
        // Otherwise, we might interrupt something else, e.g. a bow being drawn back
        if (this.isItem(this.kit.getPlayer().getItemInHand())) {
            this.kit.getPlayer().setItemInHand(this.getItem());
            this.kit.getPlayer().updateInventory();
        }
    }

    public void modify(UnaryOperator<ItemBuilder> editor) {
        editor.apply(new ItemBuilder(this.getItem(), false));
        this.update(this.kit.getPlayer().getInventory());
//        kit.getPlayer().updateInventory();
    }

}
