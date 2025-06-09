package com.mcpvp.battle.hud.impl;

import com.mcpvp.battle.hud.HeadIndicator;
import com.mcpvp.common.util.chat.C;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class HealthHeadIndicator extends HeadIndicator {

    public HealthHeadIndicator(Plugin plugin, Player observer) {
        super(plugin, observer, "hp", C.RED + "\u2764");
    }

    @Override
    public boolean canSeeIndicatorOn(Player target) {
        return true;
    }

    @Override
    public int getIndicatorValue(Player target) {
        return (int) target.getHealth();
    }

}
