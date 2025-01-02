package com.mcpvp.battle.flag;

import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.time.Expiration;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Represents all basic flag functions. Implementations of this control basic
 * state change and visuals, but don't determine when a flag can be stolen,
 * captured, etc.
 *
 * @author NomNuggetNom
 */
public interface IBattleFlag {
	
	/**
	 * Drops the flag at the given location.
	 *
	 * @param location The location to drop the flag, usually the eye location
	 * of the carrier.
	 * @param item The item generated from the drop attempt. Can be null.
	 */
	void drop(Location location, Item item);
	
	/**
	 * Called when the given player successfully steals a flag. A steal is
	 * *only* when a flag is taken from its home, not picked up.
	 *
	 * @param carrier The player who stole the flag.
	 */
	void steal(Player carrier);
	
	/**
	 * Called when the given player successfully picks up a flag. This can either be
	 * a player stealing a flag from its home, or picking it up from the ground.
	 *
	 * @param carrier The player who picked up the flag.
	 */
	void pickup(Player carrier);
	
	/**
	 * Called when the carrier captures the flag.
	 */
	void capture();
	
	/**
	 * Resets the flag to its initial state, e.g. deletes any dropped items,
	 * removes any carrier, and restores the flag to its home.
	 */
	void reset();
	
	/**
	 * @return The epoch millisecond that the flag was dropped.
	 */
	long getDroppedAt();
	
	/**
	 * @return The epoch millisecond that the flag was last stolen.
	 */
	long getStolenAt();
	
	/**
	 * @return If the flag is at home, e.g. has no carrier and is not dropped.
	 */
	boolean isHome();
	
	/**
	 * @return If the flag is dropped on the ground.
	 */
	boolean isDropped();
	
	/**
	 * @param item The item to check.
	 * @return If the item is this flag.
	 */
	boolean isItem(ItemStack item);
	
	/**
	 * @return The player who is carrying the flag. Null when the flag is at home or dropped.
	 */
	Player getCarrier();
	
	/**
	 * @return The location of the flag no matter what state its in. For example, if it's being
	 * held by a player, the player's location is returned.
	 */
	Location getLocation();
	
	/**
	 * @return The location of this flag when it's home.
	 */
	Location getHome();
	
	/**
	 * Triggers an update of the flag's state and can do things such as recoloring on the fly.
	 */
	void update();
	
	/**
	 * @return The team that this flag belongs to.
	 */
	BattleTeam getTeam();
	
	/**
	 * Controls if the flag is locked, meaning it can't be stolen.
	 *
	 * @param locked If the flag should be locked.
	 */
	void setLocked(boolean locked);
	
	/**
	 * @return If the flag should is locked, meaning it can't be stolen.
	 */
	boolean isLocked();
	
	/**
	 * @return An expiration that represents when a flag can be picked up by a member
	 * of another team.
	 */
	Expiration getPickupExpiration();
	
	/**
	 * @return An expiration that represents when a flag should be automatically restored.
	 */
	Expiration getRestoreExpiration();

	/**
	 * Called every tick to perform updates.
	 *
	 * @param tick The tick number.
	 */
	default void onTick(long tick) {
	
	}
	
}
