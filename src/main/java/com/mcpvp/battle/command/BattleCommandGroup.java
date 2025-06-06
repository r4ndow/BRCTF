package com.mcpvp.battle.command;

import com.mcpvp.common.command.EasyCommandGroup;

import java.util.List;

public class BattleCommandGroup extends EasyCommandGroup {

    public BattleCommandGroup(String name) {
        super(name);
    }

    public BattleCommandGroup(String name, List<String> aliases) {
        super(name, aliases);
    }

    @Override
    protected String getFallbackPrefix() {
        return "mcctf";
    }
}
