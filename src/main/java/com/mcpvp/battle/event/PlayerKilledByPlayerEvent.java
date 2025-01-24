package com.mcpvp.battle.event;

import com.mcpvp.common.event.EasyEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PlayerKilledByPlayerEvent extends EasyEvent {

    private final Player killed;
    private final Player killer;

}
