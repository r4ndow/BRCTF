package com.mcpvp.battle.event;

import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Fired when a player walks into spawn.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PlayerEnterSpawnEvent extends EasyEvent {

    private final Player player;
    private final BattleTeam team;
    private final PlayerMoveEvent cause;

}
