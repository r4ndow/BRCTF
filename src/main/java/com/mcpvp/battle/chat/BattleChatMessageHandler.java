package com.mcpvp.battle.chat;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.chat.Colors;
import com.mcpvp.common.event.EasyListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class BattleChatMessageHandler implements EasyListener {

    @Getter
    private final BattlePlugin plugin;
    private final Battle battle;

    private final Set<UUID> globalChatToggled = ConcurrentHashMap.newKeySet();

    @EventHandler(ignoreCancelled = true)
    public void onToggleChannel(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null) {
            return;
        }

        String trimmed = raw.trim();
        if (!trimmed.equalsIgnoreCase("/a") && !trimmed.equalsIgnoreCase("/all") && !trimmed.equalsIgnoreCase("/g")) {
            return;
        }

        UUID id = event.getPlayer().getUniqueId();
        boolean global;
        if (this.globalChatToggled.remove(id)) {
            global = false;
        } else {
            this.globalChatToggled.add(id);
            global = true;
        }

        event.getPlayer().sendMessage(C.cmdPass() + "Chat set to " + C.hl(global ? "all" : "team") + ".");
        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.globalChatToggled.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Optional<BattleTeam> senderTeam = Optional.ofNullable(this.battle.getGame().getTeamManager().getTeam(event.getPlayer()));

        boolean global = event.getMessage().startsWith("!")
                || senderTeam.isEmpty()
                || this.globalChatToggled.contains(event.getPlayer().getUniqueId());

        List<Player> recipients = new ArrayList<>();
        if (global) {
            recipients.addAll(Bukkit.getOnlinePlayers());
        } else {
            recipients.addAll(senderTeam.get().getPlayers());

            recipients.addAll(
                    Bukkit.getOnlinePlayers().stream()
                            .filter(Predicate.not(this.battle.getGame()::isParticipant))
                            .toList()
            );
        }

        String message = this.formatMessage(
                global,
                event.getMessage(),
                event.getPlayer().getName(),
                senderTeam.map(BattleTeam::getColor).map(Colors::getChatString).orElse(C.WHITE)
        );

        recipients.forEach(player -> player.sendMessage(message));
    }

    private String formatMessage(boolean global, String message, String author, String teamColor) {

        return new StringBuilder()
                .append(author)
                .append(teamColor)
                .append(" »")
                .append(" ")
                .append(global ? C.GOLD + "/g " : "")
                .append(C.R)
                .append(global && message.startsWith("!") ? message.substring(1) : message)
                .toString();
    }

}
