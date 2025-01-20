package com.mcpvp.common.util;

import com.mcpvp.common.item.ItemUtil;
import com.mcpvp.common.util.chat.C;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Some general Scoreboard utilities to help in making nifty looking
 * Scoreboards.
 *
 * @author NomNuggetNom
 */
public class ScoreboardUtil {

    public static final String SPACER = "{s}";
    private static int maxLength = 16;

    public static void setMaxLength(int max) {
        maxLength = max;
    }

    /**
     * Truncates the given entry name instead of truncating the entry value.
     * Useful for displaying scores without using a score int value.
     *
     * @param entryName  The name of the entry.
     * @param entryValue The "value" of the entry, which is displayed next to
     *                   the name.
     * @return A String that can be displayed on a Scoreboard
     */
    public static String autoSize(String entryName, Object entryValue) {
        int valLength = (" " + entryValue).length();
        if (maxLength - valLength < 0)
            return entryValue.toString();
        entryName = entryName.substring(0, Math.min(maxLength - valLength, entryName.length()));
        return entryName + " " + entryValue;
    }

    /**
     * Uses the fallbacks or truncates the given entry name instead of
     * truncating the entry value. Useful for displaying scores without using a
     * score int value.
     *
     * @param entryName  The name of the entry
     * @param entryValue The "value" of the entry, which is displayed next to
     *                   the name.
     * @param fallbacks  Substitutes for the entryName that will be used instead
     *                   of directly cutting short the entryName.
     * @return A String that can be displayed on a Scoreboard
     */
    public static String autoSize(String entryName, Object entryValue, String... fallbacks) {
        if ((entryName + " " + entryValue).length() > maxLength)
            for (String newName : fallbacks)
                if ((newName + " " + entryValue).length() <= maxLength)
                    return newName + " " + entryValue;
        return autoSize(entryName, entryValue);
    }

    /**
     * Adds all the given entries to the given Objective in order, where the
     * first argument is the top of the Scoreboard and the last argument is the
     * bottom of the Scoreboard.
     */
    public static void addScores(Objective objective, String... entries) {
        addScores(objective, new ArrayList<>(Arrays.asList(entries)));
    }

    /**
     * Adds all the given entries to the given Objective in order, where the
     * first argument is the top of the Scoreboard and the last argument is the
     * bottom of the Scoreboard.
     */
    public static void addScores(Objective objective, List<String> entries) {
        String lastSpace = "";
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            if (entry.equals(SPACER))
                entry = (lastSpace += " ");
            objective.getScore(entry).setScore(entries.size() - i);
        }
    }

    /**
     * Adds all the given entries to the given board in order, where the
     * first argument is the top of the Scoreboard and the last argument is the
     * bottom of the Scoreboard. If entries are longer than 16 characters, it chops
     * it up into a team to display.
     */
    public static void addLargeScores(Scoreboard board, Objective objective, List<String> entries) {
        for (String entry : entries) {
            if (entry.length() > 16) {
                String prefix = "";
                String entrie = "";
                String suffix = "";
                if (entry.length() <= 32) {
                    // No need for a suffix.
                    prefix = entry.substring(0, 16);
                    entrie = entry.substring(16);
                } else {
                    // Use a suffix.
                    prefix = entry.substring(0, 16);
                    entrie = entry.substring(16, 32);
                    suffix = entry.substring(32);
                }
                if (board.getTeam(prefix) != null)
                    board.getTeam(prefix).unregister();
                Team t = board.registerNewTeam(prefix);
                t.setPrefix(prefix);
                t.setSuffix(suffix);
                // Append a reset character to the entry name
                // If it's a player name, this prevents weird tab behavior
                // There's probably a better solution to this
                t.addEntry((entrie + C.R).transform(s -> s.substring(0, Math.min(s.length(), 16))));
            }

            objective.getScore(entry).setScore(entries.size() - entries.indexOf(entry));
        }
    }

    /**
     * Splits the given value to a new line if necessary, without truncating the
     * name.
     *
     * @param entryName  The name of the entry.
     * @param entryValue The "value" of the entry, which is displayed next to or
     *                   below the name.
     * @return A list of all the lines necessary to display the entry.
     */
    public static List<String> wrap(String entryName, String entryValue) {
        return wrap(entryName, entryValue, false);
    }

    /**
     * Splits the given value to a new line if necessary, without truncating the
     * name.
     *
     * @param entryName  The name of the entry
     * @param entryValue The "value" of the entry, which is displayed next to or
     *                   below the name.
     * @param indent     Whether or not to indent any subsequent wrapped lines that
     *                   follow the entryName.
     * @return A list of all the lines necessary to display the entry.
     */
    public static List<String> wrap(String entryName, String entryValue, boolean indent) {
        if ((entryName + " " + entryValue).length() < maxLength)
            return Arrays.asList(entryName + " " + entryValue);
        ArrayList<String> wrapped = new ArrayList<>();
        wrapped.add(entryName);
        for (String entry : wrap(entryValue))
            if (indent && (" " + entry).length() < maxLength)
                wrapped.add(" " + entry);
            else
                wrapped.add(entry);
        return wrapped;
    }

    /**
     * Convenience method that calls {@link Util#cutStringAtLastWord(int, String)}
     * with a length of 16.
     *
     * @param entry The entry to display.
     * @return A list of all the lines necessary to display the entry.
     */
    public static List<String> wrap(String entry) {
        return ItemUtil.wrapWithColor(entry, maxLength);
    }

    /**
     * Resets any entries in the Scoreboard that are not present in the given scores.
     *
     * @param scoreboard The Scoreboard to reset entries in.
     * @param scores     The entries to keep.
     */
    public static void resetChanged(Scoreboard scoreboard, List<String> scores) {
        for (String entry : scoreboard.getEntries())
            if (!scores.contains(entry))
                scoreboard.resetScores(entry);
    }

    /**
     * Resets any entries in the Objective that are not present in the given scores.
     *
     * @param objective The Objective to reset entries in.
     * @param scores    The entries to keep.
     */
    public static void resetChanged(Objective objective, List<String> scores) {
        for (String entry : getScores(objective))
            if (!scores.contains(entry))
                objective.getScoreboard().resetScores(entry);
    }

    /**
     * @param objective The objective to get the scores for.
     * @return A list of scores that are registered for the objective.
     */
    public static List<String> getScores(Objective objective) {
        List<String> scores = new ArrayList<>();

        for (String s : objective.getScoreboard().getEntries())
            if (objective.getScore(s).isScoreSet())
                scores.add(s);

        return scores;
    }

    /**
     * Resets the score of every entry in the given Scoreboard.
     */
    public static void resetScores(Scoreboard scoreboard) {
        scoreboard.getEntries().forEach(scoreboard::resetScores);
    }

    /**
     * Resets all objectives in the given Scoreboard.
     */
    public static void resetObjectives(Scoreboard scoreboard) {
        scoreboard.getObjectives().forEach(Objective::unregister);
    }

    /**
     * Unregisters all teams in the given Scoreboard.
     */
    public static void resetTeams(Scoreboard scoreboard) {
        scoreboard.getTeams().forEach(Team::unregister);
    }
}
