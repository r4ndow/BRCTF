package com.mcpvp.battle.chat;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.GameDeathEvent;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.kit.Kit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Optional;

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

        BattleTeam killerTeam = this.plugin.getBattle().getGame().getTeamManager().getTeam(event.getKiller());
        BattleTeam killedTeam = this.plugin.getBattle().getGame().getTeamManager().getTeam(event.getKilled());
        BattleKit killerKit = this.plugin.getBattle().getKitManager().get(event.getKiller());

        StringBuilder message = new StringBuilder();
        message.append(killedTeam.getColor().getChat());
        message.append(event.getKilled().getName());
        message.append(C.R);
        message.append(" was killed by ");
        message.append(killerTeam.getColor().getChat());
        message.append(event.getKiller().getName());
        message.append(C.GRAY);
        message.append(" (");
        message.append(C.R);
        message.append(Optional.ofNullable(killerKit).map(Kit::getName).orElse("None"));
        message.append(C.GRAY);
        message.append(", ");
        message.append(C.R);
        message.append(Optional.ofNullable(killerKit).map(BattleKit::getFoodItemCount).orElse(0));
        message.append(C.GRAY);
        message.append(" food)");

        event.getDeathEvent().setDeathMessage(null);
        event.getKiller().sendMessage(message.toString());
        event.getKilled().sendMessage(message.toString());
    }

    @EventHandler(ignoreCancelled = true, priority = HIGHEST)
    public void onDeath(GameDeathEvent event) {
        BattleTeam team = this.plugin.getBattle().getGame().getTeamManager().getTeam(event.getPlayer());
        if (team != null) {
            // A weird and probably incorrect way of coloring the name of the player who died
            event.getDeathEvent().setDeathMessage(
                event.getDeathEvent().getDeathMessage().replaceFirst(
                    event.getPlayer().getName(),
                    team.getColor().getChatString() + event.getPlayer().getName() + C.R
                )
            );
        }
    }

}
