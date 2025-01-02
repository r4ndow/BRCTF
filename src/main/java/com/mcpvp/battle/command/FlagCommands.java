package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.util.cmd.CmdUtil;
import com.mcpvp.common.command.EasyCommand;
import com.mcpvp.common.command.EasyCommandGroup;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlagCommands extends EasyCommandGroup {

	private final Battle battle;

	public FlagCommands(Battle battle) {
		super("flag");
		this.battle = battle;
		addCommand(new FlagJumpCommand());
	}

	public List<String> matchTeam(List<String> args, int position) {
		if (args.size() < position) {
			return Collections.emptyList();
		}

		return CmdUtil.partialMatches(battle.getGame().getTeamManager().getTeams().stream()
				.filter(bt -> bt.getName().toLowerCase().contains(args.get(position)))
				.map(bt -> bt.getName().toLowerCase())
				.toList(),
				args.get(position));
	}

	public class FlagJumpCommand extends EasyCommand {

		public FlagJumpCommand() {
			super("jump");
		}

		@Override
		public boolean onCommand(CommandSender sender, String label, List<String> args) {
			battle.getGame().getTeamManager().getTeams().stream()
					.filter(bt -> bt.getName().toLowerCase().contains(args.get(0))).findAny().ifPresent(bt -> {
						((Player) sender).teleport(bt.getFlag().getLocation());
					});
			return true;
		}

		@Override
		public List<String> onTabComplete(CommandSender sender, String alias, List<String> args) {
			return matchTeam(args, args.size() - 1);
		}
	}

}
