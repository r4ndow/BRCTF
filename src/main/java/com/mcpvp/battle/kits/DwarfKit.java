package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.event.EasyEvent;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.movement.CancelNextFallTask;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.task.FillExpBarTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.BlockUtil;
import com.mcpvp.common.util.EntityUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class DwarfKit extends BattleKit {

    private static final Duration SMASH_COOLDOWN = Duration.seconds(8);
    private static final double DWARF_SMASH_LAUNCH_VELOCITY = 1.5;
    private static final int DWARF_SMASH_RANGE = 6;
    private static final int MAX_LEVEL = 4;
    private static final double MIN_SMASH_FALLOFF_MULTIPLIER = 0.5;
    private static final int DWARF_SMASH_MIN_DAMAGE = 10;
    private static final int DWARF_SMASH_MAX_DAMAGE = 30;
    private static final double ENEMY_LIFT_VELOCITY = 0.3;
    private static final List<UnaryOperator<ItemBuilder>> SWORD_TIERS = List.of(
        item -> item
            .type(Material.WOOD_SWORD)
            .enchant(Enchantment.DAMAGE_ALL, 2),
        item -> item
            .type(Material.STONE_SWORD)
            .enchant(Enchantment.DAMAGE_ALL, 2),
        item -> item
            .type(Material.IRON_SWORD)
            .enchant(Enchantment.DAMAGE_ALL, 2),
        item -> item
            .type(Material.DIAMOND_AXE)
            .enchant(Enchantment.DAMAGE_ALL, 3)
    );

    private final Expiration downgrade = new Expiration();
    private final Expiration smashCooldown = new Expiration();
    private KitItem sword;
    private SmashItem smash;
    private int level = 1;
    private State state = State.READY;
    private Location lastApex;

    public DwarfKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Dwarf";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            new ItemStack(Material.CHAINMAIL_BOOTS),
            new ItemStack(Material.DIAMOND_LEGGINGS),
            new ItemStack(Material.DIAMOND_CHESTPLATE),
            new ItemStack(Material.CHAINMAIL_HELMET)
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        this.sword = new KitItem(this,
            ItemBuilder.of(Material.WOOD_SWORD)
                .name("Dwarf Weapon")
                .enchant(Enchantment.DAMAGE_ALL, 2)
                .unbreakable()
                .build()
        );

        return new KitInventoryBuilder()
            .add(this.sword)
            .addFood(3)
            .add(this.smash = new SmashItem())            
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onKill(PlayerKilledByPlayerEvent event) {
        if (event.getKiller() == this.getPlayer()) {
            this.setState(State.READY);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (this.downgrade.isExpired() && this.level != 1) {
            this.setLevel(this.level - 1);
        }

        if (this.state == State.FLYING && this.getPlayer().getVelocity().getY() - Math.abs(EntityUtil.GRAVITY) <= 0) {
            this.setState(State.FALLING);
        }

        if (this.state == State.COOLDOWN && this.smashCooldown.isExpired()) {
            this.setState(State.READY);
        }

        if ((this.state == State.FALLING || this.state == State.SPIKING) && EntityUtil.isOnGround(this.getPlayer())) {
            // If they reach the ground without taking fall damage, short-circuit
            this.smash(this.getPlayer().getLocation());
        }
    }

    private void launch(boolean upwards) {
        this.getPlayer().setExp(0);

        Vector launch;
        if (upwards) {
            launch = this.getPlayer().getEyeLocation().getDirection();
            launch.multiply(DWARF_SMASH_LAUNCH_VELOCITY * 1.25);
        } else {
            launch = new Vector(0, DWARF_SMASH_LAUNCH_VELOCITY, 0);
        }

        this.getPlayer().setVelocity(launch);

        DwarfKit.this.getPlayer().getWorld().playSound(DwarfKit.this.getPlayer().getEyeLocation(),
            Sound.IRONGOLEM_THROW,
            1.0f,
            0.7f
        );

        // Guesstimate the ticks it will take to reach the apex of the velocity
        // This could be wrong if the player bumps their head, but it's okay
        long ticksUntilApex = (long) Math.abs((long) this.getPlayer().getVelocity().getY() / EntityUtil.GRAVITY) + 1;
        this.animateExp(new FillExpBarTask(this.getPlayer(), Duration.ticks(ticksUntilApex)));

        this.attach(new CancelNextFallTask(this.getPlugin(), this.getPlayer()));
        this.attach(EasyTask.of(task -> {
            // The player has reached the apex of their launch
            if (this.getPlayer().getVelocity().getY() - EntityUtil.GRAVITY <= 0) {
                this.lastApex = this.getPlayer().getLocation();
                task.cancel();
            }
        }).runTaskTimer(this.getPlugin(), 0, 1));

        this.setState(State.FLYING);
    }

    private void pull() {
        this.setState(State.SPIKING);
        this.getPlayer().setVelocity(new Vector(0, -2 * DWARF_SMASH_LAUNCH_VELOCITY, 0));
    }

    private void smash(Location location) {
        final State previousState = this.state;

        this.setState(State.COOLDOWN);
        this.getPlayer().setExp(0);

        this.animateExp(new FillExpBarTask(this.getPlayer(), SMASH_COOLDOWN));
        this.smashCooldown.expireIn(SMASH_COOLDOWN);

        if (this.getDamage((this.lastApex.getBlockY() - location.getBlockY())) == 0) {
            return;
        }

        location.getWorld().playSound(location, Sound.ANVIL_LAND, 0.5f, 0.5f);

        BlockUtil.getBlocksInRadius(location.getBlock(), DWARF_SMASH_RANGE, 0).forEach(block -> {
            this.attach(EasyTask.of(() -> {
                new ParticlePacket(EnumParticle.EXPLOSION_NORMAL)
                    .at(block.getLocation().add(0.5, 0, 0.5))
                    .send();
            }).runTaskLater(this.getPlugin(), (int) block.getLocation().distance(location)));
        });

        List<Player> damaged = this.getEnemies().stream()
            .filter(enemy -> enemy.getLocation().distance(location) <= DWARF_SMASH_RANGE)
            .toList();

        int nextLevel = Math.min(this.level + damaged.size(), MAX_LEVEL);
        if (nextLevel == MAX_LEVEL && !damaged.isEmpty()) {
            location.getWorld().playSound(location, Sound.WITHER_DEATH, 1f, 0.5f);
        }

        damaged.forEach(enemy -> {
            double damage = this.getDamage(Math.abs(this.lastApex.getBlockY() - location.getBlockY()));
            double distance = this.getPlayer().getLocation().distance(enemy.getLocation());
            double range = 1 - MIN_SMASH_FALLOFF_MULTIPLIER;
            double scale = (DWARF_SMASH_RANGE - distance) / DWARF_SMASH_RANGE;
            scale = scale * range + (1 - range);
            damage = damage * scale;

            damage = Math.max(DWARF_SMASH_MIN_DAMAGE, damage);
            damage = Math.min(DWARF_SMASH_MAX_DAMAGE, damage);

            // 15% damage buff when spiking
            if (previousState == State.SPIKING) {
                damage *= 1.15;
            }

            enemy.damage(damage, this.getPlayer());
            enemy.setVelocity(enemy.getVelocity().add(new Vector(0, ENEMY_LIFT_VELOCITY, 0)));

            new SmashEvent(this.getPlayer(), enemy).call();
        });

        this.setLevel(nextLevel);
    }

    private int getDamage(int fallDistance) {
        if (fallDistance < 3) {
            return 0;
        }
        return fallDistance;
    }

    private void setLevel(int level) {
        if (this.level == level) {
            return;
        }

        this.level = level;

        if (level < SWORD_TIERS.size()) {
            this.sword.modify(SWORD_TIERS.get(level - 1));
        }

        this.downgrade.expireIn(Duration.seconds(10 - (1.5 * (level - 1))));
    }

    private void setState(State state) {
        this.state = state;
        this.smash.setState(state);
    }

    @EventHandler
    public void onTakeFallDamage(EntityDamageEvent event) {
        if (event.getEntity() == this.getPlayer() &&
            event.getCause() == EntityDamageEvent.DamageCause.FALL &&
            (this.state == State.FALLING || this.state == State.SPIKING)
        ) {
            this.smash(this.getPlayer().getLocation());
        }
    }

    class SmashItem extends KitItem {

        public SmashItem() {
            super(
                DwarfKit.this,
                ItemBuilder.of(Material.ANVIL).name("Dwarf Smash").build()
            );

            this.onInteract(ev -> {
                ev.setCancelled(true);

                switch (DwarfKit.this.state) {
                    case READY -> {
                        DwarfKit.this.launch(EventUtil.isLeftClick(ev));
                        this.setPlaceholder();
                    }
                    case FALLING -> {
                        DwarfKit.this.pull();
                        this.setPlaceholder();
                    }
                }
            });
        }

        @SuppressWarnings("DuplicateBranchesInSwitch")
        public void setState(State state) {
            this.modify(item -> item.name("Dwarf Smash"));

            switch (state) {
                case READY -> this.restore();
                case FLYING -> this.setPlaceholder();
                case FALLING -> this.modify(item -> item.type(Material.ARROW).name("Spike"));
                case SPIKING -> this.setPlaceholder();
                case COOLDOWN -> this.setPlaceholder();
            }
        }

    }

    enum State {
        READY,
        FLYING,
        FALLING,
        SPIKING,
        COOLDOWN
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    public static class SmashEvent extends EasyEvent {

        private final Player dwarf;
        private final Player damaged;

    }

}
