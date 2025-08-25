package com.mcpvp.common.event;

import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.stream.Stream;

public class EventUtil {

    public static boolean isRightClick(PlayerInteractEvent event) {
        return event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    public static boolean isLeftClick(PlayerInteractEvent event) {
        return event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK;
    }

    /**
     * Sets the base damage of this event and removes all other damage modifiers,
     * meaning that the final damage of the event should be the number passed.
     *
     * @param event  The event to set the damage for.
     * @param damage The damage to make the event.
     */
    public static void setDamage(EntityDamageEvent event, double damage) {
        Stream.of(EntityDamageEvent.DamageModifier.values())
            .filter(event::isApplicable)
            .forEach(mod -> event.setDamage(mod, 0));

        event.setDamage(EntityDamageEvent.DamageModifier.BASE, damage);
    }

}
