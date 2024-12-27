package com.mcpvp.battle.flag;

import com.mcpvp.battle.config.BattleTeamConfig;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.util.BattleUtil;
import com.mcpvp.common.item.ItemBuilder;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WoolFlag extends AbstractFlag {

	private final Location spawn;
	private final List<Item> visuals = new ArrayList<>();
	private Item home;
	private Item dropped;

	public WoolFlag(BattleTeam team, Location spawn) {
		super(team);
		this.spawn = spawn;
	}

	@Override
	public void drop(Location location, Item item) {
		Player carrier = Objects.requireNonNull(getCarrier());

		if (item != null) {
			// Triggered by a player attempting to drop an item
			this.dropped = item;
		} else {
			// Triggered by something else, like a player logging out or dying
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
		if (home != null) {
			home.setItemStack(getItem());
		} else {
			home = BattleUtil.spawnWool(getHome(), getItem());
		}
	}

	@Override
	public void placeGhost() {
		if (home != null) {
			home.setItemStack(BattleUtil.getColoredWool(DyeColor.WHITE));
		} else {
			home = BattleUtil.spawnWool(getHome(), ItemBuilder.of(getItem()).color(Color.WHITE).build());
		}
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
	public void reset() {
		if (home != null) {
			home.remove();
			home = null;
		}
		super.reset();
	}

	@Override
	protected ItemStack getItem() {
		return ItemBuilder.of(BattleUtil.getColoredWool(getTeam().getColor().DYE))
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

		if (tick % 4 != 0) {
			return;
		}

		// Spawn a visual indicator that the player is holding the flag.
		Location l = getCarrier().getLocation().add(0, 2.5, 0);
		Item i = getCarrier().getWorld().dropItem(l, BattleUtil.getColoredWool(getTeam().getColor().DYE));
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
