package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EntityUtil;
import com.mcpvp.common.util.chat.C;
import com.mcpvp.common.util.nms.ActionbarUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class NinjaKit extends BattleKit {

    private static final Duration DUST_CHECK_INTERVAL = Duration.milliseconds(500);
    private static final Duration REFILL_TIME = Duration.secs(3);
    private static final Duration HEAL_WITHOUT_FLAG_TIME = Duration.seconds(1);
    private static final Duration HEAL_WITH_FLAG_TIME = Duration.seconds(2);
    private static final int EGG_USES = 2;
    private static final float EGG_MANA_COST = 1.0f / EGG_USES;

    private KitItem sword;
    private boolean invisible;
    private final Expiration combatCooldown = new Expiration();

    public NinjaKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
        getPlayer().setExp(1f);
    }

    @Override
    public String getName() {
        return "Ninja";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[0];
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        sword = new KitItem(
                this,
                ItemBuilder.of(Material.GOLD_SWORD)
                        .name("Ninja Sword")
                        .enchant(Enchantment.DAMAGE_ALL, 5)
                        .unbreakable()
                        .build()
        );

        return new KitInventoryBuilder()
                .add(sword)
                .add(new PearlItem())
                .add(new EggItem())
                .add(new DustItem())
                .build();
    }

    @EventHandler
    public void cancelFallDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() == getPlayer() && event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getDamage() == 5) {
            event.setDamage(4);
            // This event is cancelled because it's effectively friendly fire
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageDealtByPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() == getPlayer() && event.getEntity() instanceof Player) {
            combatCooldown.expireIn(Duration.seconds(1.5));
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        increaseEggMana();
        attemptHeal(event);
    }

    public void attemptHeal(TickEvent event) {
        boolean canHeal = sword.isItem(getPlayer().getItemInHand()) && getPlayer().isSneaking() && getPlayer().isBlocking();
        boolean withFlagHeal = hasFlag() && event.isInterval(HEAL_WITH_FLAG_TIME);
        boolean withoutFlagHeal = !hasFlag() && event.isInterval(HEAL_WITHOUT_FLAG_TIME);

        if (canHeal && (withFlagHeal || withoutFlagHeal)) {
            getPlayer().setHealth(Math.min(getPlayer().getMaxHealth(), getPlayer().getHealth() + 1));
        }
    }

    private void enforceVisibility() {
        if (invisible) {
            // Hide the player for any enemies who can see them
            getEnemies().stream()
                    .filter(player -> player.canSee(getPlayer()))
                    .forEach(player -> player.hidePlayer(getPlayer()));

            if (!getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 0, true, false));
            }
        } else {
            // Show the player to any enemies who can't see them
            getEnemies().stream()
                    .filter(player -> !player.canSee(getPlayer()))
                    .forEach(player -> player.showPlayer(getPlayer()));

            if (getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        // Ensure players are always visible to teammates
        getTeammates().stream()
                .filter(player -> !player.canSee(getPlayer()))
                .forEach(player -> player.showPlayer(getPlayer()));
    }

    public void increaseEggMana() {
        if (!invisible) {
            getPlayer().setExp(Math.min(getPlayer().getExp() + 1f / REFILL_TIME.ticks(), 1.0f));
        }
    }

    private void applyEggEffectNearby(Egg egg) {
        EntityUtil.getNearbyEntities(egg.getLocation(), Player.class, 3).forEach(player -> {
            this.applyEggEffect(player, false);
        });
        egg.getLocation().getWorld().createExplosion(egg.getLocation(), 0F, false);
    }

    private void applyEggEffect(Player player, boolean direct) {
        if (getTeammates().contains(player)) {
            return;
        }

        // Existing potion effects need to be removed to reset the time
        player.removePotionEffect(PotionEffectType.CONFUSION);
        player.removePotionEffect(PotionEffectType.SLOW);
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (direct ? 9 : 7) * 20, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (direct ? 9 : 7) * 20, 0));
    }

    @Override
    public void shutdown() {
        // Restore visibility to normal
        invisible = false;
        enforceVisibility();

        super.shutdown();
    }

    public class DustItem extends KitItem {

        public DustItem() {
            super(
                    NinjaKit.this,
                    ItemBuilder.of(Material.REDSTONE)
                            .name("Invisibility Dust")
                            .amount(64)
                            .build(),
                    true
            );
        }

        @EventHandler
        public void onTick(TickEvent event) {
            if (event.isInterval(DUST_CHECK_INTERVAL)) {
                adjustRedstoneAmount();
            }

            enforceDust();
        }

        private void enforceDust() {
            boolean redstoneAllowed = !hasFlag() && combatCooldown.isExpired();

            if (!redstoneAllowed && this.getItem().getType() == Material.REDSTONE) {
                switchToGlowstone();
            }

            if (redstoneAllowed && this.getItem().getType() != Material.REDSTONE && !this.isPlaceholder()) {
                switchToRedstone();
            }

            invisible = this.isItem(getPlayer().getItemInHand()) && this.getItem().getType() == Material.REDSTONE;
            enforceVisibility();
        }

        private void adjustRedstoneAmount() {
            if (!this.isItem(getPlayer().getItemInHand()) || this.getItem().getType() != Material.REDSTONE) {
                return;
            }

            if (inSpawn()) {
                return;
            }

            int amount = this.getItem().getAmount();
            if (getPlayer().isSprinting()) {
                amount -= 6;
            } else if (getPlayer().isSneaking()) {
                amount -= 1;
            } else {
                amount -= 2;
            }

            if (amount > 0) {
                int finalAmount = amount;
                this.modify(builder -> builder.amount(finalAmount));
            } else {
                this.modify(builder -> builder.amount(1));
                this.setPlaceholder();
            }
        }

        private void switchToGlowstone() {
            this.modify(ib -> ib.type(Material.GLOWSTONE_DUST));
        }

        private void switchToRedstone() {
            this.modify(ib -> ib.type(Material.REDSTONE));
        }

    }

    public class PearlItem extends KitItem {

        public PearlItem() {
            super(
                    NinjaKit.this,
                    ItemBuilder.of(Material.ENDER_PEARL)
                            .name("Teleportation Sphere")
                            .build()
            );
        }

        @EventHandler
        public void throwPearl(PlayerInteractEvent event) {
            if (!EventUtil.isRightClick(event) || !this.isItem(event.getPlayer().getItemInHand())) {
                return;
            }

            if (this.isPlaceholder()) {
                return;
            }

            event.setCancelled(true);
            this.setPlaceholder();
            EnderPearl ep = getPlayer().launchProjectile(EnderPearl.class);

            getBattle().getProjectileManager().register(ep)
                    .onHit(pl -> this.restore())
                    .onMiss(this::restore);
        }

    }

    public class EggItem extends KitItem {

        public EggItem() {
            super(
                    NinjaKit.this,
                    ItemBuilder.of(Material.EGG)
                            .name("Flash Bomb")
                            .amount(10)
                            .build()
            );
        }

        @EventHandler
        public void throwEgg(PlayerInteractEvent event) {
            if (!EventUtil.isRightClick(event) || !this.isItem(event.getPlayer().getItemInHand())) {
                return;
            }

            if (this.isPlaceholder()) {
                return;
            }

            if (getPlayer().getExp() < EGG_MANA_COST) {
                ActionbarUtil.send(getPlayer(), C.cmdFail() + "Not enough mana!");
                return;
            }

            event.setCancelled(true);
            this.decrement(true);
            Egg e = getPlayer().launchProjectile(Egg.class);

            getBattle().getProjectileManager().register(e)
                    .onHitEvent(hitEvent -> {
                        if (hitEvent.getEntity() instanceof Player hit) {
                            e.getLocation().getWorld().createExplosion(e.getLocation(), 0F, false);
                            applyEggEffect(hit, true);
                        }
                    })
                    .onCollideBlock(hitEvent -> applyEggEffectNearby(e));

            getPlayer().setExp(Math.max(getPlayer().getExp() - EGG_MANA_COST, 0f));
        }

    }

}
