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
import com.mcpvp.common.time.Duration;
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

import static com.mcpvp.battle.match.BattleMatchStructureRestrictions.*;

public class BattlemageKit extends BattleKit {

    public static final int LIGHTNING_DAMAGE = 2;
    public static final double LIGHTNING_DIST = 3;
    public static final Duration FREEZE_DURATION = Duration.seconds(2);
    public static final double FREEZE_RANGE = 3;
    public static final int TELEPORT_DISTANCE = 7;

    private KitItem axe;

    public BattlemageKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Battlemage";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
                // BOOTS = #e0ebf2
                ItemBuilder.of(Material.LEATHER_BOOTS)
                        .color(Color.fromRGB(0xE0EBF2))
                        .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
                        .build(),

                // LEGGINGS = #c1d7e4
                ItemBuilder.of(Material.LEATHER_LEGGINGS)
                        .color(Color.fromRGB(0xC1D7E4))
                        .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
                        .build(),

                // CHESTPLATE = IRON CHESTPLATE
                ItemBuilder.of(Material.IRON_CHESTPLATE)
                        .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
                        .build(),

                // HELMET = #a1c2d7
                ItemBuilder.of(Material.LEATHER_HELMET)
                        .color(Color.fromRGB(0xA1C2D7))
                        .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
                        .build(),
        };
    }


    @Override
    public Map<Integer, KitItem> createItems() {
        this.axe = new KitItem(
                this,
                ItemBuilder.of(Material.IRON_AXE)
                        .name("Battlemage Axe")
                        .enchant(Enchantment.DAMAGE_ALL, 2)
                        .unbreakable()
                        .build()
        );
        // Hotbar order:
        // 1) Iron Sword
        // 2) Food
        // 3) Lightning Spell
        // 4) Freeze Spell
        // 5) Heal Spell
        // 6) Teleport Spell
        return new KitInventoryBuilder()
                .add(this.axe)
                .addFood(2)
                .add(new LightningSpell())
                .add(new FreezeSpell())
                .add(new HealSpell())
                .addCompass(8)
                //.add(new TeleportSpell())
                .build();
    }

    class LightningSpell extends CooldownItem {

        public LightningSpell() {
            super(
                    BattlemageKit.this,
                    ItemBuilder.of(Material.STONE_HOE).name("Lightning Spell").build(),
                    Duration.seconds(7)
            );
        }

        @Override
        protected boolean showDurabilityRecharge() {
            return true;
        }

        @Override
        protected boolean shouldTrigger(PlayerInteractEvent event) {
            return EventUtil.isRightClick(event);
        }

        @Override
        public void onUse(PlayerInteractEvent event) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                this.strike(event.getClickedBlock().getLocation());
            } else {
                BattlemageKit.this.getGame().getWorld().playSound(
                        BattlemageKit.this.getPlayer().getEyeLocation(),
                        Sound.SHOOT_ARROW,
                        1f,
                        0.5f
                );
                Egg egg = event.getPlayer().launchProjectile(Egg.class);
                egg.setShooter(BattlemageKit.this.getPlayer());
                egg.setVelocity(egg.getVelocity().multiply(1));
                BattlemageKit.this.attach(egg);

                Location spawned = event.getPlayer().getLocation().add(0, 1, 0);

                // Optional lifetime clamp was here using MAX_SPELL_PROJECTILE_DIST_SQUARED.

                BattlemageKit.this.attach(new InteractiveProjectile(this.getPlugin(), egg)
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

            LightningStrike lightning =
                    BattlemageKit.this.getPlayer().getWorld().strikeLightningEffect(block.getLocation());

            for (Player enemy : BattlemageKit.this.getEnemies()) {
                if (enemy.getLocation().distanceSquared(
                        block.getLocation().add(0.5, 0.5, 0.5)
                ) > Math.pow(LIGHTNING_DIST, 2)) {
                    continue;
                }

                if (BattlemageKit.this.getGame().getTeamManager()
                        .getTeam(enemy).isInSpawn(enemy)) {
                    return;
                }

                enemy.damage(LIGHTNING_DAMAGE);

                Location loc = lightning.getLocation().add(0.5, 0.1, 0.5);
                loc.setY(enemy.getLocation().getY());
                enemy.setVelocity(
                        enemy.getLocation().toVector()
                                .subtract(loc.toVector())
                                .normalize()
                                .multiply(2)
                );

                new MageStrikeEvent(enemy).call();
            }
        }

    }

    class FreezeSpell extends CooldownItem {

        public FreezeSpell() {
            super(
                    BattlemageKit.this,
                    ItemBuilder.of(Material.IRON_HOE).name("Freeze Spell").build(),
                    Duration.seconds(7)
            );
        }

        @Override
        protected boolean showDurabilityRecharge() {
            return true;
        }

        @Override
        protected boolean shouldTrigger(PlayerInteractEvent event) {
            return EventUtil.isRightClick(event);
        }

        @Override
        public void onUse(PlayerInteractEvent event) {
            Snowball snowball = event.getPlayer().launchProjectile(Snowball.class);
            snowball.setShooter(BattlemageKit.this.getPlayer());
            snowball.setVelocity(snowball.getVelocity().multiply(1));
            BattlemageKit.this.attach(snowball);

            Location spawned = event.getPlayer().getLocation().add(0, 1, 0);

            // Optional lifetime clamp was here using MAX_SPELL_PROJECTILE_DIST_SQUARED.

            BattlemageKit.this.getGame().getWorld().playSound(
                    BattlemageKit.this.getPlayer().getEyeLocation(),
                    Sound.SHOOT_ARROW,
                    1f,
                    0.5f
            );

            BattlemageKit.this.attach(new InteractiveProjectile(this.getPlugin(), snowball)
                    .onDeath(() -> {
                        this.checkNearbyPlayers(snowball.getLocation());
                    })
                    .onHitPlayer(player -> {
                        if (BattlemageKit.this.isTeammate(player)) {
                            return;
                        }

                        if (BattlemageKit.this.getGame().getTeamManager()
                                .getTeam(player).isInSpawn(player)) {
                            return;
                        }

                        this.freezePlayer(player);
                    })
            );
        }

        private void checkNearbyPlayers(Location location) {
            for (Player enemy : BattlemageKit.this.getEnemies()) {
                if (enemy.getLocation().distance(location) <= FREEZE_RANGE) {
                    if (BattlemageKit.this.getGame().getTeamManager()
                            .getTeam(enemy).isInSpawn(enemy)) {
                        continue;
                    }
                    this.freezePlayer(enemy);
                }
            }
        }

        private void freezePlayer(Player player) {
            this.centerPlayer(player);
            IceBoxStructure iceBox = new IceBoxStructure(player);
            BattlemageKit.this.placeStructure(iceBox, player.getLocation().getBlock());
        }

        private void centerPlayer(Player player) {
            Location location = player.getLocation();
            location.setX(player.getLocation().getBlock().getX() + 0.5);
            location.setY(player.getLocation().getBlock().getY());
            location.setZ(player.getLocation().getBlock().getZ() + 0.5);
            player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }

    }

    class TeleportSpell extends CooldownItem {

        private static final Duration TELEPORT_COOLDOWN = Duration.seconds(10);
        private static final float HORIZONTAL_PITCH_THRESHOLD = 15f;
        private static final double RAY_STEP = 0.25;
        private static final double SLAB_Y_OFFSET = 0.5;
        private static final double BELOW_EPSILON = 0.1;
        private static final double UP_LOOK_THRESHOLD = 0.1;

        private static final java.util.Set<Material> TRANSPARENT =
                java.util.Collections.unmodifiableSet(
                        java.util.Collections.singleton(Material.AIR)
                );

        public TeleportSpell() {
            super(
                    BattlemageKit.this,
                    ItemBuilder.of(Material.BLAZE_ROD).name("Teleport Spell").build(),
                    TELEPORT_COOLDOWN
            );
        }

        @Override
        protected boolean showDurabilityRecharge() {
            return true;
        }

        @Override
        protected boolean shouldTrigger(PlayerInteractEvent event) {
            return EventUtil.isRightClick(event);
        }

        @Override
        public void onUse(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            Location start = player.getLocation();

            Location dest = findBlockCenterTeleport(player, TELEPORT_DISTANCE);

            if (dest == null) {
                float pitch = start.getPitch();

                if (Math.abs(pitch) <= HORIZONTAL_PITCH_THRESHOLD) {
                    dest = blinkHorizontal(player, TELEPORT_DISTANCE);
                } else {
                    dest = blinkDirectional(player, TELEPORT_DISTANCE);
                }
            }

            Block below = start.clone().subtract(0, BELOW_EPSILON, 0).getBlock();
            Material belowType = below.getType();
            boolean onSlab =
                    belowType.name().contains("SLAB")
                            || belowType.name().contains("STEP");

            if (onSlab && dest.getBlock().equals(start.getBlock())) {
                float pitch = start.getPitch();

                if (Math.abs(pitch) <= HORIZONTAL_PITCH_THRESHOLD) {
                    dest = blinkHorizontalFromOffset(player, TELEPORT_DISTANCE, SLAB_Y_OFFSET);
                } else {
                    dest = blinkDirectionalFromOffset(player, TELEPORT_DISTANCE, SLAB_Y_OFFSET);
                }
            }

            player.teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.getWorld().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, 1f);
        }

        private Location blinkHorizontal(Player player, double maxDistance) {
            Location start = player.getLocation();
            Vector dir = start.getDirection();
            dir.setY(0);
            if (dir.lengthSquared() == 0) {
                return start;
            }
            dir.normalize();

            Location lastSafe = walkAlongDirection(start, dir, maxDistance);
            lastSafe.setYaw(start.getYaw());
            lastSafe.setPitch(start.getPitch());
            return lastSafe;
        }

        private Location blinkDirectional(Player player, double maxDistance) {
            Location start = player.getLocation();
            Vector dir = start.getDirection().normalize();

            Location lastSafe = walkAlongDirection(start, dir, maxDistance);
            lastSafe.setYaw(start.getYaw());
            lastSafe.setPitch(start.getPitch());
            return lastSafe;
        }

        private Location blinkHorizontalFromOffset(Player player, double maxDistance, double yOffset) {
            Location base = player.getLocation();
            Location start = base.clone().add(0, yOffset, 0);
            Vector dir = start.getDirection();
            dir.setY(0);
            if (dir.lengthSquared() == 0) {
                return base;
            }
            dir.normalize();

            Location lastSafe = walkAlongDirection(start, dir, maxDistance);
            lastSafe.setYaw(base.getYaw());
            lastSafe.setPitch(base.getPitch());
            return lastSafe;
        }

        private Location blinkDirectionalFromOffset(Player player, double maxDistance, double yOffset) {
            Location base = player.getLocation();
            Location start = base.clone().add(0, yOffset, 0);
            Vector dir = start.getDirection().normalize();

            Location lastSafe = walkAlongDirection(start, dir, maxDistance);
            lastSafe.setYaw(base.getYaw());
            lastSafe.setPitch(base.getPitch());
            return lastSafe;
        }

        private Location walkAlongDirection(Location start, Vector dir, double maxDistance) {
            Location lastSafe = start.clone();

            for (double d = RAY_STEP; d <= maxDistance; d += RAY_STEP) {
                Location feetLoc = start.clone().add(dir.clone().multiply(d));
                Block feet = feetLoc.getBlock();
                Block head = feet.getRelative(BlockFace.UP);

                if (feet.getType().isSolid() || head.getType().isSolid()) {
                    break;
                }
                lastSafe = feetLoc;
            }

            return lastSafe;
        }

        private Location findBlockCenterTeleport(Player player, int maxDistance) {
            Vector look = player.getEyeLocation().getDirection();
            if (look.getY() > UP_LOOK_THRESHOLD) {
                return null;
            }

            java.util.List<Block> sight = player.getLineOfSight(TRANSPARENT, maxDistance);
            Block target = null;
            for (Block b : sight) {
                if (!b.getType().isSolid()) {
                    continue;
                }
                target = b;
                break;
            }
            if (target == null) {
                return null;
            }

            Location dest = target.getLocation().add(0.5, 1, 0.5);

            Location current = player.getLocation();
            dest.setYaw(current.getYaw());
            dest.setPitch(current.getPitch());

            Block feet = dest.getBlock();
            Block head = feet.getRelative(BlockFace.UP);
            if (feet.getType().isSolid() || head.getType().isSolid()) {
                return null;
            }

            return dest;
        }
    }

    class IceBoxStructure extends Structure {

        private final LivingEntity target;

        public IceBoxStructure(LivingEntity target) {
            super(BattlemageKit.this.getBattle().getStructureManager(), BattlemageKit.this.getPlayer());
            this.target = target;
            this.removeAfter(FREEZE_DURATION);
        }

        @Override
        protected void build(Block center, StructureBuilder builder) {
            builder.ignoreRestrictions(
                    NEAR_SPAWN, NEAR_RESTRICTED, NEAR_PLAYER, NEAR_FLAG
            );

            // Expand the bounding box around the target by one block.
            AxisAlignedBB bb =
                    ((CraftLivingEntity) this.target).getHandle().getBoundingBox().grow(1, 1, 1);
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
            return BattlemageKit.this.getPlugin();
        }

    }

    class HealSpell extends CooldownItem {

        public HealSpell() {
            super(
                    BattlemageKit.this,
                    ItemBuilder.of(Material.GOLD_HOE).name("Heal Spell").build(),
                    Duration.seconds(7)
            );
        }

        @Override
        protected boolean showDurabilityRecharge() {
            return true;
        }

        @Override
        protected boolean shouldTrigger(PlayerInteractEvent event) {
            return EventUtil.isRightClick(event);
        }

        @Override
        public void onUse(PlayerInteractEvent event) {
            PotionEffect effect = new PotionEffect(
                    PotionEffectType.REGENERATION,
                    Duration.seconds(6).toTicks(),
                    3
            );

            ThrownPotion potion = event.getPlayer().launchProjectile(ThrownPotion.class);
            BattlemageKit.this.attach(potion);
            potion.setMetadata("mage", new FixedMetadataValue(this.getPlugin(), true));
            potion.setShooter(event.getPlayer());
            ItemStack item = potion.getItem();
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            meta.clearCustomEffects();
            meta.setMainEffect(PotionEffectType.REGENERATION);
            meta.addCustomEffect(effect, true);
            item.setItemMeta(meta);
            potion.setItem(item);
        }

    }

    @Getter
    @RequiredArgsConstructor
    public static class MageStrikeEvent extends EasyEvent {

        private final Player player;

    }

}
