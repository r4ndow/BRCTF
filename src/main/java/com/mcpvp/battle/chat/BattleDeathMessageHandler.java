package com.mcpvp.battle.chat;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.GameDeathEvent;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.chat.C;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import static org.bukkit.event.EventPriority.HIGHEST;

@Getter
@RequiredArgsConstructor
public class BattleDeathMessageHandler implements EasyListener {

    private final BattlePlugin plugin;

    @EventHandler(ignoreCancelled = true, priority = HIGHEST)
    @SuppressWarnings("StringBufferReplaceableByString")
    public void onDeath(PlayerKilledByPlayerEvent event) {
        EntityDamageEvent lastDamageCause = event.getKilled().getLastDamageCause();
        if (lastDamageCause == null) {
            return;
        }

        BattleTeam killerTeam = plugin.getBattle().getGame().getTeamManager().getTeam(event.getKiller());
        BattleTeam killedTeam = plugin.getBattle().getGame().getTeamManager().getTeam(event.getKilled());

        StringBuilder message = new StringBuilder();
        message.append(killedTeam.getColor().getChat());
        message.append(event.getKilled().getName());
        message.append(C.R);
        message.append(" was killed by ");
        message.append(killerTeam.getColor().getChat());
        message.append(event.getKiller().getName());

        event.getDeathEvent().setDeathMessage(message.toString());
    }

    @EventHandler(ignoreCancelled = true, priority = HIGHEST)
    public void onDeath(GameDeathEvent event) {
        BattleTeam team = plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        if (team != null) {
            // A weird and probably incorrect way of coloring the name of the player who died
            event.getDeathEvent().setDeathMessage(
                event.getDeathEvent().getDeathMessage().replace(
                    event.getPlayer().getName(),
                    team.getColor().getChatString() + event.getPlayer().getName() + C.R
                )
            );
        }
    }

}
