package com.mcpvp.battle.flag;

import com.mcpvp.battle.team.BattleTeam;
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
 */
@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractFlag implements IBattleFlag {

    private final BattleTeam team;
    private Player carrier;
    private Expiration pickupExpiration = new Expiration();
    private Expiration restoreExpiration = new Expiration();
    private boolean locked;
    private long droppedAt = 0;
    private long stolenAt = 0;

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

    @Override
    public void drop(Location location, @Nullable Item item) {
        this.droppedAt = System.currentTimeMillis();
        this.pickupExpiration.expireIn(Duration.milliseconds(1000));
        this.restoreExpiration.expireIn(Duration.seconds(15));

        if (item != null) {
            item.setPickupDelay(0);
        }

        this.setCarrier(null);
    }

    @Override
    public void pickup(Player carrier) {
        this.setCarrier(carrier);
        this.removeEntity();
        EffectUtil.fakeLightning(this.getLocation());
    }

    @Override
    public void steal(Player carrier) {
        this.stolenAt = System.currentTimeMillis();
        this.pickup(carrier);
        this.placeGhost();
    }

    @Override
    public void capture() {
        this.reset();
        EffectUtil.fakeLightning(this.getLocation());
    }

    protected final void setCarrier(Player carrier) {
        final Player previous = this.carrier;
        this.carrier = carrier;

        this.onCarrierChange(previous, carrier);
    }

    @Override
    public void reset() {
        this.stolenAt = 0;
        this.droppedAt = 0;
        this.setLocked(false);
        this.setCarrier(null);
        this.removeEntity();
        this.placeFlag();
    }

    @Override
    public Expiration getPickupExpiration() {
        return this.pickupExpiration;
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

    @Override
    public boolean isItem(ItemStack item) {
        return NBTUtil.hasString(item, "flag", String.valueOf(this.getTeam().getId()));
    }
}
