package com.mcpvp.battle.kits;

import com.mcpvp.common.time.Expiration;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.item.CooldownItem;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.movement.CancelNextFallTask;
import com.mcpvp.common.time.Duration;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThiefKit extends BattleKit {

    // Configurable variables
    private static final Duration STOLEN_ITEM_DISPLAY_TIME = Duration.seconds(10);
    private static final Duration STOLEN_ITEM_RETURN_TIME = Duration.seconds(7);
    private static final Duration STEAL_COOLDOWN = Duration.seconds(3);
    private static final Duration GRAPPLE_COOLDOWN = Duration.seconds(8);
    private static final int SPEED_DURATION_TICKS = 80; // 4 seconds

    private static final Duration VICTIM_STEAL_COOLDOWN = Duration.seconds(15);
    private static final Map<UUID, Expiration> VICTIM_STEAL_PROTECTION = new HashMap<>();


    // Grappling Hook physics constants
    private static final double HORIZONTAL_IMPULSE_MULTIPLIER = 1.6;
    private static final double VERTICAL_IMPULSE_BASE = 1.2;
    private static final double HORIZONTAL_DISTANCE_SCALE = 5.0;
    private static final double VERTICAL_REDUCTION_FACTOR = 0.4;

    private final Map<Player, StolenItemData> stolenItems = new HashMap<>();

    private StealAbility stealAbility;
    private GrapplingHook grapplingHook;

    public ThiefKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Thief";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
                ItemBuilder.of(Material.LEATHER_BOOTS)
                        .enchant(Enchantment.PROTECTION_FALL, 3)
                        .build(),
                ItemBuilder.of(Material.LEATHER_LEGGINGS)
                        .color(Color.fromRGB(64, 64, 64)) // #404040
                        .build(),
                ItemBuilder.of(Material.LEATHER_CHESTPLATE)
                        .color(Color.fromRGB(60, 65, 85)) // #3C4155
                        .build(),
                ItemBuilder.of(Material.LEATHER_HELMET)
                        .color(Color.fromRGB(64, 64, 64)) // #404040
                        .build()
        };
    }


    @Override
    public Map<Integer, KitItem> createItems() {
        this.stealAbility = new StealAbility();
        this.grapplingHook = new GrapplingHook();
        return new KitInventoryBuilder()
                .add(ItemBuilder.of(Material.WOOD_SWORD)
                        .name("Thief Sword")
                        .enchant(Enchantment.DAMAGE_ALL, 1)
                        .enchant(Enchantment.KNOCKBACK, 2)
                        .unbreakable())
                .addFood(5)
                .add(this.stealAbility)
                .add(this.grapplingHook)
                .addCompass(8)
                .build();
    }

    @EventHandler
    public void onHitWithStealAbility(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (attacker != this.getPlayer()) return;
        if (!this.stealAbility.isItem(this.getPlayer().getItemInHand())) return;

        // Use our logical cooldown
        if (this.stealAbility.isOnHitCooldown()) {
            event.setCancelled(true);
            this.getPlayer().sendMessage(C.warn(C.RED) + "Steal is on cooldown!");
            return;
        }

        // Same team check
        if (this.getBattle().getGame().getTeamManager().isSameTeam(attacker, victim)) {
            return;
        }

        if (this.attemptSteal(victim)) {
            this.stealAbility.startHitCooldown();
        }
    }

    private boolean attemptSteal(Player victim) {
        Player thief = this.getPlayer();

        ItemStack heldItem = victim.getItemInHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            thief.sendMessage(C.warn(C.RED) + "Target has no item to steal!");
            return false;
        }

        if (isNonStealableItem(heldItem)) {
            thief.sendMessage(C.warn(C.RED) + "You cannot steal that item");
            return false;
        }

        Expiration protection = VICTIM_STEAL_PROTECTION.get(victim.getUniqueId());
        if (protection != null && !protection.isExpired()) {
            long secondsLeft = Math.max(0, protection.getRemaining().seconds());
            thief.sendMessage(
                    C.warn(C.RED) + C.hl(victim.getName()) + " cannot be stolen from for " + secondsLeft + " seconds!"
            );
            return false;
        }

        int freeSlot = thief.getInventory().firstEmpty();
        if (freeSlot == -1) {
            thief.sendMessage(C.warn(C.RED) + "Your inventory is full! You cannot steal");
            return false;
        }

        ItemStack placeholder = ItemBuilder.of(Material.STAINED_GLASS_PANE)
                .name(C.GRAY + "Stolen!")
                .data((short) 14)
                .build();

        int victimSlot = victim.getInventory().getHeldItemSlot();

        victim.getInventory().setItem(victimSlot, placeholder);
        victim.updateInventory();

        ItemStack stolenCopy = heldItem.clone();
        thief.getInventory().setItem(freeSlot, stolenCopy);
        thief.updateInventory();

        StolenItemData data = new StolenItemData(
                victim,
                thief,
                heldItem.clone(),
                victimSlot,
                freeSlot
        );
        this.stolenItems.put(victim, data);

        thief.sendMessage(C.info(C.GREEN) + "You stole " + C.hl(victim.getName()) + "'s item!");
        victim.sendMessage(C.warn(C.RED) + C.hl(thief.getName()) + " stole your item!");

        thief.playSound(thief.getLocation(), Sound.ITEM_PICKUP, 1.0f, 1.0f);
        victim.playSound(victim.getLocation(), Sound.ITEM_PICKUP, 1.0f, 1.0f);

        this.attach(new BukkitRunnable() {
            @Override
            public void run() {
                StolenItemData current = stolenItems.get(victim);
                if (current == null) {
                    return;
                }

                Player t = current.getThief();
                if (t == null || !t.isOnline()) {
                    return;
                }

                ItemStack inSlot = t.getInventory().getItem(current.getThiefSlot());
                if (inSlot != null && inSlot.isSimilar(current.getOriginalItem())) {
                    t.getInventory().setItem(current.getThiefSlot(), null);
                    t.updateInventory();
                }
            }
        }.runTaskLater(this.getPlugin(), STOLEN_ITEM_DISPLAY_TIME.ticks()));

        this.attach(new BukkitRunnable() {
            @Override
            public void run() {
                returnStolenItem(victim);
            }
        }.runTaskLater(this.getPlugin(), STOLEN_ITEM_RETURN_TIME.ticks()));

        VICTIM_STEAL_PROTECTION.put(
                victim.getUniqueId(),
                Expiration.after(VICTIM_STEAL_COOLDOWN)
        );

        return true;
    }

    private boolean isNonStealableItem(ItemStack item) {
        if (item == null) {
            return false;
        }

        // is it a placeholder?
        if (item.getType() == Material.STAINED_GLASS_PANE) {
            return true;
        }

        if (item.getType() == Material.COMPASS) {
            return true;
        }
        // Any team flag (wool or banner)
        return this.getBattle().getGame().getTeamManager().getTeams().stream()
                .anyMatch(team -> team.getFlag().isItem(item));
    }




    private void returnStolenItem(Player victim) {
        StolenItemData data = this.stolenItems.remove(victim);
        if (data == null) return;

        Player thief = data.getThief();

        // Clean up thief's copy if still present
        if (thief != null && thief.isOnline()) {
            ItemStack inSlot = thief.getInventory().getItem(data.getThiefSlot());
            if (inSlot != null && inSlot.isSimilar(data.getOriginalItem())) {
                thief.getInventory().setItem(data.getThiefSlot(), null);
            } else {
                // Fallback: remove the first matching stack anywhere
                for (int i = 0; i < thief.getInventory().getSize(); i++) {
                    ItemStack stack = thief.getInventory().getItem(i);
                    if (stack != null && stack.isSimilar(data.getOriginalItem())) {
                        thief.getInventory().setItem(i, null);
                        break;
                    }
                }
            }
            thief.updateInventory();
        }

        // Restore the original item to the victim
        if (victim.isOnline()) {
            victim.getInventory().setItem(data.getVictimSlot(), data.getOriginalItem());
            victim.updateInventory();

            victim.playSound(victim.getLocation(), Sound.ITEM_PICKUP, 1.0f, 0.1f);
            victim.sendMessage(C.info(C.GREEN) + "Your stolen item has been returned!");
        }

        if (this.getPlayer().isOnline()) {
            this.getPlayer().sendMessage(
                    C.info(C.GRAY) + "Stolen item returned to " + victim.getName()
            );
        }
    }



    @Override
    public void shutdown() {
        // Return all stolen items when kit is destroyed
        for (Player victim : new HashMap<>(this.stolenItems).keySet()) {
            returnStolenItem(victim);
        }
        super.shutdown();
    }

    class StealAbility extends CooldownItem {

        // Separate cooldown just for hits
        private final Expiration hitCooldown = new Expiration();

        public StealAbility() {
            super(
                    ThiefKit.this,
                    ItemBuilder.of(Material.STICK)
                            .name("Steal")
                            .desc(C.GRAY + "Hit an enemy to steal their held item.", 40)
                            .build(),
                    STEAL_COOLDOWN
            );
        }

        @Override
        protected boolean autoUse() {
            return false;
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {}

        @Override
        protected void onFailedUse() {
            ThiefKit.this.getPlayer().sendMessage(
                    C.warn(C.RED) + "Steal is on cooldown!"
            );
        }

        // Logical cooldown for hits
        public boolean isOnHitCooldown() {
            return !this.hitCooldown.isExpired();
        }

        public void startHitCooldown() {
            this.hitCooldown.expireIn(STEAL_COOLDOWN);


            this.setPlaceholder();
            this.startCooldown();
        }

        /**
         * Show the stolen item *visually* in this slot without changing
         * the InteractiveItem's NBT tag. This uses modify(), not setItem().
         */
        public void showStolenAppearance(ItemStack victimItem) {
            this.modify(builder -> {
                builder
                        .type(victimItem.getType())
                        .durability(victimItem.getDurability());

                // Optional: copy basic name
                if (victimItem.hasItemMeta() && victimItem.getItemMeta().hasDisplayName()) {
                    builder.rawName(victimItem.getItemMeta().getDisplayName());
                } else {
                    builder.name(victimItem.getType().toString());
                }

                // Optional: copy enchantments
                victimItem.getEnchantments().forEach(
                        (ench, level) -> builder.enchant(ench, level)
                );

                // IMPORTANT: return the builder to satisfy UnaryOperator<ItemBuilder>
                return builder;
            });
        }

        /**
         * Restore the ability back to its original Steal item.
         */
        public void clearStolenAppearance() {
            this.restore(); // CooldownItem/KitItem restore() is safe: it uses original clone with correct tag
        }
    }


    class GrapplingHook extends CooldownItem {

        private final Expiration grappleCooldown = new Expiration();

        private FishHook activeHook;

        public GrapplingHook() {
            super(
                    ThiefKit.this,
                    ItemBuilder.of(Material.FISHING_ROD)
                            .name("Grappling Hook")
                            .desc(C.GRAY + "Right-click while your hook is attached to grapple towards it.", 40)
                            .build(),
                    GRAPPLE_COOLDOWN
            );
        }

        // --- Configuration & Base Behavior ---

        @Override
        protected boolean showExpRecharge() { return true; }

        @Override
        protected boolean showDurabilityRecharge() { return true; }

        @Override
        protected boolean autoUse() { return false; }

        @Override
        protected void onUse(PlayerInteractEvent event) {}

        // --- Event Handling ---

        /**
         * Captures the hook entity when the player launches it.
         */
        @EventHandler
        public void onLaunch(ProjectileLaunchEvent event) {
            if (!(event.getEntity() instanceof FishHook hook)) return;
            if (hook.getShooter() != ThiefKit.this.getPlayer()) return;

            this.activeHook = hook;
        }

        @EventHandler
        public void onFish(PlayerFishEvent event) {
            Player player = ThiefKit.this.getPlayer();

            if (event.getPlayer() != player) return;
            if (!this.isItem(player.getItemInHand())) return;

            // If the event is effectively "reeling in" or clicking while out
            // We check if we have a valid active hook.
            if (this.activeHook == null || !this.activeHook.isValid()) {
                return;
            }

            // Standard Cooldown Check
            if (!this.grappleCooldown.isExpired()) {
                player.sendMessage(C.warn(C.RED) + "Grappling hook is on cooldown!");
                return;
            }

            // Physics Check: Is the hook actually attached to something?
            if (!isAnchored(this.activeHook)) {
                player.sendMessage(C.warn(C.RED) + "Grappling hook must be attached to a block!");
                return;
            }

            // Execute Ability
            performGrapple(player, this.activeHook);

            // Cleanup
            this.activeHook = null;
        }

        // --- Logic & Physics ---

        private void performGrapple(Player player, FishHook hook) {
            Vector impulse = calculateImpulse(player.getLocation().toVector(), hook.getLocation().toVector());

            player.setVelocity(impulse);
            playGrappleEffects(player);

            // Logic cooldown
            this.grappleCooldown.expireIn(GRAPPLE_COOLDOWN);
            // Visual cooldown (Item bar)
            this.startCooldown();
        }

        /**
         * Consolidated check for wall or ground attachment.
         */
        private boolean isAnchored(FishHook hook) {
            org.bukkit.block.Block block = hook.getLocation().getBlock();

            // Check immediate block, block below, and surrounding blocks (NSEW)
            return isSolid(block) ||
                    isSolid(block.getRelative(0, -1, 0)) ||
                    isSolid(block.getRelative(0, 0, -1)) || // North
                    isSolid(block.getRelative(0, 0, 1)) ||  // South
                    isSolid(block.getRelative(1, 0, 0)) ||  // East
                    isSolid(block.getRelative(-1, 0, 0));   // West
        }

        private boolean isSolid(org.bukkit.block.Block block) {
            return block.getType().isSolid();
        }

        private Vector calculateImpulse(Vector origin, Vector target) {
            double deltaX = target.getX() - origin.getX();
            double deltaZ = target.getZ() - origin.getZ();
            double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            // Normalize direction
            Vector direction = new Vector(deltaX, 0, deltaZ).normalize();

            // Scale mechanics
            double horizontalMult = Math.min(1.0, horizontalDist / HORIZONTAL_DISTANCE_SCALE);
            double verticalMult = 1.0 - (horizontalMult * VERTICAL_REDUCTION_FACTOR);

            return new Vector(
                    direction.getX() * (HORIZONTAL_IMPULSE_MULTIPLIER * horizontalMult),
                    VERTICAL_IMPULSE_BASE * verticalMult,
                    direction.getZ() * (HORIZONTAL_IMPULSE_MULTIPLIER * horizontalMult)
            );
        }

        private void playGrappleEffects(Player player) {
            player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 1.0f, 1.2f);

            new CancelNextFallTask(ThiefKit.this.getPlugin(), player).register();

            ThiefKit.this.getBattle().getKitManager().find(player).ifPresent(kit ->
                    kit.addTemporaryEffect(new PotionEffect(PotionEffectType.SPEED, SPEED_DURATION_TICKS, 0))
            );
        }
    }

    @Getter
    private static class StolenItemData {
        private final Player victim;
        private final Player thief;
        private final ItemStack originalItem;
        private final int victimSlot;
        private final int thiefSlot;

        public StolenItemData(Player victim,
                              Player thief,
                              ItemStack originalItem,
                              int victimSlot,
                              int thiefSlot) {
            this.victim = victim;
            this.thief = thief;
            this.originalItem = originalItem;
            this.victimSlot = victimSlot;
            this.thiefSlot = thiefSlot;
        }
    }


}
