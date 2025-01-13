package com.mcpvp.battle.event;

import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.common.event.EasyEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;

/**
 * Fired when a player is participating in the game (even if the game is not started or ongoing). This could be because
 * they joined while a game was in progress, stopped spectating, a game just started, etc.
 * This is essentially the opposite of the {@link PlayerResignEvent}.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PlayerParticipateEvent extends EasyEvent {
	
	private final Player player;
	private final BattleGame game;
	
}
