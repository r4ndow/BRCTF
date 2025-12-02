package com.mcpvp.battle.flag;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagCaptureEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.Colors;
import com.mcpvp.common.event.EasyListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Getter
@RequiredArgsConstructor
public class FlagMatchPointNotifier implements EasyListener {

    private final BattlePlugin plugin;
    private final BattleGame game;

    @EventHandler
    public void onCapture(FlagCaptureEvent event) {
        BattleTeam scoringTeam = event.getPlayerTeam();
        if (scoringTeam == null) {
            return;
        }

        int capsToWin = this.game.getConfig().getCaps();
        int currentCaps = scoringTeam.getCaptures();

        // After this capture, is this team now on match point?
        if (capsToWin - currentCaps != 1) {
            return;
        }

        Colors teamColor = scoringTeam.getColor();

        Bukkit.getScheduler().runTaskLater(
                this.plugin,
                () -> {
                    String yourTeamMessage =
                            "§5§l» " + teamColor.getChatString() + "Your team §7needs 1 more capture to win!";
                    String enemyTeamMessage =
                            "§5§l» " + teamColor.getChatString() + "Enemy team §7needs 1 more capture to win!";

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        BattleTeam playerTeam = this.game.getTeamManager().getTeam(player);

                        if (playerTeam != null && playerTeam.equals(scoringTeam)) {
                            player.sendMessage(yourTeamMessage);
                        } else if (playerTeam != null) {
                            player.sendMessage(enemyTeamMessage);
                        } else {
                            // Treat spectators as seeing the enemy notification
                            player.sendMessage(enemyTeamMessage);
                        }

                        player.playSound(
                                player.getEyeLocation(),
                                Sound.ZOMBIE_UNFECT,
                                1.0f,
                                1.3f
                        );
                    }
                },
                20L * 6 // 6 seconds
        );
    }
}
