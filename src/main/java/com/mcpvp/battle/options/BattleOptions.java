package com.mcpvp.battle.options;

import com.mcpvp.battle.BattlePlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;

@Getter
@AllArgsConstructor
public class BattleOptions {

    /**
     * Most options can be read straight from the input file, so we use delegate to make those available.
     */
    @Delegate
    private final BattleOptionsInput input;
    private final BattlePlugin plugin;

}
