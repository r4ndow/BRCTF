package com.mcpvp.battle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mcpvp.battle.flag.display.FlagDisplayChannel;
import com.mcpvp.common.preference.Preference;

import java.util.Set;

public class BattlePreferences {

    public static final Preference<Set<FlagDisplayChannel>> FLAG_DISPLAY =
        Preference.of("flag.display", new TypeReference<>() {});

}
