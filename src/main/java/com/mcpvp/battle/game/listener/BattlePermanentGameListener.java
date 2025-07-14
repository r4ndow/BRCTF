package com.mcpvp.battle.game.listener;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerEnterSpawnEvent;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.event.PlayerResignEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.kit.BattleKitManager;
import com.mcpvp.battle.kit.BattleKitType;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.kit.KitAttemptSelectEvent;
import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.util.movement.SpongeUtil;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.Optional;

@Log4j2
@Getter
@AllArgsConstructor
public class BattlePermanentGameListener implements EasyListener {

    private final BattlePlugin plugin;
    private final BattleGame game;

    // ===============================
    //  EVENT CREATION AND FORWARDING
    // ===============================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Ensure that every join results in either a participate or resign event
        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            new PlayerParticipateEvent(event.getPlayer(), game).call();
        } else {
            new PlayerResignEvent(event.getPlayer(), game).call();
        }

        // No matter what, they should be in the world of the current game
        if (event.getPlayer().getWorld() != game.getWorld()) {
            event.getPlayer().teleport(game.getConfig().getSpawn());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        new PlayerResignEvent(event.getPlayer(), game).call();
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        new PlayerResignEvent(event.getPlayer(), game).call();
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        GameMode prev = event.getPlayer().getGameMode();
        GameMode next = event.getNewGameMode();

        if (prev == GameMode.SURVIVAL) {
            // Switching out of survival, probably to spectate
            new PlayerResignEvent(event.getPlayer(), game).call();
        } else if (next == GameMode.SURVIVAL) {
            // Switching into survival, probably to play
            new PlayerParticipateEvent(event.getPlayer(), game).call();
        }
    }

    @EventHandler
    public void onMoveIntoSpawn(PlayerMoveEvent event) {
        if (!game.isParticipant(event.getPlayer())) {
            return;
        }

        game.getTeamManager().getTeams().forEach(bt -> {
            if (!bt.isInSpawn(event.getFrom()) && bt.isInSpawn(event.getTo())) {
                new PlayerEnterSpawnEvent(event.getPlayer(), bt, event).call();
            }
        });
    }

    @EventHandler
    public void onTeleportIntoSpawn(PlayerTeleportEvent event) {
        if (!game.isParticipant(event.getPlayer())) {
            return;
        }

        game.getTeamManager().getTeams().forEach(bt -> {
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
                    new PlayerKilledByPlayerEvent(event.getEntity(), damager).call();
                }
            }
        }
    }

    // ==============
    // EVENT HANDLING
    // ==============

    @EventHandler(priority = EventPriority.LOW)
    public void selectAutoTeam(PlayerParticipateEvent event) {
        if (game.getTeamManager().getTeam(event.getPlayer()) == null) {
            BattleTeam toJoin = game.getTeamManager().selectAutoTeam();
            game.getTeamManager().setTeam(event.getPlayer(), toJoin);
        }
    }

    @EventHandler
    public void selectDefaultKit(PlayerParticipateEvent event) {
        // For players who join without a kit selected, make sure they have one before they are respawned
        // The kit creation/equipping will be handled by the game
        KitDefinition selected = game.getBattle().getKitManager().getSelected(event.getPlayer());
        if (selected == null) {
            game.getBattle().getKitManager().setSelected(event.getPlayer(), BattleKitType.HEAVY, true);
        }
    }

    @EventHandler
    public void onResign(PlayerResignEvent event) {
        game.remove(event.getPlayer());
    }

    @EventHandler
    public void onKitSelectAttempt(KitAttemptSelectEvent event) {
        BattleKitManager kitManager = game.getBattle().getKitManager();
        if (kitManager.isDisabled(event.getKitDefinition())) {
            event.deny("This class is disabled");
        }

        Optional<Integer> limit = kitManager.getLimit(event.getKitDefinition());
        if (limit.isPresent()) {
            BattleTeam team = game.getTeamManager().getTeam(event.getPlayer());
            long selected = team.getPlayers().stream().filter(player -> {
                return game.getBattle().getKitManager().isSelected(player, event.getKitDefinition());
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
                SpongeUtil.launch(plugin, player, under);
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
        if (game.getTeamManager().getTeams().stream().noneMatch(bt ->
                bt.getFlag().isItem(event.getItemDrop().getItemStack())
        )) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (game.isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (game.isParticipant(event.getPlayer()) && event.getBlockPlaced() != null) {
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
        if (event.getPlayer() instanceof Player p && game.isParticipant(p)) {
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

}
