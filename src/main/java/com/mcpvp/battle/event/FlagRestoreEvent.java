package com.mcpvp.battle.event;

import com.mcpvp.battle.flag.IBattleFlag;
import com.mcpvp.common.event.EasyEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class FlagRestoreEvent extends EasyEvent {

    private final IBattleFlag flag;

}
