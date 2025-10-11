package com.mcpvp.battle.map.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.options.BattleOptionsInput;

import java.io.File;
import java.util.List;

/**
 * Determines maps that are available to be played.
 */
public interface BattleMapSource {

    static BattleMapSource from(
        ObjectMapper objectMapper,
        BattleOptionsInput.MapSourceOptions source
    ) {
        if (source instanceof BattleOptionsInput.CentralMapSourceOptions cms) {
            return new CentralMapSource(objectMapper, cms);
        } else if (source instanceof BattleOptionsInput.CustomMapSourceOptions cms) {
            return new CustomMapSource(objectMapper, cms);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Performs any required setup. Called on plugin enable.
     */
    void init();

    /**
     * Retrieves all known maps, some of which may not be functioning properly.
     *
     * @return All known maps.
     */
    List<BattleMapData> getAll();

    /**
     * Retrieves the relevant directory with map data for the given map.
     *
     * @param map The map to get a file from.
     * @return The directory containing the world data for the map.
     */
    File getWorldData(BattleMapData map);

    /**
     * Retrieves a list of maps known to be functioning properly.
     *
     * @return All functioning maps.
     */
    default List<BattleMapData> getFunctional() {
        return this.getAll().stream().filter(BattleMapData::isFunctional).toList();
    }

}
