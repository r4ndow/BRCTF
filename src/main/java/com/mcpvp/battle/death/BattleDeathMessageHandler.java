package com.mcpvp.battle.death;

import com.mcpvp.battle.BattlePlugin;
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

}
