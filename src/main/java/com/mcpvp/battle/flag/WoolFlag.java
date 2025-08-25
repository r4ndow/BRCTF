package com.mcpvp.battle.flag;

import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.util.BattleUtil;
import com.mcpvp.common.item.ItemBuilder;
import lombok.NonNull;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WoolFlag extends AbstractFlag {

    private final Location spawn;
    private final List<Item> visuals = new ArrayList<>();
    @NonNull
    private Item home;
    private Item dropped;

    public WoolFlag(BattleTeam team, Location spawn) {
        super(team);
        this.spawn = spawn;
        this.home = BattleUtil.spawnWool(getHome(), getItem());
    }

    @Override
    public void drop(Location location, @Nullable Item item) {
        if (item != null) {
            // Triggered by a player attempting to drop an item
            this.dropped = item;
        } else {
            // Triggered by something else, like a player logging out or dying
            Player carrier = Objects.requireNonNull(getCarrier());
            this.dropped = carrier.getWorld().dropItem(carrier.getLocation().add(0, 1.5, 0), getItem());
            this.dropped.setVelocity(carrier.getEyeLocation().getDirection().normalize().multiply(0.35));
        }

        super.drop(location, item);
    }

    @Override
    public boolean isHome() {
        return getCarrier() == null && dropped == null;
    }

    @Override
    public boolean isDropped() {
        return dropped != null;
    }

    @Override
    public void removeEntity() {
        if (dropped != null) {
            dropped.remove();
            dropped = null;
        }
    }

    @Override
    public void placeFlag() {
        home.setItemStack(getItem());
    }

    @Override
    public void placeGhost() {
        home.setItemStack(ItemBuilder.of(getItem().clone()).color(DyeColor.WHITE).build());
    }

    @Override
    public Location getLocation() {
        if (getCarrier() != null) {
            return getCarrier().getLocation();
        } else if (dropped != null) {
            return dropped.getLocation().clone().add(0, 1.8, 0);
        }
        return getHome();
    }

    @Override
    public Location getHome() {
        return spawn;
    }

    @Override
    public void update() {
        if (isHome()) {
            placeFlag();
        } else if (dropped != null) {
            dropped.setItemStack(getItem());
        } else if (getCarrier() != null) {
            getCarrier().getInventory().all(Material.WOOL).forEach((s, i) -> getCarrier().getInventory().setItem(s, getItem()));
            getCarrier().updateInventory();
        }
    }

    @Override
    public boolean isGhostFlag(ItemStack item) {
        return item.getType() == Material.WOOL && item.getDurability() == DyeColor.WHITE.getData();
    }

    @Override
    protected ItemStack getItem() {
        return ItemBuilder.of(BattleUtil.getColoredWool(getTeam().getColor().getDye()))
                .name(getTeam().getName() + " Flag")
                .tag("flag", String.valueOf(getTeam().getId()))
                .build();
    }

    @Override
    public void onTick(long tick) {
        super.onTick(tick);

        // Remove any visuals that exist if there is no carrier.
        if (getCarrier() == null || !getCarrier().isOnline() || getCarrier().isDead()) {
            visuals.removeIf(i -> {
                i.remove();
                return true;
            });
            return;
        }

        if (getCarrier() == null) {
            return;
        }

        if (tick % 5 != 0) {
            return;
        }

        // Spawn a visual indicator that the player is holding the flag.
        Location l = getCarrier().getEyeLocation().add(0, 0.5, 0);
        Item i = getCarrier().getWorld().dropItem(l, BattleUtil.getColoredWool(getTeam().getColor().getDye()));
        i.setVelocity(i.getVelocity().multiply(new Vector(0.3, 2, 0.3)));
        visuals.add(i);

        // Remove any older visual items so they don't hang around forever.
        visuals.removeIf(it -> {
            if (it.getTicksLived() > 12) {
                it.remove();
                return true;
            }
            return false;
        });
    }

}
