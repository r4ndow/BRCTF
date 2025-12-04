package com.mcpvp.battle.role;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.event.FlagCaptureEvent;
import com.mcpvp.battle.event.FlagDropEvent;
import com.mcpvp.battle.event.FlagRecoverEvent;
import com.mcpvp.battle.event.FlagStealEvent;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.common.event.EasyListener;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

/**
 * Reacts to flag state changes and refreshes per-player objectives in chat.
 */
@RequiredArgsConstructor
public class RoleObjectiveListener implements EasyListener {

    private final Plugin plugin;
    private final Battle battle;
    private final RoleManager roleManager;

    public void init() {
        this.register();
    }

    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }

    private void updateAllParticipants() {
        for (Player player : this.battle.getGame().getParticipants()) {
            //this.roleManager.checkAndAnnounceObjective(player);
        }
    }

    @EventHandler
    public void onFlagSteal(FlagStealEvent event) {
        this.updateAllParticipants();
    }

    @EventHandler
    public void onFlagDrop(FlagDropEvent event) {
        this.updateAllParticipants();
    }

    @EventHandler
    public void onFlagRecover(FlagRecoverEvent event) {
        this.updateAllParticipants();
    }

    @EventHandler
    public void onFlagCapture(FlagCaptureEvent event) {
        this.updateAllParticipants();
    }

    @EventHandler
    public void onParticipate(PlayerParticipateEvent event) {
        //this.roleManager.checkAndAnnounceObjective(event.getPlayer());
    }
}
