package com.mcpvp.common.util;

import com.mcpvp.common.chat.C;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

/**
 * Some general Scoreboard utilities to help in making nifty looking
 * Scoreboards.
 *
 * @author NomNuggetNom
 */
public class ScoreboardUtil {

    private static final int MAX_LENGTH = 48;

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
        if (MAX_LENGTH - valLength < 0) {
            return entryValue.toString();
        }
        entryName = entryName.substring(0, Math.min(MAX_LENGTH - valLength, entryName.length()));

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
        if ((entryName + " " + entryValue).length() > MAX_LENGTH) {
            for (String newName : fallbacks) {
                if ((newName + " " + entryValue).length() <= MAX_LENGTH) {
                    return newName + " " + entryValue;
                }
            }
        }

        return autoSize(entryName, entryValue);
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
                String prefix;
                String entrie;
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
                if (board.getTeam(prefix) != null) {
                    board.getTeam(prefix).unregister();
                }
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
     * @param entry The entry to display.
     * @return A list of all the lines necessary to display the entry.
     */
    public static List<String> wrap(String entry) {
        return C.wrapWithColor(entry, MAX_LENGTH);
    }

    /**
     * Resets any entries in the Scoreboard that are not present in the given scores.
     *
     * @param scoreboard The Scoreboard to reset entries in.
     * @param scores     The entries to keep.
     */
    public static void resetChanged(Scoreboard scoreboard, List<String> scores) {
        for (String entry : scoreboard.getEntries()) {
            // The scoreboard entries also contain player names for under-name displays
            // So if it resolves to a player, avoid resetting it
            if (Bukkit.getPlayer(entry) == null && !scores.contains(entry)) {
                scoreboard.resetScores(entry);
            }
        }
    }

}
