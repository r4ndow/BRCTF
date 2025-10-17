package com.mcpvp.battle.options;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
    private final MatchOptions match = MatchOptions.builder().build();
    @Builder.Default
    private final GameOptions game = GameOptions.builder().build();
    @Builder.Default
    private final MapOptions maps = MapOptions.builder().build();
    @Builder.Default
    private final MapTesterOptions mapTester = MapTesterOptions.builder().build();

    @Data
    @Builder
    @Jacksonized
    public static class MatchOptions {

        @Builder.Default
        private final int games = 3;

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
        private final FlagType flagType = FlagType.WOOL;

    }

    @Data
    @Builder
    @Jacksonized
    public static class MapOptions {

        @Builder.Default
        private final List<MapSourceOptions> sources = List.of(CentralMapSourceOptions.builder().build());
        @Builder.Default
        private final Map<BattleMapCategory, Boolean> categories = new LinkedHashMap<>() {
            {
                for (int i = 0; i <= BattleMapCategory.WAVE20.ordinal(); i++) {
                    this.put(BattleMapCategory.values()[i], true);
                }
                for (int i = BattleMapCategory.WAVE20.ordinal() + 1; i < BattleMapCategory.values().length; i++) {
                    this.put(BattleMapCategory.values()[i], false);
                }
                this.put(BattleMapCategory.DEFAULT, false);
                this.put(BattleMapCategory.ORIGINAL, true);
                this.put(BattleMapCategory.VAULT, true);
                this.put(BattleMapCategory.VARIATION, true);
            }
        };
        @Builder.Default
        private final List<Integer> disable = new ArrayList<>();

    }

    @JsonSubTypes(value = {
        @JsonSubTypes.Type(value = CentralMapSourceOptions.class, name = "central"),
        @JsonSubTypes.Type(value = CustomMapSourceOptions.class, name = "custom")
    })
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    public sealed interface MapSourceOptions permits CentralMapSourceOptions, CustomMapSourceOptions {

    }

    @Data
    @Builder
    @Jacksonized
    public static final class CentralMapSourceOptions implements MapSourceOptions {

        @Builder.Default
        private final String dir = "plugins/mcctf/maps";
        @Builder.Default
        private final String json = "plugins/mcctf/maps.json";

    }

    @Data
    @Builder
    @Jacksonized
    public static final class CustomMapSourceOptions implements MapSourceOptions {

        private final String dir;

    }

    @Data
    @Builder
    @Jacksonized
    public static class MapTesterOptions {

        @Builder.Default
        private final boolean enabled = false;
        @Builder.Default
        private final MapSourceOptions mapSource = CentralMapSourceOptions.builder().build();
        @Builder.Default
        private final String outputDir = "plugins/mcctf/maps_testing";
        @Builder.Default
        private final int runId = 1;

    }

    public enum FlagType {
        WOOL, BANNER
    }

}
