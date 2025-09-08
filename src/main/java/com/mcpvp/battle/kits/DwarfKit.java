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
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.task.FillExpBarTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.BlockUtil;
import com.mcpvp.common.util.EntityUtil;
import com.mcpvp.common.movement.CancelNextFallTask;
import lombok.Data;
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
        sword = new KitItem(this,
            ItemBuilder.of(Material.WOOD_SWORD)
                .enchant(Enchantment.DAMAGE_ALL, 2)
                .name("Dwarf Weapon")
                .unbreakable()
                .build()
        );

        return new KitInventoryBuilder()
            .add(sword)
            .add(smash = new SmashItem())
            .addFood(3)
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onKill(PlayerKilledByPlayerEvent event) {
        if (event.getKiller() == getPlayer()) {
            setState(State.READY);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (downgrade.isExpired() && level != 1) {
            setLevel(level - 1);
        }

        if (state == State.FLYING && getPlayer().getVelocity().getY() - Math.abs(EntityUtil.GRAVITY) <= 0) {
            setState(State.FALLING);
        }

        if (state == State.COOLDOWN && smashCooldown.isExpired()) {
            setState(State.READY);
        }

        if ((state == State.FALLING || state == State.SPIKING) && EntityUtil.isOnGround(getPlayer())) {
            // If they reach the ground without taking fall damage, short-circuit
            smash(getPlayer().getLocation());
        }
    }

    private void launch(boolean upwards) {
        getPlayer().setExp(0);

        Vector launch;
        if (upwards) {
            launch = getPlayer().getEyeLocation().getDirection();
            launch.multiply(DWARF_SMASH_LAUNCH_VELOCITY * 1.25);
        } else {
            launch = new Vector(0, DWARF_SMASH_LAUNCH_VELOCITY, 0);
        }

        getPlayer().setVelocity(launch);

        // Guesstimate the ticks it will take to reach the apex of the velocity
        // This could be wrong if the player bumps their head, but it's okay
        long ticksUntilApex = (long) Math.abs((long) getPlayer().getVelocity().getY() / EntityUtil.GRAVITY) + 1;
        animateExp(new FillExpBarTask(getPlayer(), Duration.ticks(ticksUntilApex)));

        attach(new CancelNextFallTask(getPlugin(), getPlayer()));
        attach(EasyTask.of(task -> {
            // The player has reached the apex of their launch
            if (getPlayer().getVelocity().getY() - EntityUtil.GRAVITY <= 0) {
                lastApex = getPlayer().getLocation();
                task.cancel();
            }
        }).runTaskTimer(getPlugin(), 0, 1));

        setState(State.FLYING);
    }

    private void pull() {
        setState(State.SPIKING);
        getPlayer().setVelocity(new Vector(0, -2 * DWARF_SMASH_LAUNCH_VELOCITY, 0));
    }

    private void smash(Location location) {
        final State previousState = state;

        setState(State.COOLDOWN);
        getPlayer().setExp(0);

        animateExp(new FillExpBarTask(getPlayer(), SMASH_COOLDOWN));
        smashCooldown.expireIn(SMASH_COOLDOWN);

        if (getDamage((lastApex.getBlockY() - location.getBlockY())) == 0) {
            return;
        }

        location.getWorld().playSound(location, Sound.ANVIL_LAND, 0.5f, 0.5f);

        BlockUtil.getBlocksInRadius(location.getBlock(), DWARF_SMASH_RANGE, 0).forEach(block -> {
            attach(EasyTask.of(() -> {
                new ParticlePacket(EnumParticle.EXPLOSION_NORMAL)
                    .at(block.getLocation().add(0.5, 0, 0.5))
                    .send();
            }).runTaskLater(getPlugin(), (int) block.getLocation().distance(location)));
        });

        List<Player> damaged = getEnemies().stream()
            .filter(enemy -> enemy.getLocation().distance(location) <= DWARF_SMASH_RANGE)
            .toList();

        int nextLevel = Math.min(level + damaged.size(), MAX_LEVEL);
        if (nextLevel == MAX_LEVEL && !damaged.isEmpty()) {
            location.getWorld().playSound(location, Sound.WITHER_DEATH, 1f, 0.5f);
        }

        damaged.forEach(enemy -> {
            double damage = getDamage(Math.abs(lastApex.getBlockY() - location.getBlockY()));
            double distance = getPlayer().getLocation().distance(enemy.getLocation());
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

            enemy.damage(damage, getPlayer());
            enemy.setVelocity(enemy.getVelocity().add(new Vector(0, ENEMY_LIFT_VELOCITY, 0)));

            new SmashEvent(getPlayer(), enemy).call();
        });

        setLevel(nextLevel);
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
            sword.modify(SWORD_TIERS.get(level - 1));
        }

        downgrade.expireIn(Duration.seconds(10 - (1.5 * (level - 1))));
    }

    private void setState(State state) {
        this.state = state;
        this.smash.setState(state);
    }

    @EventHandler
    public void onTakeFallDamage(EntityDamageEvent event) {
        if (event.getEntity() == getPlayer() &&
            event.getCause() == EntityDamageEvent.DamageCause.FALL &&
            (state == State.FALLING || state == State.SPIKING)
        ) {
            smash(getPlayer().getLocation());
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

                switch (state) {
                    case READY -> {
                        launch(EventUtil.isLeftClick(ev));
                        setPlaceholder();
                    }
                    case FALLING -> {
                        pull();
                        setPlaceholder();
                    }
                }
            });
        }

        public void setState(State state) {
            modify(item -> item.name("Dwarf Smash"));

            switch (state) {
                case READY -> restore();
                case FLYING -> setPlaceholder();
                case FALLING -> modify(item -> item.type(Material.ARROW).name("Spike"));
                case SPIKING -> setPlaceholder();
                case COOLDOWN -> setPlaceholder();
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
    @RequiredArgsConstructor
    public static class SmashEvent extends EasyEvent {

        private final Player dwarf;
        private final Player damaged;

    }

}
