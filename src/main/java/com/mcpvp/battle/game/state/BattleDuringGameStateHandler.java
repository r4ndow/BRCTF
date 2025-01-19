package com.mcpvp.battle.game.state;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagCaptureEvent;
import com.mcpvp.battle.event.PlayerJoinTeamEvent;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.flag.*;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameState;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.kit.KitSelectedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class BattleDuringGameStateHandler extends BattleGameStateHandler {

    public BattleDuringGameStateHandler(BattlePlugin plugin, BattleGame game) {
        super(plugin, game);
    }

    @Override
    public void enterState() {
        super.enterState();

        attach(new FlagStealMonitor(plugin, game.getBattle(), game));
        attach(new FlagDropMonitor(plugin, game.getBattle(), game));
        attach(new FlagPickupMonitor(plugin, game.getBattle(), game));
        attach(new FlagRecoverMonitor(plugin, game.getBattle(), game));
        attach(new FlagCaptureMonitor(plugin, game.getBattle(), game));

        game.getTeamManager().getTeams().forEach(bt -> {
            bt.getFlag().setLocked(false);
        });

        game.getBattle().getMatch().getTimer().setSeconds(game.getConfig().getTime() * 60);
    }

    @Override
    public void leaveState() {
        super.leaveState();
        Bukkit.broadcastMessage("Game over!");
    }

    @EventHandler
    public void onJoinTeam(PlayerJoinTeamEvent event) {
        game.respawn(event.getPlayer());
    }

    @EventHandler
    public void onParticipate(PlayerParticipateEvent event) {
        game.respawn(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKitSelected(KitSelectedEvent event) {
        game.respawn(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && player.getHealth() - event.getFinalDamage() <= 0) {
            game.respawn(player);

            // Canceling the event causes no damage splat animation
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onActualDeath(PlayerDeathEvent event) {
        // This should never happen, but just to be safe...
        game.respawn(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (game.isParticipant(event.getPlayer())) {
            game.respawn(event.getPlayer());
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCapture(FlagCaptureEvent event) {
        // Game over, the team won!
        if (event.getPlayerTeam().getCaptures() == game.getConfig().getCaps()) {
            game.setState(BattleGameState.AFTER);
        }
    }

}
