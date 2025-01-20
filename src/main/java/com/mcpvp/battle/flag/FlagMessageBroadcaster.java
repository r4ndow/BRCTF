package com.mcpvp.battle.flag;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.*;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.util.chat.C;
import com.mcpvp.common.event.EasyListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;

@Getter
@RequiredArgsConstructor
public class FlagMessageBroadcaster implements EasyListener {

    private final BattlePlugin plugin;

    public void broadcast(String message) {
        plugin.getBattle().getGame().getParticipants().forEach(player -> {
            player.sendMessage(message);
        });
    }

    @EventHandler
    public void onCapture(FlagCaptureEvent event) {
        String name = event.getPlayerTeam().getColor() + event.getPlayer().getName() + C.R;
        String team = event.getCapturedFlag().getTeam().getColor() + event.getCapturedFlag().getTeam().getName() + C.R;
        String msg = "%s captured the %s flag!".formatted(name, team);
        broadcast(msg);
    }

    @EventHandler
    public void onDrop(FlagDropEvent event) {
        BattleTeam playerTeam = plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        String name = playerTeam.getColor() + event.getPlayer().getName() + C.R;
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "%s dropped the %s flag!".formatted(name, team);
        broadcast(msg);
    }

    @EventHandler
    public void onPickup(FlagPickupEvent event) {
        BattleTeam playerTeam = plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        String name = playerTeam.getColor() + event.getPlayer().getName() + C.R;
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "%s picked up the %s flag!".formatted(name, team);
        broadcast(msg);
    }

    @EventHandler
    public void onRecover(FlagRecoverEvent event) {
        BattleTeam playerTeam = plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        String name = playerTeam.getColor() + event.getPlayer().getName() + C.R;
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "%s recovered the %s flag!".formatted(name, team);
        broadcast(msg);
    }

    @EventHandler
    public void onRestore(FlagRestoreEvent event) {
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "The %s flag has been restored!".formatted(team);
        broadcast(msg);
    }

    @EventHandler
    public void onSteal(FlagStealEvent event) {
        BattleTeam playerTeam = plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        String name = playerTeam.getColor() + event.getPlayer().getName() + C.R;
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "%s stole the %s flag!".formatted(name, team);
        broadcast(msg);
    }

}
