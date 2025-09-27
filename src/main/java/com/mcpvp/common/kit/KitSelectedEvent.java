package com.mcpvp.common.kit;

import com.mcpvp.common.event.EasyEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

/**
 * Called when a player successfully selects a kit.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class KitSelectedEvent extends EasyEvent {

    private final Player player;
    private final KitDefinition kitDefinition;
    private final boolean respawn;

}
