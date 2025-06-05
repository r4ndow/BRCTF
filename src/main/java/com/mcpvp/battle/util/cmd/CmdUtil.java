package com.mcpvp.battle.util.cmd;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CmdUtil {

    public static List<String> partialMatches(Collection<String> list, String query) {
        return list.stream()
                .map(String::toLowerCase)
                .filter(string -> string.startsWith(query.toLowerCase()))
                .toList();
    }

}
