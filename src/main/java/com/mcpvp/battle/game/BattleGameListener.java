package com.mcpvp.battle.game;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerEnterSpawnEvent;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.event.PlayerResignEvent;
import com.mcpvp.battle.kit.BattleKitManager;
import com.mcpvp.battle.kit.BattleKitType;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.kit.KitAttemptSelectEvent;
import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.movement.SpongeUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.Optional;

@Log4j2
@Getter
@AllArgsConstructor
public class BattleGameListener implements EasyListener {

    private final BattlePlugin plugin;
    private final BattleGame game;

    // ===============================
    //  EVENT CREATION AND FORWARDING
    // ===============================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Ensure that every join results in either a participate or resign event
        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            new PlayerParticipateEvent(event.getPlayer(), this.game).call();
        } else {
            new PlayerResignEvent(event.getPlayer(), this.game).call();
        }

        // No matter what, they should be in the world of the current game
        if (event.getPlayer().getWorld() != this.game.getWorld()) {
            event.getPlayer().teleport(this.game.getConfig().getSpawn());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        new PlayerResignEvent(event.getPlayer(), this.game).call();
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        new PlayerResignEvent(event.getPlayer(), this.game).call();
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        GameMode prev = event.getPlayer().getGameMode();
        GameMode next = event.getNewGameMode();

        if (prev == GameMode.SURVIVAL) {
            // Switching out of survival, probably to spectate
            new PlayerResignEvent(event.getPlayer(), this.game).call();
        } else if (next == GameMode.SURVIVAL) {
            // Switching into survival, probably to play
            new PlayerParticipateEvent(event.getPlayer(), this.game).call();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMoveIntoSpawn(PlayerMoveEvent event) {
        if (!this.game.isParticipant(event.getPlayer())) {
            return;
        }

        this.game.getTeamManager().getTeams().forEach(bt -> {
            if (!bt.isInSpawn(event.getFrom()) && bt.isInSpawn(event.getTo())) {
                new PlayerEnterSpawnEvent(event.getPlayer(), bt, event).call();
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleportIntoSpawn(PlayerTeleportEvent event) {
        if (!this.game.isParticipant(event.getPlayer())) {
            return;
        }

        this.game.getTeamManager().getTeams().forEach(bt -> {
            if (!bt.isInSpawn(event.getFrom()) && bt.isInSpawn(event.getTo())) {
                new PlayerEnterSpawnEvent(event.getPlayer(), bt, event).call();
            }
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getEntity().getLastDamageCause() != null) {
            if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent edbee) {
                if (edbee.getDamager() instanceof Player damager) {
                    new PlayerKilledByPlayerEvent(event, event.getEntity(), damager).call();
                } else if (edbee.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
                    new PlayerKilledByPlayerEvent(event, event.getEntity(), shooter).call();
                }
            }
        }
    }

    // ================
    //  EVENT HANDLING
    // ================

    @EventHandler(priority = EventPriority.LOW)
    public void selectAutoTeam(PlayerParticipateEvent event) {
        if (this.game.getTeamManager().getTeam(event.getPlayer()) == null) {
            BattleTeam toJoin = this.game.getTeamManager().selectAutoTeam();
            this.game.getTeamManager().setTeam(event.getPlayer(), toJoin);
        }
    }

    @EventHandler
    public void selectDefaultKit(PlayerParticipateEvent event) {
        // For players who join without a kit selected, make sure they have one before they are respawned
        // The kit creation/equipping will be handled by the game
        KitDefinition selected = this.game.getBattle().getKitManager().getSelected(event.getPlayer());
        if (selected == null) {
            this.game.getBattle().getKitManager().setSelected(event.getPlayer(), BattleKitType.HEAVY, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResign(PlayerResignEvent event) {
        this.game.remove(event.getPlayer());
    }

    @EventHandler
    public void onKitSelectAttempt(KitAttemptSelectEvent event) {
        BattleKitManager kitManager = this.game.getBattle().getKitManager();
        if (kitManager.isDisabled(event.getKitDefinition())) {
            event.deny("This class is disabled");
        }

        Optional<Integer> limit = kitManager.getLimit(event.getKitDefinition());
        if (limit.isPresent()) {
            BattleTeam team = this.game.getTeamManager().getTeam(event.getPlayer());
            long selected = team.getPlayers().stream().filter(player -> {
                return this.game.getBattle().getKitManager().isSelected(player, event.getKitDefinition());
            }).count();

            if (selected >= limit.get()) {
                event.deny("Only %s players can use %s".formatted(limit.get(), event.getKitDefinition().getName()));
            }
        }
    }

    @EventHandler
    public void onWalkOntoSponge(TickEvent event) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Block under = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (under.getType() == Material.SPONGE) {
                SpongeUtil.launch(this.plugin, player, under);
            }
        });
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        event.setCancelled(true);
        if (event.getEntity() instanceof Player player) {
            player.setFoodLevel(50);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        // FlagListener will handle the flag being dropped
        if (this.game.getTeamManager().getTeams().stream().noneMatch(bt ->
            bt.getFlag().isItem(event.getItemDrop().getItemStack())
        )) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (this.game.isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (this.game.isParticipant(event.getPlayer()) && event.getBlockPlaced() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRemoveArmor(InventoryClickEvent event) {
        if (event.getSlotType() == SlotType.ARMOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        // Prevent the death effect skeleton from combusting
        if (event.getEntityType() == EntityType.SKELETON) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player p && this.game.isParticipant(p)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        event.setCancelled(event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    @EventHandler
    public void onPickupArrow(PlayerPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.ARROW) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        event.setMotd(this.plugin.getBattle().getMatch().getMotd());
    }

}
