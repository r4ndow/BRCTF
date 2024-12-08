package com.mcpvp.battle.event;

import org.bukkit.entity.Player;

import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.common.event.EasyEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Fired when a player is no longer participating in the game. This could be because
 * they left the server, started spectating, etc. This is essentially the opposite 
 * of the {@link PlayerParticipateEvent}.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PlayerResignEvent extends EasyEvent {
    
	private final Player player;
	private final BattleGame game;

}
