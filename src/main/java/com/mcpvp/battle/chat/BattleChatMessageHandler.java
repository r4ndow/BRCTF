package com.mcpvp.battle.chat;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.chat.Colors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Collection;
import java.util.Optional;

@RequiredArgsConstructor
public class BattleChatMessageHandler implements EasyListener {

    @Getter
    private final BattlePlugin plugin;
    private final Battle battle;

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Optional<BattleTeam> senderTeam = Optional.ofNullable(battle.getGame().getTeamManager().getTeam(event.getPlayer()));

        boolean global = event.getMessage().startsWith("!") || senderTeam.isEmpty();
        Collection<? extends Player> recipients = global ? Bukkit.getOnlinePlayers() : senderTeam.get().getPlayers();
        String message = formatMessage(
            global,
            event.getMessage(),
            event.getPlayer().getName(),
            senderTeam.map(BattleTeam::getColor).map(Colors::getChatString).orElse(C.WHITE)
        );

        recipients.forEach(player -> player.sendMessage(message));
    }

    private String formatMessage(boolean global, String message, String author, String teamColor) {
        //noinspection StringBufferReplaceableByString
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
