package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.BlockUtil;
import com.mcpvp.common.util.EffectUtil;
import com.mcpvp.common.util.EntityUtil;
import com.mcpvp.common.util.chat.C;
import com.mcpvp.common.util.movement.CancelNextFallTask;
import com.mcpvp.common.util.nms.ActionbarUtil;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class ElfKit extends BattleKit {

    private static final int ARROW_COUNT = 16;
    private static final Duration SHIELD_FILL = Duration.seconds(15);
    private static final Duration ARROW_RESTORE = Duration.seconds(3);
    private static final Duration SHIELD_REGEN_COOLDOWN = Duration.seconds(3);

    private final Expiration shieldRegenExpiration = new Expiration();
    private KitItem sword;
    private KitItem arrows;
    private ProjectileShield shield;

    public ElfKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
        getPlayer().setExp(1.0f);
    }

    @Override
    public String getName() {
        return "Elf";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            ItemBuilder.of(Material.LEATHER_BOOTS)
                .color(DyeColor.BROWN)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .build(),
            ItemBuilder.of(Material.LEATHER_LEGGINGS)
                .color(Color.LIME)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .build(),
            ItemBuilder.of(Material.LEATHER_CHESTPLATE)
                .color(Color.GREEN)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .build(),
            null
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        sword = new KitItem(
            this,
            ItemBuilder.of(Material.WOOD_SWORD)
                .unbreakable()
                .name("Elf Sword")
                .enchant(Enchantment.DAMAGE_ALL, 3, false)
                .enchant(Enchantment.DURABILITY, 10, true)
                .build()
        );
        arrows = new KitItem(this, ItemBuilder.of(Material.ARROW).name("Elf Arrow").amount(ARROW_COUNT).build(), true);

        return new KitInventoryBuilder()
            .add(sword)
            .add(new PureElement())
            .add(new WindElement())
            .add(new WaterElement())
            .addFood(5)
            .add(arrows)
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (getPlayer().getExp() > 0
            && sword.isItem(getPlayer().getItemInHand())
            && getPlayer().isSneaking()
            && getPlayer().isBlocking()
        ) {
            if (shield == null) {
                shield = new ProjectileShield();
                attach((EasyLifecycle) shield);
            }
        } else {
            if (shield != null) {
                // The player ran out of XP, add a delay until they can regenerate XP again
                if (getPlayer().getExp() == 0) {
                    shieldRegenExpiration.expireIn(SHIELD_REGEN_COOLDOWN);
                }

                shield.shutdown();
                shield = null;
            }

            if (getPlayer().getExp() < 1 && shieldRegenExpiration.isExpired()) {
                float increased = (float) (getPlayer().getExp() + (double) 1 / (double) SHIELD_FILL.toTicks());
                getPlayer().setExp(Math.min(increased, 1));
            }
        }

        if (event.isInterval(ARROW_RESTORE)) {
            arrows.increment(ARROW_COUNT);
        }
    }

    protected double getAreaOfEffect(float drawStrength, int max) {
        return drawStrength * max;
    }

    abstract class Element extends KitItem {

        public Element(ItemStack itemStack) {
            super(ElfKit.this, itemStack);
            this.onInteract(event -> {
                if (EventUtil.isLeftClick(event)) {
                    this.onInteract(event);
                }
            });
        }

        @EventHandler
        public void onTick(TickEvent event) {
            if (isItem(getPlayer().getItemInHand())) {
                if (getItem().getType() != Material.BOW) {
                    modify(item -> item.type(Material.BOW));
                }
            } else {
                if (getItem().getType() == Material.BOW) {
                    restore();
                }
            }
        }

        @EventHandler
        public void onShootEvent(EntityShootBowEvent event) {
            if (isItem(event.getBow())) {
                onShoot(event);
                attach(new InteractiveProjectile(getPlugin(), (Projectile) event.getProjectile())
                    .singleEventOnly()
                    .onHitEvent(ev -> this.onLand(ev.getEntity().getLocation(), event))
                    .onDeath(() -> this.onLand(event.getProjectile().getLocation(), event))
                    .onDamageEvent(ev -> {
                        if (ev.getEntity() instanceof Player hit) {
                            this.onHit(hit, event, ev);
                        }
                    }));
            }

            if (arrows.getItem().getAmount() == 1) {
                arrows.setPlaceholder();
            }
        }

        @EventHandler
        public void onHitEvent(EntityDamageByEntityEvent event) {
            if (event.getDamager() == getPlayer() && isItem(getPlayer().getItemInHand())) {
                onPunch();
            }
        }

        public abstract void onHit(Player hit, EntityShootBowEvent shootEvent, EntityDamageByEntityEvent damageEvent);

        public abstract void onLand(Location landed, EntityShootBowEvent shootEvent);

        public void onShoot(EntityShootBowEvent event) {

        }

        public void onInteract(PlayerInteractEvent event) {

        }

        public void onPunch() {

        }

    }

    class PureElement extends Element {

        private static final Duration PURE_SHOT_WINDOW = Duration.seconds(2);

        private final Expiration doTrueDamage = new Expiration();

        public PureElement() {
            super(ItemBuilder.of(Material.GHAST_TEAR).name("Pure Element").build());
        }

        @Override
        public void onShoot(EntityShootBowEvent event) {
            ParticlePacket pp = new ParticlePacket(EnumParticle.FIREWORKS_SPARK).count(1).setData(0.035f);
            EffectUtil.trail(event.getProjectile(), pp).runTaskTimer(getPlugin(), 0, 1);
        }

        @Override
        public void onHit(Player hit, EntityShootBowEvent shootEvent, EntityDamageByEntityEvent damageEvent) {
            if (isTeammate(hit)) {
                return;
            }

            if (shootEvent.getForce() == 1) {
                hit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 15, 5));
                hit.setVelocity(new Vector(0, -1, 0));

                if (doTrueDamage.isExpired()) {
                    EventUtil.setDamage(damageEvent, 12);

                    new ParticlePacket(EnumParticle.FIREWORKS_SPARK)
                        .at(hit.getEyeLocation())
                        .count(25)
                        .setData(0.5f)
                        .send();

                    doTrueDamage.expireIn(PURE_SHOT_WINDOW);
                }
            }
        }

        @Override
        public void onLand(Location landed, EntityShootBowEvent shootEvent) {
            doTrueDamage.expireNow();
        }

    }

    class WindElement extends Element {

        private static final Duration FLY_COOLDOWN = Duration.milliseconds(200);

        private final Expiration flyCooldown = new Expiration();

        public WindElement() {
            super(ItemBuilder.of(Material.FEATHER).name("Wind Element").build());
        }

        @Override
        public void onHit(Player hit, EntityShootBowEvent shootEvent, EntityDamageByEntityEvent damageEvent) {
            onLand(damageEvent.getDamager().getLocation(), shootEvent);
            damageEvent.setCancelled(true);
        }

        @Override
        public void onLand(Location landed, EntityShootBowEvent shootEvent) {
            if (shootEvent.getForce() != 1) {
                return;
            }

            if (getGame().getTeamManager().getTeams().stream().anyMatch(team ->
                team.getFlag().getLocation().distance(landed) < 15)
            ) {
                return;
            }

            double aoe = getAreaOfEffect(shootEvent.getForce(), 5);
            getEnemies().stream()
                .filter(player -> player.getLocation().distance(landed) <= aoe)
                .filter(player -> getGame().getTeamManager().getTeam(player) != null)
                .filter(player -> getGame().getTeamManager().getTeam(player).isInSpawn(player))
                .forEach(enemy -> {
                    // The strength of the effect on a scale of 0 to 1.0
                    double strength = (aoe - enemy.getLocation().distance(landed)) / aoe;

                    if (!EntityUtil.isOnGround(enemy)) {
                        strength -= 0.35;
                    }

                    if (getPlayer().isSneaking()) {
                        strength /= 2;
                    }

                    // Use the eye location of the player so that they are launched upwards.
                    Vector push = enemy.getEyeLocation().toVector().subtract(landed.toVector());
                    push.add(enemy.getVelocity());
                    double y = Math.max(enemy.getVelocity().getY(), push.getY() - 0.5);
                    push.setY(y);
                    enemy.setVelocity(push.multiply(strength));

                    new CancelNextFallTask(getPlugin(), enemy);

                    new ParticlePacket(EnumParticle.CLOUD).at(landed).count(10).setData(0.05f).send();
                });
        }

        @Override
        public void onInteract(PlayerInteractEvent event) {
            if (!flyCooldown.isExpired()) {
                return;
            }

            if (!getPlayer().getInventory().contains(Material.ARROW, 2)) {
                ActionbarUtil.send(getPlayer(), String.format("%sYou need at least %s2 %sarrows to use this!", C.GRAY, C.WHITE, C.GRAY));
                return;
            }

            windPush(getPlayer());

            flyCooldown.expireIn(FLY_COOLDOWN);
        }

        private void windPush(Player shooter) {
            // Push the player in the opposite direction of where they were looking.
            Vector push = shooter.getEyeLocation().clone().getDirection().multiply(-1);
            push.multiply(0.8);
            push.setX(push.getX() * 0.9);
            push.setZ(push.getZ() * 0.9);
            shooter.setVelocity(push);

            new ParticlePacket(EnumParticle.CLOUD)
                .at(shooter.getLocation())
                .count(10)
                .setData(0.05f)
                .send();

            arrows.decrement(true);
            arrows.decrement(true);

            attach(new CancelNextFallTask(getPlugin(), getPlayer()));
        }

    }

    class WaterElement extends Element {

        public WaterElement() {
            super(ItemBuilder.of(new ItemStack(Material.INK_SACK, 1, DyeColor.BLUE.getDyeData())).name("Water Element").build());
        }

        @Override
        public void onShoot(EntityShootBowEvent event) {
            super.onShoot(event);
            EffectUtil.trail(event.getProjectile(), new ParticlePacket(EnumParticle.WATER_SPLASH).count(20)).runTaskTimer(getPlugin(), 0, 1);
        }

        @Override
        public void onHit(Player hit, EntityShootBowEvent shootEvent, EntityDamageByEntityEvent damageEvent) {
            onLand(hit.getLocation(), shootEvent);
            damageEvent.setCancelled(true);
        }

        @Override
        public void onLand(Location landed, EntityShootBowEvent shootEvent) {
            Runnable extinguishRunnable = getExtinguishRunnable(landed, shootEvent);

            if (shootEvent.getForce() == 1) {
                final Expiration expiration = new Expiration().expireIn(Duration.seconds(8));
                EasyTask.of(task -> {
                    if (expiration.isExpired()) {
                        task.cancel();
                        return;
                    }

                    extinguishRunnable.run();
                }).runTaskTimer(getPlugin(), 0, 1);
            } else {
                extinguishRunnable.run();
            }
        }

        private Runnable getExtinguishRunnable(Location landed, EntityShootBowEvent shootEvent) {
            double aoe = getAreaOfEffect(shootEvent.getForce(), 3);
            return () -> {
                // Give potion effects to teammates
                EntityUtil.getNearbyEntities(landed, Player.class, 2, 1, 2).stream()
                    .filter(ElfKit.this::isTeammate)
                    .forEach(player -> {
                        if (shootEvent.getForce() == 1) {
                            player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                            player.addPotionEffect(
                                new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Duration.seconds(5).ticks(), 0)
                            );

                            player.removePotionEffect(PotionEffectType.REGENERATION);
                            player.addPotionEffect(
                                new PotionEffect(PotionEffectType.REGENERATION, Duration.seconds(5).ticks(), 2)
                            );
                        }

                        player.setFireTicks(0);
                    });

                // Extinguish nearby Pyro fires
                getBattle().getStructureManager().getStructures().stream()
                    .filter(structure -> structure instanceof PyroKit.PyroFire)
                    .filter(structure -> structure.getCenter().getLocation().distance(landed) <= aoe)
                    .forEach(fire -> {
                        fire.remove();

                        fire.getCenter().getWorld().playSound(fire.getCenter().getLocation(), Sound.FIZZ, 1.0f, 1.0f);
                    });

                // Visual effect
                BlockUtil.getBlocksInRadius(landed.getBlock(), (int) aoe, 0).forEach(block -> {
                    ParticlePacket.of(EnumParticle.WATER_SPLASH).at(block.getLocation()).count(1).send();
                });
            };
        }

    }

    class ProjectileShield extends EasyLifecycle implements EasyListener {

        private static final Duration SHIELD_EMPTY = Duration.seconds(5);
        private static final double SHIELD_RADIUS = 2;

        private final Set<Entity> reflected = new HashSet<>();

        public ProjectileShield() {
            attach((EasyListener) this);
            attach(EasyTask.of(() -> {
                getParticleSphere(
                    getPlayer().getLocation().add(0, 1, 0), 100, SHIELD_RADIUS
                ).forEach(location -> {
                    new ParticlePacket(EnumParticle.VILLAGER_HAPPY).at(location).setShowFar(true).send();
                });
            }).runTaskTimer(getPlugin(), 0, 20));
        }

        @EventHandler
        public void onTick(TickEvent event) {
            getGame().getWorld().getEntities()
                .stream()
                .filter(e ->
                    e.getLocation().add(e.getVelocity()).distance(getPlayer().getLocation()) <= SHIELD_RADIUS
                ).forEach(this::attemptReflection);

            // Reduce the Player's XP
            float decreased = (float) (getPlayer().getExp() - (double) 1 / (double) SHIELD_EMPTY.toTicks());
            getPlayer().setExp(Math.max(decreased, 0));
        }

        @EventHandler
        public void onDamagedByProjectile(EntityDamageByEntityEvent event) {
            if (event.getEntity() != getPlayer()) {
                return;
            }

            if (attemptReflection(event.getEntity())) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onDwarfSmash(DwarfKit.SmashEvent event) {
            if (event.getDamaged() == getPlayer()) {
                getPlayer().setExp(0);
            }
        }

        private boolean attemptReflection(Entity entity) {
            if (reflected.contains(entity)) {
                return false;
            }

            Player shooter = getShooter(entity);
            if (shooter == null) {
                return false;
            }

            if (isTeammate(shooter)) {
                return false;
            }

            reflect(entity, shooter);
            return true;
        }

        private void reflect(Entity entity, Player shooter) {
            // Lightning strike from a mage breaks the shield
            if (entity instanceof Egg && getBattle().getKitManager().isPlaying(shooter, MageKit.class)) {
                getPlayer().setExp(0f);
                return;
            }

            reflected.add(entity);
            bounce(entity);

            // Preserve the shooter of the ender pearl so that the Ninja doesn't get teleported
            if (entity instanceof EnderPearl && getBattle().getKitManager().isPlaying(shooter, NinjaKit.class)) {
                return;
            }

            setShooter(entity, getPlayer());
        }

        private void bounce(Entity entity) {
            Vector v = entity.getVelocity().clone();
            v.multiply(-1);
            entity.setVelocity(v);
        }

        private Player getShooter(Entity entity) {
            if (entity instanceof Projectile projectile) {
                return (Player) projectile.getShooter();
            } else if (entity instanceof TNTPrimed tnt) {
                return (Player) tnt.getMetadata("shooter").get(0).value();
            }
            return null;
        }

        private void setShooter(Entity entity, Player player) {
            if (entity instanceof Projectile projectile) {
                projectile.setShooter(player);
            } else if (entity instanceof TNTPrimed tnt) {
                tnt.setMetadata("shooter", new FixedMetadataValue(getPlugin(), player));
            }
        }

        @Override
        public Plugin getPlugin() {
            return ElfKit.this.getPlugin();
        }

    }

    private List<Location> getParticleSphere(Location center, int count, double radius) {
        double phi = Math.PI * (Math.sqrt(5) - 1);
        List<Location> locations = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            double y = 1.0 - (i / (count - 1.0)) * 2.0;
            double r = Math.sqrt(1 - y * y);
            double theta = phi * i;

            double x = Math.cos(theta) * r;
            double z = Math.sin(theta) * r;

            // Get the location of the point along the edge of the sphere
            Location loc = center.clone().add(
                x * radius, y * radius, z * radius
            );
            locations.add(loc);
        }
        return locations;
    }

}
