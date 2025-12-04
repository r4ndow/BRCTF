package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.hud.WaitUpManager;
import com.mcpvp.battle.chat.BattleChatMessageHandler;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameState;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class BattleMatch {

    private final BattlePlugin plugin;
    private final Battle battle;
    @Getter
    private final List<BattleGame> games;
    @Getter
    private final BattleMatchTimer timer = new BattleMatchTimer();
    @Getter
    private int currentGameIndex = 0;

    public BattleGame getCurrentGame() {
        return this.games.get(this.currentGameIndex);
    }

    public void start() {
        new BattleChatMessageHandler(this.plugin, this.battle).register();
        new WaitUpManager(this.plugin, this.battle).register();
        BattleMatchStructureRestrictions.register(this, this.battle.getStructureManager());

        Bukkit.getScheduler().runTaskTimer(this.plugin, this.getTimerTask(), 0, 20);

        this.getCurrentGame().setup(Collections.emptyMap());
    }

    /**
     * Advances to the next game in the match.
     */
    private void advanceGame() {
        // Shut down the current game
        BattleGame current = this.getCurrentGame();
        current.stop();

        // Preserve team IDs
        Map<BattleTeam, Set<Player>> playerMap = current.getTeamManager().getPlayerMap();

        // Start the next game
        if (this.currentGameIndex + 1 == this.games.size()) {
            Bukkit.broadcastMessage("All done!");
            Bukkit.shutdown();
        } else {
            BattleGame next = this.games.get(++this.currentGameIndex);
            next.setup(playerMap);
        }
    }

    /**
     * Advance the state of the current game, or proceed to the next game entirely.
     */
    public void advanceStateOrGame() {
        BattleGameState state = this.getCurrentGame().getState();
        if (state == null) {
            throw new IllegalStateException("Game state was null");
        }

        if (state.getNext() != null) {
            this.getCurrentGame().setState(state.getNext());
        } else {
            // Advance to the next game
            this.advanceGame();
        }
    }

    public void insertNextGame(int map) {
        BattleMapData battleMapData = this.battle.getMapManager().loadMap(map);
        BattleGame game = this.battle.getGameManager().create(
            battleMapData, this.battle.getMapManager().getWorldData(battleMapData), this.games.size() + 1
        );
        this.games.add(this.currentGameIndex + 1, game);
    }

    /**
     * @return A task that runs every second to advance the game timer, or proceed to the
     * next game in the match.
     */
    private Runnable getTimerTask() {
        return () -> {
            if (this.timer.isPaused()) {
                return;
            }

            if (this.getCurrentGame().getState() == BattleGameState.BEFORE) {
                int seconds = this.timer.getSeconds();
                if (seconds == 15 || seconds == 10 || (seconds <= 5 && seconds >= 1)) {
                    String message = (C.YELLOW +"The match will start in " + C.AQUA +seconds + C.YELLOW+ " seconds!");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
                        player.playSound(player.getEyeLocation(), Sound.CLICK, 1.0f, 2.0f);
//                        if (seconds <= 5 && seconds >= 1) {
//                            player.playSound(player.getEyeLocation(), Sound.CLICK, 1.0f, 2.0f);
//                        }
                    }
                }
            }

            if (this.timer.getSeconds() == 0) {
                this.advanceStateOrGame();
            } else if (!this.getCurrentGame().getParticipants().isEmpty()) {
                this.timer.setSeconds(this.timer.getSeconds() - 1);
            }
        };
    }

    public String getMotd() {
        StringBuilder motd = new StringBuilder();

        if (this.getCurrentGame() == null) {
            motd.append("Starting up!");
            return motd.toString();
        }

        if (this.getCurrentGameIndex() == 0 && this.getCurrentGame().getState() == BattleGameState.BEFORE) {
            motd.append(C.cmd(C.YELLOW))
                .append(C.WHITE)
                .append("Starting soon! ")
                .append("\n")
                .append(C.info(C.YELLOW))
                .append(C.WHITE)
                .append("Map: ")
                .append(this.getCurrentGame().getMap().getName());
        } else {
            motd.append(C.cmd(C.GREEN))
                .append(C.WHITE)
                .append("In progress! Game ")
                .append(this.getCurrentGameIndex() + 1)
                .append(" of ")
                .append(this.getGames().size()).append(" ")
                .append("\n")
                .append(C.info(C.GREEN))
                .append(C.WHITE)
                .append("Map: ")
                .append(this.getCurrentGame().getMap().getName());
        }

        return motd.toString();
    }

}
