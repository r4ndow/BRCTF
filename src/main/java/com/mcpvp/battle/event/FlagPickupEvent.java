package com.mcpvp.battle.event;

import com.mcpvp.battle.flag.BattleFlag;
import org.bukkit.entity.Player;

/**
 * Called when a player has met all other conditions to pick up a flag from the ground.
 * If this event is allowed, they will pick up the flag.
 *
 * @see FlagStealEvent
 */
public class FlagPickupEvent extends FlagTakeEvent {

    public FlagPickupEvent(Player player, BattleFlag flag) {
        super(player, flag);
    }

}
