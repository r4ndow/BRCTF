package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.item.CooldownItem;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureBuilder;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.util.BlockUtil;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EngineerKit extends BattleKit {

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
            .add(ItemBuilder.of(Material.STONE_SWORD).name("Engineer Sword").unbreakable().enchant(Enchantment.DAMAGE_ALL, 1))
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
            launchGrenade(event);
        }

        @Override
        protected void onFailedUse() {
            // Controlled explosion of the previously launched grenade
            if (lastGrenade != null && !lastGrenade.isDead()) {
                lastGrenade.setFuseTicks(0);
            }
        }

        public void launchGrenade(PlayerInteractEvent event) {
            if (!EventUtil.isRightClick(event) || inSpawn()) {
                return;
            }

            event.setCancelled(true);

            // Launch the grenade
            Location position = getPlayer().getLocation().clone().add(0, 1, 0);
            Vector velocity = position.getDirection().normalize().multiply(1.3);
            TNTPrimed tnt = (TNTPrimed) getPlayer().getWorld().spawnEntity(position, EntityType.PRIMED_TNT);
            tnt.setFuseTicks(TNT_FUSE_TIME.ticks());
            tnt.setVelocity(velocity);
            tnt.setMetadata("shooter", new FixedMetadataValue(getPlugin(), getPlayer()));
            lastGrenade = tnt;
            attach(tnt);

            // Item decorating
            this.modify(item -> item.type(Material.WOOD_SPADE));
            EasyTask.of(this::restore).runTaskLater(getPlugin(), LAUNCHER_COOLDOWN.ticks());

            // Queue the TNT for exploding
            new GrenadeExplosionTask(tnt, () -> {
                if (tnt.getFuseTicks() > 10) {
                    tnt.setFuseTicks(10);
                }
            });

            // Visual effect
            EffectUtil.colorTrail(tnt, getTeam().getColor().COLOR).runTaskTimer(getPlugin(), 0, 1);
        }

        @EventHandler(ignoreCancelled = true)
        public void onDamagedByGrenade(EntityDamageByEntityEvent event) {
            if (event.getDamager() != lastGrenade) {
                return;
            }

            // The shooter of the grenade might have been changed by an Elf reflection
            Player shooter = (Player) lastGrenade.getMetadata("shooter").get(0).value();

            if (!(event.getEntity() instanceof Player hit) || getGame().getTeamManager().isSameTeam(shooter, hit)) {
                event.setCancelled(true);
                return;
            }

            if (hit.getLocation().distance(event.getDamager().getLocation()) > TNT_MAX_DISTANCE) {
                event.setCancelled(true);
                return;
            }

            EventUtil.setDamage(event, calculateGrenadeDamage(event.getDamager().getLocation(), hit));
        }

        @EventHandler
        public void onGrenadeExplode(EntityExplodeEvent event) {
            if (event.getEntity() != lastGrenade) {
                return;
            }

            // Cancel the explosion damage
            event.blockList().clear();
            event.setYield(0f);

            // Remove nearby medic webs
            List<Block> blocksInRadius = BlockUtil.getBlocksInRadius(event.getEntity().getLocation().getBlock(), EXPLOSION_RADIUS);
            getBattle().getStructureManager().getStructures().stream()
                .filter(structure -> structure instanceof MedicKit.MedicWeb)
                .filter(structure -> isEnemy(structure.getOwner()))
                .filter(structure -> structure.getBlocks().stream().anyMatch(blocksInRadius::contains))
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
            if (tnt.isDead()) {
                cancel();
                return;
            }

            if (tnt.isOnGround() || tnt.getLocation().getBlock().getType() == Material.WEB) {
                afterLanding.run();
                cancel();
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
            setPlaceholder();
            Snowball snowball = getPlayer().launchProjectile(Snowball.class);

            new InteractiveProjectile(EngineerKit.this.getPlugin(), snowball)
                .singleEventOnly()
                .onHitEvent(projectileHitEvent -> {
                    Block block = projectileHitEvent.getEntity().getLocation().getBlock().getRelative(BlockFace.DOWN);
                    SpeedBeacon speedBeacon = new SpeedBeacon(this);
                    if (!placeStructure(speedBeacon, block)) {
                        restore();
                    }
                })
                .onDamageEvent(damageEvent -> {
                    damageEvent.setCancelled(true);
                    restore();
                })
                .onDeath(this::restore)
                .register();
        }

    }

    public class SpeedBeacon extends Structure {

        private static final double AURA_RADIUS = 5.0;
        private static final Duration ANIMATION_INTERVAL = Duration.seconds(0.5);
        private static final Duration DEPLOY_TIME = Duration.seconds(10);
        private static final int SPEED_TIER = 2;
        private static final Duration SPEED_TIME = Duration.seconds(6);
        private static final double AURA_HEIGHT = 10.0;

        private final KitItem item;
        private ArmorStand armorStand;
        private Item glassPane;

        public SpeedBeacon(KitItem item) {
            super(getBattle().getStructureManager(), getPlayer());
            this.item = item;
            removeAfter(DEPLOY_TIME);
        }

        @Override
        protected void build(Block center, StructureBuilder builder) {
            builder.setBlock(center, Material.BEACON);

            Stream.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
                .map(center::getRelative)
                .forEach(block -> builder.setBlock(block, Material.DOUBLE_STEP));

            Location itemLocation = center.getLocation().add(0.5, 4, 0.5);

            glassPane = getOwner().getWorld().dropItem(itemLocation,
                ItemBuilder.of(Material.SUGAR)
                    .tag("prevent_merge", UUID.randomUUID().toString())
                    .build()
            );
            glassPane.setPickupDelay(99999);

            armorStand = center.getWorld().spawn(itemLocation, ArmorStand.class);
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setMarker(true);
            armorStand.setPassenger(glassPane);

            attach(armorStand);
            attach(glassPane);
        }

        @EventHandler
        public void onTick(TickEvent event) {
            giveSpeedEffect();

            if (event.isInterval(ANIMATION_INTERVAL)) {
                animateParticleRing();
            }
        }

        private void giveSpeedEffect() {
            getTeammates().stream()
                .filter(Predicate.not(this::hasBetterSpeed))
                .filter(this::isCloseEnough)
                .forEach(teammate -> {
                    teammate.removePotionEffect(PotionEffectType.SPEED);
                    teammate.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, SPEED_TIME.ticks(), SPEED_TIER - 1));
                });
        }

        private boolean hasBetterSpeed(Player player) {
            //noinspection SimplifyStreamApiCallChains
            return player.getActivePotionEffects().stream()
                .filter(effect -> effect.getType() == PotionEffectType.SPEED)
                .filter(effect -> effect.getAmplifier() + 1 >= SPEED_TIER)
                .filter(effect -> effect.getDuration() >= SPEED_TIME.ticks())
                .findAny()
                .isPresent();
        }

        private boolean isCloseEnough(Player player) {
            return player.getLocation().toVector().distance(
                getCenter().getLocation().toVector().setY(player.getLocation().getY())
            ) <= AURA_RADIUS && Math.abs(player.getLocation().getY() - getCenter().getLocation().getY() - 1) <= AURA_HEIGHT;
        }

        private void animateParticleRing() {
            List<Location> locations = getParticleRing(getCenter().getLocation(), 20, AURA_RADIUS);
            for (int i = 0; i < locations.size(); i++) {
                // Alternate white and team colored particles
                if (i % 2 == 0) {
                    ParticlePacket.colored(getTeam().getColor().COLOR).at(locations.get(i)).showFar().send();
                } else {
                    ParticlePacket.of(EnumParticle.FIREWORKS_SPARK).at(locations.get(i)).showFar().send();
                }
            }
        }

        @EventHandler
        public void onItemPickup(PlayerPickupItemEvent event) {
            if (event.getItem().equals(glassPane)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onItemMerge(ItemMergeEvent event) {
            if (event.getEntity() == glassPane) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageEvent event) {
            if (event.getEntity() == armorStand || event.getEntity() == glassPane) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            if (event.getEntity() == armorStand || event.getEntity() == glassPane) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
            if (event.getRightClicked() == armorStand) {
                event.setCancelled(true);
            }
        }

        @Override
        public void shutdown() {
            super.shutdown();
            item.restore();
        }

        @Override
        public Plugin getPlugin() {
            return EngineerKit.this.getPlugin();
        }

    }

    class HealingAuraPlacer extends CooldownItem {

        private static final Duration HEALING_COOLDOWN = Duration.seconds(5);
        private static final int HEAL_RADIUS = 4;
        private static final Duration HEALING_DURATION = Duration.seconds(5);
        private static final int HEALING_REGEN_TIER = 3;

        public HealingAuraPlacer() {
            super(
                EngineerKit.this,
                ItemBuilder.of(Material.CAKE).name("Healing Aura").build(),
                HEALING_COOLDOWN
            );
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
            event.setCancelled(true);
            setPlaceholder();

            doHealPulse();
            animateRing();
        }

        protected void doHealPulse() {
            getTeammates().stream()
                .filter(teammate -> teammate.getLocation().distance(getPlayer().getLocation()) <= HEAL_RADIUS)
                .forEach(teammate -> {
                    PotionEffect effect = new PotionEffect(
                        PotionEffectType.REGENERATION, HEALING_DURATION.ticks(), HEALING_REGEN_TIER - 1
                    );
                    teammate.removePotionEffect(PotionEffectType.REGENERATION);
                    teammate.addPotionEffect(effect);

                    getBattle().getKitManager().get(teammate).restoreFoodItem();
                });
        }

        protected void animateRing() {
            double radius = 0;
            int points = 4;

            while (radius < HEAL_RADIUS) {
                final double r = radius;
                attach(EasyTask.of(() -> {
                    getParticleRing(getPlayer().getLocation(), (int) (points * r), r).forEach(location -> {
                        ParticlePacket.of(EnumParticle.HEART).at(location).send();
                    });
                }).runTaskLater(getPlugin(), (long) r));
                radius += 0.75;
            }
        }

    }

    private List<Location> getParticleRing(Location center, int count, double radius) {
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Get the location of the point along the edge of the circle
            Location loc = center.clone().add(
                radius * Math.cos(2 * Math.PI / count * i) + 0.5,
                1.25,
                radius * Math.sin(2 * Math.PI / count * i) + 0.5
            );
            locations.add(loc);
        }
        return locations;
    }

}
