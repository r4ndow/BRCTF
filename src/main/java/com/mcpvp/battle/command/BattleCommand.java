package com.mcpvp.battle.command;

import com.mcpvp.common.command.EasyCommand;

import java.util.List;

public abstract class BattleCommand extends EasyCommand {

    protected BattleCommand(String name) {
        super(name);
    }

    protected BattleCommand(String name, List<String> aliases) {
        super(name, aliases);
    }

    @Override
    protected String getFallbackPrefix() {
        return "mcctf";
    }

}
