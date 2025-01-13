package com.mcpvp.battle.util;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Bridges the gap between DyeColors and ChatColors as best as possible.
 *
 * @author NomNuggetNom
 */
public enum Colors {
    BLACK(DyeColor.BLACK, ChatColor.BLACK, C.BLACK),
    DARK_BLUE(DyeColor.BLUE, ChatColor.DARK_BLUE, C.DARK_BLUE),
    DARK_GREEN(DyeColor.GREEN, ChatColor.DARK_GREEN, C.DARK_GREEN),
    DARK_AQUA(DyeColor.CYAN, ChatColor.DARK_AQUA, C.DARK_AQUA),
    DARK_RED(DyeColor.RED, ChatColor.DARK_RED, C.DARK_RED),
    DARK_PURPLE(DyeColor.MAGENTA, ChatColor.DARK_PURPLE, C.DARK_PURPLE),
    GOLD(DyeColor.ORANGE, ChatColor.GOLD, C.GOLD),
    GRAY(getGray(), ChatColor.GRAY, C.GRAY),
    DARK_GRAY(DyeColor.GRAY, ChatColor.DARK_GRAY, C.DARK_GRAY),
    BLUE(DyeColor.BLUE, ChatColor.BLUE, C.BLUE),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE, ChatColor.AQUA, C.AQUA),
    GREEN(DyeColor.LIME, ChatColor.GREEN, C.GREEN),
    AQUA(DyeColor.CYAN, ChatColor.AQUA, C.AQUA),
    RED(DyeColor.RED, ChatColor.RED, C.RED),
    PINK(DyeColor.PINK, ChatColor.LIGHT_PURPLE, C.LIGHT_PURPLE),
    LIGHT_PURPLE(DyeColor.PURPLE, ChatColor.LIGHT_PURPLE, C.LIGHT_PURPLE),
    PURPLE(DyeColor.PURPLE, ChatColor.DARK_PURPLE, C.DARK_PURPLE),
    YELLOW(DyeColor.YELLOW, ChatColor.YELLOW, C.YELLOW),
    BROWN(DyeColor.BROWN, ChatColor.BLACK, C.BLACK),
    WHITE(DyeColor.WHITE, ChatColor.WHITE, C.WHITE);

    public static final Colors[] VALUES = values();

    public final Color COLOR;
    public final DyeColor DYE;
    public final ChatColor CHAT;
    public final String CHAT_STRING;

    Colors(DyeColor dye, ChatColor chat, String string) {
        COLOR = dye.getColor();
        DYE = dye;
        CHAT = chat;
        CHAT_STRING = string;
    }

    public static Colors find(Color col) {
        return match(col, c -> c.COLOR);
    }

    public static Colors find(DyeColor col) {
        return match(col, c -> c.DYE);
    }

    public static Colors find(ChatColor col) {
        return match(col, c -> c.CHAT);
    }

    public static Colors find(String col) {
        return match(col, c -> c.CHAT_STRING);
    }

    private static <T> Colors match(T object, Function<Colors, T> func) {
        return Stream.of(Colors.values()).filter(c -> func.apply(c).equals(object)).findFirst().orElse(null);
    }

    private static DyeColor getGray() {
        for (DyeColor dc : DyeColor.values()) {
            if (dc.name().equalsIgnoreCase("gray") || dc.name().equalsIgnoreCase("silver"))
                return dc;
        }
        return null;
    }

    @Override
    public String toString() {
        return CHAT.toString();
    }

}
