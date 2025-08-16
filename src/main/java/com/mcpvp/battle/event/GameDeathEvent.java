package com.mcpvp.battle.event;

import com.mcpvp.common.event.EasyCancellableEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

@Getter
@RequiredArgsConstructor
public class GameDeathEvent extends EasyCancellableEvent {

    private final Player player;
    private final Location location;
    private final PlayerDeathEvent deathEvent;

}
