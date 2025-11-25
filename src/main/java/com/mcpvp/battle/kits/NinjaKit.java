package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagStartStealEvent;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.util.nms.ActionbarUtil;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EntityUtil;
import com.mcpvp.common.visibility.VisibilityManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

        this.getPlayer().setExp(1f);
        this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 1));
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
        this.sword = new KitItem(
            this,
            ItemBuilder.of(Material.GOLD_SWORD)
                .name("Ninja Sword")
                .enchant(Enchantment.DAMAGE_ALL, 5)
                .unbreakable()
                .build()
        );

        return new KitInventoryBuilder()
            .add(this.sword)
            .add(new PearlItem())
            .add(new EggItem())
            .add(new DustItem())
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void cancelFallDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() == this.getPlayer() && event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getDamage() == 5) {
            event.setDamage(4);
            // This event is cancelled because it's effectively friendly fire
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageDealtByPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() == this.getPlayer() && event.getEntity() instanceof Player) {
            this.combatCooldown.expireIn(Duration.seconds(1.5));
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        this.increaseEggMana();
        this.attemptHeal(event);

        if (this.invisible) {
            this.getGame().getTeamManager().getTeams().forEach(
                team -> team.getFlagManager().resetStealTimer(this.getPlayer())
            );
        }
    }

    @EventHandler
    public void onStartFlagSteal(FlagStartStealEvent flagStartStealEvent) {
        if (!this.isPlayer(flagStartStealEvent.getPlayer())) {
            return;
        }

        if (this.invisible) {
            flagStartStealEvent.setCancelled(true);
        } else {
            flagStartStealEvent.setRequiredStealTime(Duration.seconds(1.5));
        }
    }

    public void attemptHeal(TickEvent event) {
        boolean canHeal = this.sword.isItem(this.getPlayer().getItemInHand()) && this.getPlayer().isSneaking();
        boolean withFlagHeal = this.hasFlag() && event.isInterval(HEAL_WITH_FLAG_TIME);
        boolean withoutFlagHeal = !this.hasFlag() && event.isInterval(HEAL_WITHOUT_FLAG_TIME);

        if (canHeal && (withFlagHeal || withoutFlagHeal)) {
            this.getPlayer().setHealth(Math.min(this.getPlayer().getMaxHealth(), this.getPlayer().getHealth() + 1));
        }
    }

    // TODO ninjas need to be impacted by projectiles while invisible
    private void enforceVisibility() {
        VisibilityManager visibilityManager = this.getBattle().getVisibilityManager();

        if (this.invisible) {
            // Hide the player for any enemies who can see them
            this.getEnemies().stream()
                .filter(player -> visibilityManager.canSee(player, this.getPlayer()))
                .forEach(player -> visibilityManager.hide(player, this.getPlayer()));

            if (!this.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 0, false, false));
            }
        } else {
            // Show the player to any enemies who can't see them
            this.getEnemies().stream()
                .filter(player -> !visibilityManager.canSee(player, this.getPlayer()))
                .forEach(player -> visibilityManager.show(player, this.getPlayer()));

            if (this.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                this.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        // Ensure players are always visible to teammates
        this.getTeammates().stream()
            .filter(player -> !visibilityManager.canSee(player, this.getPlayer()))
            .forEach(player -> visibilityManager.show(player, this.getPlayer()));
    }

    public void increaseEggMana() {
        if (!this.invisible) {
            this.getPlayer().setExp(Math.min(this.getPlayer().getExp() + 1f / REFILL_TIME.ticks(), 1.0f));
        }
    }

    private void applyEggEffectNearby(Egg egg) {
        EntityUtil.getNearbyEntities(egg.getLocation(), Player.class, 3).forEach(player -> {
            this.applyEggEffect(player, false);
        });
        egg.getLocation().getWorld().createExplosion(egg.getLocation(), 0F, false);
    }

    private void applyEggEffect(Player player, boolean direct) {
        if (this.isTeammate(player)) {
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (direct ? 9 : 7) * 20, 1), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (direct ? 9 : 7) * 20, 0), true);
    }

    @Override
    public void shutdown() {
        // Restore visibility to normal
        this.invisible = false;

        if (this.getPlayer().isOnline()) {
            this.enforceVisibility();
        }

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
                this.adjustRedstoneAmount();
            }

            this.enforceDust();
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onDamageWithDust(EntityDamageByEntityEvent event) {
            if (event.getDamager() != NinjaKit.this.getPlayer()) {
                return;
            }

            if (this.isItem(NinjaKit.this.getPlayer().getItemInHand()) && this.getItem().getType() == Material.REDSTONE) {
                event.setCancelled(true);
            }
        }

        private void enforceDust() {
            boolean redstoneAllowed = !NinjaKit.this.hasFlag() && NinjaKit.this.combatCooldown.isExpired();

            if (!redstoneAllowed && this.getItem().getType() == Material.REDSTONE) {
                this.switchToGlowstone();
            }

            if (redstoneAllowed && this.getItem().getType() != Material.REDSTONE && !this.isPlaceholder()) {
                this.switchToRedstone();
            }

            NinjaKit.this.invisible = this.isItem(NinjaKit.this.getPlayer().getItemInHand()) && this.getItem().getType() == Material.REDSTONE;
            NinjaKit.this.enforceVisibility();
        }

        private void adjustRedstoneAmount() {
            if (!this.isItem(NinjaKit.this.getPlayer().getItemInHand()) || this.getItem().getType() != Material.REDSTONE) {
                return;
            }

            if (NinjaKit.this.inSpawn()) {
                return;
            }

            int amount = this.getItem().getAmount();
            if (NinjaKit.this.getPlayer().isSprinting()) {
                amount -= 6;
            } else if (NinjaKit.this.getPlayer().isSneaking()) {
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

            event.setCancelled(true);

            if (this.isPlaceholder()) {
                return;
            }

            this.setPlaceholder();
            EnderPearl ep = NinjaKit.this.getPlayer().launchProjectile(EnderPearl.class);

            NinjaKit.this.attach(new InteractiveProjectile(this.getPlugin(), ep)
                .singleEventOnly()
                .onHitPlayer(player -> this.restore())
                .onDeath(this::restore)
            );

            NinjaKit.this.attach(ep);
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

            event.setCancelled(true);

            if (this.isPlaceholder()) {
                return;
            }

            if (NinjaKit.this.getPlayer().getExp() < EGG_MANA_COST) {
                ActionbarUtil.send(NinjaKit.this.getPlayer(), C.cmdFail() + "Not enough mana!");
                return;
            }

            this.decrement(true);
            Egg e = NinjaKit.this.getPlayer().launchProjectile(Egg.class);

            NinjaKit.this.attach(new InteractiveProjectile(this.getPlugin(), e)
                .singleEventOnly()
                .onDamageEvent(damageEvent -> {
                    if (damageEvent.getEntity() instanceof Player hit) {
                        e.getLocation().getWorld().createExplosion(e.getLocation(), 0F, false);
                        NinjaKit.this.applyEggEffect(hit, true);
                    }
                })
                .onHitEvent(hitEvent -> NinjaKit.this.applyEggEffectNearby(e))
            );

            NinjaKit.this.getPlayer().setExp(Math.max(NinjaKit.this.getPlayer().getExp() - EGG_MANA_COST, 0f));
        }

    }

}
