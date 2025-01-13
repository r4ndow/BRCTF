package com.mcpvp.battle.scoreboard;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.game.BattleGameState;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.util.C;
import com.mcpvp.battle.util.ScoreboardUtil;
import com.mcpvp.common.EasyLifecycle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class BattleScoreboardManager extends EasyLifecycle {
	
	private final BattlePlugin plugin;
	private final Battle battle;
	
	public void init() {
		attach(new BattleScoreboardListener(plugin, battle, this));
	}
	
	public Scoreboard create() {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective objective = scoreboard.registerNewObjective("score", "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(getTitle());
		
		for (BattleTeam bt : battle.getGame().getTeamManager().getTeams()) {
			createTeam(scoreboard, bt);
		}
		
		return scoreboard;
	}
	
	public void setTeam(Player player, BattleTeam bt) {
		for (Scoreboard sb : getAllScoreboards()) {
			getScoreboardTeam(sb, bt).addEntry(player.getName());
		}
	}

	public void refresh(Player player) {
		List<String> scores = getScores(player);
		ScoreboardUtil.resetChanged(player.getScoreboard(), scores);
		ScoreboardUtil.addLargeScores(player.getScoreboard(), player.getScoreboard().getObjective(DisplaySlot.SIDEBAR), scores);
		player.getScoreboard().getObjective(DisplaySlot.SIDEBAR).setDisplayName(getTitle());
	}
	
	private List<Scoreboard> getAllScoreboards() {
		return Bukkit.getOnlinePlayers().stream().map(Player::getScoreboard).toList();
	}
	
	private void createTeam(Scoreboard scoreboard, BattleTeam bt) {
		// Register the team
		Team team = scoreboard.registerNewTeam(getTeamName(bt));
		team.setAllowFriendlyFire(true);
		team.setPrefix(bt.getColor().toString());
		
		// Add all players to the team. From here the join and quit
		// events are responsible for updating team members
		for (Player player : Bukkit.getOnlinePlayers())
			if (bt.contains(player))
				team.addEntry(player.getName());
	}
	
	private Team getScoreboardTeam(Scoreboard scoreboard, BattleTeam bt) {
		return scoreboard.getTeam(getTeamName(bt));
	}
	
	private String getTeamName(BattleTeam bt) {
		return "team_" + bt.getId();
	}
	
	private List<String> getScores(Player player) {
        ScoreboardUtil.setMaxLength(48);
		List<String> scores = new ArrayList<>();
		
		BattleGameState state = battle.getGame().getState();
		if (state == null) {
			return scores;
		}

		switch (state) {
			case BEFORE -> {
				scores.addAll(getMapDetails());
			}
			case DURING -> {
				// Add scores for the player's team first
				BattleTeam team = battle.getGame().getTeamManager().getTeam(player);
				if (team != null) {
					scores.addAll(getScoresForTeam(player, team));
				}

				// Add scores for all other teams
				battle.getGame().getTeamManager().getTeams().forEach(bt -> {
					if (bt != team) {
						scores.addAll(getScoresForTeam(player, bt));
					}
				});
			}
		}
		
		return scores;
	}

	private List<String> getMapDetails() {
		BattleMapData map = battle.getGame().getMap();
        List<String> scores = new ArrayList<>();

		String[] authors = {"?"};
		if (map.getAuthor() != null)
			authors = map.getAuthor().split(", ");
		
		scores.addAll(ScoreboardUtil.wrap(" " + C.WHITE + C.B + "Players"));
		scores.add(ScoreboardUtil.autoSize("  " + C.GREEN + "Online", C.R + battle.getGame().getParticipants().size()));
		scores.add("  ");
		
		scores.addAll(ScoreboardUtil.wrap(" " + C.WHITE + C.B + "Map"));
		scores.add(ScoreboardUtil.autoSize("  " + C.GREEN + "Name", C.R + map.getName()));
		
		if (authors.length > 1) {
			scores.add("  " + C.GREEN + "By");
			for (String author : authors) {
				scores.add("   " + author);
			}
		} else {
			scores.add(ScoreboardUtil.autoSize("  " + C.GREEN + "By", C.R + map.getAuthor()));
		}

		return scores;
	}

    private List<String> getScoresForTeam(Player player, BattleTeam team) {
		boolean sameTeam = battle.getGame().getTeamManager().getTeam(player) == team;
        List<String> scores = new ArrayList<>();

        if (sameTeam) {
            scores.add(" " + C.B + team.getName() + C.WHITE + " - Your Team");
		} else {
			scores.add(" " + C.B + team.getName());
		}
        
        scores.add(ScoreboardUtil.autoSize("  " + team.getColor() + "Captures" + C.R, team.getCaptures() + "/" + battle.getGame().getConfig().getCaps(), "  Caps"));
        
        List<String> location = getFlagLoc(team);
        
        scores.add("  " + team.getColor() + "Flag " + C.R + location.get(0));
        
        if (location.size() > 1) {
            scores.add(C.GRAY + "  * " + location.get(1));
        } else { // Add a unique space
            scores.add(ChatColor.COLOR_CHAR + "" + team.getName().charAt(0) + ChatColor.RESET);
		}

        return scores;
    }

	private List<String> getFlagLoc(BattleTeam t) {
        List<String> loc = new ArrayList<>();

        if (t.getFlag().isHome()) {
            loc.add("Home");
            return loc;
        }

        if (t.getFlag().getCarrier() != null) {
            loc.add("Taken");
            loc.add("Held by " + t.getColor().CHAT + t.getFlag().getCarrier().getName());
        }

        if (t.getFlag().isDropped()) {
            long timeLeft = t.getFlag().getRestoreExpiration().getRemaining().seconds();
            String timer = Math.max(0, timeLeft) + "s";

            loc.add("Dropped");
            loc.add("Resets in " + timer);
        }

        return loc;
    }


	private String getTitle() {
        String display = "[" + (battle.getMatch().getCurrentGameIndex() + 1) + "/" + battle.getMatch().getGames().size() + "] ";
        String timer = formatDuration(Duration.ofSeconds((long) battle.getMatch().getTimer().getSeconds()));
        
		if (battle.getGame().getState() == null) {
			return "???";
		}

        switch (battle.getGame().getState()) {
            case BEFORE -> {
				display += "Starts in " + timer;
            }
            case DURING -> {
				display += "Ends in " + timer;
            }
            case AFTER -> {
				display += "Game over " + timer;
            }
        }

        return display;
    }
	
	private String formatDuration(Duration duration) {
		return String.format("%02d:%02d",
			duration.toMinutesPart(), 
			duration.toSecondsPart()
		);
	}

	
}
