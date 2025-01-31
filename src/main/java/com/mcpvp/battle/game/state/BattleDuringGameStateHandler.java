package com.mcpvp.battle.game.state;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.*;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameState;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.kit.KitSelectedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Optional;

public class BattleDuringGameStateHandler extends BattleGameStateHandler {

    public BattleDuringGameStateHandler(BattlePlugin plugin, BattleGame game) {
        super(plugin, game);
    }

    @Override
    public void enterState() {
        super.enterState();

        game.getTeamManager().getTeams().forEach(bt -> {
            bt.getFlag().setLocked(false);
        });

        game.getBattle().getMatch().getTimer().setSeconds(game.getConfig().getTime() * 60);
        game.getBattle().getMatch().getTimer().setPaused(false);
    }

    @Override
    public void leaveState() {
        super.leaveState();
        Bukkit.broadcastMessage("Game over!");
    }

    @EventHandler
    public void onJoinTeam(PlayerJoinTeamEvent event) {
        game.respawn(event.getPlayer(), false);
    }

    @EventHandler
    public void onParticipate(PlayerParticipateEvent event) {
        game.respawn(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKitSelected(KitSelectedEvent event) {
        game.respawn(event.getPlayer(), false);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.setKeepInventory(false);
        event.setKeepLevel(false);
        event.setNewExp(0);
        event.setNewLevel(0);
        event.setDroppedExp(0);
        event.getDrops().clear();
        event.setDeathMessage(null);

        game.respawn(event.getEntity(), true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // This should not happen, but just to be safe...
        if (game.isParticipant(event.getPlayer())) {
            game.respawn(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onDamageWhileInSpawn(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        BattleTeam team = game.getTeamManager().getTeam(player);

        if (team.isInSpawn(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageSameTeam(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player damaged && event.getDamager() instanceof Player damager) {
            BattleTeam damagedTeam = game.getTeamManager().getTeam(damaged);
            BattleTeam damagerTeam = game.getTeamManager().getTeam(damager);

            if (damagedTeam == damagerTeam) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void passiveHealInSpawn(TickEvent event) {
        game.getParticipants().forEach(p -> {
            BattleTeam team = game.getTeamManager().getTeam(p);
            if (team != null && team.isInSpawn(p) && event.getTick() % 20 == 0) {
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1));
            }
        });
    }

    @EventHandler
    public void killInEnemySpawn(PlayerEnterSpawnEvent event) {
        if (game.getTeamManager().getTeam(event.getPlayer()) != event.getTeam()) {
            game.respawn(event.getPlayer(), true);
        }
    }

    @EventHandler
    public void loseFlagInSpawn(PlayerEnterSpawnEvent event) {
        Optional<BattleTeam> carryingFlag = game.getTeamManager().getTeams().stream()
                .filter(bt -> bt.getFlag().getCarrier() == event.getPlayer())
                .findAny();
        carryingFlag.ifPresent(battleTeam -> battleTeam.getFlagManager().restore());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCapture(FlagCaptureEvent event) {
        if (game.getWinner() != null) {
            game.setState(BattleGameState.AFTER);
        }
    }

    @EventHandler
    public void onPlayerKillPlayer(PlayerKilledByPlayerEvent event) {
        System.out.printf("%s killed by %s%n", event.getKilled(), event.getKiller());
        game.editStats(event.getKiller(), s -> {
            s.setKills(s.getKills());
            s.setStreak(s.getStreak() + 1);
        });
    }

}
