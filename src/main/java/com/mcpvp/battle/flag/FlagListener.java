package com.mcpvp.battle.flag;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagPoisonEvent;
import com.mcpvp.battle.event.FlagTakeEvent;
import com.mcpvp.battle.event.PlayerResignEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.options.BattleOptionsInput;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.shape.Cuboid;
import com.mcpvp.common.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

@Getter
@RequiredArgsConstructor
public class FlagListener implements EasyListener {

    private final BattlePlugin plugin;
    private final BattleGame game;

    @EventHandler
    public void onTick(TickEvent event) {
        this.processRestoration(event);
        this.resetStealTimers();

        this.processSteals();
    }

    private void processSteals() {
        this.game.getTeamManager().getTeams().forEach(flagTeam -> {
            BattleFlag flag = flagTeam.getFlag();
            Cuboid homeCaptureArea = this.getCaptureArea(flag.getHome());

            this.game.getParticipants()
                    .stream()
                    .filter(player -> {
                        if (flag.isHome()) {
                            return homeCaptureArea.contains(player.getLocation());
                        } else {
                            return player.getLocation().distance(flag.getLocation()) <= 1.5;
                        }
                    })
                    .filter(player ->
                            !this.game.getTeamManager().getTeam(player).isInSpawn(player))
                    .forEach(player -> {
                        BattleTeam playerTeam = this.game.getTeamManager().getTeam(player);

                        if (flagTeam != playerTeam) {
                            if (flag.isHome()) {
                                flagTeam.getFlagManager().attemptSteal(player);
                            } else if (flag.isDropped() && flag.getPickupExpiration().isExpired()) {
                                flagTeam.getFlagManager().pickup(player);
                            }
                        } else {
                            if (!flag.isHome() && flag.isDropped()) {
                                flagTeam.getFlagManager().recover(player);
                            } else if (flag.isHome()) {
                                this.game.getTeamManager().getTeams().stream()
                                        .filter(bt -> bt.getFlag().getCarrier() == player)
                                        .forEach(bt -> {
                                            bt.getFlagManager().capture(player, playerTeam);
                                        });
                            }
                        }
                    });
        });
    }

    private Cuboid getCaptureArea(Location flagHome) {
        Location corner1 = flagHome.clone().add(-1, -1, -1);
        Location corner2 = flagHome.clone().add(1, 0, 1);
        return new Cuboid(corner1, corner2);
    }

    private void dealFlagPoison(TickEvent event) {
        if (event.isInterval(Duration.seconds(15))) {
            this.game.getTeamManager().getTeams().stream().map(BattleTeam::getFlag).forEach(flag -> {
                if (flag.getCarrier() != null) {
                    if (!new FlagPoisonEvent(flag.getCarrier()).callIsCancelled()) {
                        String message = "%s%s flag poisoned you!".formatted(
                                C.warn(C.RED), flag.getTeam().getColoredName() + C.GRAY
                        );

                        flag.getCarrier().sendMessage(message);
                        flag.getCarrier().damage(3);
                    }
                }
            });
        }
    }

    private void resetStealTimers() {
        for (Player player : this.game.getParticipants()) {
            BattleTeam team = this.game.getTeamManager().getTeam(player);
            if (team == null) {
                continue;
            }

            for (BattleTeam bt : this.game.getTeamManager().getTeams()) {
                if (team == bt || !bt.getFlag().isHome()) {
                    continue;
                }

                Cuboid captureArea = this.getCaptureArea(bt.getFlag().getHome());
                if (!captureArea.contains(player.getLocation())) {

                    bt.getFlagManager().resetStealTimer(player);
                }
            }
        }
    }

    private void processRestoration(TickEvent event) {
        this.game.getTeamManager().getTeams().forEach(bt -> {
            BattleFlag flag = bt.getFlag();
            if (flag.isDropped() && flag.getRestoreExpiration().isExpired()) {
                bt.getFlagManager().restore();
            }

            flag.onTick(event.getTick());
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        this.game.getTeamManager().getTeams().forEach(bt -> {
            if (bt.getFlag().isItem(event.getItemDrop().getItemStack())) {
                bt.getFlagManager().drop(event.getPlayer(), event.getItemDrop());
            }
        });
    }

    @EventHandler
    public void onResign(PlayerResignEvent event) {
        this.game.getTeamManager().getTeams().forEach(bt -> {
            if (bt.getFlag().getCarrier() == event.getPlayer()) {
                bt.getFlagManager().drop(event.getPlayer(), null);
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
    public void onFlagInVoid(TickEvent event) {
        this.game.getTeamManager().getTeams().stream()
                .map(BattleTeam::getFlag)
                .filter(flag -> flag.getLocation().getY() < 0)
                .forEach(flag -> flag.getTeam().getFlagManager().restore());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWoolPlaceCapture(BlockPlaceEvent event) {
        if (this.game.getBattle().getOptions().getGame().getFlagType() != BattleOptionsInput.FlagType.WOOL) {
            return;
        }

        if (event.getBlock().getType() != Material.WOOL) {
            return;
        }

        Player player = event.getPlayer();
        if (!this.game.isParticipant(player)) {
            return;
        }

        BattleTeam playerTeam = this.game.getTeamManager().getTeam(player);
        if (playerTeam == null) {
            return;
        }

        BattleFlag playerFlag = playerTeam.getFlag();
        if (!playerFlag.isHome()) {
            return;
        }

        Cuboid captureArea = this.getCaptureArea(playerFlag.getHome());
        if (!captureArea.contains(event.getBlock().getLocation())) {
            return;
        }

        this.game.getTeamManager().getTeams().stream()
                .filter(bt -> bt != playerTeam)
                .filter(bt -> bt.getFlag().getCarrier() == player)
                .filter(bt -> bt.getFlag().isItem(event.getItemInHand()))
                .findFirst()
                .ifPresent(enemyTeam -> {
                    Block block = event.getBlock();
                    Location flagHome = playerFlag.getHome();

                    boolean isAtFlagLocation = block.getLocation().getBlockX() == flagHome.getBlockX() &&
                            block.getLocation().getBlockY() == flagHome.getBlockY() &&
                            block.getLocation().getBlockZ() == flagHome.getBlockZ();

                    if (!isAtFlagLocation) {
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            Material originalType = block.getType();
                            byte originalData = block.getData();

                            block.setType(Material.GLOWSTONE, false);

                            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                if (block.getType() == Material.GLOWSTONE) {
                                    block.setType(originalType, false);
                                    block.setData(originalData, false);
                                }
                            }, 40L);
                        });
                    }

                    enemyTeam.getFlagManager().capture(player, playerTeam);
                });
    }
}
