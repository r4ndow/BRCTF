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
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public class FlagListener implements EasyListener {

    private final BattlePlugin plugin;
    private final BattleGame game;

    @EventHandler
    public void onTick(TickEvent event) {
        game.getTeamManager().getTeams().forEach(bt -> {
            IBattleFlag flag = bt.getFlag();
            if (flag.isDropped() && flag.getRestoreExpiration().isExpired()) {
                new FlagRestoreEvent(flag).call();
            }

            flag.onTick(event.getTick());
        });
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
                    new FlagStealEvent(event.getPlayer(), bt.getFlag()).call();
                } else {
                    if (bt.getFlag().getPickupExpiration().isExpired()) {
                        new FlagPickupEvent(event.getPlayer(), bt.getFlag()).call();
                    }
                }
            } else if (!bt.getFlag().isHome()) {
                new FlagRecoverEvent(event.getPlayer(), bt.getFlag()).call();
            }
        });
    }

    @EventHandler
    public void onCapture(PlayerPickupItemEvent event) {
        if (!game.getTeamManager().getTeams().stream().anyMatch(bt -> bt.getFlag().isItem(event.getItem().getItemStack()))) {
            return;
        }

        BattleTeam team = game.getTeamManager().getTeam(event.getPlayer());
        Optional<BattleTeam> carried = game.getTeamManager().getTeams().stream().filter(bt -> {
            return bt.getFlag().getCarrier() == event.getPlayer();
        }).findFirst();

        if (!team.getFlag().isItem(event.getItem().getItemStack())) {
            return;
        }

        if (!carried.isPresent()) {
            return;
        }

        if (!team.getFlag().isHome()) {
            return;
        }

        new FlagCaptureEvent(event.getPlayer(), team, carried.get().getFlag()).call();
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        game.getTeamManager().getTeams().forEach(bt -> {
            if (bt.getFlag().isItem(event.getItemDrop().getItemStack())) {
                new FlagDropEvent(event.getPlayer(), bt.getFlag(), event.getItemDrop()).call();
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
                new FlagDropEvent(event.getPlayer(), bt.getFlag(), null).call();
            }
        });
    }

}
