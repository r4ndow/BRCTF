package com.mcpvp.battle.event;

import com.mcpvp.common.event.EasyEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PlayerKilledByPlayerEvent extends EasyEvent {

    private final PlayerDeathEvent deathEvent;
    private final Player killed;
    private final Player killer;

}
