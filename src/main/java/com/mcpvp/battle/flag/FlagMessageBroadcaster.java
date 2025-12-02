package com.mcpvp.battle.flag;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.BattlePreferences;
import com.mcpvp.battle.event.*;
import com.mcpvp.battle.flag.display.FlagDisplayChannel;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EasyListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.Sound;

import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class FlagMessageBroadcaster implements EasyListener {

    private final BattlePlugin plugin;

    private void playGlobalSound(Sound sound, float pitch) {
        Bukkit.getOnlinePlayers().forEach(player ->
                player.playSound(player.getLocation(), sound, 1.0f, pitch)
        );
    }


    private void broadcast(String message) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            this.plugin.getBattle().getPreferenceManager()
                .find(player, BattlePreferences.FLAG_DISPLAY)
                .orElse(Set.of(FlagDisplayChannel.CHAT))
                .forEach(displayChannel -> {
                    displayChannel.getSender().accept(player, message);
                });
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCapture(FlagCaptureEvent event) {
        String name = event.getPlayerTeam().getColor() + event.getPlayer().getName() + C.R;
        String team = event.getCapturedFlag().getTeam().getColor() + event.getCapturedFlag().getTeam().getName() + C.R;
        String msg = "%s captured the %s flag!".formatted(name, team);
        this.broadcast(msg);

        this.playGlobalSound(Sound.PORTAL_TRAVEL,
                2.0f);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDrop(FlagDropEvent event) {
        BattleTeam playerTeam = this.plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        String name = playerTeam.getColor() + event.getPlayer().getName() + C.R;
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "%s dropped the %s flag!".formatted(name, team);
        this.broadcast(msg);

        this.playGlobalSound(Sound.LAVA_POP, 1.2f);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPickup(FlagPickupEvent event) {
        BattleTeam playerTeam = this.plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        String name = playerTeam.getColor() + event.getPlayer().getName() + C.R;
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "%s picked up the %s flag!".formatted(name, team);
        this.broadcast(msg);

        this.playGlobalSound(Sound.FIZZ, 1.0f);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRecover(FlagRecoverEvent event) {
        BattleTeam playerTeam = this.plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        String name = playerTeam.getColor() + event.getPlayer().getName() + C.R;
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "%s recovered the %s flag!".formatted(name, team);
        this.broadcast(msg);

        this.playGlobalSound(Sound.ORB_PICKUP, 1.0f);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRestore(FlagRestoreEvent event) {
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "The %s flag has been restored!".formatted(team);
        this.broadcast(msg);

        this.playGlobalSound(Sound.ORB_PICKUP, 1.0f);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSteal(FlagStealEvent event) {
        BattleTeam playerTeam = this.plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        String name = playerTeam.getColor() + event.getPlayer().getName() + C.R;
        String team = event.getFlag().getTeam().getColor() + event.getFlag().getTeam().getName() + C.R;
        String msg = "%s stole the %s flag!".formatted(name, team);
        this.broadcast(msg);

        this.playGlobalSound(Sound.FIZZ, 1.0f);
    }

}
