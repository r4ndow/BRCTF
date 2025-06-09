package com.mcpvp.battle.hud;

import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

@Getter
@RequiredArgsConstructor
public abstract class HeadIndicator implements EasyListener {

    protected final Plugin plugin;
    /**
     * Player who sees this indicator
     */
    protected final Player observer;
    /**
     * Unique string for the scoreboard objective
     */
    protected final String id;
    /**
     * Symbol to display next to the number
     */
    protected final String symbol;

    /**
     * Determines whether the observer can see this indicator on the given player
     *
     * @param target The other player being observed
     * @return True if the observer should see it, false otherwise
     */
    public abstract boolean canSeeIndicatorOn(Player target);

    /**
     * Determines the number to be displayed
     *
     * @param target Number to get for player being looked at by observer
     * @return The number to be displayed
     */
    public abstract int getIndicatorValue(Player target);

    /**
     * This is the value shown when the observer shouldn't see the real data for the target
     *
     * @param target Player for whom bogus data should be shown
     * @return Junk value, default 0
     */
    public int getObscuredValue(Player target) {
        return 0;
    }

    @Override
    public void unregister() {
        EasyListener.super.unregister();
        remove();
    }

    public final void apply() {
        Scoreboard s = observer.getScoreboard();
        Objective o;
        if (s.getObjective(id) == null) {
            o = s.registerNewObjective(id, "dummy");
        } else {
            o = s.getObjective(id);
        }

        o.setDisplayName(symbol);
        o.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    public final void remove() {
        Objective o = observer.getScoreboard().getObjective(id);
        if (o != null) {
            o.unregister();
        }
        observer.getScoreboard().clearSlot(DisplaySlot.BELOW_NAME);
    }

    /**
     * Updates the value of this indicator for every other observed player.
     */
    public void refresh() {
        if (observer.getScoreboard().getObjective(id) == null) {
            return;
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target == null) {
                continue;
            }

            Scoreboard scoreboard = observer.getScoreboard();

            if (canSeeIndicatorOn(target)) {
                scoreboard.getObjective(id).getScore(target.getName()).setScore(getIndicatorValue(target));
            } else {
                scoreboard.getObjective(id).getScore(target.getName()).setScore(getObscuredValue(target));
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        refresh();
    }

}
