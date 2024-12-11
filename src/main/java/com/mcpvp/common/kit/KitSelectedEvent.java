package com.mcpvp.common.kit;

import org.bukkit.entity.Player;

import com.mcpvp.common.event.EasyEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * Called when a player successfully selects a kit.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class KitSelectedEvent extends EasyEvent {
    
    private final Player player;
    private final KitDefinition kitDefinition;

}
