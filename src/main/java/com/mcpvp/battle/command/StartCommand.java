package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.game.BattleGameState;
import org.bukkit.command.CommandSender;

import java.util.List;

public class StartCommand extends BattleCommand {

    private final Battle battle;

    public StartCommand(Battle battle) {
        super("start");
        this.battle = battle;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        battle.getGame().setState(BattleGameState.DURING);
        return true;
    }
}
