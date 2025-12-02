package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.item.CooldownItem;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.event.EasyEvent;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.shape.Cuboid;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureBuilder;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.util.EffectUtil;
import com.mcpvp.common.util.EntityUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Objects;

import static com.mcpvp.battle.match.BattleMatchStructureRestrictions.*;

public class MageKit extends BattleKit {

    public static final int DAMAGE_ARROW_DIST = 15;
    public static final int DAMAGE_ARROW_DIST_SQUARED = (int) Math.pow(DAMAGE_ARROW_DIST, 2);
    public static final int DAMAGE_ARROW_DAMAGE = 9; // 4.5 hearts
    public static final int FIRE_IMPACT_DAMAGE = 5;
    public static final Duration FIRE_DURATION = Duration.seconds(4);
    public static final int LIGHTNING_DAMAGE = 6; // 3 hearts
    public static final double LIGHTNING_DIST = 3;
    public static final Duration FREEZE_DURATION = Duration.seconds(3);

    public MageKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Mage";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            ItemBuilder.of(Material.LEATHER_BOOTS)
                .color(DyeColor.BLUE.getColor(), true)
                .enchant(Enchantment.PROTECTION_FALL, 1)
                .build(),
            ItemBuilder.of(Material.LEATHER_LEGGINGS)
                .color(DyeColor.BLUE.getColor(), true)
                .enchant(Enchantment.PROTECTION_FIRE, 1)
                .build(),
            ItemBuilder.of(Material.LEATHER_CHESTPLATE)
                .color(DyeColor.BLUE.getColor(), true)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2)
                .build(),
            null,
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        return new KitInventoryBuilder()
            .add(new DamageSpell())
            .add(new FlameSpell())
            .add(new LightningSpell())
            .add(new FreezeSpell())
            .add(new HealSpell())
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onEnderPearl(PlayerTeleportEvent event) {
        if (event.getPlayer() == this.getPlayer() && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            event.setCancelled(true);
        }
    }

    class DamageSpell extends CooldownItem {

        public DamageSpell() {
            super(
                MageKit.this,
                ItemBuilder.of(Material.DIAMOND_HOE).name("Damage Spell").build(),
                Duration.milliseconds(750)
            );
        }

        @Override
        protected boolean showDurabilityRecharge() {
            return true;
        }

        @Override
        public void onUse(PlayerInteractEvent event) {
            Location spawned = event.getPlayer().getLocation().add(0, 1, 0);
            Arrow arrow = event.getPlayer().launchProjectile(Arrow.class);
            arrow.setShooter(MageKit.this.getPlayer());
            MageKit.this.attach(arrow);
            Vector v = arrow.getVelocity().clone().add(arrow.getVelocity().clone());

            MageKit.this.attach(EasyTask.of(task -> {
                if (arrow.isDead() || arrow.getLocation().distanceSquared(spawned) >= DAMAGE_ARROW_DIST_SQUARED) {
                    arrow.remove();
                    task.cancel();
                } else {
                    arrow.setVelocity(v);
                }
            }).runTaskTimer(this.getPlugin(), 0, 1));

            MageKit.this.attach(new InteractiveProjectile(this.getPlugin(), arrow)
                .onDeath(() -> {
                    FireworkEffect effect = FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL)
                        .withColor(
                            MageKit.this.getGame().getTeamManager().getTeam(MageKit.this.getPlayer()).getColor().getColor(),
                            Color.PURPLE
                        )
                        .build();

                    EffectUtil.sendInstantFirework(effect, arrow.getLocation());
                })
                .onHitEvent(hitEvent -> {
                    arrow.remove();
                })
                .onDamageEvent(ev -> {
                    // The arrow hitting the player actually deals the damage.
                    if (!(ev.getEntity() instanceof Player hit)) {
                        return;
                    }

                    if (MageKit.this.isTeammate(hit)) {
                        return;
                    }

                    double mult = 1.00;
                    // 7 should be base damage, then do 2 more
                    ev.setDamage(DAMAGE_ARROW_DAMAGE * mult);
                })
            );
        }

    }

    class FlameSpell extends CooldownItem {

        public FlameSpell() {
            super(
                MageKit.this,
                ItemBuilder.of(Material.WOOD_HOE).name("Flame Spell").build(),
                Duration.milliseconds(2500)
            );
        }

        @Override
        protected boolean showDurabilityRecharge() {
            return true;
        }

        @Override
        public void onUse(PlayerInteractEvent event) {
            EnderPearl pearl = event.getPlayer().launchProjectile(EnderPearl.class);
            pearl.setShooter(MageKit.this.getPlayer());
            pearl.setFireTicks(Duration.seconds(60).toTicks());
            MageKit.this.attach(pearl);

            event.getPlayer().getWorld().playEffect(event.getPlayer().getEyeLocation(), Effect.BLAZE_SHOOT, 0);

            MageKit.this.attach(new InteractiveProjectile(this.getPlugin(), pearl)
                .singleEventOnly()
                .onDeath(pearl::remove)
                .onHitPlayer(player -> {
                    if (MageKit.this.isTeammate(player)) {
                        return;
                    }

                    player.damage(FIRE_IMPACT_DAMAGE, (Entity) pearl.getShooter());
                    player.setFireTicks(FIRE_DURATION.toTicks());
                    pearl.remove();
                })
            );
        }

    }

    class LightningSpell extends CooldownItem {

        public LightningSpell() {
            super(
                MageKit.this,
                ItemBuilder.of(Material.STONE_HOE).name("Lightning Spell").build(),
                Duration.seconds(5)
            );
        }

        @Override
        protected boolean showDurabilityRecharge() {
            return true;
        }

        @Override
        public void onUse(PlayerInteractEvent event) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                this.strike(event.getClickedBlock().getLocation());
            } else {
                Egg egg = event.getPlayer().launchProjectile(Egg.class);
                egg.setShooter(MageKit.this.getPlayer());
                egg.setVelocity(egg.getVelocity().multiply(3));
                MageKit.this.attach(egg);

                // Remove the projectile after 3 ticks
                // This will trigger the `onMiss` hook of the ProjectileManager
                MageKit.this.attach(EasyTask.of(egg::remove).runTaskLater(this.getPlugin(), 3));

                MageKit.this.attach(new InteractiveProjectile(this.getPlugin(), egg)
                    .onDeath(() -> this.strike(egg.getLocation()))
                    .onHitPlayer(player -> this.strike(player.getLocation()))
                );
            }
        }

        private void strike(Location location) {
            Block block = location.getBlock();

            // Find the first non-empty ground block to strike
            while (block.getY() > 0 && block.isEmpty()) {
                block = block.getRelative(BlockFace.DOWN);
            }

            LightningStrike lightning = MageKit.this.getPlayer().getWorld().strikeLightningEffect(block.getLocation());

            for (Player enemy : MageKit.this.getEnemies()) {
                if (enemy.getLocation().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) > Math.pow(LIGHTNING_DIST, 2)) {
                    continue;
                }

                if (MageKit.this.getGame().getTeamManager().getTeam(enemy).isInSpawn(enemy)) {
                    return;
                }

                enemy.damage(LIGHTNING_DAMAGE);
                MageKit.this.getGame().getWorld().playSound(MageKit.this.getPlayer().getEyeLocation(), Sound.SHOOT_ARROW, 1f, 0.5f);

                Location loc = lightning.getLocation().add(0.5, 0, 0.5);
                loc.setY(enemy.getLocation().getY() - 0.2);
                enemy.setVelocity(enemy.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2));

                new MageStrikeEvent(enemy).call();
            }
        }

    }

    class FreezeSpell extends CooldownItem {

        public FreezeSpell() {
            super(
                MageKit.this,
                ItemBuilder.of(Material.IRON_HOE).name("Freeze Spell").build(),
                Duration.seconds(6.5)
            );
        }

        @Override
        protected boolean showDurabilityRecharge() {
            return true;
        }

        @Override
        public void onUse(PlayerInteractEvent event) {
            Snowball snowball = event.getPlayer().launchProjectile(Snowball.class);
            snowball.setShooter(MageKit.this.getPlayer());
            MageKit.this.attach(snowball);

            MageKit.this.getGame().getWorld().playSound(MageKit.this.getPlayer().getEyeLocation(), Sound.SHOOT_ARROW, 1f, 0.5f);

            MageKit.this.attach(new InteractiveProjectile(this.getPlugin(), snowball)
                .onHitPlayer(player -> {
                    if (MageKit.this.isTeammate(player)) {
                        return;
                    }

                    if (MageKit.this.getGame().getTeamManager().getTeam(player).isInSpawn(player)) {
                        return;
                    }

                    this.centerPlayer(player);
                    IceBoxStructure iceBox = new IceBoxStructure(player);
                    MageKit.this.placeStructure(iceBox, player.getLocation().getBlock());
                })
            );
        }

        private void centerPlayer(Player player) {
            Location location = player.getLocation();
            location.setX(player.getLocation().getBlock().getX() + 0.5);
            location.setY(player.getLocation().getBlock().getY());
            location.setZ(player.getLocation().getBlock().getZ() + 0.5);
            player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }

    }

    class IceBoxStructure extends Structure {

        private final LivingEntity target;

        public IceBoxStructure(LivingEntity target) {
            super(MageKit.this.getBattle().getStructureManager(), MageKit.this.getPlayer());
            this.target = target;
            this.removeAfter(FREEZE_DURATION);
        }

        @Override
        protected void build(Block center, StructureBuilder builder) {
            builder.ignoreRestrictions(
                NEAR_SPAWN, NEAR_RESTRICTED, NEAR_PLAYER, NEAR_FLAG, IN_FLAG
            );

            // Expand the bounding box around the target by one block.
            AxisAlignedBB bb = ((CraftLivingEntity) this.target).getHandle().getBoundingBox().grow(1, 1, 1);
            // Form the cube that will surround the target.
            Location corner1 = new Location(center.getWorld(), bb.a, bb.b, bb.c);
            Location corner2 = new Location(center.getWorld(), bb.d, bb.e, bb.f);
            Cuboid cube = new Cuboid(corner1, corner2);

            for (Location face : cube.getFaces()) {
                if (!face.getBlock().isEmpty()) {
                    continue;
                }

                builder.setBlock(face.getBlock(), Material.ICE);
            }
        }

        @Override
        public Plugin getPlugin() {
            return MageKit.this.getPlugin();
        }

    }

    class HealSpell extends CooldownItem {

        public HealSpell() {
            super(
                MageKit.this,
                ItemBuilder.of(Material.GOLD_HOE).name("Heal Spell").build(),
                Duration.seconds(8)
            );
        }

        @Override
        protected boolean showDurabilityRecharge() {
            return true;
        }

        @Override
        public void onUse(PlayerInteractEvent event) {
            PotionEffect effect = new PotionEffect(PotionEffectType.REGENERATION, Duration.seconds(5).toTicks(), 4);

            if (EventUtil.isRightClick(event)) {
                ThrownPotion potion = event.getPlayer().launchProjectile(ThrownPotion.class);
                MageKit.this.attach(potion);
                potion.setMetadata("mage", new FixedMetadataValue(this.getPlugin(), true));
                potion.setShooter(event.getPlayer());
                ItemStack item = potion.getItem();
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                meta.clearCustomEffects();
                meta.setMainEffect(PotionEffectType.REGENERATION);
                meta.addCustomEffect(effect, true);
                item.setItemMeta(meta);
                potion.setItem(item);
            } else {
                //noinspection deprecation
                MageKit.this.getPlayer().getWorld().playEffect(MageKit.this.getPlayer().getLocation(), Effect.POTION_BREAK, PotionEffectType.REGENERATION.getId());
                EntityUtil.getNearbyEntities(MageKit.this.getPlayer().getLocation(), Player.class, 2, 1, 2).stream()
                    .filter(MageKit.this::isTeammate)
                    .map(MageKit.this.getBattle().getKitManager()::get)
                    .filter(Objects::nonNull)
                    .forEach(kit -> kit.addTemporaryEffect(effect));
            }
        }

    }

    @Getter
    @RequiredArgsConstructor
    public static class MageStrikeEvent extends EasyEvent {

        private final Player player;

    }

}
