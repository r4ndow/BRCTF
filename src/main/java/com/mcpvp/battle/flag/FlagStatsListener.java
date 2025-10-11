package com.mcpvp.battle.flag;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagCaptureEvent;
import com.mcpvp.battle.event.FlagRecoverEvent;
import com.mcpvp.battle.event.FlagStealEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.common.event.EasyListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;

/**
 * Listens for flag interactions and increments player stats.
 */
@Getter
@RequiredArgsConstructor
public class FlagStatsListener implements EasyListener {

    private final BattlePlugin plugin;
    private final BattleGame game;

    @EventHandler
    public void onCapture(FlagCaptureEvent event) {
        this.game.editStats(event.getPlayer(), stats -> stats.setCaptures(stats.getCaptures() + 1));
    }

    @EventHandler
    public void onSteal(FlagStealEvent event) {
        this.game.editStats(event.getPlayer(), stats -> stats.setSteals(stats.getSteals() + 1));
    }

    @EventHandler
    public void onRecover(FlagRecoverEvent event) {
        this.game.editStats(event.getPlayer(), stats -> stats.setRecovers(stats.getRecovers() + 1));
    }

}
