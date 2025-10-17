package com.mcpvp.battle.flag;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagPoisonEvent;
import com.mcpvp.battle.event.FlagTakeEvent;
import com.mcpvp.battle.event.PlayerResignEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;

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
        this.processRestoration(event);
        this.resetStealTimers();
        this.dealFlagPoison(event);
        this.processSteals();
    }

    private void processSteals() {
        this.game.getTeamManager().getTeams().forEach(flagTeam -> {
            BattleFlag flag = flagTeam.getFlag();

            this.game.getParticipants()
                .stream()
                .filter(player ->
                    player.getLocation().distance(flag.getLocation()) <= FLAG_DIST
                ).forEach(player -> {
                    BattleTeam playerTeam = this.game.getTeamManager().getTeam(player);

                    if (flagTeam != playerTeam) {
                        if (flag.isHome()) {
                            flagTeam.getFlagManager().attemptSteal(player);
                        } else if (flag.isDropped() && flag.getPickupExpiration().isExpired()) {
                            flagTeam.getFlagManager().pickup(player);
                        }
                    } else {
                        if (!flag.isHome()) {
                            flagTeam.getFlagManager().recover(player);
                        } else {
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

                if (player.getLocation().distance(bt.getFlag().getLocation()) > FLAG_DIST) {
                    // If they've strayed from the flag, make sure they're not considered to be stealing
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
            .filter(flag -> flag.getLocation().getY() <= 0)
            .forEach(flag -> flag.getTeam().getFlagManager().restore());
    }

}
