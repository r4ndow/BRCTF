package com.mcpvp.common.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import com.mcpvp.common.event.EasyListener;

import lombok.Getter;

public class InteractiveItemManager implements EasyListener {
    
    @Getter
    private final Plugin plugin;
    private final Set<InteractiveItem> items = new HashSet<>();

    public InteractiveItemManager(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        items.stream().filter(ii -> ii.isItem(event.getItem())).forEach(ii -> ii.onInteractEvent(event));
    }

    public void register(InteractiveItem item) {
        items.add(item);
    }

    public void unregister(InteractiveItem item) {
        items.remove(item);
    }

}
