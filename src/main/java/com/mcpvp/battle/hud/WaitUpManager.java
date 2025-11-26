package com.mcpvp.battle.hud;

import com.mcpvp.battle.Battle;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.LookUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class WaitUpManager implements EasyListener {

    private final Plugin plugin;
    private final Battle battle;
    private final Map<Player, Expiration> expirations = new HashMap<>();

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!this.expirations.containsKey(event.getPlayer()) || !this.expirations.get(event.getPlayer()).isExpired()) {
            this.expirations.put(event.getPlayer(), Expiration.after(Duration.ms(500)));
            return;
        }

        LookUtil.getFirstPlayerInLineOfSight(event.getPlayer())
            .filter(player -> this.battle.getGame().getTeamManager().isSameTeam(event.getPlayer(), player))
            .ifPresent(player -> {
                player.sendMessage("%s%s%s wants you to wait up!".formatted(C.info(C.PURPLE), C.hl(event.getPlayer().getName()), C.GRAY));
                player.playSound(player.getEyeLocation(), Sound.NOTE_PLING, 1.0f, 0.9f);

                event.getPlayer().sendMessage("%sYou asked %s%s to wait up".formatted(C.cmdPass(), C.hl(player.getName()), C.GRAY));

                this.expirations.get(event.getPlayer()).expireIn(Duration.seconds(3));
            });
    }

}
