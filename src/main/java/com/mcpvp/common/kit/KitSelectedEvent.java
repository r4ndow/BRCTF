package com.mcpvp.common.kit;

import org.bukkit.entity.Player;

import com.mcpvp.common.event.EasyCancellableEvent;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class KitSelectedEvent extends EasyCancellableEvent {
    
    private final Player player;
    private final KitType<?> kitType;

}
