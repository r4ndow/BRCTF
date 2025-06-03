package com.mcpvp.battle.game;

import lombok.Data;

@Data
public class BattleGamePlayerStats {

    private int kills;
    private int deaths;
    private int streak;
    private int bestStreak;
    private int steals;
    private int captures;
    private int recovers;

}
