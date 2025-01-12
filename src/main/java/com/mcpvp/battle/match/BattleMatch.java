package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameState;
import com.mcpvp.battle.team.BattleTeam;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
		return games.get(currentGameIndex);
	}

	public void start() {
		new BattleMatchListener(plugin, battle).register();

		Bukkit.getScheduler().runTaskTimer(plugin, getTimerTask(), 0, 20);

		getCurrentGame().setup();
	}

	/**
	 * Advances to the next game in the match.
	 */
	private void advanceGame() {
		// Shut down the current game
		BattleGame current = getCurrentGame();
		current.stop();

		// Preserve team IDs
		Map<BattleTeam, Set<Player>> playerMap = current.getTeamManager().getPlayerMap();

		// Start the next game
		if (currentGameIndex + 1 == games.size()) {
			Bukkit.broadcastMessage("All done!");
			Bukkit.shutdown();
		} else {
			BattleGame next = games.get(++currentGameIndex);
			next.setup();

			// Move everyone to their proper teams
			playerMap.forEach((oldTeam, players) -> {
				BattleTeam nextTeam = next.getTeamManager().getTeam(oldTeam.getId());
				players.forEach(player -> {
					next.getTeamManager().setTeam(player, nextTeam);
				});
			});
		}
	}

	/**
	 * Advance the state of the current game, or proceed to the next game entirely.
	 */
	public void advanceStateOrGame() {
		BattleGameState state = getCurrentGame().getState();
		if (state == null) {
			throw new IllegalStateException("Game state was null");
		}

		if (state.getNext() != null) {
			getCurrentGame().setState(state.getNext());
		} else {
			// Advance to the next game
			advanceGame();
		}
	}

	/**
	 * @return A task that runs every second to advanve the game timer, or proceed to the
	 * next game in the match.
	 */
	private Runnable getTimerTask() {
		return () -> {
			if (timer.isPaused()) {
				return;
			}

			if (timer.getSeconds() == 0) {
				advanceStateOrGame();
			} else {
				timer.setSeconds(timer.getSeconds() - 1);
			}
		};
	}

}
