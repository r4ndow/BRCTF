package com.mcpvp.battle.command;

import com.mcpvp.common.command.EasyCommand;

public abstract class BattleCommand extends EasyCommand {

    protected BattleCommand(String name) {
        super(name);
    }

    @Override
    protected String getFallbackPrefix() {
        return "mcctf";
    }
}
