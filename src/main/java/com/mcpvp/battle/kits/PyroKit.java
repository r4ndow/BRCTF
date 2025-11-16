package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.BattleKitType;
import com.mcpvp.battle.match.BattleMatchStructureRestrictions;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.item.ItemUtil;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.nms.ActionbarUtil;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureBuilder;
import com.mcpvp.common.structure.StructureManager;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.util.EntityUtil;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class PyroKit extends BattleKit {

    private static final int EXPLOSION_RADIUS = 3;
    private static final int FRENZY_RADIUS = 5;
    private static final int HITS_TO_FULL = 4;
    private static final float XP_PER_HIT = 1f / HITS_TO_FULL;
    private static final int TRUE_DAMAGE = 7;
    private static final Duration FIRE_DELAY = Duration.seconds(5);
    private static final Duration FRENZY_LENGTH = Duration.seconds(3);
    private static final Duration FS_RESTORE = Duration.seconds(4);
    private static final int FS_USAGE = 3;

    private KitItem axe;
    private KitItem flint;
    private boolean frenzy;

    public PyroKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Pyro";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            ItemBuilder.of(Material.LEATHER_BOOTS).color(Color.fromRGB(0x993333)).build(),
            ItemBuilder.of(Material.LEATHER_LEGGINGS).color(Color.fromRGB(0x592626)).build(),
            ItemBuilder.of(Material.IRON_CHESTPLATE).build(),
            ItemBuilder.of(Material.LEATHER_HELMET).color(Color.fromRGB(0x592626)).build()
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        this.axe = new KitItem(
            this,
            ItemBuilder.of(Material.DIAMOND_AXE)
                .name("Pyro Axe")
                .unbreakable()
                .build()
        );
        this.axe.onInteract(this::onUseAxe);

        this.flint = new KitItem(
            this,
            ItemBuilder.of(Material.FLINT_AND_STEEL)
                .name("Pyro Flint and Steel")
                .build()
        );
        this.flint.onInteract(this::onFlint);

        return new KitInventoryBuilder()
            .add(this.axe)
            .addFood(4)
            .add(this.flint)
            .add(ItemBuilder.of(Material.BOW).name("Pyro Bow").unbreakable())
            .add(ItemBuilder.of(Material.ARROW).name("Pyro Arrow").amount(25), true)
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (event.getEntity() != this.getPlayer() || event.getForce() < 0.99) {
            return;
        }

        this.attach(new InteractiveProjectile(this.getPlugin(), (Projectile) event.getProjectile())
            .onHitEvent(ev -> this.explode((Arrow) ev.getEntity()))
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageWithAxe(EntityDamageByEntityEvent event) {
        if (event.getDamager() != this.getPlayer() || !(event.getEntity() instanceof Player damaged)) {
            return;
        }

        if (!this.axe.isItem(this.getPlayer().getItemInHand())) {
            return;
        }

        // Deal "true" damage to enemies who are on fire and not playing medic
        if (damaged.getFireTicks() > 0 && !this.getBattle().getKitManager().isSelected(damaged, BattleKitType.MEDIC)) {
            EventUtil.setDamage(event, this.frenzy ? TRUE_DAMAGE * 2 : TRUE_DAMAGE);
        }

        // Deal extra damage if the kit owner is on fire
        if (this.getPlayer().getFireTicks() > 0) {
            event.setDamage(event.getDamage() + 0.5);
        }

        if (this.frenzy) {
            // Buff when the player is on fire during frenzy
            if (this.getPlayer().getFireTicks() > 0) {
                event.setDamage(event.getDamage() + 0.5);
            }
        } else {
            // Charge up XP for frenzy mode
            this.getPlayer().setExp(Math.min(this.getPlayer().getExp() + XP_PER_HIT, 1f));
            this.updateExp();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        float durability = ItemUtil.getItemDurability(this.flint.getItem());
        final float newDurability = durability + (1f / FS_RESTORE.ticks());
        this.flint.modify(builder -> builder.durabilityPercent(newDurability));

        if (event.getTick() % 5 == 0) {
            this.attemptFrenzyParticles();
        }
    }

    private void onUseAxe(PlayerInteractEvent event) {
        if (!EventUtil.isRightClick(event) || this.frenzy || this.getPlayer().getExp() != 1) {
            return;
        }

        this.enterFrenzy();
    }

    private void onFlint(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getBlockFace() == null || !EventUtil.isRightClick(event)) {
            return;
        }

        final float segment = 1f / FS_USAGE;
        float current;
        if ((current = ItemUtil.getItemDurability(this.flint.getItem())) >= segment) {
            // Enough durability to use it, decrement
            this.flint.modify(ib -> ib.durabilityPercent(current - segment));

            Block block = event.getClickedBlock().getRelative(event.getBlockFace());
            PyroFire fire = new PyroFire(this.getBattle().getStructureManager());
            this.placeStructure(fire, block);
        } else {
            // No durability!
            event.setCancelled(true);
            ActionbarUtil.send(this.getPlayer(), C.warn(C.RED) + "Your flint and steel is on cooldown!");
        }
    }

    private void explode(Arrow arrow) {
        // The projectile source might be changed by an Elf shield
        if (!(arrow.getShooter() instanceof Player shooter)) {
            return;
        }

        // Ignite nearby enemies
        EntityUtil.getNearbyEntities(arrow.getLocation(), Player.class, EXPLOSION_RADIUS).stream()
            .filter(p -> !this.getBattle().getGame().getTeamManager().isSameTeam(shooter, p))
            .forEach(enemy -> {
                enemy.setFireTicks(Duration.seconds(3.5).ticks());
            });

        // Visual effect
        arrow.getWorld().createExplosion(arrow.getLocation(), 0f, false);
    }

    private void enterFrenzy() {
        this.frenzy = true;
        ParticlePacket.of(EnumParticle.FLAME).at(this.getPlayer().getLocation().add(0, 0.5, 0)).count(100).spread(0.09f).send();
        this.getPlayer().getWorld().playSound(this.getPlayer().getLocation(), Sound.EXPLODE, 0.5f, 0.5f);
        this.getPlayer().setExp(0);

        // Light nearby enemies on fire
        EntityUtil.getNearbyEntities(this.getPlayer().getLocation(), Player.class, FRENZY_RADIUS).stream()
            .filter(p -> !this.getBattle().getGame().getTeamManager().isSameTeam(this.getPlayer(), p))
            .forEach(enemy -> {
                enemy.setFireTicks(60);
            });

        this.attach(Bukkit.getScheduler().runTaskLater(this.getPlugin(), this::exitFrenzy, FRENZY_LENGTH.ticks()));
    }

    private void attemptFrenzyParticles() {
        if (this.frenzy) {
            ParticlePacket.of(EnumParticle.FLAME).at(this.getPlayer().getLocation()).count(15).offset(0.3f, 1f, 0.3f).send();
        }
    }

    private void exitFrenzy() {
        this.frenzy = false;
        this.getPlayer().sendMessage(C.warn(C.AQUA) + "Your frenzy fades...");
        ParticlePacket.of(EnumParticle.SMOKE_LARGE).at(this.getPlayer().getLocation()).count(25).offset(0.5f, 1f, 0.5f).send();
        this.getPlayer().getWorld().playSound(this.getPlayer().getLocation(), Sound.BLAZE_DEATH, 0.5f, 1.0f);
        this.updateExp();
    }

    private void updateExp() {
        if (this.getPlayer().getExp() == 1) {
            this.axe.modify(ItemBuilder::dummyEnchant);
            this.getPlayer().sendMessage(C.info(C.RED) + "Right click axe to enter " + C.hl("frenzy mode") + "!");
        } else {
            this.axe.modify(ItemBuilder::removeDummyEnchant);
        }
    }

    public class PyroFire extends Structure {

        private Block center;

        public PyroFire(StructureManager manager) {
            super(manager, PyroKit.this.getPlayer());
            this.removeAfter(FIRE_DELAY);
        }

        @Override
        public void build(Block center, StructureBuilder builder) {
            builder.ignoreRestrictions(
                BattleMatchStructureRestrictions.NEAR_SPAWN,
                BattleMatchStructureRestrictions.NEAR_FLAG,
                BattleMatchStructureRestrictions.NEAR_PLAYER,
                BattleMatchStructureRestrictions.NEAR_RESTRICTED
            );
            builder.setBlock(center, Material.FIRE);
            this.center = center;
        }

        @Override
        public Plugin getPlugin() {
            return PyroKit.this.getPlugin();
        }

        @EventHandler
        public void onInteract(PlayerInteractEvent event) {
            if (this.getBlocks().contains(event.getClickedBlock())) {
                this.remove();
            }
        }

        @EventHandler
        public void onFireDamage(EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.FIRE && event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK) {
                return;
            }

            if (!(event.getEntity() instanceof Player damaged)) {
                return;
            }

            if (!PyroKit.this.getBattle().getGame().getTeamManager().isSameTeam(PyroKit.this.getPlayer(), damaged)) {
                return;
            }

            if (damaged.getLocation().distance(this.center.getLocation().add(0.5, 0, 0.5)) > 1.25) {
                return;
            }

            event.setCancelled(true);
            damaged.setFireTicks(0);

            // Without this call, the damaged player would remain on fire when the structure disappears
            // Note that `attach` is not used to prevent it from being cancelled on removal
            Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> damaged.setFireTicks(0), 1);
        }

    }

}
