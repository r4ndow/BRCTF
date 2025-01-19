package com.mcpvp.battle.event;

import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;

/**
 * Fired when a player walks into spawn.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EnterSpawnEvent extends EasyEvent {

    private final Player player;
    private final BattleTeam team;

}
