package com.mcpvp.battle.map.manager;

import com.mcpvp.battle.map.BattleMapData;

import java.io.File;
import java.util.List;

/**
 * Picks maps based on what's available in {@link com.mcpvp.battle.map.repo.MapRepo}.
 */
public interface MapManager {

    /**
     * Retrieves a list of maps which are enabled.
     *
     * @return All enabled and functional maps.
     */
    List<BattleMapData> getEnabled();

    /**
     * Checks if the given ID is a known map ID.
     *
     * @param id The ID to check.
     * @return True if the ID is a known map.
     */
    boolean isMap(int id);

    /**
     * Produces a list of map IDs to be played. Takes into account hardcoded
     * map preferences as well as (eventually) map sets.
     *
     * @param games The number of games to get maps for.
     * @return A list of IDs to play.
     */
    List<Integer> pickMaps(int games);

    /**
     * Load data for a specific map ID.
     *
     * @param id The ID of the map to load.
     * @return The loaded data.
     */
    BattleMapData loadMap(int id);

    /**
     * Loads results from {@link #pickMaps(int)}.
     *
     * @param games The number of games to play.
     * @return A list of loaded maps.
     */
    List<BattleMapData> loadMaps(int games);

    /**
     * Locates the folder that contains the Minecraft world data for the given map ID.
     *
     * @param map The map.
     * @return The folder containing the Minecraft world data.
     */
    File getWorldData(BattleMapData map);

    /**
     * Specifies a list of IDs which should be played next time the server is booted.
     *
     * @param ids The IDs to play next match.
     */
    void setOverride(List<Integer> ids);

    /**
     * Ditches any existing IDs which should be played next time the server is booted.
     */
    void clearOverride();

    /**
     * @return A list of map data which has been manually specified to be played. An empty list is returned if
     * no such maps exist.
     */
    List<BattleMapData> getOverride();

}
