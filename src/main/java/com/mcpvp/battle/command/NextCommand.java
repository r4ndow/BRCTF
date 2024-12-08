package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.game.BattleGameState;
import org.bukkit.command.CommandSender;

import java.util.List;

public class NextCommand extends BattleCommand {
	
	private Battle battle;
	
	public NextCommand(Battle battle) {
		super("next");
		this.battle = battle;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, String label, List<String> args) {
		BattleGameState curr = battle.getGame().getState();
		if (curr == null) {
			return false;
		}
		BattleGameState next = BattleGameState.values()[
			curr.ordinal() + 1 % BattleGameState.values().length
			];
		battle.getGame().setState(next);
		sender.sendMessage(curr.name() + " -> " + next.name());
		return true;
	}
}
