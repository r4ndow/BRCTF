package com.mcpvp.battle.options;

import com.mcpvp.battle.map.BattleMapCategory;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the data of the `config.json` file and needs to match it exactly. It is transformed into a
 * {@link BattleOptions} instance for programmatic use.
 */
@Getter
@Setter
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class BattleOptionsInput {

    /**
     * The layout version. Can be used to do migrations.
     */
    @Builder.Default
    private int version = 1;
    @Builder.Default
    private boolean testMode = false;
    //	@Builder.Default
//	private FlagType flagType = FlagType.WOOL;
    @Builder.Default
    private String defaultKit = "heavy";
    @Builder.Default
    private boolean weather = false;
    @Builder.Default
    private Integer ninjaCapLimit = null;
    @Builder.Default
    private boolean ghostCapping = true;
    @Builder.Default
    private boolean overtime = false;
    @Builder.Default
    private final MapTesterOptions mapTester = MapTesterOptions.builder().build();
    @Builder.Default
    private final MapOptions maps = MapOptions.builder().build();
    @Builder.Default
    private final MatchOptions match = MatchOptions.builder().build();
    @Builder.Default
    private final GameOptions game = GameOptions.builder().build();

    @Data
    @Builder
    @Jacksonized
    public static class MatchOptions {

        @Builder.Default
        private final int games = 3;
        @Builder.Default
        private final boolean allowUnevenSwitch = true;

    }

    @Data
    @Builder
    @Jacksonized
    public static class GameOptions {

        @Builder.Default
        private int secondsBeforeGame = 30;
        @Builder.Default
        private int secondsAfterGame = 15;
        @Builder.Default
        private int minimumPlayerCount = 4;

    }

    @Data
    @Builder
    @Jacksonized
    public static class MapOptions {

        @Builder.Default
        private final String dir = "plugins/mcctf/maps";
        @Builder.Default
        private final String mapsJson = "plugins/mcctf/maps.json";
        @Builder.Default
        private final Map<BattleMapCategory, Boolean> categories = new LinkedHashMap<>() {
            {
                for (int i = 0; i <= BattleMapCategory.WAVE20.ordinal(); i++) {
                    put(BattleMapCategory.values()[i], true);
                }
                for (int i = BattleMapCategory.WAVE20.ordinal() + 1; i < BattleMapCategory.values().length; i++) {
                    put(BattleMapCategory.values()[i], false);
                }
                put(BattleMapCategory.DEFAULT, false);
                put(BattleMapCategory.ORIGINAL, true);
                put(BattleMapCategory.VAULT, true);
                put(BattleMapCategory.VARIATION, true);
            }
        };
        @Builder.Default
        private final List<Integer> disable = new ArrayList<>();

    }

    @Data
    @Builder
    @Jacksonized
    public static class MapTesterOptions {

        @Builder.Default
        private final boolean enabled = false;
        @Builder.Default
        private final String outputDir = "plugins/mcctf/maps_testing";
        @Builder.Default
        private final int runId = 1;

    }

}
