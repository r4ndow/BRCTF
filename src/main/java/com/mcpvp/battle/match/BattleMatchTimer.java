package com.mcpvp.battle.match;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BattleMatchTimer {

    private int seconds = 60;
    private boolean paused = true;

}
