package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.item.CooldownItem;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.util.nms.ActionbarUtil;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureBuilder;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EffectUtil;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.mcpvp.battle.match.BattleMatchStructureRestrictions.NEAR_PLAYER;
import static com.mcpvp.battle.match.BattleMatchStructureRestrictions.NEAR_SPAWN;

public class EngineerKit extends BattleKit {

    private static final Map<Player, Expiration> HEAL_COOLDOWNS = new HashMap<>();
    private static final Duration HEALING_COOLDOWN = Duration.seconds(5);

    public EngineerKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Engineer";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            ItemBuilder.of(Material.IRON_BOOTS).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).build(),
            ItemBuilder.of(Material.LEATHER_LEGGINGS).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2).build(),
            ItemBuilder.of(Material.LEATHER_CHESTPLATE).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2).build(),
            ItemBuilder.of(Material.IRON_HELMET).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).build()
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        return new KitInventoryBuilder()
            .add(ItemBuilder.of(Material.STONE_SWORD)
                .name("Engineer Sword")
                .enchant(Enchantment.DAMAGE_ALL, 1)
                .unbreakable())
            .addFood(4)
            .add(new GrenadeLauncher())
            .add(new HealingAuraPlacer())
            .add(new SpeedBeaconPlacer())
            .addCompass(8)
            .build();
    }

    class GrenadeLauncher extends CooldownItem {

        private static final Duration LAUNCHER_COOLDOWN = Duration.seconds(5.5);
        private static final Duration TNT_FUSE_TIME = Duration.seconds(3);
        private static final int TNT_MAX_DISTANCE = 5;
        private static final int TNT_MAX_DAMAGE = 2 * 4;
        private static final int EXPLOSION_RADIUS = 3;

        private TNTPrimed lastGrenade;

        public GrenadeLauncher() {
            super(
                EngineerKit.this,
                ItemBuilder.of(Material.GOLD_SPADE)
                    .name("Grenade Launcher")
                    .enchant(Enchantment.DAMAGE_ALL, 1)
                    .unbreakable()
                    .build(),
                LAUNCHER_COOLDOWN
            );
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
            this.launchGrenade(event);
        }

        @Override
        protected void onFailedUse() {
            // Controlled explosion of the previously launched grenade
            if (this.lastGrenade != null && !this.lastGrenade.isDead()) {
                this.lastGrenade.setFuseTicks(0);
            }
        }

        public void launchGrenade(PlayerInteractEvent event) {
            event.setCancelled(true);

            // Launch the grenade
            Location position = EngineerKit.this.getPlayer().getLocation().clone().add(0, 1, 0);
            Vector velocity = position.getDirection().normalize().multiply(1.3);
            TNTPrimed tnt = (TNTPrimed) EngineerKit.this.getPlayer().getWorld().spawnEntity(position, EntityType.PRIMED_TNT);
            tnt.setFuseTicks(TNT_FUSE_TIME.ticks());
            tnt.setVelocity(velocity);
            tnt.setMetadata("shooter", new FixedMetadataValue(this.getPlugin(), EngineerKit.this.getPlayer()));
            this.lastGrenade = tnt;
            EngineerKit.this.attach(tnt);

            // Item decorating
            this.modify(item -> item.type(Material.WOOD_SPADE));
            EasyTask.of(this::restore).runTaskLater(this.getPlugin(), LAUNCHER_COOLDOWN.ticks());

            // Queue the TNT for exploding
            EngineerKit.this.attach(new GrenadeExplosionTask(tnt, () -> {
                if (tnt.getFuseTicks() > 10) {
                    tnt.setFuseTicks(10);
                }
            }).runTaskTimer(this.getPlugin(), 0, 1));

            // Visual effect
            EffectUtil.colorTrail(tnt, EngineerKit.this.getTeam().getColor().getColor()).runTaskTimer(this.getPlugin(), 0, 1);
        }

        @EventHandler(ignoreCancelled = true)
        public void onDamagedByGrenade(EntityDamageByEntityEvent event) {
            if (event.getDamager() != this.lastGrenade) {
                return;
            }

            // The shooter of the grenade might have been changed by an Elf reflection
            Player shooter = (Player) this.lastGrenade.getMetadata("shooter").get(0).value();

            if (!(event.getEntity() instanceof Player hit) || EngineerKit.this.getGame().getTeamManager().isSameTeam(shooter, hit)) {
                event.setCancelled(true);
                return;
            }

            if (hit.getLocation().distance(event.getDamager().getLocation()) > TNT_MAX_DISTANCE) {
                event.setCancelled(true);
                return;
            }

            EventUtil.setDamage(event, this.calculateGrenadeDamage(event.getDamager().getLocation(), hit));
        }

        @EventHandler
        public void onGrenadeExplode(EntityExplodeEvent event) {
            if (event.getEntity() != this.lastGrenade) {
                return;
            }

            // Cancel the explosion damage
            event.blockList().clear();
            event.setYield(0f);

            // Remove nearby medic webs
            EngineerKit.this.getBattle().getStructureManager().getStructures().stream()
                .filter(structure -> structure instanceof MedicKit.MedicWeb)
                .filter(structure -> EngineerKit.this.isEnemy(structure.getOwner()))
                .filter(structure -> structure.distance(event.getEntity().getLocation()) <= EXPLOSION_RADIUS)
                .forEach(Structure::remove);
        }

        private double calculateGrenadeDamage(Location explosion, Player target) {
            double distance = explosion.distance(target.getLocation());
            double impact = (TNT_MAX_DISTANCE - distance) / TNT_MAX_DISTANCE;
            return Math.max(Math.ceil(TNT_MAX_DAMAGE * impact), 0);
        }

    }

    @RequiredArgsConstructor
    static class GrenadeExplosionTask extends BukkitRunnable {

        private final TNTPrimed tnt;
        private final Runnable afterLanding;

        @Override
        public void run() {
            if (this.tnt.isDead()) {
                this.cancel();
                return;
            }

            if (this.tnt.isOnGround() || this.tnt.getLocation().getBlock().getType() == Material.WEB) {
                this.afterLanding.run();
                this.cancel();
            }
        }

    }

    class SpeedBeaconPlacer extends CooldownItem {

        private static final Duration BEACON_COOLDOWN = Duration.seconds(10);

        public SpeedBeaconPlacer() {
            super(
                EngineerKit.this,
                ItemBuilder.of(Material.IRON_AXE).name("Speed Beacon").build(),
                BEACON_COOLDOWN
            );
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
            event.setCancelled(true);
            this.setPlaceholder();
            Snowball snowball = EngineerKit.this.getPlayer().launchProjectile(Snowball.class);

            EngineerKit.this.attach(new InteractiveProjectile(EngineerKit.this.getPlugin(), snowball)
                .singleEventOnly()
                .onHitEvent(projectileHitEvent -> {
                    Block block = projectileHitEvent.getEntity().getLocation().getBlock().getRelative(BlockFace.DOWN);
                    SpeedBeacon speedBeacon = new SpeedBeacon();
                    if (!EngineerKit.this.placeStructure(speedBeacon, block)) {
                        this.restore();
                    }
                })
                .onDamageEvent(damageEvent -> {
                    damageEvent.setCancelled(true);
                    this.restore();
                })
                .onDeath(this::restore));
        }

    }

    public class SpeedBeacon extends Structure {

        private static final double AURA_RADIUS = 5.0;
        private static final Duration ANIMATION_INTERVAL = Duration.seconds(0.5);
        private static final Duration DEPLOY_TIME = Duration.seconds(10);
        private static final int SPEED_TIER = 2;
        private static final Duration SPEED_TIME = Duration.seconds(4.5);
        private static final double AURA_HEIGHT = 10.0;
        private static final PotionEffect SPEED_EFFECT = new PotionEffect(
            PotionEffectType.SPEED, SPEED_TIME.ticks(), SPEED_TIER - 1
        );

        private ArmorStand armorStand;
        private Item glassPane;

        public SpeedBeacon() {
            super(EngineerKit.this.getBattle().getStructureManager(), EngineerKit.this.getPlayer());
            this.removeAfter(DEPLOY_TIME);
        }

        @Override
        protected void build(Block center, StructureBuilder builder) {
            builder.ignoreRestrictions(NEAR_PLAYER, NEAR_SPAWN);
            builder.setBlock(center, Material.BEACON);

            Stream.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
                .map(center::getRelative)
                .forEach(block -> builder.setBlock(block, Material.DOUBLE_STEP));

            Location itemLocation = center.getLocation().add(0.5, 4, 0.5);

            this.glassPane = this.getOwner().getWorld().dropItem(itemLocation,
                ItemBuilder.of(Material.SUGAR)
                    .tag("prevent_merge", UUID.randomUUID().toString())
                    .build()
            );
            this.glassPane.setPickupDelay(99999);

            this.armorStand = center.getWorld().spawn(itemLocation, ArmorStand.class);
            this.armorStand.setGravity(false);
            this.armorStand.setVisible(false);
            this.armorStand.setMarker(true);
            this.armorStand.setPassenger(this.glassPane);

            this.attach(this.armorStand);
            this.attach(this.glassPane);
        }

        @EventHandler
        public void onTick(TickEvent event) {
            this.giveSpeedEffect();

            if (event.isInterval(ANIMATION_INTERVAL)) {
                this.animateParticleRing();
            }
        }

        private void giveSpeedEffect() {
            EngineerKit.this.getTeammates().stream()
                .filter(this::isCloseEnough)
                .forEach(teammate -> {
                    EngineerKit.this.getBattle().getKitManager().find(teammate).ifPresent(kit -> {
                        kit.addTemporaryEffect(SPEED_EFFECT);
                    });
                });
        }

        private boolean isCloseEnough(Player player) {
            return player.getLocation().toVector().distance(
                this.getCenter().getLocation().toVector().setY(player.getLocation().getY())
            ) <= AURA_RADIUS && Math.abs(player.getLocation().getY() - this.getCenter().getLocation().getY() - 1) <= AURA_HEIGHT;
        }

        private void animateParticleRing() {
            List<Location> locations = EffectUtil.getParticleRing(this.getCenter().getLocation(), 20, AURA_RADIUS);
            for (int i = 0; i < locations.size(); i++) {
                // Alternate white and team colored particles
                if (i % 2 == 0) {
                    ParticlePacket.colored(EngineerKit.this.getTeam().getColor().getColor()).at(locations.get(i)).showFar().send();
                } else {
                    ParticlePacket.of(EnumParticle.FIREWORKS_SPARK).at(locations.get(i)).showFar().send();
                }
            }
        }

        @EventHandler
        public void onItemPickup(PlayerPickupItemEvent event) {
            if (event.getItem().equals(this.glassPane)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onItemMerge(ItemMergeEvent event) {
            if (event.getEntity() == this.glassPane) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageEvent event) {
            if (event.getEntity() == this.armorStand || event.getEntity() == this.glassPane) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            if (event.getEntity() == this.armorStand || event.getEntity() == this.glassPane) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
            if (event.getRightClicked() == this.armorStand) {
                event.setCancelled(true);
            }
        }

        @Override
        public Plugin getPlugin() {
            return EngineerKit.this.getPlugin();
        }

    }

    class HealingAuraPlacer extends CooldownItem {

        private static final Duration ITEM_COOLDOWN = Duration.seconds(8);
        private static final Duration HEALING_DURATION = Duration.seconds(5);
        private static final int HEAL_RADIUS = 4;
        private static final int HEALING_REGEN_TIER = 3;

        public HealingAuraPlacer() {
            super(
                EngineerKit.this,
                ItemBuilder.of(Material.CAKE).name("Healing Aura").build(),
                ITEM_COOLDOWN
            );
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
            event.setCancelled(true);
            this.setPlaceholder();
            this.doHealPulse();
            this.animateRing();
        }

        protected void doHealPulse() {
            EngineerKit.this.getTeammates().stream()
                .filter(teammate -> teammate.getLocation().distance(EngineerKit.this.getPlayer().getLocation()) <= HEAL_RADIUS)
                .filter(teammate -> {
                    if (HEAL_COOLDOWNS.containsKey(teammate) && !HEAL_COOLDOWNS.get(teammate).isExpired()) {
                        String duration = HEAL_COOLDOWNS.get(teammate).getRemaining().formatText();
                        ActionbarUtil.send(teammate, "%sYou can't be healed for %s second(s)".formatted(C.GRAY, C.hl(duration)));
                        return false;
                    }

                    return true;
                })
                .forEach(teammate -> {
                    PotionEffect effect = new PotionEffect(
                        PotionEffectType.REGENERATION, HEALING_DURATION.ticks(), HEALING_REGEN_TIER - 1
                    );
                    EngineerKit.this.getBattle().getKitManager().find(teammate).ifPresent(kit -> {
                        kit.addTemporaryEffect(effect);
                        kit.restoreFoodItem();
                    });

                    HEAL_COOLDOWNS.put(teammate, Expiration.after(HEALING_COOLDOWN));
                });
        }

        protected void animateRing() {
            double radius = 0;
            int points = 4;

            while (radius < HEAL_RADIUS) {
                final double r = radius;
                EngineerKit.this.attach(EasyTask.of(() -> {
                    EffectUtil.getParticleRing(EngineerKit.this.getPlayer().getLocation(), (int) (points * r), r).forEach(location -> {
                        ParticlePacket.of(EnumParticle.HEART).at(location).send();
                    });
                }).runTaskLater(this.getPlugin(), (long) r));
                radius += 0.75;
            }
        }

    }


}
