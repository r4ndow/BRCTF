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
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EntityUtil;
import com.mcpvp.common.util.nms.ActionbarUtil;
import com.mcpvp.common.visibility.VisibilityManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class Ninja2Kit extends BattleKit {

    private static final Duration DUST_CHECK_INTERVAL = Duration.milliseconds(1000);
    private static final Duration REFILL_TIME = Duration.secs(3);
    private static final Duration HEAL_WITHOUT_FLAG_TIME = Duration.seconds(1);
    private static final Duration HEAL_WITH_FLAG_TIME = Duration.seconds(2);
    private static final int EGG_USES = 2;
    private static final float EGG_MANA_COST = 1.0f / EGG_USES;

    private boolean ignoreNextPearlFallDamage;

    private KitItem sword;
    private boolean invisible;
    private PearlItem pearlItem;
    private boolean hadFlag;
    //private final Expiration combatCooldown = new Expiration();

    public Ninja2Kit(BattlePlugin plugin, Player player) {
        super(plugin, player);

        this.getPlayer().setExp(1f);
        this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0));
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
                .enchant(Enchantment.DAMAGE_ALL, 4)
                .unbreakable()
                .build()
        );

        this.pearlItem = new PearlItem();

        return new KitInventoryBuilder()
            .add(this.sword)
            .addFood(2)
            .add(this.pearlItem)
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnderPearlDamage(EntityDamageEvent event) {
        if (event.getEntity() != this.getPlayer()) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        if (this.ignoreNextPearlFallDamage) {
            this.ignoreNextPearlFallDamage = false;
            event.setCancelled(true);
        }
    }


//    @EventHandler(ignoreCancelled = true)
//    public void onDamageDealtByPlayer(EntityDamageByEntityEvent event) {
//        if (event.getDamager() == this.getPlayer() && event.getEntity() instanceof Player) {
//            this.combatCooldown.expireIn(Duration.seconds(1.5));
//        }
//    }

    @EventHandler
    public void onTick(TickEvent event) {
        this.increaseEggMana();
        this.attemptHeal(event);

        boolean hasFlagNow = this.hasFlag();
        if (hasFlagNow != this.hadFlag && this.pearlItem != null) {
            if (hasFlagNow) {
                this.pearlItem.setPlaceholder();   // visually block the pearl
            } else {
                this.pearlItem.restore();          // re‑enable when flag is dropped/returned
            }
        }
        this.hadFlag = hasFlagNow;

//        if (this.invisible) {
//            this.getGame().getTeamManager().getTeams().forEach(
//                team -> team.getFlagManager().resetStealTimer(this.getPlayer())
//            );
//        }
    }

    @EventHandler
    public void onStartFlagSteal(FlagStartStealEvent flagStartStealEvent) {
        if (!this.isPlayer(flagStartStealEvent.getPlayer())) {
            return;
        }

//        if (this.invisible) {
//            flagStartStealEvent.setCancelled(true);
//        } else {
//            flagStartStealEvent.setRequiredStealTime(Duration.seconds(1.5));
//        }

        flagStartStealEvent.setRequiredStealTime(Duration.seconds(1.5));
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

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.CONFUSION,
                Duration.seconds(6).ticks(),
                1,
                true
        ));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW,
                Duration.seconds(4).ticks(),
                0,
                true
        ));
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
                Ninja2Kit.this,
                ItemBuilder.of(Material.REDSTONE)
                    .name("Invisibility Dust")
                    .amount(32)
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
            if (event.getDamager() != Ninja2Kit.this.getPlayer()) {
                return;
            }

            if (this.isItem(Ninja2Kit.this.getPlayer().getItemInHand()) && this.getItem().getType() == Material.REDSTONE) {
                event.setCancelled(true);
            }
        }

        private void enforceDust() {
            boolean wasInvisible = Ninja2Kit.this.invisible;
            Ninja2Kit.this.invisible = this.isItem(Ninja2Kit.this.getPlayer().getItemInHand())
                    && this.getItem().getType() == Material.REDSTONE;

            if (Ninja2Kit.this.invisible && !wasInvisible) {
                Ninja2Kit.this.getPlayer().playSound(
                        Ninja2Kit.this.getPlayer().getLocation(),
                        Sound.STEP_SNOW,
                        1.0f,
                        0.1f
                );

                Ninja2Kit.this.getPlayer().sendMessage(
                        C.warn(C.AQUA) + "You vanish from sight..."
                );
            } else if (!Ninja2Kit.this.invisible && wasInvisible) {
                Ninja2Kit.this.getPlayer().sendMessage(
                        C.warn(C.PURPLE) + "You are visible!"
                );
            }

            Ninja2Kit.this.enforceVisibility();
        }


        private void adjustRedstoneAmount() {
            if (!this.isItem(Ninja2Kit.this.getPlayer().getItemInHand())
                    || this.getItem().getType() != Material.REDSTONE) {
                return;
            }

            if (Ninja2Kit.this.inSpawn()) {
                return;
            }

            int amount = this.getItem().getAmount();

            // Unified drain rate: same as the old sneaking drain rate
            amount -= 1;

            if (amount > 0) {
                int finalAmount = amount;
                this.modify(builder -> builder.amount(finalAmount));
            } else {
                // When fully drained, keep 1 as placeholder and mark it
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
                Ninja2Kit.this,
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

            if (Ninja2Kit.this.hasFlag()) {
                Ninja2Kit.this.getPlayer().sendMessage(
                        C.cmdFail() + "You can’t throw pearls with the flag!"
                );
                Ninja2Kit.this.getPlayer().playSound(
                        Ninja2Kit.this.getPlayer().getLocation(),
                        Sound.ENDERMAN_TELEPORT,
                        1.0f,
                        0.6f
                );
                return;
            }

            this.setPlaceholder();
            EnderPearl ep = Ninja2Kit.this.getPlayer().launchProjectile(EnderPearl.class);

            Ninja2Kit.this.attach(new InteractiveProjectile(this.getPlugin(), ep)
                    .singleEventOnly()
                    .onHitPlayer(player -> this.restore())
                    .onDeath(this::restore)
            );

            Ninja2Kit.this.attach(ep);
        }


    }

    public class EggItem extends KitItem {

        public EggItem() {
            super(
                Ninja2Kit.this,
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

            if (Ninja2Kit.this.getPlayer().getExp() < EGG_MANA_COST) {
                ActionbarUtil.send(Ninja2Kit.this.getPlayer(), C.cmdFail() + "Not enough mana!");
                return;
            }

            this.decrement(true);
            Egg e = Ninja2Kit.this.getPlayer().launchProjectile(Egg.class);

            Ninja2Kit.this.attach(new InteractiveProjectile(this.getPlugin(), e)
                .singleEventOnly()
                .onDamageEvent(damageEvent -> {
                    if (damageEvent.getEntity() instanceof Player hit) {
                        e.getLocation().getWorld().createExplosion(e.getLocation(), 0F, false);
                        Ninja2Kit.this.applyEggEffect(hit, true);
                    }
                })
                .onHitEvent(hitEvent -> Ninja2Kit.this.applyEggEffectNearby(e))
            );

            Ninja2Kit.this.getPlayer().setExp(Math.max(Ninja2Kit.this.getPlayer().getExp() - EGG_MANA_COST, 0f));
        }

    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().equals(this.getPlayer())
                || event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        if (this.hasFlag()) {
            event.setCancelled(true);
            Ninja2Kit.this.getPlayer().sendMessage(
                    C.cmdFail() + "You can’t throw pearls with the flag!"
            );
            Ninja2Kit.this.getPlayer().playSound(
                    Ninja2Kit.this.getPlayer().getLocation(),
                    Sound.ENDERMAN_TELEPORT,
                    1.0f,
                    0.6f
            );
            return;
        }

        this.ignoreNextPearlFallDamage = true;

        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.3f);
        }, 1L);
    }



}
