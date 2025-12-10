package com.mcpvp.battle.hud.impl;

import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.hud.HeadIndicator;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.time.Expiration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class ScoutSwapIndicator extends HeadIndicator {

    private final BattleGame game;
    private final Map<Player, Expiration> cooldownMap;

    public ScoutSwapIndicator(Plugin plugin, Player observer, BattleGame game, Map<Player, Expiration> cooldownMap) {
        super(plugin, observer, "fi", C.AQUA + "⬤");
        this.game = game;
        this.cooldownMap = cooldownMap;
    }

    @Override
    public boolean canSeeIndicatorOn(Player target) {
        return !this.game.getTeamManager().isSameTeam(this.observer, target);
    }

    @Override
    public int getIndicatorValue(Player target) {
        return Math.max(0, Math.toIntExact(this.cooldownMap.getOrDefault(target, new Expiration()).getRemaining().seconds()));
    }

}
