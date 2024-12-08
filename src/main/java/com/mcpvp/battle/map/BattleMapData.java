package com.mcpvp.battle.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@Jacksonized
@AllArgsConstructor
public class BattleMapData {
	
	/**
	 * Sequential, unique ID of the map
	 */
	private final int id;
	
	/**
	 * Human friendly name
	 */
	private final String name;
	
	/**
	 * Name of the author
	 */
	private final String author;
	
	/**
	 * Date the map was created, mostly for posterity
	 */
	private final LocalDateTime created;
	
	/**
	 * If the map has been successfully tested and seems to be functioning properly.
	 * This will be set to false if the map file is not found.
	 */
	private boolean functional;
	
	/**
	 * The name of the file that contains the map file data. This is relative to the
	 * map directory in the config.
	 */
	private final String file;
	
	/**
	 * Old-school metadata for specifying flags and such.
	 */
	private final String metadata;
	
	/**
	 * The category or map wave.
	 */
	@Builder.Default
	BattleMapCategory category = BattleMapCategory.DEFAULT;
	
	/**
	 * Any errors detected on previous attempts of map loads.
	 */
	@Builder.Default
	List<String> errors = new ArrayList<>();
	
}
