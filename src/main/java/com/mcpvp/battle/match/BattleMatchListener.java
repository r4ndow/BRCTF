package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.util.chat.C;
import com.mcpvp.common.util.chat.Colors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Optional;

@RequiredArgsConstructor
public class BattleMatchListener implements EasyListener {

    @Getter
    private final BattlePlugin plugin;
    private final Battle battle;

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Optional<BattleTeam> senderTeam = Optional.ofNullable(battle.getGame().getTeamManager().getTeam(event.getPlayer()));

        if (event.getMessage().startsWith("!") || senderTeam.isEmpty()) {
            // Global message
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.sendMessage(formatMessage(
                    true,
                    event.getMessage(),
                    event.getPlayer().getName(),
                    senderTeam.map(BattleTeam::getColor).map(Colors::toString).orElse(C.WHITE)
                ));
            });
        } else {
            // Team message
            senderTeam.get().getPlayers().forEach(player -> {
                player.sendMessage(formatMessage(
                    false,
                    event.getMessage(),
                    event.getPlayer().getName(),
                    senderTeam.map(BattleTeam::getColor).map(Colors::toString).orElse(C.WHITE)
                ));
            });
        }
    }

    private String formatMessage(boolean global, String message, String author, String teamColor) {
        return new StringBuilder()
            .append(author)
            .append(teamColor)
            .append(">")
            .append(" ")
            .append(global ? C.GOLD + "/a " : "")
            .append(C.R)
            .append(global && message.startsWith("!") ? message.substring(1) : message)
            .toString();
    }

}
