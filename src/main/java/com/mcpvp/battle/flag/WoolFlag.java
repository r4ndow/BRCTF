package com.mcpvp.battle.flag;

import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.util.BattleUtil;
import com.mcpvp.common.item.ItemBuilder;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WoolFlag extends BattleFlag {

    @Getter
    private final Plugin plugin;
    private final Location spawn;
    private final List<Item> visuals = new ArrayList<>();
    @NonNull
    private final Item home;
    private Item dropped;

    public WoolFlag(Plugin plugin, BattleTeam team) {
        super(team);
        this.plugin = plugin;
        this.spawn = team.getConfig().getFlag();
        this.home = BattleUtil.spawnWool(this.getHome(), this.getItem());
    }

    @Override
    public void drop(Location location, @Nullable Item item) {
        if (item != null) {
            // Triggered by a player attempting to drop an item
            this.dropped = item;
        } else {
            // Triggered by something else, like a player logging out or dying
            Player carrier = Objects.requireNonNull(this.getCarrier());
            this.dropped = carrier.getWorld().dropItem(carrier.getLocation().add(0, 1.5, 0), this.getItem());
            this.dropped.setVelocity(carrier.getEyeLocation().getDirection().normalize().multiply(0.35));
        }

        super.drop(location, item);
    }

    @Override
    public void removeEntity() {
        if (this.dropped != null) {
            this.dropped.remove();
            this.dropped = null;
        }
    }

    @Override
    public void placeGhost() {
        this.home.setItemStack(ItemBuilder.of(this.getItem().clone()).color(DyeColor.WHITE).build());
    }

    @Override
    public void placeFlag() {
        this.home.setItemStack(this.getItem());
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
            return this.dropped.getLocation();
        }
        return this.getHome();
    }

    @Override
    public Location getHome() {
        return this.spawn;
    }

    @Override
    protected ItemStack getItem() {
        return ItemBuilder.of(BattleUtil.getColoredWool(this.getTeam().getColor().getDye()))
            .name(this.getTeam().getName() + " Flag")
            .tag("flag", String.valueOf(this.getTeam().getId()))
            .build();
    }

    @Override
    public void remove() {
        this.reset();
        this.home.remove();
    }

    @Override
    public void onTick(long tick) {
        super.onTick(tick);

        // Remove any visuals that exist if there is no carrier.
        if (this.getCarrier() == null || !this.getCarrier().isOnline() || this.getCarrier().isDead()) {
            this.visuals.removeIf(i -> {
                i.remove();
                return true;
            });
            return;
        }

        if (this.getCarrier() == null) {
            return;
        }

        if (tick % 5 != 0) {
            return;
        }

        // Spawn a visual indicator that the player is holding the flag.
        Location l = this.getCarrier().getEyeLocation().add(0, 0.5, 0);
        Item i = this.getCarrier().getWorld().dropItem(l, BattleUtil.getColoredWool(this.getTeam().getColor().getDye()));
        i.setVelocity(i.getVelocity().multiply(new Vector(0.3, 2, 0.3)));
        this.visuals.add(i);

        // Remove any older visual items so they don't hang around forever.
        this.visuals.removeIf(it -> {
            if (it.getTicksLived() > 12) {
                it.remove();
                return true;
            }
            return false;
        });
    }

    @EventHandler
    public void onPickupVisuals(PlayerPickupItemEvent event) {
        if (this.isItem(event.getItem().getItemStack())|| this.visuals.contains(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemMerge(ItemMergeEvent event) {
        if (this.isItem(event.getEntity().getItemStack())) {
            event.setCancelled(true);
        }

        // For the visual stream of wool above the players head
        if (this.visuals.contains(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Item item && this.isItem(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFlagDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && this.isItem(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        if (this.isItem(event.getEntity().getItemStack())) {
            event.setCancelled(true);
        }
    }

}
