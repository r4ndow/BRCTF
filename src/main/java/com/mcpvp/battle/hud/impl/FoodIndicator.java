package com.mcpvp.battle.hud.impl;

import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.hud.HeadIndicator;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.chat.C;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FoodIndicator extends HeadIndicator {

    private final BattleGame game;

    public FoodIndicator(Plugin plugin, Player observer, BattleGame game) {
        super(plugin, observer, "fi", C.GREEN + C.B + "+");
        this.game = game;
    }

    @Override
    public boolean canSeeIndicatorOn(Player target) {
        return this.game.getTeamManager().isSameTeam(this.observer, target);
    }

    @Override
    public int getIndicatorValue(Player target) {
        return this.game.getBattle().getKitManager().find(target).map(BattleKit::getFoodItemCount).orElse(0);
    }

}
