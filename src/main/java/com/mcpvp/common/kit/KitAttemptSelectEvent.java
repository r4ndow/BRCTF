package com.mcpvp.common.kit;

import org.bukkit.entity.Player;

import com.mcpvp.common.event.EasyCancellableEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * Called when a player attempts to select a kit. This might be cancelled.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class KitAttemptSelectEvent extends EasyCancellableEvent {
    
    private final Player player;
    private final KitDefinition kitDefinition;

}
