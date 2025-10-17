package com.mcpvp.battle.flag;

import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.util.BattleUtil;
import com.mcpvp.common.item.ItemBuilder;
import lombok.Getter;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Objects;

public class BannerFlag extends BattleFlag {

    @Getter
    private final Plugin plugin;
    private final ArmorStand home;
    private ArmorStand dropped;
    private ItemStack carrierHelmet;

    public BannerFlag(Plugin plugin, BattleTeam team) {
        super(team);
        this.plugin = plugin;
        this.home = this.spawnBanner(team.getFlag().getHome(), team.getColor().getDye());
    }

    @Override
    public void drop(Location location, @Nullable Item item) {
        Player carrier = Objects.requireNonNull(this.getCarrier());
        carrier.getInventory().setHelmet(this.carrierHelmet);

        this.dropped = carrier.getWorld().spawn(carrier.getLocation(), ArmorStand.class);
        this.dropped.setVisible(false);
        this.dropped.setBasePlate(false);
        this.dropped.setHelmet(this.getItem());
        this.dropped.setVelocity(
            item != null ? item.getVelocity() : carrier.getEyeLocation().getDirection().normalize().multiply(0.35)
        );
        this.launchGroundDetection(this.dropped);

        if (item != null) {
            item.remove();
        }

        super.drop(location, item);
    }

    @Override
    protected void removeEntity() {
        if (this.dropped != null) {
            this.dropped.remove();
            this.dropped = null;
        }
    }

    @Override
    protected void placeGhost() {
        this.home.setHelmet(BattleUtil.getColoredBanner(DyeColor.WHITE));
    }

    @Override
    protected void placeFlag() {
        this.home.setHelmet(BattleUtil.getColoredBanner(this.getTeam().getColor().getDye()));
    }

    @Override
    public boolean isHome() {
        return this.getCarrier() == null && this.dropped == null;
    }

    @Override
    public boolean isDropped() {
        return this.dropped != null;
    }

    @Override
    public Location getLocation() {
        if (this.getCarrier() != null) {
            return this.getCarrier().getLocation();
        } else if (this.dropped != null) {
            return this.dropped.getLocation().clone().add(0, 1.8, 0);
        }
        return this.getHome();
    }

    @Override
    public Location getHome() {
        return this.getTeam().getConfig().getFlag();
    }

    @Override
    protected ItemStack getItem() {
        return ItemBuilder.of(BattleUtil.getColoredBanner(this.getTeam().getColor().getDye()))
            .name(this.getTeam().getName() + " Flag")
            .tag("flag", String.valueOf(this.getTeam().getId()))
            .build();
    }

    @Override
    protected void onCarrierChange(Player previous, Player current) {
        if (previous != null) {
            previous.getInventory().setHelmet(this.carrierHelmet);
        }
        if (current != null) {
            this.carrierHelmet = current.getInventory().getHelmet();
            current.getInventory().setHelmet(this.getItem());
        }

        super.onCarrierChange(previous, current);
    }

    @EventHandler
    public void onFlagDamage(EntityDamageEvent event) {
        if (event.getEntity() == this.home || event.getEntity() == this.dropped) {
            event.setCancelled(true);
        }
    }

    private void launchGroundDetection(ArmorStand as) {
        new BukkitRunnable() {
            boolean hitGround = false;
            Vector last = as.getVelocity();

            @Override
            public void run() {
                if (!as.isValid()) {
                    this.cancel();
                    return;
                }

                // The stand has hit the ground, and now needs to be falsely
                // relocated.
                if (as.isOnGround() && !this.hitGround) {
                    this.hitGround = true;
                } else if (this.hitGround) {
                    double x = this.last.getX();
                    double y = as.getVelocity().getY();
                    double z = this.last.getZ();
                    Vector v = new Vector(x, y, z);
                    as.teleport(as.getLocation().add(v));

                    // higher number = closer to ground
                    // 1.53 is close, but sometimes glitches
                    // 1.52 is close, but rarely glitches
                    Block b = as.getLocation().add(0, 1.51, 0).getBlock();
                    if (!b.isEmpty() && b.getType().isSolid()) {
                        as.setGravity(false);
                        this.cancel();
                    }
                } else {
                    this.last = as.getVelocity();
                }
            }
        }.runTaskTimer(this.plugin, 1, 1);
    }

    private ArmorStand spawnBanner(Location location, DyeColor color) {
        Location spawnAt = location.clone();
        spawnAt.add(location.getDirection().getX() * 0.25, -1.8, location.getDirection().getZ() * 0.25);

        ArmorStand as = location.getWorld().spawn(spawnAt, ArmorStand.class);
        as.setMarker(true);
        as.setFireTicks(Integer.MAX_VALUE);
        as.setGravity(false);
        as.setVisible(false);
        as.setBasePlate(false);
        as.setHelmet(BattleUtil.getColoredBanner(color));

        return as;
    }

}
