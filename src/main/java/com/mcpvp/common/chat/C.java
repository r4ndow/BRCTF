package com.mcpvp.common.chat;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A collection of chat codes.
 *
 * @author NomNuggetNom
 */
@SuppressWarnings("unused")
public class C {

    public static final String BLACK = ChatColor.BLACK.toString();
    public static final String DBLUE = ChatColor.DARK_BLUE.toString();
    public static final String DARK_BLUE = DBLUE;
    public static final String DGREEN = ChatColor.DARK_GREEN.toString();
    public static final String DARK_GREEN = DGREEN;
    public static final String DAQUA = ChatColor.DARK_AQUA.toString();
    public static final String DARK_AQUA = DAQUA;
    public static final String DRED = ChatColor.DARK_RED.toString();
    public static final String DARK_RED = DRED;
    public static final String DPURPLE = ChatColor.DARK_PURPLE.toString();
    public static final String DARK_PURPLE = DPURPLE;
    public static final String GOLD = ChatColor.GOLD.toString();
    public static final String GRAY = ChatColor.GRAY.toString();
    public static final String DGRAY = ChatColor.DARK_GRAY.toString();
    public static final String DARK_GRAY = DGRAY;
    public static final String BLUE = ChatColor.BLUE.toString();
    public static final String GREEN = ChatColor.GREEN.toString();
    public static final String AQUA = ChatColor.AQUA.toString();
    public static final String RED = ChatColor.RED.toString();
    public static final String LPURPLE = ChatColor.LIGHT_PURPLE.toString();
    public static final String LIGHT_PURPLE = LPURPLE;
    public static final String YELLOW = ChatColor.YELLOW.toString();
    public static final String WHITE = ChatColor.WHITE.toString();
    public static final String MAGIC = ChatColor.MAGIC.toString();
    public static final String PURPLE = LPURPLE;
    public static final String CYAN = AQUA;
    public static final String ORANGE = GOLD;
    public static final String R = ChatColor.RESET.toString();
    public static final String RESET = R;
    public static final String B = ChatColor.BOLD.toString();
    public static final String BOLD = B;
    public static final String S = ChatColor.STRIKETHROUGH.toString();
    public static final String STRIKETHROUGH = S;
    public static final String U = ChatColor.UNDERLINE.toString();
    public static final String UNDERLINE = U;
    public static final String I = ChatColor.ITALIC.toString();
    public static final String ITALIC = I;

    /**
     * Used for command result feedback.
     */
    private static final String COMMAND_SYMBOL = ">";
    /**
     * Used for general information or "nice to know" things.
     */
    private static final String INFO_SYMBOL = "*";
    /**
     * Used for important information, warnings, or alerts.
     */
    private static final String WARNING_SYMBOL = "!!";

    /**
     * Mixes the given colors, properly ordering them with respect to the
     * behavior of coloring and formatting codes.
     *
     * @param codes The codes to order.
     * @return The ordered color codes, combined into a single String.
     */
    public static String mix(String... codes) {
        Set<ChatColor> colors = Stream.of(codes)
            .map(ChatColor::getLastColors)
            .map(s -> s.substring(1))
            .map(ChatColor::getByChar)
            .collect(Collectors.toSet());
        StringBuilder sb = new StringBuilder();

        // Add colors first.
        colors.stream().filter(ChatColor::isColor).forEach(sb::append);
        // Then formatters.
        colors.stream().filter(ChatColor::isFormat).forEach(sb::append);

        return sb.toString();
    }

    /**
     * Bolds the given color.
     *
     * @param color The color to bold.
     * @return The given color with bold appended.
     */
    public static String b(String color) {
        return color + C.B;
    }

    /**
     * Strikes the given color.
     *
     * @param color The color to strike.
     * @return The given color with strike appended.
     */
    public static String s(String color) {
        return color + C.S;
    }

    /**
     * Underlines the given color.
     *
     * @param color The color to underline.
     * @return The given color with underline appended.
     */
    public static String u(String color) {
        return color + C.U;
    }

    /**
     * Italicizes the given color.
     *
     * @param color The color to italicize.
     * @return The given color with italics appended.
     */
    public static String i(String color) {
        return color + C.I;
    }

    /**
     * @return The command symbol in the given color.
     */
    public static String cmd(String color) {
        return color + B + COMMAND_SYMBOL + R + GRAY + " ";
    }

    /**
     * @return The String used to symbolize that a command was successful.
     */
    public static String cmdSuccess() {
        return cmd(GREEN);
    }

    /**
     * Alias of {@link #cmdSuccess()}.
     *
     * @return The String used to symbolize that a command was successful.
     */
    public static String cmdPass() {
        return cmdSuccess();
    }

    /**
     * @return The String used to symbolize that a command failed.
     */
    public static String cmdFail() {
        return cmd(RED);
    }

    /**
     * @param color The color to make the symbol.
     * @return The info symbol in the given color.
     */
    public static String info(String color) {
        return color + B + INFO_SYMBOL + R + GRAY + " ";
    }

    /**
     * @param color The color to make the symbol.
     * @return The warning symbol in the given color.
     */
    public static String warn(String color) {
        return color + B + WARNING_SYMBOL + R + GRAY + " ";
    }

    /**
     * @param text The text to highlight.
     * @return A white version of the given text, followed by the GRAY color
     * code. Useful for highlighting in GRAY messages.
     */
    public static String highlight(Object text) {
        return WHITE + text + GRAY;
    }

    /**
     * @param text  The text to highlight.
     * @param color The color to highlight the text.
     * @return A colored version of the given text, followed by the GRAY color
     * code. Useful for highlighting in GRAY messages.
     */
    public static String highlight(Object text, String color) {
        return color + text + GRAY;
    }

    /**
     * @param text  The text to highlight.
     * @param color The color to highlight the text.
     * @return A colored version of the given text, followed by the GRAY color
     * code. Useful for highlighting in GRAY messages.
     */
    public static String hl(Object text, String color) {
        return color + text + GRAY;
    }

    /**
     * @param text The text to highlight.
     * @return A colored version of the given text, followed by the GRAY color
     * code. Useful for highlighting in GRAY messages.
     */
    public static String hl(String text) {
        return highlight(text);
    }

    /**
     * @param text The text to style as a link.
     * @return An underlined, aqua version of the text.
     */
    public static String link(String text) {
        return link(text, AQUA);
    }

    /**
     * @param text  The text to style as a link (underline).
     * @param color The color to make the text. Aqua by default.
     * @return The stylized text.
     */
    public static String link(String text, String color) {
        return color + C.U + text + GRAY;
    }

    /**
     * @param color  The color of the HR.
     * @param length The length of the HR.
     * @return A horizontal rule of the given color that is the given length.
     */
    public static String hr(String color, int length) {
        return color + S + StringUtils.repeat(" ", length) + R;
    }

    /**
     * @param text The text you want to remove color from
     * @return A colorless text
     */
    public static String strip(String text) {
        return ChatColor.stripColor(text);
    }

    /**
     * Shortened version of {@link ChatColor#translateAlternateColorCodes(char, String)}.
     *
     * @param alternate - the alternate color code character to replace the original.
     * @param message   - the message containing the alternate code character to be swapped.
     * @return the message with all alternate characters swapped for the original.
     */
    public static String translate(char alternate, String message) {
        return ChatColor.translateAlternateColorCodes(alternate, message);
    }

    /**
     * Shortened version of {@link ChatColor#translateAlternateColorCodes(char, String)},
     * using alternate code '&' as default
     *
     * @param message - the message containing the alternate code character to be swapped.
     * @return the message with all alternate characters swapped for the original.
     */
    public static String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Checks if is if this code is a format code as opposed to a color code.
     *
     * @return if this code is a format code as opposed to a color code
     */
    public static boolean isFormatting(String str) {
        return str.equals(R) || str.equals(B) || str.equals(S) || str.equals(U) || str.equals(I);
    }

    /**
     * Wraps the given String, but avoids cutting color characters off.
     *
     * @param string     The String to wrap.
     * @param lineLength The length of each line.
     * @return A list of wrapped text.
     */
    public static List<String> wrapWithColor(String string, int lineLength) {
        int length = translateLength(string, lineLength);
        List<String> lines;
        if (length == string.length()) {
            lines = new ArrayList<>();
            lines.add(string);
        } else {
            int lastSpace = string.lastIndexOf(' ', length);
            length = lastSpace == -1 ? length : lastSpace + 1;
            String line = string.substring(0, length).trim();
            lines = wrapWithColor(ChatColor.getLastColors(line) + string.substring(length).trim(), lineLength);
            lines.add(0, line);
        }
        return lines;
    }

    /**
     * @deprecated Unknown what the purpose of this is. It would be just as easy
     * to strip color codes and find the length (?)
     */
    @Deprecated
    public static int translateLength(String string, int length) {
        int nonColorCharCount = 0;
        boolean previousWasColorChar = false;
        for (int i = 0; i < string.length(); i++) {
            if (previousWasColorChar) {
                previousWasColorChar = false;
            } else if (string.charAt(i) == ChatColor.COLOR_CHAR) {
                previousWasColorChar = true;
            } else {
                nonColorCharCount++;
                if (nonColorCharCount == length) {
                    return i + 1;
                }
            }
        }
        return string.length();
    }
}
