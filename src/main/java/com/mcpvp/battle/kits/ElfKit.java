package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
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
import com.mcpvp.common.chat.C;
import com.mcpvp.common.movement.CancelNextFallTask;
import com.mcpvp.common.nms.ActionbarUtil;
import lombok.extern.log4j.Log4j2;
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

@Log4j2
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
        this.getPlayer().setExp(1.0f);
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
        this.sword = new KitItem(
            this,
            ItemBuilder.of(Material.WOOD_SWORD)
                .name("Elf Sword")
                .unbreakable()
                .enchant(Enchantment.DAMAGE_ALL, 3, false)
                .enchant(Enchantment.DURABILITY, 10, true)
                .build()
        );
        this.arrows = new KitItem(
            this,
            ItemBuilder.of(Material.ARROW)
                .name("Elf Arrow")
                .amount(ARROW_COUNT)
                .build(),
            true
        );

        return new KitInventoryBuilder()
            .add(this.sword)
            .add(new PureElement())
            .add(new WindElement())
            .add(new WaterElement())
            .addFood(5)
            .add(this.arrows)
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (this.getPlayer().getExp() > 0
            && this.sword.isItem(this.getPlayer().getItemInHand())
            && this.getPlayer().isSneaking()
            && this.getPlayer().isBlocking()
        ) {
            if (this.shield == null) {
                this.shield = new ProjectileShield();
                this.attach((EasyLifecycle) this.shield);
            }
        } else {
            if (this.shield != null) {
                // The player ran out of XP, add a delay until they can regenerate XP again
                if (this.getPlayer().getExp() == 0) {
                    this.shieldRegenExpiration.expireIn(SHIELD_REGEN_COOLDOWN);
                }

                this.shield.shutdown();
                this.shield = null;
            }

            if (this.getPlayer().getExp() < 1 && this.shieldRegenExpiration.isExpired()) {
                float increased = (float) (this.getPlayer().getExp() + (double) 1 / (double) SHIELD_FILL.toTicks());
                this.getPlayer().setExp(Math.min(increased, 1));
            }
        }

        if (event.isInterval(ARROW_RESTORE)) {
            this.arrows.increment(ARROW_COUNT);
        }
    }

    @EventHandler
    public void onKill(PlayerKilledByPlayerEvent event) {
        if (event.getKiller() == this.getPlayer()) {
            this.arrows.increment(ARROW_COUNT);
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
            if (this.isItem(ElfKit.this.getPlayer().getItemInHand())) {
                if (this.getItem().getType() != Material.BOW && !isPlaceholder()) {
                    this.modify(item -> item.type(Material.BOW));
                }
            } else {
                if (this.getItem().getType() == Material.BOW) {
                    this.restore();
                }
            }
        }

        @EventHandler
        public void onShootEvent(EntityShootBowEvent event) {
            if (!this.isItem(event.getBow())) {
                return;
            }

            this.onShoot(event);
            ElfKit.this.attach(new InteractiveProjectile(this.getPlugin(), (Projectile) event.getProjectile())
                .onHitEvent(ev -> this.onLand(ev.getEntity().getLocation(), event))
                .onDamageEvent(ev -> {
                    if (ev.getEntity() instanceof Player hit) {
                        this.onHit(hit, event, ev);
                    }
                }));

            // The arrow item gets out of sync, not exactly sure why
            ElfKit.this.arrows.refresh(ElfKit.this.getPlayer().getInventory());

            if (ElfKit.this.arrows.getItem().getAmount() == 1) {
                ElfKit.this.arrows.setPlaceholder();
            }
        }

        public abstract void onHit(Player hit, EntityShootBowEvent shootEvent, EntityDamageByEntityEvent damageEvent);

        public abstract void onLand(Location landed, EntityShootBowEvent shootEvent);

        public void onShoot(EntityShootBowEvent event) {

        }

        public void onInteract(PlayerInteractEvent event) {

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
            EffectUtil.trail(event.getProjectile(), pp).runTaskTimer(this.getPlugin(), 0, 1);
        }

        @Override
        public void onHit(Player hit, EntityShootBowEvent shootEvent, EntityDamageByEntityEvent damageEvent) {
            if (ElfKit.this.isTeammate(hit)) {
                return;
            }

            if (shootEvent.getForce() == 1) {
                hit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 15, 5));
                hit.setVelocity(new Vector(0, -1, 0));

                if (this.doTrueDamage.isExpired()) {
                    EventUtil.setDamage(damageEvent, 12);

                    new ParticlePacket(EnumParticle.FIREWORKS_SPARK)
                        .at(hit.getEyeLocation())
                        .count(25)
                        .setData(0.5f)
                        .send();

                    this.doTrueDamage.expireIn(PURE_SHOT_WINDOW);
                }
            }
        }

        @Override
        public void onLand(Location landed, EntityShootBowEvent shootEvent) {
            this.doTrueDamage.expireNow();
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
            this.onLand(damageEvent.getDamager().getLocation(), shootEvent);
            damageEvent.setCancelled(true);
        }

        @Override
        public void onLand(Location landed, EntityShootBowEvent shootEvent) {
            if (shootEvent.getForce() != 1) {
                return;
            }

            if (ElfKit.this.getGame().getTeamManager().getTeams().stream().anyMatch(team ->
                team.getFlag().getLocation().distance(landed) < 15)
            ) {
                return;
            }

            double aoe = ElfKit.this.getAreaOfEffect(shootEvent.getForce(), 5);
            ElfKit.this.getEnemies().stream()
                .filter(player -> player.getLocation().distance(landed) <= aoe)
                .filter(player -> ElfKit.this.getGame().getTeamManager().getTeam(player) != null)
                .filter(player -> !ElfKit.this.getGame().getTeamManager().getTeam(player).isInSpawn(player))
                .forEach(enemy -> {
                    // The strength of the effect on a scale of 0 to 1.0
                    double strength = (aoe - enemy.getLocation().distance(landed)) / aoe;

                    if (!EntityUtil.isOnGround(enemy)) {
                        strength -= 0.35;
                    }

                    if (ElfKit.this.getPlayer().isSneaking()) {
                        strength /= 2;
                    }

                    // Use the eye location of the player so that they are launched upwards.
                    Vector push = enemy.getEyeLocation().toVector().subtract(landed.toVector());
                    push.add(enemy.getVelocity());
                    double y = Math.max(enemy.getVelocity().getY(), push.getY() - 0.5);
                    push.setY(y);
                    enemy.setVelocity(push.multiply(strength));

                    new CancelNextFallTask(this.getPlugin(), enemy);

                    new ParticlePacket(EnumParticle.CLOUD).at(landed).count(10).setData(0.05f).send();
                });
        }

        @Override
        public void onInteract(PlayerInteractEvent event) {
            if (!this.flyCooldown.isExpired()) {
                return;
            }

            if (!ElfKit.this.getPlayer().getInventory().contains(Material.ARROW, 2)) {
                ActionbarUtil.send(ElfKit.this.getPlayer(), String.format("%sYou need at least %s2 %sarrows to use this!", C.GRAY, C.WHITE, C.GRAY));
                return;
            }

            this.windPush(ElfKit.this.getPlayer());

            this.flyCooldown.expireIn(FLY_COOLDOWN);
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

            ElfKit.this.arrows.decrement(true);
            ElfKit.this.arrows.decrement(true);

            ElfKit.this.attach(new CancelNextFallTask(this.getPlugin(), ElfKit.this.getPlayer()));
        }

    }

    class WaterElement extends Element {

        private static final Duration COOLDOWN = Duration.seconds(15);

        public WaterElement() {
            super(ItemBuilder.of(
                new ItemStack(Material.INK_SACK, 1, DyeColor.BLUE.getDyeData())
            ).name("Water Element").build());
        }

        @Override
        public void onShoot(EntityShootBowEvent event) {
            super.onShoot(event);
            EffectUtil.trail(event.getProjectile(), new ParticlePacket(EnumParticle.WATER_SPLASH).count(20)).runTaskTimer(this.getPlugin(), 0, 1);

            ElfKit.this.attach(EasyTask.of(this::restore).runTaskLater(this.getPlugin(), COOLDOWN.ticks()));
            this.setPlaceholder();
        }

        @Override
        public void onHit(Player hit, EntityShootBowEvent shootEvent, EntityDamageByEntityEvent damageEvent) {
            this.onLand(hit.getLocation(), shootEvent);
            damageEvent.setCancelled(true);
        }

        @Override
        public void onLand(Location landed, EntityShootBowEvent shootEvent) {
            Runnable extinguishRunnable = this.getExtinguishRunnable(landed, shootEvent);

            if (shootEvent.getForce() == 1) {
                final Expiration expiration = Expiration.after(Duration.seconds(8));
                EasyTask.of(task -> {
                    if (expiration.isExpired()) {
                        task.cancel();
                        return;
                    }

                    extinguishRunnable.run();
                }).runTaskTimer(this.getPlugin(), 0, 1);
            } else {
                extinguishRunnable.run();
            }
        }

        private Runnable getExtinguishRunnable(Location landed, EntityShootBowEvent shootEvent) {
            double aoe = ElfKit.this.getAreaOfEffect(shootEvent.getForce(), 3);
            return () -> {
                // Give potion effects to teammates
                EntityUtil.getNearbyEntities(landed, Player.class, 2, 1, 2).stream()
                    .filter(ElfKit.this::isTeammate)
                    .forEach(player -> {
                        if (shootEvent.getForce() == 1) {
                            player.addPotionEffect(
                                new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Duration.seconds(5).ticks(), 0)
                            );

                            player.addPotionEffect(
                                new PotionEffect(PotionEffectType.REGENERATION, Duration.seconds(5).ticks(), 2)
                            );
                        }

                        player.setFireTicks(0);
                    });

                // Extinguish nearby Pyro fires
                ElfKit.this.getBattle().getStructureManager().getStructures().stream()
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
            this.attach((EasyListener) this);
            this.attach(EasyTask.of(() -> {
                EffectUtil.getParticleSphere(
                    ElfKit.this.getPlayer().getLocation().add(0, 1, 0), 100, SHIELD_RADIUS
                ).forEach(location -> {
                    new ParticlePacket(EnumParticle.VILLAGER_HAPPY).at(location).setShowFar(true).send();
                });
            }).runTaskTimer(this.getPlugin(), 0, 20));
        }

        @EventHandler
        public void onTick(TickEvent event) {
            ElfKit.this.getGame().getWorld().getEntities()
                .stream()
                .filter(e ->
                    e.getLocation().add(e.getVelocity()).distance(ElfKit.this.getPlayer().getLocation()) <= SHIELD_RADIUS
                ).forEach(this::attemptReflection);

            // Reduce the Player's XP
            float decreased = (float) (ElfKit.this.getPlayer().getExp() - (double) 1 / (double) SHIELD_EMPTY.toTicks());
            ElfKit.this.getPlayer().setExp(Math.max(decreased, 0));
        }

        @EventHandler
        public void onDamagedByProjectile(EntityDamageByEntityEvent event) {
            if (event.getEntity() != ElfKit.this.getPlayer()) {
                return;
            }

            if (this.attemptReflection(event.getEntity())) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onDwarfSmash(DwarfKit.SmashEvent event) {
            if (event.getDamaged() == ElfKit.this.getPlayer()) {
                ElfKit.this.getPlayer().setExp(0);
            }
        }

        private boolean attemptReflection(Entity entity) {
            if (this.reflected.contains(entity)) {
                return false;
            }

            Player shooter = this.getShooter(entity);
            if (shooter == null) {
                return false;
            }

            if (ElfKit.this.isTeammate(shooter)) {
                return false;
            }

            this.reflect(entity, shooter);
            return true;
        }

        private void reflect(Entity entity, Player shooter) {
            // Lightning strike from a mage breaks the shield
            if (entity instanceof Egg && ElfKit.this.getBattle().getKitManager().isPlaying(shooter, MageKit.class)) {
                ElfKit.this.getPlayer().setExp(0f);
                return;
            }

            this.reflected.add(entity);
            this.bounce(entity);

            // Preserve the shooter of the ender pearl so that the Ninja doesn't get teleported
            if (entity instanceof EnderPearl && ElfKit.this.getBattle().getKitManager().isPlaying(shooter, NinjaKit.class)) {
                return;
            }

            this.setShooter(entity, ElfKit.this.getPlayer());
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
                tnt.setMetadata("shooter", new FixedMetadataValue(this.getPlugin(), player));
            }
        }

        @Override
        public Plugin getPlugin() {
            return ElfKit.this.getPlugin();
        }

    }


}
