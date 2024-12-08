package com.mcpvp.battle.map.repo;

import com.mcpvp.battle.map.BattleMapData;

import java.util.List;

/**
 * Determines maps that are available to be played.
 */
public interface MapRepo {
	
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
	 * Retrieves a list of maps known to be functioning properly.
	 *
	 * @return All functioning maps.
	 */
	List<BattleMapData> getFunctional();
	
	/**
	 * Retrieves a list of maps which are enabled.
	 *
	 * @return All enabled and functional maps.
	 */
	List<BattleMapData> getEnabled();
	
}
