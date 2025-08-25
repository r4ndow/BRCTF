package com.mcpvp.battle.game.state;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.*;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameState;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitSelectedEvent;
import com.mcpvp.common.util.chat.C;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;
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
        // Shut down all kits
        game.getParticipants().forEach(player -> {
            Optional.ofNullable(game.getBattle().getKitManager().get(player)).ifPresent(Kit::shutdown);
        });

        super.leaveState();

        // Send a summary message
        List<String> summary = new ArrayList<>();
        summary.add(" ");
        summary.add(C.YELLOW + "✦ " + C.GOLD + "✦ " + C.b(C.R) + "GAME SUMMARY" + C.YELLOW + " ✦" + C.GOLD + " ✦");

        BattleTeam winner = game.getWinner();
        if (winner == null) {
            summary.add(C.info(C.GOLD) + "Nobody won!");
        } else {
            BattleTeam loser = game.getTeamManager().getNext(winner);
            summary.add("%sWinner: %s (%s - %s)".formatted(
                C.info(C.GOLD), winner.getColoredName(), winner.getColor().toString() + winner.getCaptures(), loser.getColor().toString() + loser.getCaptures()
            ));
        }

        summary.forEach(Bukkit::broadcastMessage);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.setKeepInventory(false);
        event.setKeepLevel(false);
        event.setNewExp(0);
        event.setNewLevel(0);
        event.setDroppedExp(0);
        event.getDrops().clear();

        if (new GameDeathEvent(event.getEntity(), event.getEntity().getLocation().clone(), event).call()) {
            event.setDeathMessage(null);
            return;
        }

        game.respawn(event.getEntity(), true);
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
    public void onRespawn(PlayerRespawnEvent event) {
        // This should not happen, but just to be safe...
        if (game.isParticipant(event.getPlayer())) {
            game.respawn(event.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageWhileInSpawn(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        BattleTeam team = game.getTeamManager().getTeam(player);

        if (team.isInSpawn(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageSameTeam(EntityDamageByEntityEvent event) {
        BattleTeam damagerTeam = null;
        BattleTeam damagedTeam = null;

        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                damagerTeam = game.getTeamManager().getTeam(shooter);
            }
        }

        if (event.getDamager() instanceof Player damager) {
            damagerTeam = game.getTeamManager().getTeam(damager);
        }

        if (event.getEntity() instanceof Player damaged) {
            damagedTeam = game.getTeamManager().getTeam(damaged);
        }

        if (damagedTeam == damagerTeam) {
            event.setCancelled(true);
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
            // If this is caused by teleporting, we need to cancel the teleport
            // Otherwise the player will be respawned, then the teleport will go through
            if (event.getCause() instanceof PlayerTeleportEvent) {
                event.getCause().setCancelled(true);
            }

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
        game.editStats(event.getKiller(), s -> {
            s.setKills(s.getKills() + 1);
            s.setStreak(s.getStreak() + 1);
        });
    }

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Item) {
            event.setCancelled(true);
        }
    }

}
