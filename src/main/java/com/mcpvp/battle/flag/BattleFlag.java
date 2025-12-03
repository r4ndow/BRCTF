package com.mcpvp.battle.flag;

import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.item.NBTUtil;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EffectUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

/**
 * Encapsulates functionality common to all flags such as pickup timers,
 * carrier tracking, and locking.
 *
 * @see WoolFlag
 * @see BannerFlag
 */
@Getter
@Setter
@RequiredArgsConstructor
public abstract class BattleFlag implements EasyListener {

    private final BattleTeam team;
    private Player carrier;
    /**
     * An expiration that represents when a flag can be picked up by a member
     * of another team.
     */
    private Expiration pickupExpiration = new Expiration();
    /**
     * An expiration that represents when a flag should be automatically restored.
     */
    private Expiration restoreExpiration = new Expiration();
    private boolean locked;
    private long droppedAt = 0;
    private long stolenAt = 0;

    /**
     * Drops the flag at the given location.
     *
     * @param location The location to drop the flag, usually the eye location
     *                 of the carrier.
     * @param item The item generated from the drop attempt. Can be null.
     */
    void drop(Location location, @Nullable Item item) {
        this.droppedAt = System.currentTimeMillis();
        this.pickupExpiration.expireIn(Duration.milliseconds(1000));
        this.restoreExpiration.expireIn(Duration.seconds(15));

        if (item != null) {
            item.setPickupDelay(0);
        }

        this.setCarrier(null);
    }

    /**
     * Called when the given player successfully steals a flag. A steal is
     * *only* when a flag is taken from its home, not picked up.
     *
     * @param carrier The player who stole the flag.
     */
    void steal(Player carrier) {
        this.stolenAt = System.currentTimeMillis();
        this.pickup(carrier);
        this.placeGhost();
    }

    /**
     * Called when the given player successfully picks up a flag. This can either be
     * a player stealing a flag from its home, or picking it up from the ground.
     *
     * @param carrier The player who picked up the flag.
     */
    void pickup(Player carrier) {
        this.setCarrier(carrier);
        this.removeEntity();
        EffectUtil.fakeLightning(this.getLocation());
    }

    /**
     * Called when the carrier captures the flag.
     */
    void capture(BattleTeam scored) {
        this.reset();
        EffectUtil.fakeLightning2(scored);
    }


    /**
     * Resets the flag to its initial state, e.g. deletes any dropped items,
     * removes any carrier, and restores the flag to its home.
     */
    public void reset() {
        this.stolenAt = 0;
        this.droppedAt = 0;
        this.setLocked(false);
        this.setCarrier(null);
        this.removeEntity();
        this.placeFlag();
    }

    /**
     * @param item The item to check.
     * @return If the item is this flag.
     */
    public boolean isItem(ItemStack item) {
        return NBTUtil.hasString(item, "flag", String.valueOf(this.getTeam().getId()));
    }

    public void onTick(long tick) {

    }

    /**
     * @return If the flag is at home, e.g. has no carrier and is not dropped.
     */
    public abstract boolean isHome();

    /**
     * @return If the flag is dropped on the ground.
     */
    public abstract boolean isDropped();

    /**
     * @return The location of the flag no matter what state its in. For example, if it's being
     * held by a player, the player's location is returned.
     */
    public abstract Location getLocation();

    /**
     * @return The location of this flag when it's home.
     */
    public abstract Location getHome();

    /**
     * Remove this flag, including any visuals.
     */
    public abstract void remove();

    /**
     * @return The item put in the player's inventory to represent the flag.
     */
    protected abstract ItemStack getItem();

    /**
     * Removes any dropped entities relating to the flag.
     */
    protected abstract void removeEntity();

    /**
     * Places the "ghost" version of this flag, which appears on the flag post
     * when the flag is not home.
     */
    protected abstract void placeGhost();

    /**
     * Places the real version of this flag, which appears on the flag post
     * when the flag is home.
     */
    protected abstract void placeFlag();

    protected final void setCarrier(Player carrier) {
        final Player previous = this.carrier;
        this.carrier = carrier;

        this.onCarrierChange(previous, carrier);
    }

    /**
     * Called when a carrier change occurs, including when there is no previous carrier,
     * or there is no new carrier.
     *
     * @param previous The previous carrier. Null if there was no carrier.
     * @param current  The new carrier. Null if there is no new carrier.
     */
    protected void onCarrierChange(Player previous, Player current) {
        if (previous != null) {
            previous.getInventory().remove(this.getItem());
        }

        if (current != null) {
            current.getInventory().addItem(this.getItem());
        }
    }

}
