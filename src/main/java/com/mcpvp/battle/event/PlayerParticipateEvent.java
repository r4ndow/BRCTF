package com.mcpvp.battle.event;

import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.common.event.EasyEvent;

import lombok.Data;
import org.bukkit.entity.Player;

/**
 * Fired when a player is participating in the game. This could be because
 * they joined while a game was in progress, stopped spectating, a game just started, etc.
 * they left the server, or maybe they started spectating. This is essentially the opposite of
 * the {@link PlayerResignEvent}.
 */
@Data
public class PlayerParticipateEvent extends EasyEvent {
	
	private final Player player;
	private final BattleGame game;
	
}
