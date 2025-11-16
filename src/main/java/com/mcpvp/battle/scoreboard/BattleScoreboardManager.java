package com.mcpvp.battle.scoreboard;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.game.BattleGamePlayerStats;
import com.mcpvp.battle.game.BattleGameState;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.util.ScoreboardUtil;
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
        this.attach(new BattleScoreboardListener(this.plugin, this.battle, this));
    }

    /**
     * @return A new Scoreboard instance with every team present.
     */
    public Scoreboard create() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("score", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(this.getTitle());

        for (BattleTeam bt : this.battle.getGame().getTeamManager().getTeams()) {
            this.createTeam(scoreboard, bt);
        }

        return scoreboard;
    }

    /**
     * Updates the team of the given player on all scoreboards.
     *
     * @param player The player to set the team for.
     * @param team   The team to assign the player.
     */
    public void setTeam(Player player, BattleTeam team) {
        for (Scoreboard sb : this.getAllScoreboards()) {
            this.getScoreboardTeam(sb, team).addEntry(player.getName());
        }
    }

    /**
     * Refreshes the given player's scoreboard entries, such as updating the sidebar.
     * This should be called whenever their scoreboard could be out of date.
     *
     * @param player The player to update the scoreboard for.
     */
    public void refresh(Player player) {
        List<String> scores = this.getScores(player);
        Objective objective = player.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
        ScoreboardUtil.resetChanged(player.getScoreboard(), scores);
        ScoreboardUtil.addLargeScores(player.getScoreboard(), objective, scores);
        objective.setDisplayName(this.getTitle());
    }

    /**
     * @return A list of all scoreboards. Ideally, all of these Scoreboards were created by
     * using the {@link #create()} method.
     */
    private List<Scoreboard> getAllScoreboards() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getScoreboard).toList();
    }

    /**
     * Initializes the given team on the given scoreboard. Configures all players
     * who are on the team as well.
     *
     * @param scoreboard The scoreboard to adjust.
     * @param battleTeam The team to add.
     */
    private void createTeam(Scoreboard scoreboard, BattleTeam battleTeam) {
        // Register the team
        Team team = scoreboard.registerNewTeam(this.getTeamName(battleTeam));
        team.setAllowFriendlyFire(true);
        team.setPrefix(battleTeam.getColor().toString());

        // Add all players to the team. From here the join and quit
        // events are responsible for updating team members
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (battleTeam.contains(player)) {
                team.addEntry(player.getName());
            }
        }
    }

    /**
     * Retrieves the equivalent Scoreboard team representation for the given battle team.
     *
     * @param scoreboard The scoreboard to get the team on.
     * @param bt         The battle team to get the equivalent team for.
     * @return The equivalent team, or null if not found.
     */
    private Team getScoreboardTeam(Scoreboard scoreboard, BattleTeam bt) {
        return scoreboard.getTeam(this.getTeamName(bt));
    }

    /**
     * @param bt The team to get the name for.
     * @return The name of the scoreboard Team.
     */
    private String getTeamName(BattleTeam bt) {
        return "team_" + bt.getId();
    }

    /**
     * Generates all sidebar scores for a given Player.
     *
     * @param player The player to generate scores for.
     * @return A list of all the entries to show on the sidebar for the player.
     */
    private List<String> getScores(Player player) {
        List<String> scores = new ArrayList<>();

        BattleGameState state = this.battle.getGame().getState();
        if (state == null) {
            return scores;
        }

        switch (state) {
            case BEFORE -> scores.addAll(this.getMapDetails());
            case DURING -> {
                // Add scores for the player's team first
                BattleTeam team = this.battle.getGame().getTeamManager().getTeam(player);
                if (team != null) {
                    scores.addAll(this.getScoresForTeam(player, team));
                }

                // Add scores for all other teams
                this.battle.getGame().getTeamManager().getTeams().forEach(bt -> {
                    if (bt != team) {
                        scores.addAll(this.getScoresForTeam(player, bt));
                    }
                });

                // Then add the stats of the player, e.g. kills and deaths
                scores.addAll(this.getPlayerStats(player));
            }
            case AFTER -> scores.addAll(this.getPlayerStats(player));
        }

        return scores;
    }

    /**
     * @return Map data to be shown in the pre-game
     */
    private List<String> getMapDetails() {
        BattleMapData map = this.battle.getGame().getMap();
        List<String> scores = new ArrayList<>();

        String[] authors = {"?"};
        if (map.getAuthor() != null)
            authors = map.getAuthor().split(", ");

        scores.addAll(ScoreboardUtil.wrap(" " + C.WHITE + C.B + "Players"));
        scores.add(ScoreboardUtil.autoSize("  " + C.GREEN + "Online", C.R + this.battle.getGame().getParticipants().size()));
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

    /**
     * Team specific scores.
     *
     * @param player The player who will see the scores. Needed to show the current team.
     * @param team   The team to generate scores for.
     * @return A list of the team specific scores, customized for the given player.
     */
    private List<String> getScoresForTeam(Player player, BattleTeam team) {
        boolean sameTeam = this.battle.getGame().getTeamManager().getTeam(player) == team;
        List<String> scores = new ArrayList<>();

        if (sameTeam) {
            scores.add(" " + C.B + team.getName() + C.WHITE + " - Your Team");
        } else {
            scores.add(" " + C.B + team.getName());
        }

        scores.add(ScoreboardUtil.autoSize("  " + team.getColor() + "Captures" + C.R, team.getCaptures() + "/" + this.battle.getGame().getConfig().getCaps(), "  Caps"));

        List<String> location = this.getFlagLoc(team);

        scores.add("  " + team.getColor() + "Flag " + C.R + location.get(0));

        if (location.size() > 1) {
            scores.add(C.GRAY + "  * " + location.get(1));
        } else { // Add a unique space
            scores.add(ChatColor.COLOR_CHAR + "" + team.getName().charAt(0) + ChatColor.RESET);
        }

        return scores;
    }

    /**
     * @param team The team to get the flag scores for.
     * @return A list of nicely formatted scores representing the current location
     * of the team's flag.
     */
    private List<String> getFlagLoc(BattleTeam team) {
        List<String> loc = new ArrayList<>();

        if (team.getFlag().isHome()) {
            loc.add("Home");
            return loc;
        }

        if (team.getFlag().getCarrier() != null) {
            BattleTeam heldTeam = this.battle.getGame().getTeamManager().getTeam(team.getFlag().getCarrier());
            loc.add("Taken");
            loc.add("Held by " + heldTeam.getColor().getChat() + team.getFlag().getCarrier().getName());
        }

        if (team.getFlag().isDropped()) {
            long timeLeft = team.getFlag().getRestoreExpiration().getRemaining().seconds();
            String timer = Math.max(0, timeLeft) + "s";

            loc.add("Dropped");
            loc.add("Resets in " + timer);
        }

        return loc;
    }

    /**
     * @return The title of the scoreboard.
     */
    private String getTitle() {
        String display = "[" + (this.battle.getMatch().getCurrentGameIndex() + 1) + "/" + this.battle.getMatch().getGames().size() + "] ";
        String timer = this.formatDuration(Duration.ofSeconds(this.battle.getMatch().getTimer().getSeconds()));

        if (this.battle.getMatch().getTimer().isPaused()) {
            timer += "*";
        }

        if (this.battle.getGame().getState() == null) {
            return "???";
        }

        switch (this.battle.getGame().getState()) {
            case BEFORE -> display += "Starts in " + timer;
            case DURING -> display += "Ends in " + timer;
            case AFTER -> {
                if (this.battle.getMatch().getCurrentGameIndex() + 1 == this.battle.getMatch().getGames().size()) {
                    return "Match over! Restart in " + timer;
                }
                display += "Next map in " + timer;
            }
        }

        return display;
    }

    /**
     * @param player The player to get stats for.
     * @return A list of scores for the player's stats.
     */
    private List<String> getPlayerStats(Player player) {
        List<String> scores = new ArrayList<>();
        BattleGamePlayerStats stats = this.battle.getGame().getStats(player);

        scores.addAll(ScoreboardUtil.wrap(" " + C.RESET + C.B + "Your Stats"));
        scores.addAll(ScoreboardUtil.wrap("  " + C.GOLD + "Kills " + C.R + stats.getKills()));
        scores.addAll(ScoreboardUtil.wrap("  " + C.GOLD + "Deaths " + C.R + stats.getDeaths()));

        if (this.battle.getGame().getState() == BattleGameState.DURING) {
            scores.addAll(ScoreboardUtil.wrap("  " + C.GOLD + "Streak " + C.R + stats.getStreak()));
        } else if (this.battle.getGame().getState() == BattleGameState.AFTER) {
            scores.addAll(ScoreboardUtil.wrap("  " + C.GOLD + "Best Streak " + C.R + stats.getBestStreak()));
        }

        scores.addAll(ScoreboardUtil.wrap("  " + C.GOLD + "Recoveries " + C.R + stats.getRecovers()));
        scores.addAll(ScoreboardUtil.wrap("  " + C.GOLD + "Captures " + C.R + stats.getCaptures()));

        return scores;
    }

    private String formatDuration(Duration duration) {
        return String.format("%02d:%02d",
            duration.toMinutesPart(),
            duration.toSecondsPart()
        );
    }

}
