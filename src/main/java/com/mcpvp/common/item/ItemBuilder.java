package com.mcpvp.common.item;

import com.mcpvp.common.time.Duration;
import com.mcpvp.common.util.chat.Colors;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

// import org.bukkit.inventory.ItemFlag;

/**
 * A chainable builder for {@link ItemStack} that makes it easy to modify the
 * properties of the {@link ItemStack} being built.
 * <p>
 * Modifications are performed as they are called, not all at once.
 *
 * @author NomNuggetNom
 */
@SuppressWarnings("deprecation")
public class ItemBuilder implements Cloneable {

    /**
     * The stack that is being modified.
     */
    protected ItemStack item;

    /**
     * Instantiates a new item builder using a clone of the specified {@code ItemStack}.<br>
     * All of the properties of the stack are retained and can be manipulated.
     *
     * @param item The {@link ItemStack} to be cloned then used.
     */
    public ItemBuilder(ItemStack item) {
        this(item, true);
    }

    /**
     * Instantiates a new item builder using the specified {@code ItemStack}. <br>
     * All of the properties of the stack are retained and can be manipulated.
     *
     * @param item  The {@link ItemStack} to be used.
     * @param clone Whether or not to clone the stack.
     */
    public ItemBuilder(ItemStack item, boolean clone) {
        this.item = clone ? item.clone() : item;
    }

    /**
     * Instantiates a new item builder using the specified {@code Material}.
     *
     * @param material The {@link Material} to use.
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    /**
     * Instantiates a new item builder using the specified {@code Potion}.
     *
     * @param potion The {@link Potion} to use.
     */
    public ItemBuilder(Potion potion) {
        this(Material.POTION);
        data(potion.toDamageValue());
    }

    /**
     * Instantiates a new item builder using the specified {@code Material}.
     * This is simply a shortcut method that uses the public constructor
     * {@link ItemBuilder#ItemBuilder(Material)}.
     *
     * @param material - the {@link Material} to use.
     * @return the instance created.
     */
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder of(ItemStack item) {
        return new ItemBuilder(item);
    }

    /**
     * @return A new PotionBuilder instance.
     */
    public static PotionBuilder potion() {
        return new PotionBuilder();
    }

    /**
     * Gets the {@code ItemStack} that was built.
     *
     * @return the {@link ItemStack} that was built.
     */
    public ItemStack build() {
        return item;
    }

    /**
     * Sets the ItemStack's type.
     *
     * @param material The Material to use as the type.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder type(Material material) {
        item.setType(material);
        return this;
    }

    /**
     * Sets the amount in the {@code ItemStack} via
     * {@code ItemStack#setAmount(int)}.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder amount(int quantity) {
        item.setAmount(quantity);
        return this;
    }

    /**
     * Increases the amount in the {@code ItemStack} via
     * {@code ItemStack#setAmount(int)}.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder increaseAmount(int delta) {
        item.setAmount(item.getAmount() + delta);
        return this;
    }

    /**
     * Sets the amount, but confines it to a range of 1 to 64.
     *
     * @param quantity The quantity to assign the ItemStack.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder safeAmount(int quantity) {
        if (quantity > 64)
            return amount(64);
        if (quantity < 1)
            return amount(1);
        return amount(quantity);
    }

    /**
     * Sets the {@code ItemStack}'s name to the {@code ItemStack}'s generic<br>
     * material ({@code item.getType()})'s name. This serves as a 'best guess.'
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder genericName() {
        name(WordUtils.capitalize(item.getType().toString().toLowerCase().replaceAll("_", " ")));
        return this;
    }

    /**
     * Sets the {@code ItemStack}'s name to the name specified.
     *
     * @param name - the name to give this item.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder name(String name) {
        return rawName(ChatColor.RESET + name);
    }

    /**
     * Sets the {@code ItemStack}'s name to the raw name specified.
     *
     * @param name - the name to give this item.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder rawName(String name) {
        editMeta(m -> m.setDisplayName(name));
        return this;
    }

    /**
     * Sets the lore of the item using the default vanilla appearance.<br>
     * - (lines prefixed with {@link ChatColor#DARK_PURPLE} and
     * {@link ChatColor#ITALIC})<br>
     * <br>
     * <p>
     * Use {@link #desc(List)} for more control over the appearance.
     *
     * @param lore - the lore to assign the item.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder lore(List<String> lore) {
        editMeta(m -> m.setLore(lore));
        return this;
    }

    /**
     * Collects the given lines into a List and uses {@code #setLore(List)}.
     *
     * @param lore - the array of lines that pass onto {@link #lore(List)}.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    /**
     * Sets the lore given a specified string and a character limit<br>
     * for wrapping lines. Intended for 'prettier' item lores.
     *
     * @param charsPerLine - the number of characters before wrapping the line.
     * @return this builder instance, for chaining.
     * @see ItemUtil#wrapWithColor(String, int)
     */
    public ItemBuilder lore(String lore, int charsPerLine) {
        return lore(ItemUtil.wrapWithColor(lore, charsPerLine));
    }

    /**
     * Sets a custom description for the item
     *
     * @param desc - The description to use
     * @return this builder instance, for chaining.
     */
    public ItemBuilder desc(List<String> desc) {
        ItemUtil.setDescription(item, desc);
        return this;
    }

    /**
     * Collects the given lines into a List and uses {@code #setDesc(List)}.
     *
     * @param desc - the array of lines that pass onto {@link #desc(List)}.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder desc(String... desc) {
        return desc(Arrays.asList(desc));
    }

    /**
     * Adds the given lines to the existing lore.
     *
     * @param desc - the array of lines that pass onto {@link #desc(List)}.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder addDesc(String... desc) {
        List<String> lore = ItemUtil.getLore(item);
        if (lore == null)
            lore = new ArrayList<>();
        lore.addAll(Arrays.asList(desc));
        return desc(lore);
    }

    /**
     * Adds the given lines to the existing lore.
     *
     * @param desc - the list of lines that pass onto {@link #desc(List)}.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder addDesc(List<String> desc) {
        List<String> lore = ItemUtil.getLore(item);
        if (lore == null) lore = new ArrayList<>();
        lore.addAll(desc);
        return desc(lore);
    }

    /**
     * Sets the lore given a specified string and a character limit<br>
     * for wrapping lines. Intended for 'prettier' item lores.
     *
     * @param charsPerLine - the number of characters before wrapping the line.
     * @return this builder instance, for chaining.
     * @see ItemUtil#wrapWithColor(String, int)
     */
    public ItemBuilder desc(String desc, int charsPerLine) {
        return desc(ItemUtil.wrapWithColor(desc, charsPerLine));
    }

    /**
     * Assigns an NBT value with the given key and value. This operation requires
     * a completely new copy of the ItemStack being modified, and as such will not
     * be reflected on the original ItemStack, if one was used to create this instance.
     *
     * @param key   The key of the NBT tag to assign.
     * @param value The value of the NBT tag to assign.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder tag(String key, String value) {
        item = NBTUtil.saveString(this.item, key, value);
        return this;
    }

    /**
     * Adds the specified {@code Enchantment} to the {@code ItemStack} being
     * built (only if the ItemStack IS an Enchanted Book).
     *
     * @param enchantment - the {@link Enchantment} type to be granted.
     * @param level       - the level of enchantment to be granted.
     * @param force       - if true,
     *                    {@link ItemStack#addUnsafeEnchantment(Enchantment, int)} will be
     *                    used.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder enchantBook(Enchantment enchantment, Integer level, boolean force) {
        if (item.getType() != Material.ENCHANTED_BOOK)
            return this;

        editMeta(meta -> {

            EnchantmentStorageMeta eMeta = (EnchantmentStorageMeta) meta;
            eMeta.addStoredEnchant(enchantment, level, force);

        });

        return this;
    }

    /**
     * Adds the specified {@code Enchantment} to the {@code ItemStack} being
     * built.
     *
     * @param enchantment - the {@link Enchantment} type to be granted.
     * @param level       - the level of enchantment to be granted.
     * @param force       - if true,
     *                    {@link ItemStack#addUnsafeEnchantment(Enchantment, int)} will be
     *                    used.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder enchant(Enchantment enchantment, int level, boolean force) {
        if (force)
            item.addUnsafeEnchantment(enchantment, level);
        else
            item.addEnchantment(enchantment, level);
        return this;
    }

    /**
     * Adds the specified {@code Enchantment} to the {@code ItemStack} being
     * built (unforcibly).
     *
     * @param enchantment - the {@link Enchantment} type to be granted.
     * @param level       - the level of enchantment to be granted.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        return enchant(enchantment, level, true);
    }

    /**
     * Removes the given {@code Enchantment} from the {@code ItemStack} being
     * built.
     *
     * @param enchantment the {@code Enchantment} to be removed
     * @return this builder instance, for chaining.
     */
    public ItemBuilder unenchant(Enchantment enchantment) {
        item.removeEnchantment(enchantment);
        return this;
    }

    /**
     * Sets the durability of this item, which is essentially like adding data.
     *
     * @param data The durability to set the item to.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder durability(Number data) {
        item.setDurability(data.shortValue());
        return this;
    }

    /**
     * Sets the durability of this item in percent
     */
    public ItemBuilder durabilityPercent(float percent) {
        item.setDurability((short) (item.getType().getMaxDurability() * (1 - percent)));
        return this;
    }

    /**
     * Sets the durability of this item, which is essentially like adding data.
     *
     * @param data The durability to set the item to.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder data(Number data) {
        return durability(data);
    }

    /**
     * Attempts to color the item. This can be used to color wool, carpet,
     * glass, and so on. Leather items use {@link #color(Color)}.
     *
     * @param color - the color to make the item.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder color(DyeColor color) {
        switch (item.getType()) {
            case WOOL:
            case CARPET:
            case STAINED_GLASS:
            case STAINED_GLASS_PANE:
                item.setDurability(color.getData());
                break;
            case INK_SACK:
            case STAINED_CLAY:
            case CLAY:
                item.setDurability(color.getDyeData());
                break;
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
                return color(color.getColor());
            case GLASS:
                return type(Material.STAINED_GLASS).color(color);
            case THIN_GLASS:
                return type(Material.STAINED_GLASS_PANE).color(color);
            default:
                new Exception("Attempted to color a non-colorable item").printStackTrace();
        }
        return this;
    }

    /**
     * Attempts to dye the {@code ItemStack} being built.
     *
     * @param color - the color to set this item to.
     * @return this builder instance, for chaining.
     * @see #color(Color, boolean)
     */
    public ItemBuilder color(Colors color) {
        return color(color.getDye());
    }

    /**
     * Attempts to dye the {@code ItemStack} being built. By default, this
     * blends the color. This should only be used with leather armor.
     *
     * @param color - the color to set this item to.
     * @return this builder instance, for chaining.
     * @see #color(Color, boolean)
     */
    public ItemBuilder color(Color color) {
        return color(color, false);
    }

    /**
     * Attempts to dye the {@code ItemStack} being built to the specified color.
     *
     * @param color - the {@link Color} to set this item (of 'LEATHER' origin)
     *              to.
     * @param mix   - whether the {@link Color} should be mixed with the default
     *              {@link Color}.
     * @return this builder instance, for chaining.
     * @throws IllegalStateException if item doesn't match a
     *                               leather armor piece.
     */
    public ItemBuilder color(Color color, boolean mix) {
        if (!(item.getItemMeta() instanceof LeatherArmorMeta))
            throw new IllegalStateException("Only leather items can be dyed!");

        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (mix)
            meta.setColor(color.mixColors(Bukkit.getItemFactory().getDefaultLeatherColor(), color));
        else
            meta.setColor(color);
        item.setItemMeta(meta);
        return this;
    }

    /**
     * Adds the given flags to the ItemStack.
     *
     * @param flags The flags to add.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder flag(ItemFlag... flags) {
        editMeta(m -> m.addItemFlags(flags));
        return this;
    }

    /**
     * Hides metadata that is displayed on the ItemStack, like enchantments,
     * potion effects, etc.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder hideData() {
        return flag(ItemFlag.values());
    }

    /**
     * Makes the ItemStack unbreakable, without adding any enchantment. The
     * client will show an "Unbreakable" notice in the item's lore unless
     * {@link #flag(ItemFlag...)} is called with {@link ItemFlag#HIDE_UNBREAKABLE}.
     *
     * @return this builder instance, for chaining.
     * @see #breakable()
     */
    public ItemBuilder unbreakable() {
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.spigot().setUnbreakable(true);
        return this;
    }

    /**
     * Makes the ItemStack breakable, without affecting the enchantments.
     *
     * @return this builder instance, for chaining.
     * @see #unbreakable()
     */
    public ItemBuilder breakable() {
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.spigot().setUnbreakable(false);
        return this;
    }

    /**
     * Adds a dummy enchantment to this item, which players can't see.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder dummyEnchant() {
        return enchant(Enchantment.LURE, 5).flag(ItemFlag.HIDE_ENCHANTS);
    }

    /**
     * Adds a dummy enchantment to this item, which players can't see.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder removeDummyEnchant() {
        item.removeEnchantment(Enchantment.LURE);
        editMeta(itemMeta -> itemMeta.removeItemFlags(ItemFlag.HIDE_ENCHANTS));
        return this;
    }

    /**
     * Adds x to the amount of the item stack
     */
    public ItemBuilder add(int amount) {
        return amount(item.getAmount() + amount);
    }

    /**
     * Removes x to the amount of the item stack
     */
    public ItemBuilder remove(int amount) {
        return amount(item.getAmount() - amount);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public ItemBuilder clone() {
        return new ItemBuilder(this.item, true);
    }

    /**
     * Allows for easy modification of the ItemMeta while preserving changes.
     *
     * @param consumer The consumer that edits the meta.
     */
    private void editMeta(Consumer<ItemMeta> consumer) {
        ItemMeta meta = item.getItemMeta();
        consumer.accept(meta);
        item.setItemMeta(meta);
    }

    /**
     * A special ItemBuilder especially for making potions.
     *
     * @author NomNuggetNom
     */
    public static class PotionBuilder extends ItemBuilder {

        private Potion potion;

        public PotionBuilder() {
            super(Material.POTION);
        }

        /**
         * Makes this potion a splash potion.
         *
         * @return this builder instance, for chaining.
         */
        public PotionBuilder splash() {
            potion.splash();
            return this;
        }

        /**
         * Assigns the given PotionType with the given duration and the given
         * amplifier.
         *
         * @param type      The effect to add.
         * @param duration  The duration to make the effect last.
         * @param amplifier The amplifier of the potion effect. Anything besides
         *                  0 makes a stronger potion. For example, an amplifier of 1
         *                  is equal to a strength II potion.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionEffectType type, Duration duration, int amplifier) {
            if (this.potion == null)
                this.potion = new Potion(getType(type));
            editMeta(m -> m.addCustomEffect(new PotionEffect(type, duration.ticks(), amplifier), true));
            return this;
        }

        /**
         * Assigns the given PotionType with a duration of 0 and the given
         * amplifier.
         *
         * @param type      The type to add.
         * @param amplifier The amplifier of the potion effect. Anything besides
         *                  0 makes a stronger potion. For example, an amplifier of 1
         *                  is equal to a strength II potion.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionEffectType type, int amplifier) {
            return effect(type, Duration.ZERO, amplifier);
        }

        /**
         * Assigns the given PotionType with the given duration and an
         * amplifier of 0.
         *
         * @param type     The type to add.
         * @param duration The duration to make the effect last.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionEffectType type, Duration duration) {
            return effect(type, duration, 0);
        }

        /**
         * Assigns the given PotionType with a duration of 0 and an
         * amplifier of 0.
         *
         * @param type The type to add.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionEffectType type) {
            return effect(type, Duration.ZERO, 0);
        }

        /**
         * Assigns the given PotionType with the given duration and the given
         * amplifier.
         *
         * @param type      The effect to add.
         * @param duration  The duration to make the effect last.
         * @param amplifier The amplifier of the potion effect. Anything besides
         *                  0 makes a stronger potion. For example, an amplifier of 1
         *                  is equal to a strength II potion.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionType type, Duration duration, int amplifier) {
            return effect(type.getEffectType(), duration, amplifier);
        }

        /**
         * Assigns the given PotionType with a duration of 0 and the given
         * amplifier.
         *
         * @param type      The type to add.
         * @param amplifier The amplifier of the potion effect. Anything besides
         *                  0 makes a stronger potion. For example, an amplifier of 1
         *                  is equal to a strength II potion.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionType type, int amplifier) {
            return effect(type.getEffectType(), amplifier);
        }

        /**
         * Assigns the given PotionType with the given duration and an
         * amplifier of 0.
         *
         * @param type     The type to add.
         * @param duration The duration to make the effect last.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionType type, Duration duration) {
            return effect(type.getEffectType(), duration, 0);
        }

        /**
         * Assigns the given PotionType with a duration of 0 and an
         * amplifier of 0.
         *
         * @param type The type to add.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionType type) {
            return effect(type.getEffectType(), Duration.ZERO, 0);
        }

        private PotionType getType(PotionEffectType effect) {
            for (PotionType pt : PotionType.values())
                if (pt.getEffectType() == effect)
                    return pt;
            return null;
        }

        private void editMeta(Consumer<PotionMeta> consumer) {
            PotionMeta meta = (PotionMeta) build().getItemMeta();
            consumer.accept(meta);
            build().setItemMeta(meta);
        }

        @Override
        public ItemStack build() {
            potion.apply(item);
            return item;
        }

    }

    public static class FireworkBuilder extends ItemBuilder {

        public FireworkBuilder() {
            super(Material.FIREWORK);
        }

        public FireworkBuilder effect(FireworkEffect... effects) {
            editMeta(m -> m.addEffects(effects));
            return this;
        }

        public FireworkBuilder power(int power) {
            editMeta(m -> m.setPower(power));
            return this;
        }

        public void editMeta(Consumer<FireworkMeta> consumer) {
            FireworkMeta meta = getMeta();
            consumer.accept(meta);
            item.setItemMeta(meta);
        }

        private FireworkMeta getMeta() {
            return (FireworkMeta) this.item.getItemMeta();
        }
    }

}
