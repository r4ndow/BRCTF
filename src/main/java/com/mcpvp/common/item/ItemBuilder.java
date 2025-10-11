package com.mcpvp.common.item;

import com.mcpvp.common.time.Duration;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.chat.Colors;
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
        this.data(potion.toDamageValue());
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
        return this.item;
    }

    /**
     * Sets the ItemStack's type.
     *
     * @param material The Material to use as the type.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder type(Material material) {
        this.item.setType(material);
        return this;
    }

    /**
     * Sets the amount in the {@code ItemStack} via
     * {@code ItemStack#setAmount(int)}.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder amount(int quantity) {
        this.item.setAmount(quantity);
        return this;
    }

    /**
     * Increases the amount in the {@code ItemStack} via
     * {@code ItemStack#setAmount(int)}.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder increaseAmount(int delta) {
        this.item.setAmount(this.item.getAmount() + delta);
        return this;
    }

    /**
     * Sets the amount, but confines it to a range of 1 to 64.
     *
     * @param quantity The quantity to assign the ItemStack.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder safeAmount(int quantity) {
        if (quantity > 64) {
            return this.amount(64);
        }
        if (quantity < 1) {
            return this.amount(1);
        }
        return this.amount(quantity);
    }

    /**
     * Sets the {@code ItemStack}'s name to the {@code ItemStack}'s generic<br>
     * material ({@code item.getType()})'s name. This serves as a 'best guess.'
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder genericName() {
        this.name(WordUtils.capitalize(this.item.getType().toString().toLowerCase().replaceAll("_", " ")));
        return this;
    }

    /**
     * Sets the {@code ItemStack}'s name to the name specified.
     *
     * @param name - the name to give this item.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder name(String name) {
        return this.rawName(ChatColor.RESET + name);
    }

    /**
     * Sets the {@code ItemStack}'s name to the raw name specified.
     *
     * @param name - the name to give this item.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder rawName(String name) {
        this.editMeta(m -> m.setDisplayName(name));
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
        this.editMeta(m -> m.setLore(lore));
        return this;
    }

    /**
     * Collects the given lines into a List and uses {@code #setLore(List)}.
     *
     * @param lore - the array of lines that pass onto {@link #lore(List)}.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder lore(String... lore) {
        return this.lore(Arrays.asList(lore));
    }

    /**
     * Sets the lore given a specified string and a character limit<br>
     * for wrapping lines. Intended for 'prettier' item lores.
     *
     * @param charsPerLine - the number of characters before wrapping the line.
     * @return this builder instance, for chaining.
     * @see C#wrapWithColor(String, int)
     */
    public ItemBuilder lore(String lore, int charsPerLine) {
        return this.lore(C.wrapWithColor(lore, charsPerLine));
    }

    /**
     * Sets a custom description for the item
     *
     * @param desc - The description to use
     * @return this builder instance, for chaining.
     */
    public ItemBuilder desc(List<String> desc) {
        ItemUtil.setDescription(this.item, desc);
        return this;
    }

    /**
     * Sets the lore given a specified string and a character limit<br>
     * for wrapping lines. Intended for 'prettier' item lores.
     *
     * @param charsPerLine - the number of characters before wrapping the line.
     * @return this builder instance, for chaining.
     * @see C#wrapWithColor(String, int)
     */
    public ItemBuilder desc(String desc, int charsPerLine) {
        return this.desc(C.wrapWithColor(desc, charsPerLine));
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
        this.item = NBTUtil.saveString(this.item, key, value);
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
        if (this.item.getType() != Material.ENCHANTED_BOOK) {
            return this;
        }

        this.editMeta(meta -> {
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
        if (force) {
            this.item.addUnsafeEnchantment(enchantment, level);
        } else {
            this.item.addEnchantment(enchantment, level);
        }

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
        return this.enchant(enchantment, level, true);
    }

    /**
     * Removes the given {@code Enchantment} from the {@code ItemStack} being
     * built.
     *
     * @param enchantment the {@code Enchantment} to be removed
     * @return this builder instance, for chaining.
     */
    public ItemBuilder unenchant(Enchantment enchantment) {
        this.item.removeEnchantment(enchantment);
        return this;
    }

    /**
     * Sets the durability of this item, which is essentially like adding data.
     *
     * @param data The durability to set the item to.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder durability(Number data) {
        this.item.setDurability(data.shortValue());
        return this;
    }

    /**
     * Sets the durability of this item in percent
     */
    public ItemBuilder durabilityPercent(float percent) {
        this.item.setDurability((short) (this.item.getType().getMaxDurability() * (1 - percent)));
        return this;
    }

    /**
     * Sets the durability of this item, which is essentially like adding data.
     *
     * @param data The durability to set the item to.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder data(Number data) {
        return this.durability(data);
    }

    /**
     * Attempts to color the item. This can be used to color wool, carpet,
     * glass, and so on. Leather items use {@link #color(Color)}.
     *
     * @param color - the color to make the item.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder color(DyeColor color) {
        switch (this.item.getType()) {
            case WOOL:
            case CARPET:
            case STAINED_GLASS:
            case STAINED_GLASS_PANE:
                this.item.setDurability(color.getData());
                break;
            case INK_SACK:
            case STAINED_CLAY:
            case CLAY:
                this.item.setDurability(color.getDyeData());
                break;
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
                return this.color(color.getColor());
            case GLASS:
                return this.type(Material.STAINED_GLASS).color(color);
            case THIN_GLASS:
                return this.type(Material.STAINED_GLASS_PANE).color(color);
            default:
                throw new IllegalArgumentException("Attempted to color a non-colorable item");
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
        return this.color(color.getDye());
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
        return this.color(color, false);
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
        if (!(this.item.getItemMeta() instanceof LeatherArmorMeta meta)) {
            throw new IllegalStateException("Only leather items can be dyed!");
        }

        if (mix) {
            meta.setColor(color.mixColors(Bukkit.getItemFactory().getDefaultLeatherColor(), color));
        } else {
            meta.setColor(color);
        }
        this.item.setItemMeta(meta);
        return this;
    }

    /**
     * Adds the given flags to the ItemStack.
     *
     * @param flags The flags to add.
     * @return this builder instance, for chaining.
     */
    public ItemBuilder flag(ItemFlag... flags) {
        this.editMeta(m -> m.addItemFlags(flags));
        return this;
    }

    /**
     * Hides metadata that is displayed on the ItemStack, like enchantments,
     * potion effects, etc.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder hideData() {
        return this.flag(ItemFlag.values());
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
        ItemMeta itemMeta = this.item.getItemMeta();
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
        ItemMeta itemMeta = this.item.getItemMeta();
        itemMeta.spigot().setUnbreakable(false);
        return this;
    }

    /**
     * Adds a dummy enchantment to this item, which players can't see.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder dummyEnchant() {
        return this.enchant(Enchantment.LURE, 5).flag(ItemFlag.HIDE_ENCHANTS);
    }

    /**
     * Adds a dummy enchantment to this item, which players can't see.
     *
     * @return this builder instance, for chaining.
     */
    public ItemBuilder removeDummyEnchant() {
        this.item.removeEnchantment(Enchantment.LURE);
        this.editMeta(itemMeta -> itemMeta.removeItemFlags(ItemFlag.HIDE_ENCHANTS));
        return this;
    }

    /**
     * Adds x to the amount of the item stack
     */
    public ItemBuilder add(int amount) {
        return this.amount(this.item.getAmount() + amount);
    }

    /**
     * Removes x to the amount of the item stack
     */
    public ItemBuilder remove(int amount) {
        return this.amount(this.item.getAmount() - amount);
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
        ItemMeta meta = this.item.getItemMeta();
        consumer.accept(meta);
        this.item.setItemMeta(meta);
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
            this.potion.splash();
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
            if (this.potion == null) {
                this.potion = new Potion(this.getType(type));
            }
            this.editMeta(m -> m.addCustomEffect(new PotionEffect(type, duration.ticks(), amplifier), true));
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
            return this.effect(type, Duration.ZERO, amplifier);
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
            return this.effect(type, duration, 0);
        }

        /**
         * Assigns the given PotionType with a duration of 0 and an
         * amplifier of 0.
         *
         * @param type The type to add.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionEffectType type) {
            return this.effect(type, Duration.ZERO, 0);
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
            return this.effect(type.getEffectType(), duration, amplifier);
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
            return this.effect(type.getEffectType(), amplifier);
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
            return this.effect(type.getEffectType(), duration, 0);
        }

        /**
         * Assigns the given PotionType with a duration of 0 and an
         * amplifier of 0.
         *
         * @param type The type to add.
         * @return this builder instance, for chaining.
         */
        public PotionBuilder effect(PotionType type) {
            return this.effect(type.getEffectType(), Duration.ZERO, 0);
        }

        private PotionType getType(PotionEffectType effect) {
            for (PotionType pt : PotionType.values()) {
                if (pt.getEffectType() == effect) {
                    return pt;
                }
            }

            return null;
        }

        private void editMeta(Consumer<PotionMeta> consumer) {
            PotionMeta meta = (PotionMeta) this.build().getItemMeta();
            consumer.accept(meta);
            this.build().setItemMeta(meta);
        }

        @Override
        public ItemStack build() {
            this.potion.apply(this.item);
            return this.item;
        }

    }

    public static class FireworkBuilder extends ItemBuilder {

        public FireworkBuilder() {
            super(Material.FIREWORK);
        }

        public FireworkBuilder effect(FireworkEffect... effects) {
            this.editMeta(m -> m.addEffects(effects));
            return this;
        }

        public FireworkBuilder power(int power) {
            this.editMeta(m -> m.setPower(power));
            return this;
        }

        public void editMeta(Consumer<FireworkMeta> consumer) {
            FireworkMeta meta = this.getMeta();
            consumer.accept(meta);
            this.item.setItemMeta(meta);
        }

        private FireworkMeta getMeta() {
            return (FireworkMeta) this.item.getItemMeta();
        }
    }

}
