package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.item.CooldownItem;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.time.Duration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.EntityEffect;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.kit.Kit;


import org.bukkit.Location;
import org.bukkit.Effect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;
import java.util.Map;

public class VampireKit extends BattleKit {

    public VampireKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Vampire";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
                ItemBuilder.of(Material.LEATHER_BOOTS)
                        .color(Color.fromRGB(0x0), false)
                        .build(),
                ItemBuilder.of(Material.LEATHER_LEGGINGS)
                        .color(Color.fromRGB(0x0), false)
                        .build(),
                ItemBuilder.of(Material.LEATHER_CHESTPLATE)
                        .color(Color.fromRGB(0x360F0F), false)
                        .build(),
                null
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        return new KitInventoryBuilder()
                .add(ItemBuilder.of(Material.GOLD_SWORD)
                        .name("Vampire Sword")
                        .unbreakable())
                .addFood(4)
                .add(new LifeDrain())
                .add(new NightfallVial())
                .addCompass(8)
                .build();
    }

    class LifeDrain extends CooldownItem {
        private static final Duration COOLDOWN = Duration.seconds(15);
        private static final int TRUE_DAMAGE = 10;
        private static final Duration BUFF_DURATION = Duration.seconds(10);

        public LifeDrain() {
            super(
                    VampireKit.this,
                    ItemBuilder.of(Material.GHAST_TEAR)
                            .name("Life Drain")
                            .build(),
                    COOLDOWN
            );
        }

        @Override
        protected boolean autoUse() {
            return false;
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
            event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void onHit(EntityDamageByEntityEvent event) {
            if (this.isPlaceholder()) {
                return;
            }

            if (!(event.getDamager() instanceof Player)) {
                return;
            }

            if (!event.getDamager().equals(VampireKit.this.getPlayer())) {
                return;
            }

            if (!(event.getEntity() instanceof Player victim)) {
                return;
            }

            if (VampireKit.this.isTeammate(victim)) {
                return;
            }

            if (!this.isItem(VampireKit.this.getPlayer().getItemInHand())) {
                return;
            }

            double newHealth = victim.getHealth() - TRUE_DAMAGE;

            if (newHealth <= 0) {
                event.setDamage(1000);
            } else {
                event.setCancelled(true);
                victim.setHealth(newHealth);
            }

            victim.setLastDamageCause(event);

            victim.playEffect(EntityEffect.HURT);

            VampireKit.this.getPlayer().playSound(
                    VampireKit.this.getPlayer().getLocation(),
                    Sound.DRINK,
                    1.0f,
                    2.0f
            );

            victim.playSound(
                    victim.getLocation(),
                    Sound.DRINK,
                    1.0f,
                    2.0f
            );

            VampireKit.this.addTemporaryEffect(new PotionEffect(
                    PotionEffectType.DAMAGE_RESISTANCE,
                    BUFF_DURATION.ticks(),
                    1
            ));
            VampireKit.this.addTemporaryEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    BUFF_DURATION.ticks(),
                    2
            ));
            VampireKit.this.addTemporaryEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    BUFF_DURATION.ticks(),
                    1
            ));

            BattleTeam attackerTeam = VampireKit.this.getGame()
                    .getTeamManager()
                    .getTeam(VampireKit.this.getPlayer());
            BattleTeam victimTeam = VampireKit.this.getGame()
                    .getTeamManager()
                    .getTeam(victim);

            Kit attackerKit = VampireKit.this.getBattle()
                    .getKitManager()
                    .get(VampireKit.this.getPlayer());
            Kit victimKit = VampireKit.this.getBattle()
                    .getKitManager()
                    .get(victim);

            if (attackerTeam != null) {
                String attackerKitSuffix = attackerKit != null
                        ? C.GRAY + " [" + attackerKit.getName().toLowerCase(Locale.ENGLISH) + "]"
                        : "";

                victim.sendMessage(
                        C.RED + "!! " + C.GRAY + "Your life was drained by "
                                + attackerTeam.getColor() + VampireKit.this.getPlayer().getName()
                                + attackerKitSuffix
                );
            }

            if (victimTeam != null) {
                String victimKitSuffix = victimKit != null
                        ? C.GRAY + " [" + victimKit.getName().toLowerCase(Locale.ENGLISH) + "]"
                        : "";

                VampireKit.this.getPlayer().sendMessage(
                        C.AQUA + "* " + C.GRAY + "You drained the life of "
                                + victimTeam.getColor() + victim.getName()
                                + victimKitSuffix
                );
            }

            this.decrement(true);
            this.startCooldown();
        }
    }

    class NightfallVial extends CooldownItem {
        private static final Duration COOLDOWN = Duration.seconds(20);
        private static final int DURATION_SECONDS = 10;
        private static final int BLINDNESS_DURATION = 50;
        private static final int REGENERATION_DURATION = 60;
        private static final double CUBE_SIZE = 2.5;
        private static final double PARTICLE_STEP = 0.5;
        private static final int PARTICLE_INTERVAL_TICKS = 3;
        private static final int EFFECT_INTERVAL_TICKS = 20;

        public NightfallVial() {
            super(
                    VampireKit.this,
                    ItemBuilder.potion()
                            .effect(PotionEffectType.WEAKNESS)
                            .splash()
                            .name("Nightfall Vial")
                            .build(),
                    COOLDOWN
            );
        }

        @Override
        protected boolean shouldTrigger(PlayerInteractEvent event) {
            return event.getAction().toString().contains("RIGHT_CLICK");
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
            event.setCancelled(true);

            ThrownPotion potion = VampireKit.this.getPlayer().launchProjectile(ThrownPotion.class);
            potion.setVelocity(potion.getVelocity().multiply(1.5));

            ItemStack potionItem = ItemBuilder.potion()
                    .effect(PotionEffectType.WEAKNESS)
                    .splash()
                    .build();

            potion.setItem(potionItem);
            VampireKit.this.attach(potion);

            this.decrement(true);
            this.startCooldown();

            VampireKit.this.attach(new InteractiveProjectile(this.getPlugin(), potion)
                    .singleEventOnly()
                    .onHitEvent(splashEvent -> {
                        // Cancel weakness effect from the potion
                        if (splashEvent instanceof org.bukkit.event.entity.PotionSplashEvent) {
                            org.bukkit.event.entity.PotionSplashEvent potionEvent =
                                    (org.bukkit.event.entity.PotionSplashEvent) splashEvent;
                            for (org.bukkit.entity.LivingEntity entity : potionEvent.getAffectedEntities()) {
                                potionEvent.setIntensity(entity, 0);
                            }
                        }

                        final Location center = potion.getLocation();

                        new BukkitRunnable() {
                            int ticks = 0;

                            @Override
                            public void run() {
                                if (ticks >= DURATION_SECONDS * 20) {
                                    this.cancel();
                                    return;
                                }

                                if (ticks % PARTICLE_INTERVAL_TICKS == 0) {
                                    renderCubeWalls(center);
                                }

                                if (ticks % EFFECT_INTERVAL_TICKS == 0) {
                                    applyEffectsToNearbyPlayers(center);
                                }

                                ticks++;
                            }
                        }.runTaskTimer(VampireKit.this.getPlugin(), 0L, 1L);
                    })
            );
        }

        private void renderCubeWalls(Location center) {
            for (double y = -CUBE_SIZE; y <= CUBE_SIZE; y += PARTICLE_STEP) {
                for (double i = -CUBE_SIZE; i <= CUBE_SIZE; i += PARTICLE_STEP) {
                    // North wall (Z = -size)
                    center.getWorld().playEffect(center.clone().add(i, y, -CUBE_SIZE), Effect.VOID_FOG, 0);
                    // South wall (Z = +size)
                    center.getWorld().playEffect(center.clone().add(i, y, CUBE_SIZE), Effect.VOID_FOG, 0);
                    // West wall (X = -size)
                    center.getWorld().playEffect(center.clone().add(-CUBE_SIZE, y, i), Effect.VOID_FOG, 0);
                    // East wall (X = +size)
                    center.getWorld().playEffect(center.clone().add(CUBE_SIZE, y, i), Effect.VOID_FOG, 0);
                }
            }
        }

        private void applyEffectsToNearbyPlayers(Location center) {
            for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, CUBE_SIZE, CUBE_SIZE, CUBE_SIZE)) {
                if (!(entity instanceof Player player)) {
                    continue;
                }

                if (!isInsideCube(player.getLocation(), center)) {
                    continue;
                }

                if (player.equals(VampireKit.this.getPlayer()) || VampireKit.this.isTeammate(player)) {
                    VampireKit.this.getBattle().getKitManager().find(player).ifPresent(kit -> {
                        kit.addTemporaryEffect(new PotionEffect(
                                PotionEffectType.REGENERATION,
                                REGENERATION_DURATION,
                                2
                        ));
                    });
                } else {
                    VampireKit.this.getBattle().getKitManager().find(player).ifPresent(kit -> {
                        kit.addTemporaryEffect(new PotionEffect(
                                PotionEffectType.BLINDNESS,
                                BLINDNESS_DURATION,
                                0
                        ));
                    });
                }
            }
        }

        private boolean isInsideCube(Location playerLoc, Location center) {
            return Math.abs(playerLoc.getX() - center.getX()) <= CUBE_SIZE
                    && Math.abs(playerLoc.getY() - center.getY()) <= CUBE_SIZE
                    && Math.abs(playerLoc.getZ() - center.getZ()) <= CUBE_SIZE;
        }
    }
}
