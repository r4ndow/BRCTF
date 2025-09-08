package com.mcpvp.battle.options;

import com.mcpvp.battle.BattlePlugin;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;

@Getter
@Setter
@Log4j2
public class BattleOptions {

    /**
     * Most options can be read straight from the input file, so we use delegate to make those available.
     */
    @Delegate
    private final BattleOptionsInput input;
    private final BattlePlugin plugin;

    /**
     * @param plugin The plugin which is creating the options.
     * @param input  The input, read directly from the file.
     */
    public BattleOptions(BattlePlugin plugin, BattleOptionsInput input) {
        this.plugin = plugin;
        this.input = input;
    }

}
