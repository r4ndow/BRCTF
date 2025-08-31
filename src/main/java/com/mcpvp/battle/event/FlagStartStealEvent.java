package com.mcpvp.battle.event;

import com.mcpvp.battle.flag.IBattleFlag;
import com.mcpvp.common.event.EasyCancellableEvent;
import com.mcpvp.common.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FlagStartStealEvent extends EasyCancellableEvent {

    private final Player player;
    private final IBattleFlag flag;
    private Duration requiredStealTime;

}
