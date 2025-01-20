package com.mcpvp.battle.flag;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.*;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.Optional;

/**
 * Listens for flag interactions and calls {@link FlagManager}.
 */
@Getter
@RequiredArgsConstructor
public class FlagListener implements EasyListener {

    private static final Double FLAG_DIST = 1.5;

    private final BattlePlugin plugin;
    private final BattleGame game;

    @EventHandler
    public void onTick(TickEvent event) {
        game.getTeamManager().getTeams().forEach(bt -> {
            IBattleFlag flag = bt.getFlag();
            if (flag.isDropped() && flag.getRestoreExpiration().isExpired()) {
                bt.getFlagManager().restore();
            }

            flag.onTick(event.getTick());
        });

        for (Player player : game.getParticipants()) {
            BattleTeam team = game.getTeamManager().getTeam(player);
            if (team == null) {
                continue;
            }

            for (BattleTeam bt : game.getTeamManager().getTeams()) {
                if (team == bt || !bt.getFlag().isHome()) {
                    continue;
                }

                if (player.getLocation().distance(bt.getFlag().getLocation()) > FLAG_DIST) {
                    // If they've strayed from the flag, make sure they're not considered to be stealing
                    bt.getFlagManager().stopStealAttempt(player);
                }
            }
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        BattleTeam team = game.getTeamManager().getTeam(event.getPlayer());
        game.getTeamManager().getTeams().forEach(bt -> {
            if (!bt.getFlag().isItem(event.getItem().getItemStack())) {
                return;
            }

            // Always cancel picking up the item for simplicity
            event.setCancelled(true);

            if (bt.getFlag().getCarrier() != null) {
                return;
            }

            if (bt != team) {
                if (bt.getFlag().isHome()) {
                    bt.getFlagManager().attemptSteal(event.getPlayer());
                } else {
                    if (bt.getFlag().getPickupExpiration().isExpired()) {
                        bt.getFlagManager().pickup(event.getPlayer());
                    }
                }
            } else if (!bt.getFlag().isHome()) {
                bt.getFlagManager().recover(event.getPlayer());
            }
        });
    }

    @EventHandler
    public void onCapture(PlayerPickupItemEvent event) {
        if (game.getTeamManager().getTeams().stream()
                .noneMatch(bt -> bt.getFlag().isItem(event.getItem().getItemStack()))) {
            return;
        }

        BattleTeam team = game.getTeamManager().getTeam(event.getPlayer());
        Optional<BattleTeam> carried = game.getTeamManager().getTeams().stream()
                .filter(bt -> bt.getFlag().getCarrier() == event.getPlayer())
                .findFirst();

        if (!team.getFlag().isItem(event.getItem().getItemStack())) {
            return;
        }

        if (carried.isEmpty()) {
            return;
        }

        if (!team.getFlag().isHome()) {
            return;
        }

        carried.get().getFlagManager().capture(event.getPlayer(), team);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        game.getTeamManager().getTeams().forEach(bt -> {
            if (bt.getFlag().isItem(event.getItemDrop().getItemStack())) {
                bt.getFlagManager().drop(event.getPlayer(), event.getItemDrop());
            }
        });
    }

    @EventHandler
    public void onTakeLocked(FlagTakeEvent event) {
        if (event.getFlag().isLocked()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemMerge(ItemMergeEvent event) {
        game.getTeamManager().getTeams().forEach(bt -> {
            if (bt.getFlag().isItem(event.getEntity().getItemStack())) {
                event.setCancelled(true);
            }
        });

        // For the visuals
        if (event.getEntity().getItemStack().getType() == Material.WOOL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickupVisuals(PlayerPickupItemEvent event) {
        // For the visuals
        if (event.getItem().getItemStack().getType() == Material.WOOL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        game.getTeamManager().getTeams().forEach(bt -> {
            if (bt.getFlag().isItem(event.getEntity().getItemStack())) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onResign(PlayerResignEvent event) {
        game.getTeamManager().getTeams().forEach(bt -> {
            if (bt.getFlag().getCarrier() == event.getPlayer()) {
                bt.getFlagManager().drop(event.getPlayer(), null);
            }
        });
    }

}
