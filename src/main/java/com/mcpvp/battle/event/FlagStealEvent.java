package com.mcpvp.battle.event;

import com.mcpvp.battle.flag.IBattleFlag;
import org.bukkit.entity.Player;

/**
 * Called when a player has met all other conditions to steal (take a flag from home).
 * If this event is allowed, they will steal the flag.
 *
 * @see FlagStartStealEvent
 * @see FlagPickupEvent
 */
public class FlagStealEvent extends FlagTakeEvent {

    public FlagStealEvent(Player player, IBattleFlag flag) {
        super(player, flag);
    }

}
