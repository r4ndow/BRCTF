package com.mcpvp.battle.game.state;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.event.EasyListener;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
@AllArgsConstructor
public abstract class BattleGameStateHandler extends EasyLifecycle implements EasyListener {

    protected final BattlePlugin plugin;
    protected final BattleGame game;

    public void enterState() {
        attach((EasyListener) this);
        log.info("Entering state " + this);
    }

    public void leaveState() {
        shutdown();
        log.info("Leaving state " + this);
    }

}
