package com.mcpvp.battle.event;

import com.mcpvp.battle.flag.IBattleFlag;
import org.bukkit.entity.Player;

public class FlagStealEvent extends FlagTakeEvent {

    public FlagStealEvent(Player player, IBattleFlag flag) {
        super(player, flag);
    }

}
