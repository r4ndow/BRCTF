package com.mcpvp.battle.event;

import com.mcpvp.common.event.EasyCancellableEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;

@Data
@EqualsAndHashCode(callSuper = false)
public class FlagPoisonEvent extends EasyCancellableEvent {

    private final Player player;

}
