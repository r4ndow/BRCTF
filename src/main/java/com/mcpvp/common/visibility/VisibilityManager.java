package com.mcpvp.common.visibility;

import org.bukkit.entity.Player;

public interface VisibilityManager {

    default void init() {}

    /**
     * Make it so the observer cannot see the target player.
     *
     * @param observer The observing player.
     * @param target   The player who should become invisible to the observer,
     */
    void hide(Player observer, Player target);

    /**
     * Make it so the observer can see the target player.
     *
     * @param observer The observing player.
     * @param target   The player who should become visible to the observer.
     */
    void show(Player observer, Player target);

    /**
     * Tests if the observer can see the target player.
     *
     * @param observer The observing player.
     * @param target   The player who should be checked.
     * @return Whether the observer can see the target player.
     */
    boolean canSee(Player observer, Player target);

}
