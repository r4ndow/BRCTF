package com.mcpvp.battle.game;

import javax.annotation.Nullable;

public enum BattleGameState {
	BEFORE,
	DURING,
	AFTER;

	@Nullable
	public BattleGameState getNext() {
		return switch (this) {
			case BEFORE -> DURING;
			case DURING -> AFTER;
			case AFTER -> null;
		};
	}

}
