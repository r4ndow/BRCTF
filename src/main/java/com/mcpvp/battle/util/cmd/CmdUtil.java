package com.mcpvp.battle.util.cmd;

import java.util.Collection;
import java.util.List;

public class CmdUtil {

    public static List<String> partialMatches(Collection<String> list, String query) {
        return list.stream()
                .map(String::toLowerCase)
                .filter(string -> string.startsWith(query.toLowerCase()))
                .toList();
    }

}
