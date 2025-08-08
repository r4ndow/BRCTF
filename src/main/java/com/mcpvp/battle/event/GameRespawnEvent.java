package com.mcpvp.battle.event;

import com.mcpvp.common.event.EasyEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public class GameRespawnEvent extends EasyEvent {

    private final Player player;

}
