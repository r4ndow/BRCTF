package com.mcpvp.battle.match;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.flag.FlagListener;
import com.mcpvp.battle.game.BattleGame;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.bukkit.Bukkit;

@RequiredArgsConstructor
public class BattleMatch {

	private final BattlePlugin plugin;
	private final Battle battle;
	@Getter
	private final List<BattleGame> games;
	@Getter
	private final BattleMatchTimer timer = new BattleMatchTimer();
	private int currentGameIndex = 0;

	public BattleGame getCurrentGame() {
		return games.get(currentGameIndex);
	}

	public void start() {
		new BattleMatchListener(plugin, battle).register();
		new FlagListener(plugin, battle).register();

		Bukkit.getScheduler().runTaskTimer(plugin, getTimerTask(), 0, 20);

		getCurrentGame().setup();
	}

	private void advanceGame() {
		// Shut down the current game
		BattleGame current = getCurrentGame();
		current.stop();

		// Start the next game
		if (currentGameIndex + 1 == games.size()) {
			Bukkit.broadcastMessage("All done!");
			Bukkit.shutdown();
		} else {
			BattleGame next = games.get(++currentGameIndex);
			next.setup();
		}
	}

	private Runnable getTimerTask() {
		return () -> {
			if (timer.isPaused()) {
				return;
			}

			if (timer.getSeconds() == 0) {
				if (getCurrentGame().getState().getNext() != null) {
					getCurrentGame().setState(getCurrentGame().getState().getNext());
				} else {
					// Advance to the next game
					advanceGame();
				}
			} else {
				timer.setSeconds(timer.getSeconds() - 1);
			}
		};
	}

}
